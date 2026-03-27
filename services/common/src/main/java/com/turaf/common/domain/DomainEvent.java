package com.turaf.common.domain;

import java.time.Instant;

/**
 * Base interface for all domain events in the system.
 * Domain events represent something that happened in the domain that domain experts care about.
 * 
 * All domain events must be immutable and should contain:
 * - A unique event ID for idempotency
 * - The event type for routing and processing
 * - The timestamp when the event occurred
 * - The organization ID for multi-tenancy
 * - A correlation ID for distributed tracing
 */
public interface DomainEvent {
    
    /**
     * Gets the unique identifier for this event.
     * Used for idempotency and event deduplication.
     *
     * @return The event's unique ID
     */
    String getEventId();
    
    /**
     * Gets the type of this event.
     * Used for routing and determining which handlers should process this event.
     *
     * @return The event type (e.g., "ExperimentStarted", "MetricRecorded")
     */
    String getEventType();
    
    /**
     * Gets the timestamp when this event occurred.
     *
     * @return The event timestamp
     */
    Instant getOccurredAt();
    
    /**
     * Returns the organization ID associated with this event.
     * Used for multi-tenant event filtering and routing.
     *
     * @return The organization ID
     */
    String getOrganizationId();
    
    /**
     * Returns the correlation ID for distributed tracing.
     * Used to track event chains across services.
     *
     * @return The correlation ID
     */
    String getCorrelationId();
}
