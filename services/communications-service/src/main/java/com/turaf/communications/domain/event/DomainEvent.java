package com.turaf.communications.domain.event;

import lombok.Getter;
import java.time.Instant;
import java.util.UUID;

@Getter
public abstract class DomainEvent {
    private final String eventId;
    private final Instant occurredAt;
    
    protected DomainEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.occurredAt = Instant.now();
    }
}
