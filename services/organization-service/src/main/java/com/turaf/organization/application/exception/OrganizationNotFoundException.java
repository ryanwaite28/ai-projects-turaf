package com.turaf.organization.application.exception;

/**
 * Exception thrown when an organization cannot be found.
 */
public class OrganizationNotFoundException extends RuntimeException {
    
    public OrganizationNotFoundException(String message) {
        super(message);
    }
    
    public OrganizationNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
