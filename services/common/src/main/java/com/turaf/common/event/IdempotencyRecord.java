package com.turaf.common.event;

import java.time.Instant;
import java.util.Objects;

/**
 * Record representing a processed event in the idempotency tracking system.
 * 
 * This immutable record stores information about events that have been successfully
 * processed to prevent duplicate processing. Records are stored in DynamoDB with
 * automatic TTL-based expiration after 30 days.
 * 
 * Following DDD principles:
 * - Value object representing event processing state
 * - Immutable once created
 * - Contains all information needed for idempotency checking
 * 
 * Following Clean Architecture:
 * - Pure domain model with no infrastructure dependencies
 * - No framework annotations (can be used in any layer)
 */
public class IdempotencyRecord {
    
    private final String eventId;
    private final String eventType;
    private final String handler;
    private final Instant processedAt;
    
    /**
     * Constructs an IdempotencyRecord with all required fields.
     * 
     * @param eventId unique identifier of the processed event
     * @param eventType type of the event that was processed
     * @param handler name of the handler/service that processed the event
     * @param processedAt timestamp when the event was processed
     * @throws NullPointerException if any required field is null
     */
    public IdempotencyRecord(String eventId, String eventType, String handler, Instant processedAt) {
        this.eventId = Objects.requireNonNull(eventId, "Event ID cannot be null");
        this.eventType = Objects.requireNonNull(eventType, "Event type cannot be null");
        this.handler = Objects.requireNonNull(handler, "Handler cannot be null");
        this.processedAt = Objects.requireNonNull(processedAt, "Processed timestamp cannot be null");
    }
    
    // Getters
    
    public String getEventId() {
        return eventId;
    }
    
    public String getEventType() {
        return eventType;
    }
    
    public String getHandler() {
        return handler;
    }
    
    public Instant getProcessedAt() {
        return processedAt;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IdempotencyRecord that = (IdempotencyRecord) o;
        return Objects.equals(eventId, that.eventId) &&
               Objects.equals(eventType, that.eventType) &&
               Objects.equals(handler, that.handler) &&
               Objects.equals(processedAt, that.processedAt);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(eventId, eventType, handler, processedAt);
    }
    
    @Override
    public String toString() {
        return "IdempotencyRecord{" +
               "eventId='" + eventId + '\'' +
               ", eventType='" + eventType + '\'' +
               ", handler='" + handler + '\'' +
               ", processedAt=" + processedAt +
               '}';
    }
}
