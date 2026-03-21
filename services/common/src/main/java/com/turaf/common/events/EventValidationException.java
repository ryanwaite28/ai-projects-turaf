package com.turaf.common.events;

/**
 * Exception thrown when event validation fails.
 * 
 * This exception is used to signal that an event does not conform to the
 * required schema or validation rules. It helps ensure that only valid,
 * well-formed events are published to the event bus.
 * 
 * Following exception handling best practices:
 * - Extends RuntimeException for unchecked exception handling
 * - Provides descriptive error messages for debugging
 * - Used to fail-fast when invalid events are detected
 */
public class EventValidationException extends RuntimeException {
    
    /**
     * Constructs a new EventValidationException with the specified detail message.
     * 
     * @param message the detail message explaining the validation failure
     */
    public EventValidationException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new EventValidationException with the specified detail message and cause.
     * 
     * @param message the detail message explaining the validation failure
     * @param cause the underlying cause of the validation failure
     */
    public EventValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
