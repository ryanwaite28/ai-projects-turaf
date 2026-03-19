package com.turaf.organization.application;

import com.turaf.common.domain.DomainEvent;

/**
 * Interface for publishing domain events.
 * Implementation will be provided by infrastructure layer.
 */
public interface EventPublisher {
    
    /**
     * Publish a domain event.
     *
     * @param event The domain event to publish
     */
    void publish(DomainEvent event);
}
