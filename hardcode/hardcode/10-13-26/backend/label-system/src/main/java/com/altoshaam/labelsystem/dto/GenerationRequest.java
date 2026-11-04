package com.altoshaam.labelsystem.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record GenerationRequest(
        @NotEmpty(message = "At least one label row is required")
        @Valid List<MasterLabelRowRequest> rows,
        boolean markPrinted
) {
}
