package com.altoshaam.labelsystem.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

import com.altoshaam.labelsystem.entity.AppSetting;
import com.altoshaam.labelsystem.entity.GeneratedLabel;
import com.altoshaam.labelsystem.entity.LabelItem;

class PdfLabelServiceTest {

    private final PdfLabelService service = new PdfLabelService(new BarcodeService(), 2, 5);

    @Test
    void rendersTwoLabelsPerA4PageWithVectorBarcodes() throws Exception {
        LabelItem item = new LabelItem();
        item.setCustomerPoNumber("ASI-PO-11745");
        item.setPartNumber("5014542");
        item.setDescription("GREASE CART ASSY. COMBI GREASE COLLECTION");
        item.setPerPalletQuantity(16);
        item.setPackingListNumber("11111");
        item.setRevisionLevel("");

        GeneratedLabel label = new GeneratedLabel();
        label.setLabelItem(item);
        label.setSerialNumber("543210000000083");
        label.setCopyCount(5); // 5 physical labels -> 3 A4 sheets (2 + 2 + 1)

        AppSetting settings = new AppSetting();
        settings.setSupplierId("54321");
        settings.setShipFrom("AUTOCRAT ENGINEERS\n#263 & 264, KIADB Aerospace Park, Phase-2, Bangalore");
        settings.setShipTo("ALTO-SHAAM, INC.\nW164 N9221 Water Street\nMenomonee Falls, WI 53051\nUSA");

        byte[] pdf = service.generatePdf(List.of(label), settings);

        assertTrue(pdf.length > 1000);
        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertEquals(3, document.getNumberOfPages());
        }
    }

    @Test
    void barcodeIsEncodedAsModules() {
        boolean[] bits = new BarcodeService().encodeCode128("9S543210000000083");
        assertTrue(bits.length > 50);
        assertTrue(bits[0], "a Code 128 symbol starts with a bar");
    }
}
