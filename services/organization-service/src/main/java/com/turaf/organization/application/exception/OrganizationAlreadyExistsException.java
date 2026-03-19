package com.turaf.organization.application.exception;

/**
 * Exception thrown when attempting to create an organization with a slug that already exists.
 */
public class OrganizationAlreadyExistsException extends RuntimeException {
    
    public OrganizationAlreadyExistsException(String message) {
        super(message);
    }
    
    public OrganizationAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
