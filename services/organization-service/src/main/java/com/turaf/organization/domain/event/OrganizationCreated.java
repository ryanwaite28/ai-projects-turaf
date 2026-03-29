package com.turaf.organization.domain.event;

import com.turaf.common.domain.DomainEvent;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain event fired when an organization is created.
 */
public class OrganizationCreated implements DomainEvent {
    
    private final String eventId;
    private final String organizationId;
    private final String name;
    private final String slug;
    private final String createdBy;
    private final Instant timestamp;
    private final String correlationId;
    
    public OrganizationCreated(String eventId, String organizationId, String name,
                              String slug, String createdBy, Instant timestamp) {
        this.eventId = Objects.requireNonNull(eventId, "Event ID cannot be null");
        this.organizationId = Objects.requireNonNull(organizationId, "Organization ID cannot be null");
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        this.slug = Objects.requireNonNull(slug, "Slug cannot be null");
        this.createdBy = Objects.requireNonNull(createdBy, "Created by cannot be null");
        this.timestamp = Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        this.correlationId = eventId;
    }
    
    @Override
    public String getEventId() {
        return eventId;
    }
    
    @Override
    public String getEventType() {
        return "OrganizationCreated";
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
    
    public String getName() {
        return name;
    }
    
    public String getSlug() {
        return slug;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrganizationCreated that = (OrganizationCreated) o;
        return Objects.equals(eventId, that.eventId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(eventId);
    }
}
