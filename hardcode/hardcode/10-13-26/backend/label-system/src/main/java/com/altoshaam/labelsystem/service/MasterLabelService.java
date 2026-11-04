package com.altoshaam.labelsystem.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.altoshaam.labelsystem.dto.BatchSummaryResponse;
import com.altoshaam.labelsystem.dto.GenerationRequest;
import com.altoshaam.labelsystem.dto.GenerationResponse;
import com.altoshaam.labelsystem.dto.MasterLabelRowRequest;
import com.altoshaam.labelsystem.dto.ProcessResponse;
import com.altoshaam.labelsystem.dto.ProcessedRowResponse;
import com.altoshaam.labelsystem.dto.SettingsResponse;
import com.altoshaam.labelsystem.entity.AppSetting;
import com.altoshaam.labelsystem.entity.BatchStatus;
import com.altoshaam.labelsystem.entity.GeneratedLabel;
import com.altoshaam.labelsystem.entity.LabelBatch;
import com.altoshaam.labelsystem.entity.LabelItem;
import com.altoshaam.labelsystem.entity.PrintStatus;
import com.altoshaam.labelsystem.exception.LabelValidationException;
import com.altoshaam.labelsystem.exception.NotFoundException;
import com.altoshaam.labelsystem.repository.AppSettingRepository;
import com.altoshaam.labelsystem.repository.GeneratedLabelRepository;
import com.altoshaam.labelsystem.repository.LabelBatchRepository;
import com.altoshaam.labelsystem.repository.LabelItemRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Generation flow (built to stay responsive under load):
 *
 * 1. Validate the request (no database transaction held).
 * 2. One short transaction reserves serial numbers and saves the batch, items
 *    and labels. The serial-number row lock is released as soon as it commits.
 * 3. The PDF is rendered and written OUTSIDE any transaction, so a slow render
 *    never holds a database connection or a lock.
 * 4. A tiny transaction stores the PDF path.
 *
 * If step 3 fails, the batch is still valid; the PDF is rebuilt on demand from
 * the saved labels (same serial numbers) the first time it is requested.
 */
@Service
public class MasterLabelService {

    private static final Logger log = LoggerFactory.getLogger(MasterLabelService.class);
    private static final DateTimeFormatter BATCH_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final SettingsService settingsService;
    private final AppSettingRepository appSettingRepository;
    private final LabelBatchRepository batchRepository;
    private final LabelItemRepository labelItemRepository;
    private final GeneratedLabelRepository generatedLabelRepository;
    private final MasterLabelRuleService ruleService;
    private final SerialNumberService serialNumberService;
    private final PdfLabelService pdfLabelService;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate tx;
    private final Path storageDir;
    private final int maxRows;
    private final int maxPhysicalLabels;

    public MasterLabelService(
            SettingsService settingsService,
            AppSettingRepository appSettingRepository,
            LabelBatchRepository batchRepository,
            LabelItemRepository labelItemRepository,
            GeneratedLabelRepository generatedLabelRepository,
            MasterLabelRuleService ruleService,
            SerialNumberService serialNumberService,
            PdfLabelService pdfLabelService,
            ObjectMapper objectMapper,
            TransactionTemplate transactionTemplate,
            @Value("${label.storage-dir:./data/generated}") String storageDir,
            @Value("${label.max-rows:500}") int maxRows,
            @Value("${label.max-physical-labels:5000}") int maxPhysicalLabels) {

        this.settingsService = settingsService;
        this.appSettingRepository = appSettingRepository;
        this.batchRepository = batchRepository;
        this.labelItemRepository = labelItemRepository;
        this.generatedLabelRepository = generatedLabelRepository;
        this.ruleService = ruleService;
        this.serialNumberService = serialNumberService;
        this.pdfLabelService = pdfLabelService;
        this.objectMapper = objectMapper;
        this.tx = transactionTemplate;
        this.storageDir = Path.of(storageDir).toAbsolutePath().normalize();
        this.maxRows = Math.max(1, maxRows);
        this.maxPhysicalLabels = Math.max(1, maxPhysicalLabels);
    }

    // ---------------------------------------------------------------- preview

    @Transactional(readOnly = true)
    public ProcessResponse preview(List<MasterLabelRowRequest> rows) {
        SettingsResponse settings = settingsService.get();

        List<MasterLabelRowRequest> safeRows = rows == null ? List.of() : rows;
        if (safeRows.size() > maxRows) {
            throw new LabelValidationException(
                    List.of("A maximum of " + maxRows + " rows can be processed at once."));
        }

        List<ProcessedRowResponse> processed = safeRows.stream()
                .filter(row -> row != null)
                .map(row -> ruleService.process(row, settings.defaultRevisionLevel(), settings.packingListSource()))
                .toList();

        boolean canGenerate = !processed.isEmpty()
                && processed.stream().allMatch(
                        row -> "VALID".equals(row.status()) || "WARNING".equals(row.status()))
                && settings.supplierId() != null
                && settings.supplierId().matches("\\d{1,14}")
                && settings.shipFrom() != null
                && !settings.shipFrom().isBlank()
                && settings.shipTo() != null
                && !settings.shipTo().isBlank();

        int recommended = processed.stream().mapToInt(ProcessedRowResponse::recommendedPrintQuantity).sum();
        int requested = processed.stream().mapToInt(ProcessedRowResponse::requestedPrintQuantity).sum();

        return new ProcessResponse(canGenerate, settings, processed, recommended, requested);
    }

    // --------------------------------------------------------------- generate

    /** Result of the database step, used after the transaction has committed. */
    private record PersistedBatch(
            Long id,
            String batchNumber,
            String status,
            int totalUnits,
            int totalPhysicalLabels,
            List<GeneratedLabel> labels,
            AppSetting appSetting) {
    }

    public GenerationResponse generate(GenerationRequest request) {
        if (request == null) {
            throw new LabelValidationException(List.of("Generation request cannot be null."));
        }

        SettingsResponse settings = settingsService.get();
        validateSettingsForGeneration(settings);

        List<MasterLabelRowRequest> rows = request.rows() == null ? List.of() : request.rows();
        if (rows.isEmpty()) {
            throw new LabelValidationException(List.of("At least one row is required to generate labels."));
        }
        if (rows.size() > maxRows) {
            throw new LabelValidationException(
                    List.of("A maximum of " + maxRows + " rows can be generated at once."));
        }

        // Backend re-validation (never trust the client).
        List<ProcessedRowResponse> processed = new ArrayList<>(rows.size());
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            MasterLabelRowRequest raw = rows.get(i);
            if (raw == null) {
                errors.add("Row " + (i + 1) + ": row is empty.");
                continue;
            }
            ProcessedRowResponse row = ruleService.process(
                    raw, settings.defaultRevisionLevel(), settings.packingListSource());
            processed.add(row);
            if ("ERROR".equals(row.status())) {
                final int rowNumber = i + 1;
                row.messages().forEach(message -> errors.add("Row " + rowNumber + ": " + message));
            }
        }
        if (!errors.isEmpty()) {
            throw new LabelValidationException(errors);
        }

        long totalUnitsLong = processed.stream().mapToLong(ProcessedRowResponse::labelUnits).sum();
        long totalPhysicalLong = processed.stream().mapToLong(ProcessedRowResponse::requestedPrintQuantity).sum();

        if (totalUnitsLong <= 0) {
            throw new LabelValidationException(List.of("Calculated label units must be greater than zero."));
        }
        if (totalPhysicalLong <= 0) {
            throw new LabelValidationException(List.of("Requested print quantity must be greater than zero."));
        }
        if (totalPhysicalLong > maxPhysicalLabels) {
            throw new LabelValidationException(List.of(
                    "A maximum of " + maxPhysicalLabels + " physical labels can be generated at once. "
                            + "Requested: " + totalPhysicalLong + ". Please split the job into smaller batches."));
        }

        int totalUnits = (int) totalUnitsLong;
        int totalPhysicalLabels = (int) totalPhysicalLong;

        // Step 2: short transaction (serial numbers + rows).
        PersistedBatch persisted = tx.execute(status -> persistBatch(
                request, rows, processed, settings, totalUnits, totalPhysicalLabels));
        if (persisted == null) {
            throw new IllegalStateException("Batch could not be saved.");
        }

        // Step 3 + 4: render outside any transaction.
        Path target = storageDir.resolve(persisted.batchNumber() + ".pdf");
        try {
            List<GeneratedLabel> pdfOrder = persisted.labels().stream()
                    .sorted(Comparator.comparing(GeneratedLabel::getId))
                    .toList();
            pdfLabelService.generatePdfToFile(pdfOrder, persisted.appSetting(), target);
            storePdfPath(persisted.id(), target);
        } catch (RuntimeException ex) {
            // The batch is saved. The PDF is rebuilt from the saved labels when it is first requested.
            log.error("PDF rendering failed for batch {}; it will be rebuilt on demand.",
                    persisted.batchNumber(), ex);
        }

        return new GenerationResponse(
                persisted.id(),
                persisted.batchNumber(),
                persisted.totalUnits(),
                persisted.totalPhysicalLabels(),
                persisted.status(),
                "/api/master-labels/batches/" + persisted.id() + "/pdf");
    }

    private PersistedBatch persistBatch(
            GenerationRequest request,
            List<MasterLabelRowRequest> rows,
            List<ProcessedRowResponse> processed,
            SettingsResponse settings,
            int totalUnits,
            int totalPhysicalLabels) {

        List<String> serials = serialNumberService.reserveSerialNumbers(settings.supplierId(), totalUnits);
        if (serials == null || serials.size() != totalUnits) {
            throw new IllegalStateException("Expected " + totalUnits + " serial numbers but received "
                    + (serials == null ? 0 : serials.size()));
        }

        Instant now = Instant.now();
        boolean printed = request.markPrinted();

        LabelBatch batch = new LabelBatch();
        batch.setBatchNumber("ML-" + BATCH_FORMAT.format(LocalDateTime.now(ZoneOffset.UTC)) + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        batch.setStatus(printed ? BatchStatus.PRINTED : BatchStatus.GENERATED);
        batch.setTotalRows(processed.size());
        batch.setTotalLabelUnits(totalUnits);
        batch.setTotalPhysicalLabels(totalPhysicalLabels);
        batch.setCreatedAt(now);
        batch.setPrintedAt(printed ? now : null);

        LabelBatch savedBatch = batchRepository.saveAndFlush(batch);

        int serialIndex = 0;
        List<GeneratedLabel> generatedLabels = new ArrayList<>(totalUnits);

        for (int rowIndex = 0; rowIndex < processed.size(); rowIndex++) {
            ProcessedRowResponse row = processed.get(rowIndex);
            MasterLabelRowRequest raw = rows.get(rowIndex);

            LabelItem item = new LabelItem();
            item.setBatch(savedBatch);
            item.setCustomerPoNumber(row.customerPoNumber());
            item.setInvoiceNumber(row.invoiceNumber());
            item.setPackageId(row.packageId());
            item.setItemNumber(row.itemNumber());
            item.setPartNumber(row.partNumber());
            item.setDescription(row.description());
            item.setPerPalletQuantity(row.perPalletQuantity());
            item.setTotalQuantity(row.totalQuantity());
            item.setNumberOfPallets(row.numberOfPallets());
            item.setRecommendedPrintQuantity(row.recommendedPrintQuantity());
            item.setRequestedPrintQuantity(row.requestedPrintQuantity());
            item.setLabelUnits(row.labelUnits());
            item.setCopiesPerUnitDefault(row.copiesPerUnit());
            item.setPackingListNumber(row.packingListNumber());
            item.setRevisionLevel(row.revisionLevel());
            item.setShippingScenario(ruleService.parseScenario(row.shippingScenario()));
            item.setWarnings(String.join(" | ", row.messages()));
            item.setRawInputJson(toJson(raw));

            // GeneratedLabel owns the relationship; items are saved first, labels after.
            LabelItem savedItem = labelItemRepository.saveAndFlush(item);

            int baseCopies = row.requestedPrintQuantity() / row.labelUnits();
            int remainder = row.requestedPrintQuantity() % row.labelUnits();

            for (int unit = 0; unit < row.labelUnits(); unit++) {
                if (serialIndex >= serials.size()) {
                    throw new IllegalStateException("Serial number index exceeded reserved serial numbers.");
                }
                String serial = serials.get(serialIndex++);

                GeneratedLabel label = new GeneratedLabel();
                label.setLabelItem(savedItem);
                label.setSerialNumber(serial);
                label.setBarcodeData("9S" + serial);
                label.setQuantity(row.perPalletQuantity());
                label.setCopyCount(baseCopies + (unit < remainder ? 1 : 0));
                label.setStatus(printed ? PrintStatus.PRINTED : PrintStatus.GENERATED);
                label.setCreatedAt(now);
                label.setPrintedAt(printed ? now : null);
                generatedLabels.add(label);
            }
        }

        generatedLabelRepository.saveAll(generatedLabels);
        generatedLabelRepository.flush();

        AppSetting appSetting = appSettingRepository.findById(1L)
                .orElseThrow(() -> new IllegalStateException("Application settings record not found."));

        return new PersistedBatch(
                savedBatch.getId(),
                savedBatch.getBatchNumber(),
                savedBatch.getStatus().name(),
                totalUnits,
                totalPhysicalLabels,
                generatedLabels,
                appSetting);
    }

    private void storePdfPath(Long batchId, Path target) {
        tx.executeWithoutResult(status -> batchRepository.findById(batchId)
                .ifPresent(batch -> batch.setPdfPath(target.toString())));
    }

    // -------------------------------------------------------------- PDF access

    /**
     * Returns the stored PDF. If the file is missing (failed render, cleaned disk,
     * moved server) it is rebuilt from the saved labels, so downloads never break.
     */
    public byte[] getPdf(Long batchId) {
        LabelBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new NotFoundException("Batch not found: " + batchId));

        String path = batch.getPdfPath();
        if (path != null && !path.isBlank() && Files.isReadable(Path.of(path))) {
            return pdfLabelService.readPdf(Path.of(path));
        }

        log.warn("PDF file missing for batch {}; rebuilding from saved labels.", batch.getBatchNumber());
        byte[] rebuilt = reprint(batchId);
        try {
            Path target = storageDir.resolve(batch.getBatchNumber() + ".pdf");
            Files.createDirectories(target.getParent());
            Files.write(target, rebuilt);
            storePdfPath(batchId, target);
        } catch (Exception ex) {
            log.warn("Could not cache rebuilt PDF for batch {}", batch.getBatchNumber(), ex);
        }
        return rebuilt;
    }

    @Transactional(readOnly = true)
    public List<BatchSummaryResponse> history(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return batchRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, safeLimit))
                .stream()
                .map(batch -> new BatchSummaryResponse(
                        batch.getId(),
                        batch.getBatchNumber(),
                        batch.getStatus().name(),
                        batch.getTotalRows(),
                        batch.getTotalLabelUnits(),
                        batch.getTotalPhysicalLabels(),
                        batch.getCreatedAt(),
                        batch.getPrintedAt()))
                .toList();
    }

    @Transactional
    public void markPrinted(Long batchId) {
        LabelBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new NotFoundException("Batch not found: " + batchId));

        Instant now = Instant.now();
        batch.setStatus(BatchStatus.PRINTED);
        batch.setPrintedAt(now);
        batchRepository.save(batch);

        generatedLabelRepository.markBatchPrinted(batchId, PrintStatus.PRINTED, now);
    }

    @Transactional(readOnly = true)
    public LabelBatch getBatch(Long batchId) {
        return batchRepository.findWithItemsById(batchId)
                .orElseThrow(() -> new NotFoundException("Batch not found: " + batchId));
    }

    /** Re-renders a batch from its saved labels (same serial numbers). */
    public byte[] reprint(Long batchId) {
        AppSetting settings = appSettingRepository.findById(1L)
                .orElseThrow(() -> new IllegalStateException("Application settings record not found."));

        // Load inside a transaction, render outside it.
        List<GeneratedLabel> labels = tx.execute(status -> {
            LabelBatch batch = getBatch(batchId);
            List<GeneratedLabel> loaded = batch.getItems().stream()
                    .flatMap(item -> item.getGeneratedLabels().stream())
                    .sorted(Comparator.comparing(GeneratedLabel::getId))
                    .toList();
            // Make sure the lazy LabelItem of each label is initialised before the session closes.
            loaded.forEach(label -> label.getLabelItem().getPartNumber());
            return loaded;
        });

        return pdfLabelService.generatePdf(labels, settings);
    }

    // ---------------------------------------------------------------- helpers

    private void validateSettingsForGeneration(SettingsResponse settings) {
        List<String> errors = new ArrayList<>();

        if (settings == null) {
            errors.add("Application settings are unavailable.");
        } else {
            if (settings.supplierId() == null || !settings.supplierId().matches("\\d{1,14}")) {
                errors.add("Configure an A-S supplier ID containing 1 to 14 numeric digits in Settings.");
            }
            if (settings.shipFrom() == null || settings.shipFrom().isBlank()) {
                errors.add("Configure the Ship From address in Settings.");
            }
            if (settings.shipTo() == null || settings.shipTo().isBlank()) {
                errors.add("Configure the Ship To address in Settings.");
            }
        }

        if (!errors.isEmpty()) {
            throw new LabelValidationException(errors);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "{}";
        }
    }
}
