package com.turaf.common.event;

import com.turaf.common.domain.DomainEvent;

import java.util.List;

/**
 * Interface for publishing domain events to an event bus.
 * 
 * This abstraction allows services to publish events without coupling to a specific
 * event bus implementation (EventBridge, Kafka, RabbitMQ, etc.).
 * 
 * Following SOLID principles:
 * - Interface Segregation: Minimal interface with only essential operations
 * - Dependency Inversion: Services depend on this abstraction, not concrete implementations
 * 
 * Following DDD principles:
 * - Events are published after domain state changes are persisted
 * - Publishing is idempotent (handled by implementation)
 * - Events represent facts that have occurred in the domain
 */
public interface EventPublisher {
    
    /**
     * Publishes a single domain event to the event bus.
     * 
     * The event will be wrapped in an EventEnvelope with metadata before publishing.
     * This operation should be called after the domain state change has been persisted
     * to ensure consistency.
     * 
     * @param event the domain event to publish
     * @throws EventPublishException if publishing fails
     */
    void publish(DomainEvent event);
    
    /**
     * Publishes multiple domain events to the event bus in a batch.
     * 
     * Batch publishing is more efficient than individual publishes when multiple
     * events need to be published together. The implementation should handle
     * batching constraints (e.g., EventBridge's 10 events per request limit).
     * 
     * @param events the list of domain events to publish
     * @throws EventPublishException if publishing fails
     */
    void publishBatch(List<DomainEvent> events);
}
