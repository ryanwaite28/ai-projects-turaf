package com.turaf.identity.domain.event;

import com.turaf.common.domain.DomainEvent;

import java.time.Instant;

public class UserCreated extends DomainEvent {
    
    private final String userId;
    private final String email;
    private final String name;

    public UserCreated(String eventId, String userId, String email, String name) {
        super(eventId, Instant.now());
        this.userId = userId;
        this.email = email;
        this.name = name;
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
