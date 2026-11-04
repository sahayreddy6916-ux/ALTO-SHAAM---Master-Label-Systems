package com.altoshaam.labelsystem.dto;

import java.time.Instant;

public record BatchSummaryResponse(
        Long id,
        String batchNumber,
        String status,
        int totalRows,
        int totalLabelUnits,
        int totalPhysicalLabels,
        Instant createdAt,
        Instant printedAt
) {
}
