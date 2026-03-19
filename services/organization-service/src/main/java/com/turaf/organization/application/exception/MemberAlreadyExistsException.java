package com.turaf.organization.application.exception;

/**
 * Exception thrown when attempting to add a user who is already a member of the organization.
 */
public class MemberAlreadyExistsException extends RuntimeException {
    
    public MemberAlreadyExistsException(String message) {
        super(message);
    }
    
    public MemberAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
