package com.altoshaam.labelsystem.service;

import com.altoshaam.labelsystem.dto.MasterLabelRowRequest;
import com.altoshaam.labelsystem.dto.ProcessedRowResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MasterLabelRuleServiceTest {

    private final MasterLabelRuleService service = new MasterLabelRuleService();

    @Test
    void identicalPalletUsesTwoPhysicalCopiesPerPalletByDefault() {
        ProcessedRowResponse result = service.process(
                new MasterLabelRowRequest(
                        "row-1", "ASI-PO-11745", "12345678", "", "ALT-10", "5014542",
                        "GREASE CART ASSY.", 16, 160, 10, null, "PL123456", "A", "PALLET_IDENTICAL"
                ),
                "",
                "AUTO"
        );

        assertEquals(10, result.labelUnits());
        assertEquals(2, result.copiesPerUnit());
        assertEquals(20, result.recommendedPrintQuantity());
        assertEquals("VALID", result.status());
    }

    @Test
    void detectsQuantityMismatch() {
        ProcessedRowResponse result = service.process(
                new MasterLabelRowRequest(
                        "row-2", "ASI-PO-12708", "12345678", "", "ALT-02", "5017531",
                        "FLUE", 30, 80, 2, 12, "PL123456", "", "PALLET_IDENTICAL"
                ),
                "",
                "AUTO"
        );

        assertEquals("WARNING", result.status());
        assertTrue(result.messages().stream().anyMatch(message -> message.contains("Quantity mismatch")));
    }

    @Test
    void rejectsPackingListLongerThanSpecification() {
        ProcessedRowResponse result = service.process(
                new MasterLabelRowRequest(
                        "row-3", "ASI-PO-1", "262721284", "", "ALT-01", "5017530",
                        "FLUE", 30, 180, 6, null, "", "", "PALLET_IDENTICAL"
                ),
                "",
                "INVOICE_NUMBER"
        );

        assertEquals("ERROR", result.status());
        assertTrue(result.messages().stream().anyMatch(message -> message.contains("exceeds the PDF limit of 8")));
    }

    @Test
    void zeroPalletsIsAnErrorAndDoesNotThrow() {
        ProcessedRowResponse result = service.process(
                new MasterLabelRowRequest(
                        "row-4", "ASI-PO-1", "", "PL1", "ALT-01", "5017530",
                        "FLUE", 30, 180, 0, null, "", "", "PALLET_IDENTICAL"
                ),
                "",
                "AUTO"
        );

        assertEquals("ERROR", result.status());
    }

    @Test
    void absurdPalletCountIsRejectedWithoutOverflow() {
        ProcessedRowResponse result = service.process(
                new MasterLabelRowRequest(
                        "row-5", "ASI-PO-1", "", "PL1", "ALT-01", "5017530",
                        "FLUE", 30, 180, Integer.MAX_VALUE, null, "", "", "PALLET_IDENTICAL"
                ),
                "",
                "AUTO"
        );

        assertEquals("ERROR", result.status());
    }
}
