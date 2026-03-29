package com.turaf.common.event;

/**
 * Abstraction for event idempotency checking.
 * 
 * Ensures events are processed exactly once in an at-least-once delivery system.
 * Services implement this interface in their infrastructure layer using their
 * preferred persistence mechanism (PostgreSQL, DynamoDB, etc.).
 * 
 * Following SOLID principles:
 * - Interface Segregation: Minimal interface with only essential operations
 * - Dependency Inversion: Services depend on this abstraction, not concrete implementations
 * 
 * Following event-driven architecture best practices:
 * - Prevents duplicate processing in at-least-once delivery systems
 * - Implementation should use conditional writes to prevent race conditions
 */
public interface IdempotencyChecker {
    
    /**
     * Checks if an event has already been processed.
     *
     * @param eventId the unique identifier of the event
     * @return true if the event has been processed, false otherwise
     */
    boolean isProcessed(String eventId);
    
    /**
     * Marks an event as processed.
     * 
     * Implementations should be idempotent — calling this method multiple times
     * with the same eventId should not fail or produce side effects.
     *
     * @param eventId the unique identifier of the event
     * @param eventType the type of the event
     * @param organizationId the organization context for multi-tenancy
     */
    void markProcessed(String eventId, String eventType, String organizationId);
}
