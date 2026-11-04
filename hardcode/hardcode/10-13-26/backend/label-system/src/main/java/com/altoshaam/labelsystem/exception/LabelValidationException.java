package com.altoshaam.labelsystem.exception;

import java.util.List;

public class LabelValidationException extends RuntimeException {

    private final List<String> errors;

    public LabelValidationException(List<String> errors) {
        super("Label validation failed");
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }
}
