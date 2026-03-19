package com.turaf.identity.domain.event;

import com.turaf.common.domain.DomainEvent;

import java.time.Instant;
import java.util.Objects;

public class UserProfileUpdated implements DomainEvent {
    
    private final String eventId;
    private final String userId;
    private final String organizationId;
    private final String name;
    private final Instant timestamp;

    public UserProfileUpdated(String eventId, String userId, String organizationId, String name, Instant timestamp) {
        this.eventId = Objects.requireNonNull(eventId, "Event ID cannot be null");
        this.userId = Objects.requireNonNull(userId, "User ID cannot be null");
        this.organizationId = Objects.requireNonNull(organizationId, "Organization ID cannot be null");
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        this.timestamp = Objects.requireNonNull(timestamp, "Timestamp cannot be null");
    }

    @Override
    public String getEventId() {
        return eventId;
    }
    
    @Override
    public String getEventType() {
        return "UserProfileUpdated";
    }

    @Override
    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public String getOrganizationId() {
        return organizationId;
    }

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }
}
