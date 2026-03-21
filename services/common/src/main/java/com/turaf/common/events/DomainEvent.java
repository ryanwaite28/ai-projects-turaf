package com.turaf.common.events;

import java.time.Instant;

/**
 * Base interface for all domain events in the system.
 * 
 * Domain events represent something that has happened in the domain that domain experts care about.
 * They are immutable and represent facts that have occurred.
 * 
 * Following DDD principles:
 * - Events are named in past tense (e.g., UserCreated, ExperimentCompleted)
 * - Events contain all information needed by consumers
 * - Events are immutable once created
 * - Events include organization context for multi-tenancy
 */
public interface DomainEvent {
    
    /**
     * Unique identifier for this event instance.
     * Should be a UUID to ensure global uniqueness.
     * 
     * @return the event ID
     */
    String getEventId();
    
    /**
     * Type identifier for this event.
     * Should follow the pattern: {BoundedContext}.{EventName}
     * Example: "identity.UserCreated", "experiment.ExperimentCompleted"
     * 
     * @return the event type
     */
    String getEventType();
    
    /**
     * Timestamp when the event occurred.
     * Should be set when the event is created, not when it's published.
     * 
     * @return the event timestamp
     */
    Instant getTimestamp();
    
    /**
     * Organization ID for multi-tenant isolation.
     * All events must be associated with an organization for proper data isolation.
     * 
     * @return the organization ID
     */
    String getOrganizationId();
}
