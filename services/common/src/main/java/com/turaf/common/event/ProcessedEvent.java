package com.turaf.common.event;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Entity to track processed events for idempotency.
 * Prevents duplicate processing of the same event.
 */
@Entity
@Table(name = "processed_events", indexes = {
    @Index(name = "idx_event_id", columnList = "event_id", unique = true),
    @Index(name = "idx_processed_at", columnList = "processed_at")
})
public class ProcessedEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "event_id", nullable = false, unique = true, length = 100)
    private String eventId;
    
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;
    
    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;
    
    @Column(name = "organization_id", length = 50)
    private String organizationId;
    
    protected ProcessedEvent() {
        // JPA requires a no-arg constructor
    }
    
    public ProcessedEvent(String eventId, String eventType, String organizationId) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.organizationId = organizationId;
        this.processedAt = Instant.now();
    }
    
    // Getters
    public Long getId() {
        return id;
    }
    
    public String getEventId() {
        return eventId;
    }
    
    public String getEventType() {
        return eventType;
    }
    
    public Instant getProcessedAt() {
        return processedAt;
    }
    
    public String getOrganizationId() {
        return organizationId;
    }
}
