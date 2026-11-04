package com.altoshaam.labelsystem.exception;

/** Thrown when the server is at its concurrency limit and the caller should retry shortly. */
public class ServiceBusyException extends RuntimeException {

    public ServiceBusyException(String message) {
        super(message);
    }
}
