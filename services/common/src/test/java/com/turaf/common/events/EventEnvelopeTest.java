package com.turaf.common.events;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EventEnvelope.
 * 
 * Tests cover:
 * - Event envelope creation with all fields
 * - Factory method for wrapping domain events
 * - Null validation for required fields
 * - Metadata handling
 * - Versioning support
 * - Equality and hash code contracts
 */
@DisplayName("EventEnvelope")
class EventEnvelopeTest {
    
    private TestDomainEvent testEvent;
    private String sourceService;
    private EventMetadata metadata;
    
    @BeforeEach
    void setUp() {
        testEvent = new TestDomainEvent(
            UUID.randomUUID().toString(),
            "test.EventOccurred",
            Instant.now(),
            "org-123"
        );
        sourceService = "test-service";
        metadata = new EventMetadata("correlation-123", "causation-456");
    }
    
    @Nested
    @DisplayName("Constructor")
    class ConstructorTests {
        
        @Test
        @DisplayName("should create envelope with all required fields")
        void shouldCreateEnvelopeWithAllFields() {
            EventEnvelope envelope = new EventEnvelope(
                testEvent.getEventId(),
                testEvent.getEventType(),
                1,
                testEvent.getTimestamp(),
                sourceService,
                testEvent.getOrganizationId(),
                testEvent,
                metadata
            );
            
            assertNotNull(envelope);
            assertEquals(testEvent.getEventId(), envelope.getEventId());
            assertEquals(testEvent.getEventType(), envelope.getEventType());
            assertEquals(1, envelope.getEventVersion());
            assertEquals(testEvent.getTimestamp(), envelope.getTimestamp());
            assertEquals(sourceService, envelope.getSourceService());
            assertEquals(testEvent.getOrganizationId(), envelope.getOrganizationId());
            assertEquals(testEvent, envelope.getPayload());
            assertEquals(metadata, envelope.getMetadata());
        }
        
        @Test
        @DisplayName("should create envelope with null metadata and use default")
        void shouldUseDefaultMetadataWhenNull() {
            EventEnvelope envelope = new EventEnvelope(
                testEvent.getEventId(),
                testEvent.getEventType(),
                1,
                testEvent.getTimestamp(),
                sourceService,
                testEvent.getOrganizationId(),
                testEvent,
                null
            );
            
            assertNotNull(envelope.getMetadata());
            assertNull(envelope.getMetadata().getCorrelationId());
            assertNull(envelope.getMetadata().getCausationId());
        }
        
        @Test
        @DisplayName("should throw NullPointerException when eventId is null")
        void shouldThrowWhenEventIdIsNull() {
            assertThrows(NullPointerException.class, () -> 
                new EventEnvelope(
                    null,
                    testEvent.getEventType(),
                    1,
                    testEvent.getTimestamp(),
                    sourceService,
                    testEvent.getOrganizationId(),
                    testEvent,
                    metadata
                )
            );
        }
        
        @Test
        @DisplayName("should throw NullPointerException when eventType is null")
        void shouldThrowWhenEventTypeIsNull() {
            assertThrows(NullPointerException.class, () -> 
                new EventEnvelope(
                    testEvent.getEventId(),
                    null,
                    1,
                    testEvent.getTimestamp(),
                    sourceService,
                    testEvent.getOrganizationId(),
                    testEvent,
                    metadata
                )
            );
        }
        
        @Test
        @DisplayName("should throw NullPointerException when timestamp is null")
        void shouldThrowWhenTimestampIsNull() {
            assertThrows(NullPointerException.class, () -> 
                new EventEnvelope(
                    testEvent.getEventId(),
                    testEvent.getEventType(),
                    1,
                    null,
                    sourceService,
                    testEvent.getOrganizationId(),
                    testEvent,
                    metadata
                )
            );
        }
        
        @Test
        @DisplayName("should throw NullPointerException when sourceService is null")
        void shouldThrowWhenSourceServiceIsNull() {
            assertThrows(NullPointerException.class, () -> 
                new EventEnvelope(
                    testEvent.getEventId(),
                    testEvent.getEventType(),
                    1,
                    testEvent.getTimestamp(),
                    null,
                    testEvent.getOrganizationId(),
                    testEvent,
                    metadata
                )
            );
        }
        
        @Test
        @DisplayName("should throw NullPointerException when organizationId is null")
        void shouldThrowWhenOrganizationIdIsNull() {
            assertThrows(NullPointerException.class, () -> 
                new EventEnvelope(
                    testEvent.getEventId(),
                    testEvent.getEventType(),
                    1,
                    testEvent.getTimestamp(),
                    sourceService,
                    null,
                    testEvent,
                    metadata
                )
            );
        }
        
        @Test
        @DisplayName("should throw NullPointerException when payload is null")
        void shouldThrowWhenPayloadIsNull() {
            assertThrows(NullPointerException.class, () -> 
                new EventEnvelope(
                    testEvent.getEventId(),
                    testEvent.getEventType(),
                    1,
                    testEvent.getTimestamp(),
                    sourceService,
                    testEvent.getOrganizationId(),
                    null,
                    metadata
                )
            );
        }
    }
    
    @Nested
    @DisplayName("Factory Method - wrap()")
    class WrapTests {
        
        @Test
        @DisplayName("should wrap domain event with default version and empty metadata")
        void shouldWrapDomainEvent() {
            EventEnvelope envelope = EventEnvelope.wrap(testEvent, sourceService);
            
            assertNotNull(envelope);
            assertEquals(testEvent.getEventId(), envelope.getEventId());
            assertEquals(testEvent.getEventType(), envelope.getEventType());
            assertEquals(1, envelope.getEventVersion());
            assertEquals(testEvent.getTimestamp(), envelope.getTimestamp());
            assertEquals(sourceService, envelope.getSourceService());
            assertEquals(testEvent.getOrganizationId(), envelope.getOrganizationId());
            assertEquals(testEvent, envelope.getPayload());
            assertNotNull(envelope.getMetadata());
        }
        
        @Test
        @DisplayName("should wrap domain event with custom metadata")
        void shouldWrapDomainEventWithMetadata() {
            EventEnvelope envelope = EventEnvelope.wrap(testEvent, sourceService, metadata);
            
            assertNotNull(envelope);
            assertEquals(testEvent.getEventId(), envelope.getEventId());
            assertEquals(metadata, envelope.getMetadata());
            assertEquals("correlation-123", envelope.getMetadata().getCorrelationId());
            assertEquals("causation-456", envelope.getMetadata().getCausationId());
        }
        
        @Test
        @DisplayName("should wrap domain event with custom version")
        void shouldWrapDomainEventWithVersion() {
            EventEnvelope envelope = EventEnvelope.wrap(testEvent, sourceService, 2);
            
            assertNotNull(envelope);
            assertEquals(2, envelope.getEventVersion());
            assertEquals(testEvent.getEventId(), envelope.getEventId());
        }
        
        @Test
        @DisplayName("should throw NullPointerException when event is null")
        void shouldThrowWhenEventIsNull() {
            assertThrows(NullPointerException.class, () -> 
                EventEnvelope.wrap(null, sourceService)
            );
        }
        
        @Test
        @DisplayName("should throw NullPointerException when sourceService is null")
        void shouldThrowWhenSourceServiceIsNullInWrap() {
            assertThrows(NullPointerException.class, () -> 
                EventEnvelope.wrap(testEvent, null)
            );
        }
    }
    
    @Nested
    @DisplayName("Versioning")
    class VersioningTests {
        
        @Test
        @DisplayName("should support different event versions")
        void shouldSupportVersioning() {
            EventEnvelope v1 = EventEnvelope.wrap(testEvent, sourceService, 1);
            EventEnvelope v2 = EventEnvelope.wrap(testEvent, sourceService, 2);
            EventEnvelope v3 = EventEnvelope.wrap(testEvent, sourceService, 3);
            
            assertEquals(1, v1.getEventVersion());
            assertEquals(2, v2.getEventVersion());
            assertEquals(3, v3.getEventVersion());
        }
    }
    
    @Nested
    @DisplayName("Equality and HashCode")
    class EqualityTests {
        
        @Test
        @DisplayName("should be equal when all fields match")
        void shouldBeEqualWhenFieldsMatch() {
            EventEnvelope envelope1 = EventEnvelope.wrap(testEvent, sourceService, metadata);
            EventEnvelope envelope2 = EventEnvelope.wrap(testEvent, sourceService, metadata);
            
            assertEquals(envelope1, envelope2);
            assertEquals(envelope1.hashCode(), envelope2.hashCode());
        }
        
        @Test
        @DisplayName("should not be equal when eventId differs")
        void shouldNotBeEqualWhenEventIdDiffers() {
            EventEnvelope envelope1 = EventEnvelope.wrap(testEvent, sourceService);
            
            TestDomainEvent differentEvent = new TestDomainEvent(
                UUID.randomUUID().toString(),
                testEvent.getEventType(),
                testEvent.getTimestamp(),
                testEvent.getOrganizationId()
            );
            EventEnvelope envelope2 = EventEnvelope.wrap(differentEvent, sourceService);
            
            assertNotEquals(envelope1, envelope2);
        }
        
        @Test
        @DisplayName("should not be equal when version differs")
        void shouldNotBeEqualWhenVersionDiffers() {
            EventEnvelope envelope1 = EventEnvelope.wrap(testEvent, sourceService, 1);
            EventEnvelope envelope2 = EventEnvelope.wrap(testEvent, sourceService, 2);
            
            assertNotEquals(envelope1, envelope2);
        }
        
        @Test
        @DisplayName("should handle null in equals")
        void shouldHandleNullInEquals() {
            EventEnvelope envelope = EventEnvelope.wrap(testEvent, sourceService);
            
            assertNotEquals(null, envelope);
        }
        
        @Test
        @DisplayName("should be equal to itself")
        void shouldBeEqualToItself() {
            EventEnvelope envelope = EventEnvelope.wrap(testEvent, sourceService);
            
            assertEquals(envelope, envelope);
        }
    }
    
    @Nested
    @DisplayName("toString()")
    class ToStringTests {
        
        @Test
        @DisplayName("should return string representation with key fields")
        void shouldReturnStringRepresentation() {
            EventEnvelope envelope = EventEnvelope.wrap(testEvent, sourceService, metadata);
            String result = envelope.toString();
            
            assertNotNull(result);
            assertTrue(result.contains("EventEnvelope"));
            assertTrue(result.contains(testEvent.getEventId()));
            assertTrue(result.contains(testEvent.getEventType()));
            assertTrue(result.contains(sourceService));
            assertTrue(result.contains(testEvent.getOrganizationId()));
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
