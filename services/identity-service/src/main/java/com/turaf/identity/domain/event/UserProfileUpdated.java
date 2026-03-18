package com.turaf.identity.domain.event;

import com.turaf.common.domain.DomainEvent;

import java.time.Instant;

public class UserProfileUpdated extends DomainEvent {
    
    private final String userId;
    private final String name;
    private final Instant updatedAt;

    public UserProfileUpdated(String eventId, String userId, String name, Instant updatedAt) {
        super(eventId, Instant.now());
        this.userId = userId;
        this.name = name;
        this.updatedAt = updatedAt;
    }

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
