package com.turaf.organization.domain.event;

import com.turaf.common.domain.DomainEvent;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event fired when an organization is updated.
 */
public class OrganizationUpdated implements DomainEvent {
    
    private final String eventId;
    private final String organizationId;
    private final String fieldName;
    private final String newValue;
    private final Instant timestamp;
    private final String correlationId;
    
    public OrganizationUpdated(String eventId, String organizationId, String fieldName,
                              String newValue, Instant timestamp) {
        this.eventId = Objects.requireNonNull(eventId, "Event ID cannot be null");
        this.organizationId = Objects.requireNonNull(organizationId, "Organization ID cannot be null");
        this.fieldName = Objects.requireNonNull(fieldName, "Field name cannot be null");
        this.newValue = newValue;
        this.timestamp = Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        this.correlationId = eventId;
    }
    
    @Override
    public String getEventId() {
        return eventId;
    }
    
    @Override
    public String getEventType() {
        return "OrganizationUpdated";
    }
    
    @Override
    public Instant getOccurredAt() {
        return timestamp;
    }
    
    @Override
    public String getOrganizationId() {
        return organizationId;
    }
    
    @Override
    public String getCorrelationId() {
        return correlationId;
    }
    
    public String getFieldName() {
        return fieldName;
    }
    
    public String getNewValue() {
        return newValue;
    }
}
