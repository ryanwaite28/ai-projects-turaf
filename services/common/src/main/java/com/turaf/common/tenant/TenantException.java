package com.turaf.common.tenant;

/**
 * Exception thrown when there are issues with tenant context management.
 * This typically occurs when trying to access tenant information when no context is available.
 */
public class TenantException extends RuntimeException {
    
    /**
     * Creates a new tenant exception with the specified message.
     *
     * @param message The error message
     */
    public TenantException(String message) {
        super(message);
    }
    
    /**
     * Creates a new tenant exception with the specified message and cause.
     *
     * @param message The error message
     * @param cause The underlying cause
     */
    public TenantException(String message, Throwable cause) {
        super(message, cause);
    }
}
