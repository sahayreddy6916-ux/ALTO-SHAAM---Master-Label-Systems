package com.altoshaam.labelsystem.exception;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(LabelValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(LabelValidationException ex) {
        return body(HttpStatus.BAD_REQUEST, ex.getMessage(), ex.getErrors());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleRequestValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        return body(HttpStatus.BAD_REQUEST, "Request validation failed", errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadable(HttpMessageNotReadableException ex) {
        return body(HttpStatus.BAD_REQUEST, "Malformed request body", List.of("The request body is not valid JSON."));
    }

    @ExceptionHandler({ NoResourceFoundException.class, NoHandlerFoundException.class })
    public ResponseEntity<Map<String, Object>> handleNoResource(Exception ex) {
        return body(HttpStatus.NOT_FOUND, "Not found", List.of("The requested resource does not exist."));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return body(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed", List.of(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleBadParameter(MethodArgumentTypeMismatchException ex) {
        return body(HttpStatus.BAD_REQUEST, "Invalid parameter", List.of("Invalid value for parameter '" + ex.getName() + "'."));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NotFoundException ex) {
        return body(HttpStatus.NOT_FOUND, ex.getMessage(), List.of(ex.getMessage()));
    }

    @ExceptionHandler(ServiceBusyException.class)
    public ResponseEntity<Map<String, Object>> handleBusy(ServiceBusyException ex) {
        log.warn("Rejected request because the server is busy: {}", ex.getMessage());
        ResponseEntity<Map<String, Object>> response =
                body(HttpStatus.SERVICE_UNAVAILABLE, "Server busy", List.of(ex.getMessage()));
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header(HttpHeaders.RETRY_AFTER, "5")
                .body(response.getBody());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        // Full details go to the log only; clients get a generic message.
        log.error("Unexpected server error", ex);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error",
                List.of("Something went wrong. Please try again or contact support."));
    }

    private ResponseEntity<Map<String, Object>> body(HttpStatus status, String message, List<String> errors) {
        return ResponseEntity.status(status).body(Map.of(
                "timestamp", Instant.now(),
                "message", message,
                "errors", errors));
    }
}
