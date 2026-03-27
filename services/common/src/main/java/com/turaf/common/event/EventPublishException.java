package com.turaf.common.event;

/**
 * Exception thrown when event publishing fails.
 * 
 * This exception is used to wrap underlying failures during event publishing to EventBridge,
 * including serialization errors, network failures, and EventBridge API errors.
 * 
 * Following exception handling best practices:
 * - Extends RuntimeException for unchecked exception handling
 * - Provides both message-only and message-with-cause constructors
 * - Preserves the original exception cause for debugging
 */
public class EventPublishException extends RuntimeException {
    
    /**
     * Constructs a new EventPublishException with the specified detail message.
     * 
     * @param message the detail message explaining the failure
     */
    public EventPublishException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new EventPublishException with the specified detail message and cause.
     * 
     * @param message the detail message explaining the failure
     * @param cause the underlying cause of the failure
     */
    public EventPublishException(String message, Throwable cause) {
        super(message, cause);
    }
}
