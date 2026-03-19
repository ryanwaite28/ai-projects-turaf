package com.turaf.organization.domain.common;

import java.time.Instant;

/**
 * Interface for domain events.
 * Domain events are used to capture occurrences of something that happened in the domain.
 */
public interface DomainEvent {
    
    /**
     * Get the unique identifier for this event.
     *
     * @return Event ID
     */
    String getEventId();
    
    /**
     * Get the type of this event.
     *
     * @return Event type
     */
    String getEventType();
    
    /**
     * Get the timestamp when this event occurred.
     *
     * @return Event timestamp
     */
    Instant getTimestamp();
    
    /**
     * Get the organization ID associated with this event.
     *
     * @return Organization ID
     */
    String getOrganizationId();
}
