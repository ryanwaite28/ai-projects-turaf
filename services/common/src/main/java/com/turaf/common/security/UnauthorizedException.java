package com.turaf.common.security;

/**
 * Exception thrown when a user attempts to access resources they are not authorized to access.
 * Typically used for cross-organization access attempts.
 */
public class UnauthorizedException extends RuntimeException {
    
    public UnauthorizedException(String message) {
        super(message);
    }
    
    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}
