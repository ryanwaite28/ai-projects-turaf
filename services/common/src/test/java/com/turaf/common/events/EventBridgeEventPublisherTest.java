package com.turaf.common.events;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EventBridgeEventPublisher.
 * 
 * Tests cover:
 * - Single event publishing
 * - Batch event publishing
 * - EventBridge API interaction
 * - Error handling
 * - Batching logic (10 events per request limit)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EventBridgeEventPublisher")
class EventBridgeEventPublisherTest {
    
    @Mock
    private EventBridgeClient eventBridgeClient;
    
    @Mock
    private EventSerializer serializer;
    
    private EventBridgeEventPublisher publisher;
    private TestDomainEvent testEvent;
    private String eventBusName = "test-event-bus";
    private String sourceService = "test-service";
    
    @BeforeEach
    void setUp() {
        publisher = new EventBridgeEventPublisher(
            eventBridgeClient,
            serializer,
            eventBusName,
            sourceService
        );
        
        testEvent = new TestDomainEvent(
            UUID.randomUUID().toString(),
            "test.EventOccurred",
            Instant.now(),
            "org-123"
        );
    }
    
    @Nested
    @DisplayName("Single Event Publishing")
    class SingleEventPublishingTests {
        
        @Test
        @DisplayName("should publish single event successfully")
        void shouldPublishSingleEvent() {
            // Arrange
            String eventJson = "{\"eventId\":\"123\"}";
            when(serializer.serialize(any(EventEnvelope.class))).thenReturn(eventJson);
            
            PutEventsResponse response = PutEventsResponse.builder()
                .failedEntryCount(0)
                .entries(PutEventsResultEntry.builder().eventId("evt-123").build())
                .build();
            when(eventBridgeClient.putEvents(any(PutEventsRequest.class))).thenReturn(response);
            
            // Act
            publisher.publish(testEvent);
            
            // Assert
            verify(serializer).serialize(any(EventEnvelope.class));
            verify(eventBridgeClient).putEvents(any(PutEventsRequest.class));
        }
        
        @Test
        @DisplayName("should create correct PutEventsRequest")
        void shouldCreateCorrectRequest() {
            // Arrange
            String eventJson = "{\"eventId\":\"123\"}";
            when(serializer.serialize(any(EventEnvelope.class))).thenReturn(eventJson);
            
            PutEventsResponse response = PutEventsResponse.builder()
                .failedEntryCount(0)
                .entries(PutEventsResultEntry.builder().eventId("evt-123").build())
                .build();
            when(eventBridgeClient.putEvents(any(PutEventsRequest.class))).thenReturn(response);
            
            ArgumentCaptor<PutEventsRequest> requestCaptor = ArgumentCaptor.forClass(PutEventsRequest.class);
            
            // Act
            publisher.publish(testEvent);
            
            // Assert
            verify(eventBridgeClient).putEvents(requestCaptor.capture());
            PutEventsRequest request = requestCaptor.getValue();
            
            assertEquals(1, request.entries().size());
            PutEventsRequestEntry entry = request.entries().get(0);
            assertEquals(eventBusName, entry.eventBusName());
            assertEquals("turaf." + sourceService, entry.source());
            assertEquals(testEvent.getEventType(), entry.detailType());
            assertEquals(eventJson, entry.detail());
        }
        
        @Test
        @DisplayName("should wrap event in envelope before publishing")
        void shouldWrapEventInEnvelope() {
            // Arrange
            String eventJson = "{\"eventId\":\"123\"}";
            when(serializer.serialize(any(EventEnvelope.class))).thenReturn(eventJson);
            
            PutEventsResponse response = PutEventsResponse.builder()
                .failedEntryCount(0)
                .entries(PutEventsResultEntry.builder().eventId("evt-123").build())
                .build();
            when(eventBridgeClient.putEvents(any(PutEventsRequest.class))).thenReturn(response);
            
            ArgumentCaptor<EventEnvelope> envelopeCaptor = ArgumentCaptor.forClass(EventEnvelope.class);
            
            // Act
            publisher.publish(testEvent);
            
            // Assert
            verify(serializer).serialize(envelopeCaptor.capture());
            EventEnvelope envelope = envelopeCaptor.getValue();
            
            assertEquals(testEvent.getEventId(), envelope.getEventId());
            assertEquals(testEvent.getEventType(), envelope.getEventType());
            assertEquals(sourceService, envelope.getSourceService());
            assertEquals(testEvent.getOrganizationId(), envelope.getOrganizationId());
        }
        
        @Test
        @DisplayName("should throw EventPublishException when EventBridge returns failure")
        void shouldThrowWhenEventBridgeReturnsFailure() {
            // Arrange
            String eventJson = "{\"eventId\":\"123\"}";
            when(serializer.serialize(any(EventEnvelope.class))).thenReturn(eventJson);
            
            PutEventsResponse response = PutEventsResponse.builder()
                .failedEntryCount(1)
                .entries(PutEventsResultEntry.builder()
                    .errorCode("InternalFailure")
                    .errorMessage("Internal error")
                    .build())
                .build();
            when(eventBridgeClient.putEvents(any(PutEventsRequest.class))).thenReturn(response);
            
            // Act & Assert
            EventPublishException exception = assertThrows(EventPublishException.class, () -> 
                publisher.publish(testEvent)
            );
            
            assertTrue(exception.getMessage().contains("Failed to publish event"));
            assertTrue(exception.getMessage().contains("InternalFailure"));
        }
        
        @Test
        @DisplayName("should throw EventPublishException when serialization fails")
        void shouldThrowWhenSerializationFails() {
            // Arrange
            when(serializer.serialize(any(EventEnvelope.class)))
                .thenThrow(new EventPublishException("Serialization failed"));
            
            // Act & Assert
            assertThrows(EventPublishException.class, () -> 
                publisher.publish(testEvent)
            );
        }
        
        @Test
        @DisplayName("should throw EventPublishException when EventBridge client throws")
        void shouldThrowWhenEventBridgeClientThrows() {
            // Arrange
            String eventJson = "{\"eventId\":\"123\"}";
            when(serializer.serialize(any(EventEnvelope.class))).thenReturn(eventJson);
            when(eventBridgeClient.putEvents(any(PutEventsRequest.class)))
                .thenThrow(new RuntimeException("Network error"));
            
            // Act & Assert
            EventPublishException exception = assertThrows(EventPublishException.class, () -> 
                publisher.publish(testEvent)
            );
            
            assertTrue(exception.getMessage().contains("Unexpected error"));
        }
    }
    
    @Nested
    @DisplayName("Batch Event Publishing")
    class BatchEventPublishingTests {
        
        @Test
        @DisplayName("should publish batch of events successfully")
        void shouldPublishBatchOfEvents() {
            // Arrange
            List<DomainEvent> events = Arrays.asList(
                createTestEvent(),
                createTestEvent(),
                createTestEvent()
            );
            
            when(serializer.serialize(any(EventEnvelope.class))).thenReturn("{\"eventId\":\"123\"}");
            
            PutEventsResponse response = PutEventsResponse.builder()
                .failedEntryCount(0)
                .build();
            when(eventBridgeClient.putEvents(any(PutEventsRequest.class))).thenReturn(response);
            
            // Act
            publisher.publishBatch(events);
            
            // Assert
            verify(eventBridgeClient, times(1)).putEvents(any(PutEventsRequest.class));
        }
        
        @Test
        @DisplayName("should handle empty event list")
        void shouldHandleEmptyEventList() {
            // Act
            publisher.publishBatch(Collections.emptyList());
            
            // Assert
            verify(eventBridgeClient, never()).putEvents(any(PutEventsRequest.class));
        }
        
        @Test
        @DisplayName("should handle null event list")
        void shouldHandleNullEventList() {
            // Act
            publisher.publishBatch(null);
            
            // Assert
            verify(eventBridgeClient, never()).putEvents(any(PutEventsRequest.class));
        }
        
        @Test
        @DisplayName("should partition events into batches of 10")
        void shouldPartitionEventsIntoBatchesOf10() {
            // Arrange - Create 25 events (should result in 3 batches: 10, 10, 5)
            List<DomainEvent> events = Arrays.asList(
                createTestEvent(), createTestEvent(), createTestEvent(), createTestEvent(), createTestEvent(),
                createTestEvent(), createTestEvent(), createTestEvent(), createTestEvent(), createTestEvent(),
                createTestEvent(), createTestEvent(), createTestEvent(), createTestEvent(), createTestEvent(),
                createTestEvent(), createTestEvent(), createTestEvent(), createTestEvent(), createTestEvent(),
                createTestEvent(), createTestEvent(), createTestEvent(), createTestEvent(), createTestEvent()
            );
            
            when(serializer.serialize(any(EventEnvelope.class))).thenReturn("{\"eventId\":\"123\"}");
            
            PutEventsResponse response = PutEventsResponse.builder()
                .failedEntryCount(0)
                .build();
            when(eventBridgeClient.putEvents(any(PutEventsRequest.class))).thenReturn(response);
            
            // Act
            publisher.publishBatch(events);
            
            // Assert - Should be called 3 times (10 + 10 + 5)
            verify(eventBridgeClient, times(3)).putEvents(any(PutEventsRequest.class));
        }
        
        @Test
        @DisplayName("should throw EventPublishException when batch has failures")
        void shouldThrowWhenBatchHasFailures() {
            // Arrange
            List<DomainEvent> events = Arrays.asList(
                createTestEvent(),
                createTestEvent()
            );
            
            when(serializer.serialize(any(EventEnvelope.class))).thenReturn("{\"eventId\":\"123\"}");
            
            PutEventsResponse response = PutEventsResponse.builder()
                .failedEntryCount(1)
                .entries(
                    PutEventsResultEntry.builder().eventId("evt-1").build(),
                    PutEventsResultEntry.builder()
                        .errorCode("InternalFailure")
                        .errorMessage("Internal error")
                        .build()
                )
                .build();
            when(eventBridgeClient.putEvents(any(PutEventsRequest.class))).thenReturn(response);
            
            // Act & Assert
            EventPublishException exception = assertThrows(EventPublishException.class, () -> 
                publisher.publishBatch(events)
            );
            
            assertTrue(exception.getMessage().contains("Failed to publish"));
        }
        
        @Test
        @DisplayName("should create correct batch request entries")
        void shouldCreateCorrectBatchRequestEntries() {
            // Arrange
            List<DomainEvent> events = Arrays.asList(
                createTestEvent(),
                createTestEvent()
            );
            
            when(serializer.serialize(any(EventEnvelope.class))).thenReturn("{\"eventId\":\"123\"}");
            
            PutEventsResponse response = PutEventsResponse.builder()
                .failedEntryCount(0)
                .build();
            when(eventBridgeClient.putEvents(any(PutEventsRequest.class))).thenReturn(response);
            
            ArgumentCaptor<PutEventsRequest> requestCaptor = ArgumentCaptor.forClass(PutEventsRequest.class);
            
            // Act
            publisher.publishBatch(events);
            
            // Assert
            verify(eventBridgeClient).putEvents(requestCaptor.capture());
            PutEventsRequest request = requestCaptor.getValue();
            
            assertEquals(2, request.entries().size());
            request.entries().forEach(entry -> {
                assertEquals(eventBusName, entry.eventBusName());
                assertEquals("turaf." + sourceService, entry.source());
            });
        }
    }
    
    private TestDomainEvent createTestEvent() {
        return new TestDomainEvent(
            UUID.randomUUID().toString(),
            "test.EventOccurred",
            Instant.now(),
            "org-123"
        );
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
