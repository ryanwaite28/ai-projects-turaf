package com.turaf.common.events;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EventValidator.
 * 
 * Tests cover:
 * - Valid events pass validation
 * - Invalid event IDs fail validation
 * - Invalid event types fail validation
 * - Future timestamps fail validation
 * - Missing required fields fail validation
 * - EventEnvelope validation
 */
@DisplayName("EventValidator")
class EventValidatorTest {
    
    private EventValidator validator;
    private String validEventId;
    private String validEventType;
    private Instant validTimestamp;
    private String validOrganizationId;
    
    @BeforeEach
    void setUp() {
        validator = new EventValidator();
        validEventId = UUID.randomUUID().toString();
        validEventType = "UserCreated";
        validTimestamp = Instant.now();
        validOrganizationId = "org-123";
    }
    
    @Nested
    @DisplayName("DomainEvent Validation")
    class DomainEventValidationTests {
        
        @Test
        @DisplayName("should pass validation for valid domain event")
        void shouldPassValidationForValidEvent() {
            TestDomainEvent event = new TestDomainEvent(
                validEventId, validEventType, validTimestamp, validOrganizationId
            );
            
            assertDoesNotThrow(() -> validator.validate(event));
        }
        
        @Test
        @DisplayName("should throw when domain event is null")
        void shouldThrowWhenEventIsNull() {
            EventValidationException exception = assertThrows(
                EventValidationException.class,
                () -> validator.validate((DomainEvent) null)
            );
            
            assertTrue(exception.getMessage().contains("Event cannot be null"));
        }
        
        @Test
        @DisplayName("should throw when event ID is null")
        void shouldThrowWhenEventIdIsNull() {
            TestDomainEvent event = new TestDomainEvent(
                null, validEventType, validTimestamp, validOrganizationId
            );
            
            EventValidationException exception = assertThrows(
                EventValidationException.class,
                () -> validator.validate(event)
            );
            
            assertTrue(exception.getMessage().contains("Event ID is required"));
        }
        
        @Test
        @DisplayName("should throw when event ID is blank")
        void shouldThrowWhenEventIdIsBlank() {
            TestDomainEvent event = new TestDomainEvent(
                "   ", validEventType, validTimestamp, validOrganizationId
            );
            
            EventValidationException exception = assertThrows(
                EventValidationException.class,
                () -> validator.validate(event)
            );
            
            assertTrue(exception.getMessage().contains("Event ID is required"));
        }
        
        @Test
        @DisplayName("should throw when event ID is not a valid UUID")
        void shouldThrowWhenEventIdIsNotUUID() {
            TestDomainEvent event = new TestDomainEvent(
                "not-a-uuid", validEventType, validTimestamp, validOrganizationId
            );
            
            EventValidationException exception = assertThrows(
                EventValidationException.class,
                () -> validator.validate(event)
            );
            
            assertTrue(exception.getMessage().contains("must be a valid UUID"));
        }
        
        @Test
        @DisplayName("should throw when event type is null")
        void shouldThrowWhenEventTypeIsNull() {
            TestDomainEvent event = new TestDomainEvent(
                validEventId, null, validTimestamp, validOrganizationId
            );
            
            EventValidationException exception = assertThrows(
                EventValidationException.class,
                () -> validator.validate(event)
            );
            
            assertTrue(exception.getMessage().contains("Event type is required"));
        }
        
        @Test
        @DisplayName("should throw when event type is blank")
        void shouldThrowWhenEventTypeIsBlank() {
            TestDomainEvent event = new TestDomainEvent(
                validEventId, "   ", validTimestamp, validOrganizationId
            );
            
            EventValidationException exception = assertThrows(
                EventValidationException.class,
                () -> validator.validate(event)
            );
            
            assertTrue(exception.getMessage().contains("Event type is required"));
        }
        
        @Test
        @DisplayName("should throw when event type is not PascalCase")
        void shouldThrowWhenEventTypeIsNotPascalCase() {
            TestDomainEvent event = new TestDomainEvent(
                validEventId, "user_created", validTimestamp, validOrganizationId
            );
            
            EventValidationException exception = assertThrows(
                EventValidationException.class,
                () -> validator.validate(event)
            );
            
            assertTrue(exception.getMessage().contains("must be PascalCase"));
        }
        
        @Test
        @DisplayName("should throw when event type starts with lowercase")
        void shouldThrowWhenEventTypeStartsWithLowercase() {
            TestDomainEvent event = new TestDomainEvent(
                validEventId, "userCreated", validTimestamp, validOrganizationId
            );
            
            EventValidationException exception = assertThrows(
                EventValidationException.class,
                () -> validator.validate(event)
            );
            
            assertTrue(exception.getMessage().contains("must be PascalCase"));
        }
        
        @Test
        @DisplayName("should throw when event type contains numbers")
        void shouldThrowWhenEventTypeContainsNumbers() {
            TestDomainEvent event = new TestDomainEvent(
                validEventId, "User123Created", validTimestamp, validOrganizationId
            );
            
            EventValidationException exception = assertThrows(
                EventValidationException.class,
                () -> validator.validate(event)
            );
            
            assertTrue(exception.getMessage().contains("must be PascalCase"));
        }
        
        @Test
        @DisplayName("should throw when timestamp is null")
        void shouldThrowWhenTimestampIsNull() {
            TestDomainEvent event = new TestDomainEvent(
                validEventId, validEventType, null, validOrganizationId
            );
            
            EventValidationException exception = assertThrows(
                EventValidationException.class,
                () -> validator.validate(event)
            );
            
            assertTrue(exception.getMessage().contains("Timestamp is required"));
        }
        
        @Test
        @DisplayName("should throw when timestamp is in the future")
        void shouldThrowWhenTimestampIsInFuture() {
            Instant futureTimestamp = Instant.now().plus(2, ChronoUnit.MINUTES);
            TestDomainEvent event = new TestDomainEvent(
                validEventId, validEventType, futureTimestamp, validOrganizationId
            );
            
            EventValidationException exception = assertThrows(
                EventValidationException.class,
                () -> validator.validate(event)
            );
            
            assertTrue(exception.getMessage().contains("cannot be more than"));
            assertTrue(exception.getMessage().contains("in the future"));
        }
        
        @Test
        @DisplayName("should allow timestamp within tolerance (60 seconds)")
        void shouldAllowTimestampWithinTolerance() {
            Instant nearFutureTimestamp = Instant.now().plus(30, ChronoUnit.SECONDS);
            TestDomainEvent event = new TestDomainEvent(
                validEventId, validEventType, nearFutureTimestamp, validOrganizationId
            );
            
            assertDoesNotThrow(() -> validator.validate(event));
        }
        
        @Test
        @DisplayName("should throw when organization ID is null")
        void shouldThrowWhenOrganizationIdIsNull() {
            TestDomainEvent event = new TestDomainEvent(
                validEventId, validEventType, validTimestamp, null
            );
            
            EventValidationException exception = assertThrows(
                EventValidationException.class,
                () -> validator.validate(event)
            );
            
            assertTrue(exception.getMessage().contains("Organization ID is required"));
        }
        
        @Test
        @DisplayName("should throw when organization ID is blank")
        void shouldThrowWhenOrganizationIdIsBlank() {
            TestDomainEvent event = new TestDomainEvent(
                validEventId, validEventType, validTimestamp, "   "
            );
            
            EventValidationException exception = assertThrows(
                EventValidationException.class,
                () -> validator.validate(event)
            );
            
            assertTrue(exception.getMessage().contains("Organization ID is required"));
        }
    }
    
    @Nested
    @DisplayName("EventEnvelope Validation")
    class EventEnvelopeValidationTests {
        
        private TestDomainEvent validEvent;
        
        @BeforeEach
        void setUp() {
            validEvent = new TestDomainEvent(
                validEventId, validEventType, validTimestamp, validOrganizationId
            );
        }
        
        @Test
        @DisplayName("should pass validation for valid event envelope")
        void shouldPassValidationForValidEnvelope() {
            EventEnvelope envelope = EventEnvelope.wrap(validEvent, "test-service");
            
            assertDoesNotThrow(() -> validator.validate(envelope));
        }
        
        @Test
        @DisplayName("should throw when envelope is null")
        void shouldThrowWhenEnvelopeIsNull() {
            EventValidationException exception = assertThrows(
                EventValidationException.class,
                () -> validator.validate((EventEnvelope) null)
            );
            
            assertTrue(exception.getMessage().contains("Event envelope cannot be null"));
        }
        
        @Test
        @DisplayName("should throw when envelope event version is less than 1")
        void shouldThrowWhenEventVersionIsInvalid() {
            EventEnvelope envelope = new EventEnvelope(
                validEventId, validEventType, 0, validTimestamp,
                "test-service", validOrganizationId, validEvent, new EventMetadata()
            );
            
            EventValidationException exception = assertThrows(
                EventValidationException.class,
                () -> validator.validate(envelope)
            );
            
            assertTrue(exception.getMessage().contains("Event version must be >= 1"));
        }
        
        @Test
        @DisplayName("should throw when envelope source service is null")
        void shouldThrowWhenSourceServiceIsNull() {
            EventEnvelope envelope = new EventEnvelope(
                validEventId, validEventType, 1, validTimestamp,
                null, validOrganizationId, validEvent, new EventMetadata()
            );
            
            EventValidationException exception = assertThrows(
                EventValidationException.class,
                () -> validator.validate(envelope)
            );
            
            assertTrue(exception.getMessage().contains("Source service is required"));
        }
        
        @Test
        @DisplayName("should throw when envelope source service is blank")
        void shouldThrowWhenSourceServiceIsBlank() {
            EventEnvelope envelope = new EventEnvelope(
                validEventId, validEventType, 1, validTimestamp,
                "   ", validOrganizationId, validEvent, new EventMetadata()
            );
            
            EventValidationException exception = assertThrows(
                EventValidationException.class,
                () -> validator.validate(envelope)
            );
            
            assertTrue(exception.getMessage().contains("Source service is required"));
        }
        
        @Test
        @DisplayName("should throw when envelope payload is null")
        void shouldThrowWhenPayloadIsNull() {
            EventEnvelope envelope = new EventEnvelope(
                validEventId, validEventType, 1, validTimestamp,
                "test-service", validOrganizationId, null, new EventMetadata()
            );
            
            EventValidationException exception = assertThrows(
                EventValidationException.class,
                () -> validator.validate(envelope)
            );
            
            assertTrue(exception.getMessage().contains("Payload is required"));
        }
    }
    
    @Nested
    @DisplayName("Valid Event Type Examples")
    class ValidEventTypeTests {
        
        @Test
        @DisplayName("should accept UserCreated")
        void shouldAcceptUserCreated() {
            TestDomainEvent event = new TestDomainEvent(
                validEventId, "UserCreated", validTimestamp, validOrganizationId
            );
            
            assertDoesNotThrow(() -> validator.validate(event));
        }
        
        @Test
        @DisplayName("should accept ExperimentCompleted")
        void shouldAcceptExperimentCompleted() {
            TestDomainEvent event = new TestDomainEvent(
                validEventId, "ExperimentCompleted", validTimestamp, validOrganizationId
            );
            
            assertDoesNotThrow(() -> validator.validate(event));
        }
        
        @Test
        @DisplayName("should accept OrganizationCreated")
        void shouldAcceptOrganizationCreated() {
            TestDomainEvent event = new TestDomainEvent(
                validEventId, "OrganizationCreated", validTimestamp, validOrganizationId
            );
            
            assertDoesNotThrow(() -> validator.validate(event));
        }
        
        @Test
        @DisplayName("should accept ReportGenerated")
        void shouldAcceptReportGenerated() {
            TestDomainEvent event = new TestDomainEvent(
                validEventId, "ReportGenerated", validTimestamp, validOrganizationId
            );
            
            assertDoesNotThrow(() -> validator.validate(event));
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
