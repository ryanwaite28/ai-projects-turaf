package com.turaf.organization.domain.event;

import com.turaf.common.domain.DomainEvent;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain event fired when a member is removed from an organization.
 */
public class MemberRemoved implements DomainEvent {
    
    private final String eventId;
    private final String organizationId;
    private final String userId;
    private final String removedBy;
    private final Instant timestamp;
    private final String correlationId;
    
    public MemberRemoved(String eventId, String organizationId, String userId,
                        String removedBy, Instant timestamp) {
        this.eventId = Objects.requireNonNull(eventId, "Event ID cannot be null");
        this.organizationId = Objects.requireNonNull(organizationId, "Organization ID cannot be null");
        this.userId = Objects.requireNonNull(userId, "User ID cannot be null");
        this.removedBy = Objects.requireNonNull(removedBy, "Removed by cannot be null");
        this.timestamp = Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        this.correlationId = eventId;
    }
    
    @Override
    public String getEventId() {
        return eventId;
    }
    
    @Override
    public String getEventType() {
        return "MemberRemoved";
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
    
    public String getUserId() {
        return userId;
    }
    
    public String getRemovedBy() {
        return removedBy;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MemberRemoved that = (MemberRemoved) o;
        return Objects.equals(eventId, that.eventId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(eventId);
    }
}
