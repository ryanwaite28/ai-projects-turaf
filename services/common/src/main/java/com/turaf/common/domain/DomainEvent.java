package com.turaf.common.domain;

import java.time.Instant;

/**
 * Interface for all domain events.
 * Domain events represent something significant that happened in the domain.
 * They are immutable and should be named in the past tense (e.g., ExperimentStarted, MetricRecorded).
 * 
 * All domain events must have:
 * - A unique event ID for idempotency
 * - An event type for routing and processing
 * - A timestamp of when the event occurred
 * - An organization ID for multi-tenancy
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
    Instant getTimestamp();
    
    /**
     * Gets the organization ID this event belongs to.
     * Used for multi-tenancy and data isolation.
     *
     * @return The organization ID
     */
    String getOrganizationId();
}
