package com.turaf.organization.infrastructure.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.turaf.common.domain.DomainEvent;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps domain events to JSON format for EventBridge.
 * Creates an envelope structure with metadata and payload.
 */
@Component
public class EventMapper {
    
    private final ObjectMapper objectMapper;
    
    public EventMapper() {
        this.objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
    
    /**
     * Convert domain event to JSON string with envelope structure.
     *
     * @param event The domain event
     * @return JSON string
     * @throws EventMappingException if serialization fails
     */
    public String toJson(DomainEvent event) {
        try {
            Map<String, Object> envelope = new HashMap<>();
            envelope.put("eventId", event.getEventId());
            envelope.put("eventType", event.getEventType());
            envelope.put("eventVersion", 1);
            envelope.put("timestamp", event.getOccurredAt());
            envelope.put("sourceService", "organization-service");
            envelope.put("organizationId", event.getOrganizationId());
            envelope.put("payload", event);
            
            return objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException e) {
            throw new EventMappingException("Failed to serialize event: " + event.getEventType(), e);
        }
    }
    
    /**
     * Get the ObjectMapper instance for testing purposes.
     */
    ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
