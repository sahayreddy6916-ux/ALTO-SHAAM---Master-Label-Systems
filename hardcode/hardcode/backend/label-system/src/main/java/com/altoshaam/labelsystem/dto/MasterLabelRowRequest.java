package com.altoshaam.labelsystem.dto;

public record MasterLabelRowRequest(
        String clientRowId,
        String customerPoNumber,
        String invoiceNumber,
        String packageId,
        String itemNumber,
        String partNumber,
        String description,
        Integer perPalletQuantity,
        Integer totalQuantity,
        Integer numberOfPallets,
        Integer requestedPrintQuantity,
        String packingListOverride,
        String revisionLevel,
        String shippingScenario
) {
}
