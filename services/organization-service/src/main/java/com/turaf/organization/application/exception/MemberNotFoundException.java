package com.turaf.organization.application.exception;

/**
 * Exception thrown when a member cannot be found in an organization.
 */
public class MemberNotFoundException extends RuntimeException {
    
    public MemberNotFoundException(String message) {
        super(message);
    }
    
    public MemberNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
