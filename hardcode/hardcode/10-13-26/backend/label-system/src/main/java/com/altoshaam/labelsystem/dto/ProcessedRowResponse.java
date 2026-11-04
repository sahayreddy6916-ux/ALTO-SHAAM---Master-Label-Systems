package com.altoshaam.labelsystem.dto;

import java.util.List;

public record ProcessedRowResponse(
        String clientRowId,
        String customerPoNumber,
        String invoiceNumber,
        String packageId,
        String itemNumber,
        String partNumber,
        String description,
        int perPalletQuantity,
        int totalQuantity,
        int numberOfPallets,
        String packingListNumber,
        String revisionLevel,
        String shippingScenario,
        int labelUnits,
        int copiesPerUnit,
        int recommendedPrintQuantity,
        int requestedPrintQuantity,
        String status,
        List<String> messages
) {
}
