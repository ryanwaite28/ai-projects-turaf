package com.turaf.organization.infrastructure.events;

/**
 * Exception thrown when event mapping/serialization fails.
 */
public class EventMappingException extends RuntimeException {
    
    public EventMappingException(String message) {
        super(message);
    }
    
    public EventMappingException(String message, Throwable cause) {
        super(message, cause);
    }
}
