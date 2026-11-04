package com.altoshaam.labelsystem.dto;

import jakarta.validation.constraints.NotBlank;

public record SettingsRequest(
        @NotBlank(message = "Supplier ID is required") String supplierId,
        String supplierName,
        String shipFrom,
        String shipTo,
        String packingListSource,
        String defaultRevisionLevel
) {
}
