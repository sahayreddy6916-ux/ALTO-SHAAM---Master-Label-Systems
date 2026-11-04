package com.altoshaam.labelsystem.dto;

public record GenerationResponse(
        Long batchId,
        String batchNumber,
        int labelUnits,
        int physicalLabels,
        String status,
        String pdfUrl
) {
}
