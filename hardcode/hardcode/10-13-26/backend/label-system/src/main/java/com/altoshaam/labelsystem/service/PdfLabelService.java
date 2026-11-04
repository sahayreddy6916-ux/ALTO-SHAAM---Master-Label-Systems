package com.altoshaam.labelsystem.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.util.Matrix;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.altoshaam.labelsystem.entity.AppSetting;
import com.altoshaam.labelsystem.entity.GeneratedLabel;
import com.altoshaam.labelsystem.entity.LabelItem;
import com.altoshaam.labelsystem.exception.ServiceBusyException;

/**
 * Renders Alto-Shaam B-10 Master Labels.
 *
 * Each label is exactly 6 x 4 inches (432 x 288 pt, no scaling). Two labels are
 * placed on one A4 portrait sheet, one above the other. Barcodes are drawn as
 * vector bars (Code 128), so they stay sharp and the PDF stays small.
 *
 * Load protection: at most {@code label.pdf.max-concurrent} PDFs are rendered at
 * the same time; extra callers wait and then receive a 503 if still busy.
 */
@Service
public class PdfLabelService {

    // ---------------------------------------------------------------- geometry
    private static final float PAGE_WIDTH = 432f; // 6 inches
    private static final float PAGE_HEIGHT = 288f; // 4 inches

    private static final float SHEET_WIDTH = PDRectangle.A4.getWidth();
    private static final float SHEET_HEIGHT = PDRectangle.A4.getHeight();
    private static final float SHEET_MARGIN = 72f; // 1 inch top and bottom

    private static final float LABEL_X = (SHEET_WIDTH - PAGE_WIDTH) / 2f;
    private static final float BOTTOM_LABEL_Y = SHEET_MARGIN;
    private static final float TOP_LABEL_Y = SHEET_HEIGHT - SHEET_MARGIN - PAGE_HEIGHT;

    private static final float ROW_HEIGHT = 72f;
    private static final float BORDER_WIDTH = 0.8f;
    private static final float BARCODE_HEIGHT = 37f; // about 0.5 inch
    private static final float QUIET_ZONE = 18f; // 0.25 inch
    private static final float PAGE_PADDING = 2f;

    // Read-only fonts used only to measure text (thread safe, never added to a PDF).
    private static final PDType1Font MEASURE_REGULAR = measureFont(Standard14Fonts.FontName.HELVETICA);
    private static final PDType1Font MEASURE_BOLD = measureFont(Standard14Fonts.FontName.HELVETICA_BOLD);

    private static PDType1Font measureFont(Standard14Fonts.FontName name) {
        try {
            return new PDType1Font(name);
        } catch (Exception ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    private final BarcodeService barcodeService;
    private final Semaphore renderPermits;
    private final long acquireTimeoutSeconds;

    public PdfLabelService(
            BarcodeService barcodeService,
            @Value("${label.pdf.max-concurrent:4}") int maxConcurrent,
            @Value("${label.pdf.acquire-timeout-seconds:30}") long acquireTimeoutSeconds) {
        this.barcodeService = barcodeService;
        this.renderPermits = new Semaphore(Math.max(1, maxConcurrent), true);
        this.acquireTimeoutSeconds = Math.max(1, acquireTimeoutSeconds);
    }

    /** Per-page drawing context: one content stream plus the fonts of this document. */
    private record Ctx(PDPageContentStream cs, PDType1Font regular, PDType1Font bold) {
    }

    // ------------------------------------------------------------ public API

    public byte[] generatePdf(List<GeneratedLabel> labels, AppSetting settings) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream(64 * 1024)) {
            render(labels, settings, out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to generate Master Label PDF", ex);
        }
    }

    /** Streams the PDF to a temp file and atomically moves it into place. */
    public void generatePdfToFile(List<GeneratedLabel> labels, AppSetting settings, Path target) {
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        try {
            if (target.getParent() != null) {
                Files.createDirectories(target.getParent());
            }
            try (OutputStream out = Files.newOutputStream(tmp)) {
                render(labels, settings, out);
            }
            try {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to store generated PDF", ex);
        } finally {
            try {
                Files.deleteIfExists(tmp);
            } catch (IOException ignored) {
                // nothing more to do
            }
        }
    }

    public byte[] readPdf(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (IOException ex) {
            throw new IllegalStateException("Generated PDF is not available: " + path, ex);
        }
    }

    // ------------------------------------------------------------- rendering

    private void render(List<GeneratedLabel> labels, AppSetting settings, OutputStream out) throws IOException {
        if (labels == null || labels.isEmpty()) {
            throw new IllegalArgumentException("No generated labels were supplied.");
        }

        boolean acquired;
        try {
            acquired = renderPermits.tryAcquire(acquireTimeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ServiceBusyException("Interrupted while waiting to generate labels. Please retry.");
        }
        if (!acquired) {
            throw new ServiceBusyException("The server is busy generating labels. Please retry in a moment.");
        }

        try (PDDocument document = new PDDocument()) {
            PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

            GeneratedLabel pendingLabel = null;
            for (GeneratedLabel label : labels) {
                if (label == null) {
                    continue;
                }
                if (label.getLabelItem() == null) {
                    throw new IllegalStateException("Generated label does not have a LabelItem.");
                }
                int copies = Math.max(1, label.getCopyCount());
                for (int copy = 0; copy < copies; copy++) {
                    if (pendingLabel == null) {
                        pendingLabel = label;
                    } else {
                        renderSheet(document, regular, bold, pendingLabel, label, settings);
                        pendingLabel = null;
                    }
                }
            }
            if (pendingLabel != null) {
                renderSheet(document, regular, bold, pendingLabel, null, settings);
            }
            document.save(out);
        } finally {
            renderPermits.release();
        }
    }

    /** One A4 portrait page holding up to two 6 x 4 inch labels. */
    private void renderSheet(
            PDDocument document,
            PDType1Font regular,
            PDType1Font bold,
            GeneratedLabel top,
            GeneratedLabel bottom,
            AppSetting settings) throws IOException {

        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
            Ctx ctx = new Ctx(cs, regular, bold);
            if (top != null) {
                renderLabel(ctx, top, settings, LABEL_X, TOP_LABEL_Y);
            }
            if (bottom != null) {
                renderLabel(ctx, bottom, settings, LABEL_X, BOTTOM_LABEL_Y);
            }
        }
    }

    private void renderLabel(Ctx c, GeneratedLabel label, AppSetting settings, float x, float y) throws IOException {
        LabelItem item = label.getLabelItem();
        PDPageContentStream cs = c.cs();

        cs.saveGraphicsState();
        cs.transform(Matrix.getTranslateInstance(x, y));

        // Outer border
        drawRectangle(cs, 1f, 1f, PAGE_WIDTH - 2f, PAGE_HEIGHT - 2f);

        // Row 1: FROM | TO | PACKING LIST
        float row1Y = PAGE_HEIGHT - ROW_HEIGHT;
        float fromWidth = 175f;
        float toWidth = 110f;
        float packingWidth = PAGE_WIDTH - fromWidth - toWidth;
        drawFrom(c, settings, PAGE_PADDING, row1Y, fromWidth, ROW_HEIGHT);
        drawVerticalLine(cs, fromWidth, row1Y, ROW_HEIGHT);
        drawTo(c, settings, fromWidth, row1Y, toWidth, ROW_HEIGHT);
        drawVerticalLine(cs, fromWidth + toWidth, row1Y, ROW_HEIGHT);
        drawPackingList(c, item, fromWidth + toWidth, row1Y, packingWidth, ROW_HEIGHT);
        drawHorizontalLine(cs, 0f, row1Y, PAGE_WIDTH);

        // Row 2: PART NO. | MASTER LABEL / REV / DESCRIPTION
        float row2Y = PAGE_HEIGHT - (ROW_HEIGHT * 2f);
        float partWidth = 200f;
        drawPartNumber(c, item, 0f, row2Y, partWidth, ROW_HEIGHT);
        drawVerticalLine(cs, partWidth, row2Y, ROW_HEIGHT);
        drawMasterRevisionDescription(c, item, partWidth, row2Y, PAGE_WIDTH - partWidth, ROW_HEIGHT);
        drawHorizontalLine(cs, 0f, row2Y, PAGE_WIDTH);

        // Row 3: QUANTITY | PURCHASE ORDER
        float row3Y = PAGE_HEIGHT - (ROW_HEIGHT * 3f);
        float quantityWidth = 149f;
        drawQuantity(c, item, 0f, row3Y, quantityWidth, ROW_HEIGHT);
        drawVerticalLine(cs, quantityWidth, row3Y, ROW_HEIGHT);
        drawPurchaseOrder(c, item, quantityWidth, row3Y, PAGE_WIDTH - quantityWidth, ROW_HEIGHT);
        drawHorizontalLine(cs, 0f, row3Y, PAGE_WIDTH);

        // Row 4: SERIAL NO.
        drawSerialNumber(c, label, 0f, 0f, PAGE_WIDTH, ROW_HEIGHT);

        cs.restoreGraphicsState();
    }

    // ---------------------------------------------------------------- blocks

    private void drawFrom(Ctx c, AppSetting settings, float x, float y, float width, float height)
            throws IOException {
        drawText(c, "FROM:", x + 5f, y + height - 10f, 7f, true);

        String shipFrom = settings == null ? "" : safe(settings.getShipFrom());
        String supplierId = settings == null ? "" : safe(settings.getSupplierId());

        drawAddress(c, addressLines(shipFrom, width - 10f, 4), x + 5f, y + height - 21f);

        if (!supplierId.isBlank()) {
            drawText(c, "SUPPLIER #" + supplierId, x + 5f, y + 7f, 7.5f, true);
        }
    }

    private void drawTo(Ctx c, AppSetting settings, float x, float y, float width, float height)
            throws IOException {
        drawText(c, "TO:", x + 5f, y + height - 10f, 7f, true);
        String shipTo = settings == null ? "" : safe(settings.getShipTo());
        drawAddress(c, addressLines(shipTo, width - 10f, 4), x + 5f, y + height - 21f);
    }

    private void drawAddress(Ctx c, List<String> lines, float x, float firstLineY) throws IOException {
        float currentY = firstLineY;
        for (int i = 0; i < lines.size(); i++) {
            drawText(c, lines.get(i), x, currentY, 7.2f, i == 0);
            currentY -= 9f;
        }
    }

    private void drawPackingList(Ctx c, LabelItem item, float x, float y, float width, float height)
            throws IOException {
        drawText(c, "PACKING LIST #", x + 5f, y + height - 9f, 6.8f, true);
        drawText(c, "(11K)", x + 31f, y + height - 18f, 5.5f, true);

        String packingList = safe(item.getPackingListNumber()).trim();
        String displayValue = fitTextToWidth(packingList, width - 75f, 15f, true);
        float valueWidth = textWidth(displayValue, true, 15f);
        drawText(c, displayValue, x + width - valueWidth - 30f, y + height - 18f, 15f, true);

        if (!packingList.isBlank()) {
            drawBarcode(c, "11K" + packingList, x + 28f, y + 7f, width - 38f, BARCODE_HEIGHT);
        }
    }

    private void drawPartNumber(Ctx c, LabelItem item, float x, float y, float width, float height)
            throws IOException {
        drawText(c, "PART NO.", x + 5f, y + height - 10f, 7f, true);
        drawText(c, "(P)", x + 6f, y + height - 19f, 5.5f, true);

        String partNumber = safe(item.getPartNumber()).trim();
        drawText(c, fitTextToWidth(partNumber, 170f, 17f, true), x + 43f, y + height - 19f, 17f, true);

        if (!partNumber.isBlank()) {
            drawBarcode(c, "P" + partNumber, x + 6f, y + 7f,
                    Math.min(width * 0.66f, width - 30f), BARCODE_HEIGHT);
        }
    }

    private void drawMasterRevisionDescription(Ctx c, LabelItem item, float x, float y, float width, float height)
            throws IOException {
        drawText(c, "MASTER LABEL", x + 6f, y + height - 18f, 10f, false);
        drawText(c, "REV. LEVEL", x + width - 90f, y + height - 18f, 5.5f, true);

        String revision = safe(item.getRevisionLevel()).trim();
        drawText(c, fitTextToWidth(revision, 38f, 7f, false), x + width - 43f, y + height - 29f, 7f, false);

        drawText(c, "PART DESC.", x + 6f, y + 34f, 6f, true);

        // Wrap the description into two lines using real font metrics.
        String description = safe(item.getDescription()).trim();
        String line1 = "";
        String line2 = "";
        for (String word : description.split("\\s+")) {
            String candidate = line1.isEmpty() ? word : line1 + " " + word;
            if (textWidth(candidate, false, 7f) <= width - 14f) {
                line1 = candidate;
            } else {
                line2 = line2.isEmpty() ? word : line2 + " " + word;
            }
        }
        drawText(c, fitTextToWidth(line1, width - 14f, 7f, false), x + 6f, y + 20f, 7f, false);
        drawText(c, fitTextToWidth(line2, width - 14f, 7f, false), x + 6f, y + 9f, 7f, false);
    }

    private void drawQuantity(Ctx c, LabelItem item, float x, float y, float width, float height)
            throws IOException {
        drawText(c, "QUANTITY", x + 6f, y + height - 10f, 7f, true);
        drawText(c, "(Q)", x + 8f, y + height - 19f, 5.5f, true);

        String quantity = String.valueOf(item.getPerPalletQuantity());
        drawText(c, fitText(quantity, 5), x + 49f, y + height - 20f, 17f, true);
        drawBarcode(c, "Q" + quantity, x + 6f, y + 7f, width - 12f, BARCODE_HEIGHT);
    }

    private void drawPurchaseOrder(Ctx c, LabelItem item, float x, float y, float width, float height)
            throws IOException {
        String po = safe(item.getCustomerPoNumber()).trim();
        if (!po.isBlank()) {
            drawBarcode(c, "K" + po, x + 42f, y + 30f, width - 62f, BARCODE_HEIGHT);
        }
        drawText(c, "PURCHASE ORDER #", x + 55f, y + 17f, 6f, true);
        drawText(c, "(K)", x + 83f, y + 8f, 5.2f, true);
        drawText(c, fitTextToWidth(po, width - 125f, 14f, true), x + width - 140f, y + 9f, 14f, true);
    }

    private void drawSerialNumber(Ctx c, GeneratedLabel label, float x, float y, float width, float height)
            throws IOException {
        drawText(c, "SERIAL NO.", x + 8f, y + height - 10f, 7f, true);
        drawText(c, "(9S)", x + 9f, y + height - 19f, 5.5f, true);

        String serial = safe(label.getSerialNumber()).trim();
        drawText(c, fitTextToWidth(serial, width - 85f, 17f, true), x + 49f, y + height - 19f, 17f, true);

        if (!serial.isBlank()) {
            drawBarcode(c, "9S" + serial, x + QUIET_ZONE, y + 6f, width - (QUIET_ZONE * 2f), BARCODE_HEIGHT);
        }
    }

    // --------------------------------------------------------------- barcode

    /** Draws a Code 128 barcode as vector bars filling the given width. */
    private void drawBarcode(Ctx c, String data, float x, float y, float width, float height)
            throws IOException {
        if (data == null || data.isBlank()) {
            return;
        }
        float safeWidth = Math.max(30f, width);
        boolean[] bits = barcodeService.encodeCode128(data);
        if (bits.length == 0) {
            return;
        }

        float moduleWidth = safeWidth / bits.length;
        PDPageContentStream cs = c.cs();
        int i = 0;
        while (i < bits.length) {
            if (!bits[i]) {
                i++;
                continue;
            }
            int start = i;
            while (i < bits.length && bits[i]) {
                i++;
            }
            cs.addRect(x + start * moduleWidth, y, (i - start) * moduleWidth, height);
        }
        cs.fill();
    }

    // ------------------------------------------------------------ text utils

    private List<String> addressLines(String value, float maxWidth, int maxLines) {
        List<String> result = new ArrayList<>();
        if (value == null || value.isBlank()) {
            return result;
        }

        for (String originalLine : value.replace("\r", "").trim().split("\n", -1)) {
            if (result.size() >= maxLines) {
                break;
            }
            String line = originalLine.trim();
            if (line.isBlank()) {
                continue;
            }
            if (textWidth(line, false, 7.2f) <= maxWidth) {
                result.add(barcodeService.sanitize(line));
                continue;
            }

            String current = "";
            for (String word : line.split("\\s+")) {
                String candidate = current.isBlank() ? word : current + " " + word;
                if (textWidth(candidate, false, 7.2f) <= maxWidth) {
                    current = candidate;
                } else {
                    if (!current.isBlank()) {
                        result.add(barcodeService.sanitize(current));
                    }
                    current = word;
                    if (result.size() >= maxLines) {
                        break;
                    }
                }
            }
            if (result.size() < maxLines && !current.isBlank()) {
                result.add(barcodeService.sanitize(current));
            }
        }
        return result;
    }

    private String fitText(String value, int maxChars) {
        if (value == null) {
            return "";
        }
        String cleaned = barcodeService.sanitize(value.trim());
        if (cleaned.length() <= maxChars) {
            return cleaned;
        }
        if (maxChars <= 3) {
            return cleaned.substring(0, maxChars);
        }
        return cleaned.substring(0, maxChars - 3) + "...";
    }

    private String fitTextToWidth(String value, float maxWidth, float fontSize, boolean bold) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String cleaned = barcodeService.sanitize(value.trim());

        if (textWidth(cleaned, bold, fontSize) <= maxWidth) {
            return cleaned;
        }
        for (float size = fontSize; size >= 10f; size -= 0.5f) {
            if (textWidth(cleaned, bold, size) <= maxWidth) {
                return cleaned;
            }
        }
        for (int length = cleaned.length(); length > 3; length--) {
            String candidate = cleaned.substring(0, length - 1) + "...";
            if (textWidth(candidate, bold, 10f) <= maxWidth) {
                return candidate;
            }
        }
        return "...";
    }

    private float textWidth(String value, boolean bold, float fontSize) {
        if (value == null || value.isBlank()) {
            return 0f;
        }
        String cleaned = barcodeService.sanitize(value);
        try {
            PDType1Font font = bold ? MEASURE_BOLD : MEASURE_REGULAR;
            return font.getStringWidth(cleaned) / 1000f * fontSize;
        } catch (IOException | IllegalArgumentException ex) {
            return cleaned.length() * fontSize * 0.5f;
        }
    }

    // ---------------------------------------------------------- drawing utils

    private void drawRectangle(PDPageContentStream cs, float x, float y, float width, float height)
            throws IOException {
        cs.setLineWidth(BORDER_WIDTH);
        cs.addRect(x, y, width, height);
        cs.stroke();
    }

    private void drawHorizontalLine(PDPageContentStream cs, float x, float y, float width) throws IOException {
        cs.setLineWidth(BORDER_WIDTH);
        cs.moveTo(x, y);
        cs.lineTo(x + width, y);
        cs.stroke();
    }

    private void drawVerticalLine(PDPageContentStream cs, float x, float y, float height) throws IOException {
        cs.setLineWidth(BORDER_WIDTH);
        cs.moveTo(x, y);
        cs.lineTo(x, y + height);
        cs.stroke();
    }

    private void drawText(Ctx c, String text, float x, float y, float fontSize, boolean bold)
            throws IOException {
        String cleaned = barcodeService.sanitize(text == null ? "" : text);
        if (cleaned.isBlank()) {
            return;
        }
        PDPageContentStream cs = c.cs();
        cs.beginText();
        cs.setFont(bold ? c.bold() : c.regular(), fontSize);
        cs.newLineAtOffset(x, y);
        cs.showText(cleaned);
        cs.endText();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
