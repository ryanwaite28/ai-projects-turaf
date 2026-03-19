package com.turaf.common.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class for aggregate roots in the domain model.
 * An aggregate root is an entity that serves as the entry point to an aggregate.
 * It ensures consistency boundaries and manages domain events.
 * 
 * All modifications to entities within the aggregate must go through the aggregate root.
 *
 * @param <ID> The type of the aggregate root's identifier
 */
public abstract class AggregateRoot<ID> extends Entity<ID> {
    
    private final List<DomainEvent> domainEvents = new ArrayList<>();
    
    /**
     * Protected constructor to enforce creation through subclasses.
     *
     * @param id The unique identifier for this aggregate root
     */
    protected AggregateRoot(ID id) {
        super(id);
    }
    
    /**
     * Registers a domain event that occurred within this aggregate.
     * Events will be published after the aggregate is successfully persisted.
     *
     * @param event The domain event to register
     */
    protected void registerEvent(DomainEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Domain event cannot be null");
        }
        domainEvents.add(event);
    }
    
    /**
     * Gets all domain events that have been registered but not yet published.
     *
     * @return Unmodifiable list of domain events
     */
    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }
    
    /**
     * Clears all registered domain events.
     * This should be called after events have been successfully published.
     */
    public void clearDomainEvents() {
        domainEvents.clear();
    }
    
    /**
     * Returns the number of pending domain events.
     *
     * @return Count of registered events
     */
    public int getDomainEventCount() {
        return domainEvents.size();
    }
}
