package com.turaf.organization.domain.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class for aggregate roots in DDD.
 * Aggregates are clusters of domain objects that can be treated as a single unit.
 * The aggregate root is the only member of the aggregate that outside objects are allowed to hold references to.
 *
 * @param <ID> The type of the aggregate's identifier
 */
public abstract class AggregateRoot<ID> extends Entity<ID> {
    
    private final List<DomainEvent> domainEvents = new ArrayList<>();
    
    protected AggregateRoot(ID id) {
        super(id);
    }
    
    /**
     * Register a domain event to be published.
     *
     * @param event The domain event to register
     */
    protected void registerEvent(DomainEvent event) {
        this.domainEvents.add(event);
    }
    
    /**
     * Get all domain events that have been registered.
     *
     * @return Unmodifiable list of domain events
     */
    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }
    
    /**
     * Clear all registered domain events.
     * Should be called after events have been published.
     */
    public void clearDomainEvents() {
        this.domainEvents.clear();
    }
}
