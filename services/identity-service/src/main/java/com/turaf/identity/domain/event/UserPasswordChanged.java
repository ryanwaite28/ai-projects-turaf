package com.turaf.identity.domain.event;

import com.turaf.common.domain.DomainEvent;

import java.time.Instant;

public class UserPasswordChanged extends DomainEvent {
    
    private final String userId;
    private final Instant changedAt;

    public UserPasswordChanged(String eventId, String userId, Instant changedAt) {
        super(eventId, Instant.now());
        this.userId = userId;
        this.changedAt = changedAt;
    }

    public String getUserId() {
        return userId;
    }

    public Instant getChangedAt() {
        return changedAt;
    }
}
