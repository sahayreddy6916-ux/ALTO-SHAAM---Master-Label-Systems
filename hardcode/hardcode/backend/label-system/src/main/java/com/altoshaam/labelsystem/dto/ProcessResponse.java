package com.altoshaam.labelsystem.dto;

import java.util.List;

public record ProcessResponse(
        boolean canGenerate,
        SettingsResponse settings,
        List<ProcessedRowResponse> rows,
        int totalRecommendedPrintQuantity,
        int totalRequestedPrintQuantity
) {
}
