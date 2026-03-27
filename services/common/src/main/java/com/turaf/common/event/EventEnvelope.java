package com.turaf.common.event;

import com.turaf.common.domain.DomainEvent;

import java.time.Instant;
import java.util.Objects;

/**
 * Standardized envelope for wrapping domain events before publishing.
 * 
 * The EventEnvelope provides a consistent structure for all events published to EventBridge:
 * - Event identification and versioning
 * - Source service tracking
 * - Multi-tenant organization context
 * - Correlation and causation metadata
 * - Payload containing the actual domain event
 * 
 * This follows the Envelope pattern for event-driven architectures, enabling:
 * - Event schema evolution through versioning
 * - Consistent event routing and filtering
 * - Distributed tracing across services
 * - Multi-tenant data isolation
 * 
 * Following SOLID principles:
 * - Single Responsibility: Wraps events with metadata
 * - Open/Closed: Extensible through metadata without modification
 * - Immutable: All fields are final and set at construction
 */
public class EventEnvelope {
    
    private final String eventId;
    private final String eventType;
    private final int eventVersion;
    private final Instant timestamp;
    private final String sourceService;
    private final String organizationId;
    private final Object payload;
    private final EventMetadata metadata;
    
    /**
     * Constructs an EventEnvelope with all required fields.
     * 
     * @param eventId unique identifier for the event
     * @param eventType type identifier for the event
     * @param eventVersion version number for schema evolution
     * @param timestamp when the event occurred
     * @param sourceService service that published the event
     * @param organizationId organization context for multi-tenancy
     * @param payload the actual domain event
     * @param metadata correlation and causation tracking metadata
     * @throws NullPointerException if any required field is null
     */
    public EventEnvelope(String eventId, String eventType, int eventVersion,
                        Instant timestamp, String sourceService, String organizationId,
                        Object payload, EventMetadata metadata) {
        this.eventId = Objects.requireNonNull(eventId, "Event ID cannot be null");
        this.eventType = Objects.requireNonNull(eventType, "Event type cannot be null");
        this.eventVersion = eventVersion;
        this.timestamp = Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        this.sourceService = Objects.requireNonNull(sourceService, "Source service cannot be null");
        this.organizationId = Objects.requireNonNull(organizationId, "Organization ID cannot be null");
        this.payload = Objects.requireNonNull(payload, "Payload cannot be null");
        this.metadata = metadata != null ? metadata : new EventMetadata();
    }
    
    /**
     * Factory method to wrap a DomainEvent in an EventEnvelope.
     * 
     * This is the primary way to create envelopes from domain events.
     * Uses default version 1 and creates empty metadata.
     * 
     * @param event the domain event to wrap
     * @param sourceService the service publishing the event
     * @return a new EventEnvelope wrapping the domain event
     * @throws NullPointerException if event or sourceService is null
     */
    public static EventEnvelope wrap(DomainEvent event, String sourceService) {
        Objects.requireNonNull(event, "Domain event cannot be null");
        Objects.requireNonNull(sourceService, "Source service cannot be null");
        
        return new EventEnvelope(
            event.getEventId(),
            event.getEventType(),
            1,
            event.getOccurredAt(),
            sourceService,
            event.getOrganizationId(),
            event,
            new EventMetadata()
        );
    }
    
    /**
     * Factory method to wrap a DomainEvent with custom metadata.
     * 
     * Use this when you need to propagate correlation/causation IDs
     * or add custom metadata to the event.
     * 
     * @param event the domain event to wrap
     * @param sourceService the service publishing the event
     * @param metadata custom metadata for the event
     * @return a new EventEnvelope wrapping the domain event
     * @throws NullPointerException if event or sourceService is null
     */
    public static EventEnvelope wrap(DomainEvent event, String sourceService, EventMetadata metadata) {
        Objects.requireNonNull(event, "Domain event cannot be null");
        Objects.requireNonNull(sourceService, "Source service cannot be null");
        
        return new EventEnvelope(
            event.getEventId(),
            event.getEventType(),
            1,
            event.getOccurredAt(),
            sourceService,
            event.getOrganizationId(),
            event,
            metadata
        );
    }
    
    /**
     * Factory method to wrap a DomainEvent with a specific version.
     * 
     * Use this when publishing an updated version of an event schema.
     * 
     * @param event the domain event to wrap
     * @param sourceService the service publishing the event
     * @param version the event schema version
     * @return a new EventEnvelope wrapping the domain event
     * @throws NullPointerException if event or sourceService is null
     */
    public static EventEnvelope wrap(DomainEvent event, String sourceService, int version) {
        Objects.requireNonNull(event, "Domain event cannot be null");
        Objects.requireNonNull(sourceService, "Source service cannot be null");
        
        return new EventEnvelope(
            event.getEventId(),
            event.getEventType(),
            version,
            event.getOccurredAt(),
            sourceService,
            event.getOrganizationId(),
            event,
            new EventMetadata()
        );
    }
    
    // Getters
    
    public String getEventId() {
        return eventId;
    }
    
    public String getEventType() {
        return eventType;
    }
    
    public int getEventVersion() {
        return eventVersion;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public String getSourceService() {
        return sourceService;
    }
    
    public String getOrganizationId() {
        return organizationId;
    }
    
    public Object getPayload() {
        return payload;
    }
    
    public EventMetadata getMetadata() {
        return metadata;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventEnvelope that = (EventEnvelope) o;
        return eventVersion == that.eventVersion &&
               Objects.equals(eventId, that.eventId) &&
               Objects.equals(eventType, that.eventType) &&
               Objects.equals(timestamp, that.timestamp) &&
               Objects.equals(sourceService, that.sourceService) &&
               Objects.equals(organizationId, that.organizationId) &&
               Objects.equals(payload, that.payload) &&
               Objects.equals(metadata, that.metadata);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(eventId, eventType, eventVersion, timestamp, 
                          sourceService, organizationId, payload, metadata);
    }
    
    @Override
    public String toString() {
        return "EventEnvelope{" +
               "eventId='" + eventId + '\'' +
               ", eventType='" + eventType + '\'' +
               ", eventVersion=" + eventVersion +
               ", timestamp=" + timestamp +
               ", sourceService='" + sourceService + '\'' +
               ", organizationId='" + organizationId + '\'' +
               ", metadata=" + metadata +
               '}';
    }
}
