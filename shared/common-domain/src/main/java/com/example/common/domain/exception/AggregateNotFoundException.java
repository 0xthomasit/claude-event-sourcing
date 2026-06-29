package com.example.common.domain.exception;

/**
 * Thrown when an aggregate cannot be located by its identifier.
 */
public class AggregateNotFoundException extends DomainException {

    public AggregateNotFoundException(String message) {
        super(message);
    }

    public AggregateNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
