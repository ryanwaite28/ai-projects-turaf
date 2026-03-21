package com.turaf.common.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EventSerializer.
 * 
 * Tests cover:
 * - Event envelope serialization to JSON
 * - Event envelope deserialization from JSON
 * - Java 8 time type handling
 * - Error handling for invalid JSON
 */
@DisplayName("EventSerializer")
class EventSerializerTest {
    
    private EventSerializer serializer;
    private TestDomainEvent testEvent;
    private EventEnvelope testEnvelope;
    
    @BeforeEach
    void setUp() {
        serializer = new EventSerializer();
        
        testEvent = new TestDomainEvent(
            UUID.randomUUID().toString(),
            "test.EventOccurred",
            Instant.parse("2024-03-20T12:00:00Z"),
            "org-123"
        );
        
        testEnvelope = EventEnvelope.wrap(testEvent, "test-service");
    }
    
    @Nested
    @DisplayName("Serialization")
    class SerializationTests {
        
        @Test
        @DisplayName("should serialize event envelope to JSON")
        void shouldSerializeEventEnvelope() {
            String json = serializer.serialize(testEnvelope);
            
            assertNotNull(json);
            assertTrue(json.contains(testEvent.getEventId()));
            assertTrue(json.contains(testEvent.getEventType()));
            assertTrue(json.contains("test-service"));
            assertTrue(json.contains("org-123"));
        }
        
        @Test
        @DisplayName("should serialize timestamp as ISO-8601 string")
        void shouldSerializeTimestampAsIso8601() {
            String json = serializer.serialize(testEnvelope);
            
            assertNotNull(json);
            assertTrue(json.contains("2024-03-20T12:00:00Z"));
            assertFalse(json.contains("1710936000000")); // Should not be epoch milliseconds
        }
        
        @Test
        @DisplayName("should include all envelope fields in JSON")
        void shouldIncludeAllEnvelopeFields() {
            String json = serializer.serialize(testEnvelope);
            
            assertNotNull(json);
            assertTrue(json.contains("eventId"));
            assertTrue(json.contains("eventType"));
            assertTrue(json.contains("eventVersion"));
            assertTrue(json.contains("timestamp"));
            assertTrue(json.contains("sourceService"));
            assertTrue(json.contains("organizationId"));
            assertTrue(json.contains("payload"));
            assertTrue(json.contains("metadata"));
        }
        
        @Test
        @DisplayName("should throw EventPublishException when serialization fails")
        void shouldThrowWhenSerializationFails() {
            // Create a serializer with a broken ObjectMapper
            ObjectMapper brokenMapper = new ObjectMapper() {
                @Override
                public String writeValueAsString(Object value) {
                    throw new RuntimeException("Serialization error");
                }
            };
            
            EventSerializer brokenSerializer = new EventSerializer(brokenMapper);
            
            assertThrows(EventPublishException.class, () -> 
                brokenSerializer.serialize(testEnvelope)
            );
        }
    }
    
    @Nested
    @DisplayName("Deserialization")
    class DeserializationTests {
        
        @Test
        @DisplayName("should deserialize JSON to event envelope")
        void shouldDeserializeEventEnvelope() {
            String json = serializer.serialize(testEnvelope);
            EventEnvelope deserialized = serializer.deserialize(json);
            
            assertNotNull(deserialized);
            assertEquals(testEnvelope.getEventId(), deserialized.getEventId());
            assertEquals(testEnvelope.getEventType(), deserialized.getEventType());
            assertEquals(testEnvelope.getEventVersion(), deserialized.getEventVersion());
            assertEquals(testEnvelope.getTimestamp(), deserialized.getTimestamp());
            assertEquals(testEnvelope.getSourceService(), deserialized.getSourceService());
            assertEquals(testEnvelope.getOrganizationId(), deserialized.getOrganizationId());
        }
        
        @Test
        @DisplayName("should deserialize ISO-8601 timestamp correctly")
        void shouldDeserializeTimestamp() {
            String json = serializer.serialize(testEnvelope);
            EventEnvelope deserialized = serializer.deserialize(json);
            
            assertNotNull(deserialized);
            assertEquals(Instant.parse("2024-03-20T12:00:00Z"), deserialized.getTimestamp());
        }
        
        @Test
        @DisplayName("should throw EventPublishException for invalid JSON")
        void shouldThrowForInvalidJson() {
            String invalidJson = "{invalid json}";
            
            assertThrows(EventPublishException.class, () -> 
                serializer.deserialize(invalidJson)
            );
        }
        
        @Test
        @DisplayName("should throw EventPublishException for null JSON")
        void shouldThrowForNullJson() {
            assertThrows(EventPublishException.class, () -> 
                serializer.deserialize(null)
            );
        }
        
        @Test
        @DisplayName("should throw EventPublishException for empty JSON")
        void shouldThrowForEmptyJson() {
            assertThrows(EventPublishException.class, () -> 
                serializer.deserialize("")
            );
        }
    }
    
    @Nested
    @DisplayName("Round-trip Serialization")
    class RoundTripTests {
        
        @Test
        @DisplayName("should maintain data integrity through serialize-deserialize cycle")
        void shouldMaintainDataIntegrity() {
            String json = serializer.serialize(testEnvelope);
            EventEnvelope deserialized = serializer.deserialize(json);
            
            assertEquals(testEnvelope.getEventId(), deserialized.getEventId());
            assertEquals(testEnvelope.getEventType(), deserialized.getEventType());
            assertEquals(testEnvelope.getEventVersion(), deserialized.getEventVersion());
            assertEquals(testEnvelope.getTimestamp(), deserialized.getTimestamp());
            assertEquals(testEnvelope.getSourceService(), deserialized.getSourceService());
            assertEquals(testEnvelope.getOrganizationId(), deserialized.getOrganizationId());
        }
        
        @Test
        @DisplayName("should handle metadata through round-trip")
        void shouldHandleMetadataRoundTrip() {
            EventMetadata metadata = new EventMetadata("correlation-123", "causation-456");
            metadata.addMetadata("key1", "value1");
            
            EventEnvelope envelopeWithMetadata = EventEnvelope.wrap(testEvent, "test-service", metadata);
            
            String json = serializer.serialize(envelopeWithMetadata);
            EventEnvelope deserialized = serializer.deserialize(json);
            
            assertNotNull(deserialized.getMetadata());
            assertEquals("correlation-123", deserialized.getMetadata().getCorrelationId());
            assertEquals("causation-456", deserialized.getMetadata().getCausationId());
            assertEquals("value1", deserialized.getMetadata().getMetadata("key1"));
        }
    }
    
    @Nested
    @DisplayName("ObjectMapper Configuration")
    class ObjectMapperConfigTests {
        
        @Test
        @DisplayName("should configure ObjectMapper with JavaTimeModule")
        void shouldConfigureJavaTimeModule() {
            EventSerializer newSerializer = new EventSerializer();
            
            // Verify it can handle Instant without errors
            String json = newSerializer.serialize(testEnvelope);
            assertNotNull(json);
            assertTrue(json.contains("2024-03-20T12:00:00Z"));
        }
        
        @Test
        @DisplayName("should disable WRITE_DATES_AS_TIMESTAMPS")
        void shouldDisableWriteDatesAsTimestamps() {
            ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            
            EventSerializer customSerializer = new EventSerializer(mapper);
            String json = customSerializer.serialize(testEnvelope);
            
            // Should be ISO-8601 string, not epoch milliseconds
            assertTrue(json.contains("2024-03-20T12:00:00Z"));
            assertFalse(json.matches(".*\"timestamp\":\\s*\\d+.*"));
        }
    }
    
    /**
     * Test implementation of DomainEvent for testing purposes.
     */
    private static class TestDomainEvent implements DomainEvent {
        private final String eventId;
        private final String eventType;
        private final Instant timestamp;
        private final String organizationId;
        
        public TestDomainEvent(String eventId, String eventType, Instant timestamp, String organizationId) {
            this.eventId = eventId;
            this.eventType = eventType;
            this.timestamp = timestamp;
            this.organizationId = organizationId;
        }
        
        @Override
        public String getEventId() {
            return eventId;
        }
        
        @Override
        public String getEventType() {
            return eventType;
        }
        
        @Override
        public Instant getTimestamp() {
            return timestamp;
        }
        
        @Override
        public String getOrganizationId() {
            return organizationId;
        }
    }
}
