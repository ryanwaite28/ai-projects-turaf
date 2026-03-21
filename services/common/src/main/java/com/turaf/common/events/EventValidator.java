package com.turaf.common.events;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Validator for ensuring domain events and event envelopes conform to required schemas.
 * 
 * This component validates that events meet all structural and semantic requirements
 * before they are published to the event bus. Validation includes:
 * - Required field presence
 * - Field format validation (UUID, PascalCase, etc.)
 * - Temporal constraints (timestamps not in future)
 * - Multi-tenant isolation (organization ID required)
 * 
 * Following SOLID principles:
 * - Single Responsibility: Only handles event validation
 * - Open/Closed: Extensible through additional validation methods
 * 
 * Following fail-fast principle:
 * - Throws EventValidationException immediately on first validation failure
 * - Prevents invalid events from entering the system
 */
@Component
public class EventValidator {
    
    private static final int TIMESTAMP_TOLERANCE_SECONDS = 60;
    
    /**
     * Validates a DomainEvent for required fields and format.
     * 
     * This method checks that the event has all required fields and that
     * they conform to the expected formats.
     * 
     * @param event the domain event to validate
     * @throws EventValidationException if validation fails
     */
    public void validate(DomainEvent event) {
        if (event == null) {
            throw new EventValidationException("Event cannot be null");
        }
        
        validateEventId(event.getEventId());
        validateEventType(event.getEventType());
        validateTimestamp(event.getTimestamp());
        validateOrganizationId(event.getOrganizationId());
    }
    
    /**
     * Validates an EventEnvelope for required fields and format.
     * 
     * This method performs comprehensive validation of the event envelope,
     * ensuring all metadata and payload requirements are met.
     * 
     * @param envelope the event envelope to validate
     * @throws EventValidationException if validation fails
     */
    public void validate(EventEnvelope envelope) {
        if (envelope == null) {
            throw new EventValidationException("Event envelope cannot be null");
        }
        
        // Validate event ID
        if (envelope.getEventId() == null || envelope.getEventId().isBlank()) {
            throw new EventValidationException("Event ID is required");
        }
        validateEventId(envelope.getEventId());
        
        // Validate event type
        if (envelope.getEventType() == null || envelope.getEventType().isBlank()) {
            throw new EventValidationException("Event type is required");
        }
        
        // Validate event version
        if (envelope.getEventVersion() < 1) {
            throw new EventValidationException("Event version must be >= 1, got: " + envelope.getEventVersion());
        }
        
        // Validate timestamp
        if (envelope.getTimestamp() == null) {
            throw new EventValidationException("Timestamp is required");
        }
        validateTimestamp(envelope.getTimestamp());
        
        // Validate source service
        if (envelope.getSourceService() == null || envelope.getSourceService().isBlank()) {
            throw new EventValidationException("Source service is required");
        }
        
        // Validate organization ID
        if (envelope.getOrganizationId() == null || envelope.getOrganizationId().isBlank()) {
            throw new EventValidationException("Organization ID is required");
        }
        
        // Validate payload
        if (envelope.getPayload() == null) {
            throw new EventValidationException("Payload is required");
        }
    }
    
    /**
     * Validates that the event ID is a valid UUID.
     * 
     * Event IDs must be UUIDs to ensure global uniqueness and prevent collisions.
     * 
     * @param eventId the event ID to validate
     * @throws EventValidationException if the event ID is invalid
     */
    private void validateEventId(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            throw new EventValidationException("Event ID is required");
        }
        
        try {
            UUID.fromString(eventId);
        } catch (IllegalArgumentException e) {
            throw new EventValidationException("Event ID must be a valid UUID, got: " + eventId, e);
        }
    }
    
    /**
     * Validates that the event type follows naming conventions.
     * 
     * Event types should be in PascalCase format (e.g., UserCreated, ExperimentCompleted).
     * This ensures consistency across the event-driven system.
     * 
     * @param eventType the event type to validate
     * @throws EventValidationException if the event type is invalid
     */
    private void validateEventType(String eventType) {
        if (eventType == null || eventType.isBlank()) {
            throw new EventValidationException("Event type is required");
        }
        
        // Event type should be PascalCase (starts with uppercase, contains only letters)
        if (!eventType.matches("^[A-Z][a-zA-Z]+$")) {
            throw new EventValidationException(
                "Event type must be PascalCase (e.g., UserCreated, ExperimentCompleted), got: " + eventType
            );
        }
    }
    
    /**
     * Validates that the timestamp is not in the future.
     * 
     * Events represent facts that have occurred, so their timestamps should not
     * be in the future. A small tolerance (60 seconds) is allowed for clock skew.
     * 
     * @param timestamp the timestamp to validate
     * @throws EventValidationException if the timestamp is invalid
     */
    private void validateTimestamp(Instant timestamp) {
        if (timestamp == null) {
            throw new EventValidationException("Timestamp is required");
        }
        
        Instant maxAllowedTime = Instant.now().plusSeconds(TIMESTAMP_TOLERANCE_SECONDS);
        if (timestamp.isAfter(maxAllowedTime)) {
            throw new EventValidationException(
                "Timestamp cannot be more than " + TIMESTAMP_TOLERANCE_SECONDS + 
                " seconds in the future, got: " + timestamp
            );
        }
    }
    
    /**
     * Validates that the organization ID is present.
     * 
     * All events must be associated with an organization for proper multi-tenant
     * data isolation and event routing.
     * 
     * @param organizationId the organization ID to validate
     * @throws EventValidationException if the organization ID is invalid
     */
    private void validateOrganizationId(String organizationId) {
        if (organizationId == null || organizationId.isBlank()) {
            throw new EventValidationException("Organization ID is required for multi-tenant isolation");
        }
    }
}
