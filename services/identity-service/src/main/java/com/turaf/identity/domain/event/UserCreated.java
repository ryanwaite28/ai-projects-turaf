package com.turaf.identity.domain.event;

import com.turaf.common.domain.DomainEvent;

import java.time.Instant;
import java.util.Objects;

public class UserCreated implements DomainEvent {
    
    private final String eventId;
    private final String userId;
    private final String organizationId;
    private final String email;
    private final String name;
    private final Instant timestamp;
    private final String correlationId;
    
    public UserCreated(String eventId, String userId, String organizationId, String email, String name) {
        this.eventId = Objects.requireNonNull(eventId, "Event ID cannot be null");
        this.userId = Objects.requireNonNull(userId, "User ID cannot be null");
        this.organizationId = Objects.requireNonNull(organizationId, "Organization ID cannot be null");
        this.email = Objects.requireNonNull(email, "Email cannot be null");
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        this.timestamp = Instant.now();
        this.correlationId = eventId;
    }

    @Override
    public String getEventId() {
        return eventId;
    }
    
    @Override
    public String getEventType() {
        return "UserCreated";
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
    
    public String getEmail() {
        return email;
    }
    
    public String getName() {
        return name;
    }
}
