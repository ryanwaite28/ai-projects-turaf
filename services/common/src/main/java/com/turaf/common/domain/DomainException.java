package com.turaf.common.domain;

/**
 * Base exception for all domain-level exceptions.
 * Domain exceptions represent violations of business rules or invariants.
 * They should be used when the domain model detects an invalid state or operation.
 * 
 * Each domain exception includes an error code for easier identification and handling.
 */
public class DomainException extends RuntimeException {
    
    private final String errorCode;
    
    /**
     * Creates a new domain exception with a message and error code.
     *
     * @param message Human-readable error message
     * @param errorCode Machine-readable error code for categorization
     */
    public DomainException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    /**
     * Creates a new domain exception with a message, error code, and cause.
     *
     * @param message Human-readable error message
     * @param errorCode Machine-readable error code for categorization
     * @param cause The underlying cause of this exception
     */
    public DomainException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    /**
     * Gets the error code for this exception.
     *
     * @return The error code
     */
    public String getErrorCode() {
        return errorCode;
    }
    
    @Override
    public String toString() {
        return "DomainException{" +
                "errorCode='" + errorCode + '\'' +
                ", message='" + getMessage() + '\'' +
                '}';
    }
}
