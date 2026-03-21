package com.turaf.common.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;

/**
 * Serializer for converting EventEnvelope objects to/from JSON.
 * 
 * This component handles JSON serialization for event publishing and consumption:
 * - Serializes EventEnvelope to JSON for EventBridge detail field
 * - Deserializes JSON back to EventEnvelope for event consumers
 * - Properly handles Java 8 time types (Instant, LocalDateTime, etc.)
 * - Formats timestamps as ISO-8601 strings, not epoch milliseconds
 * 
 * Following Spring Boot best practices:
 * - Configured as a Spring component for dependency injection
 * - Uses Jackson ObjectMapper for JSON processing
 * - Registers JavaTimeModule for Java 8 date/time support
 */
@Component
public class EventSerializer {
    
    private final ObjectMapper objectMapper;
    
    /**
     * Constructs an EventSerializer with properly configured ObjectMapper.
     * 
     * Configuration:
     * - JavaTimeModule: Handles Java 8 date/time types
     * - WRITE_DATES_AS_TIMESTAMPS disabled: Writes dates as ISO-8601 strings
     */
    public EventSerializer() {
        this.objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
    
    /**
     * Constructor for testing with custom ObjectMapper.
     * 
     * @param objectMapper custom ObjectMapper instance
     */
    EventSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    /**
     * Serializes an EventEnvelope to JSON string.
     * 
     * The resulting JSON will be used as the 'detail' field in EventBridge events.
     * 
     * @param envelope the event envelope to serialize
     * @return JSON string representation of the envelope
     * @throws EventPublishException if serialization fails
     */
    public String serialize(EventEnvelope envelope) {
        try {
            return objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException e) {
            throw new EventPublishException("Failed to serialize event envelope: " + envelope.getEventType(), e);
        }
    }
    
    /**
     * Deserializes a JSON string to an EventEnvelope.
     * 
     * Used by event consumers to parse EventBridge event details.
     * 
     * @param json the JSON string to deserialize
     * @return the deserialized EventEnvelope
     * @throws EventPublishException if deserialization fails
     */
    public EventEnvelope deserialize(String json) {
        try {
            return objectMapper.readValue(json, EventEnvelope.class);
        } catch (JsonProcessingException e) {
            throw new EventPublishException("Failed to deserialize event envelope from JSON", e);
        }
    }
}
