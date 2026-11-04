package com.altoshaam.labelsystem.dto;

public record SettingsResponse(
        Long id,
        String supplierId,
        String supplierName,
        String shipFrom,
        String shipTo,
        String packingListSource,
        String defaultRevisionLevel
) {
}
