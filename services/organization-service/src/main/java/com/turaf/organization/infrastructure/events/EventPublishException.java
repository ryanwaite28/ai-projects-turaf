package com.turaf.organization.infrastructure.events;

/**
 * Exception thrown when event publishing fails.
 */
public class EventPublishException extends RuntimeException {
    
    public EventPublishException(String message) {
        super(message);
    }
    
    public EventPublishException(String message, Throwable cause) {
        super(message, cause);
    }
}
