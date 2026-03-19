package com.turaf.organization.infrastructure.events;

import com.turaf.organization.domain.event.OrganizationCreated;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResultEntry;

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EventBridgePublisher.
 */
@ExtendWith(MockitoExtension.class)
class EventBridgePublisherTest {
    
    @Mock
    private EventBridgeClient eventBridgeClient;
    
    @Mock
    private EventMapper eventMapper;
    
    private EventBridgePublisher publisher;
    
    @BeforeEach
    void setUp() {
        publisher = new EventBridgePublisher(eventBridgeClient, eventMapper);
    }
    
    @Test
    void shouldPublishEventSuccessfully() {
        // Given
        OrganizationCreated event = new OrganizationCreated(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            "Test Org",
            "test-org",
            UUID.randomUUID().toString(),
            Instant.now()
        );
        
        String eventJson = "{\"eventId\":\"123\",\"eventType\":\"OrganizationCreated\"}";
        when(eventMapper.toJson(event)).thenReturn(eventJson);
        
        PutEventsResponse response = PutEventsResponse.builder()
            .failedEntryCount(0)
            .entries(Collections.emptyList())
            .build();
        when(eventBridgeClient.putEvents(any(PutEventsRequest.class))).thenReturn(response);
        
        // When
        publisher.publish(event);
        
        // Then
        verify(eventMapper).toJson(event);
        verify(eventBridgeClient).putEvents(any(PutEventsRequest.class));
    }
    
    @Test
    void shouldIncludeCorrectEventBusName() {
        // Given
        OrganizationCreated event = new OrganizationCreated(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            "Test Org",
            "test-org",
            UUID.randomUUID().toString(),
            Instant.now()
        );
        
        when(eventMapper.toJson(event)).thenReturn("{}");
        
        PutEventsResponse response = PutEventsResponse.builder()
            .failedEntryCount(0)
            .build();
        when(eventBridgeClient.putEvents(any(PutEventsRequest.class))).thenReturn(response);
        
        // When
        publisher.publish(event);
        
        // Then
        ArgumentCaptor<PutEventsRequest> requestCaptor = ArgumentCaptor.forClass(PutEventsRequest.class);
        verify(eventBridgeClient).putEvents(requestCaptor.capture());
        
        PutEventsRequest request = requestCaptor.getValue();
        assertEquals(1, request.entries().size());
        
        PutEventsRequestEntry entry = request.entries().get(0);
        assertNotNull(entry.eventBusName());
    }
    
    @Test
    void shouldSetCorrectEventSource() {
        // Given
        OrganizationCreated event = new OrganizationCreated(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            "Test Org",
            "test-org",
            UUID.randomUUID().toString(),
            Instant.now()
        );
        
        when(eventMapper.toJson(event)).thenReturn("{}");
        
        PutEventsResponse response = PutEventsResponse.builder()
            .failedEntryCount(0)
            .build();
        when(eventBridgeClient.putEvents(any(PutEventsRequest.class))).thenReturn(response);
        
        // When
        publisher.publish(event);
        
        // Then
        ArgumentCaptor<PutEventsRequest> requestCaptor = ArgumentCaptor.forClass(PutEventsRequest.class);
        verify(eventBridgeClient).putEvents(requestCaptor.capture());
        
        PutEventsRequestEntry entry = requestCaptor.getValue().entries().get(0);
        assertEquals("turaf.organization-service", entry.source());
    }
    
    @Test
    void shouldSetCorrectDetailType() {
        // Given
        OrganizationCreated event = new OrganizationCreated(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            "Test Org",
            "test-org",
            UUID.randomUUID().toString(),
            Instant.now()
        );
        
        when(eventMapper.toJson(event)).thenReturn("{}");
        
        PutEventsResponse response = PutEventsResponse.builder()
            .failedEntryCount(0)
            .build();
        when(eventBridgeClient.putEvents(any(PutEventsRequest.class))).thenReturn(response);
        
        // When
        publisher.publish(event);
        
        // Then
        ArgumentCaptor<PutEventsRequest> requestCaptor = ArgumentCaptor.forClass(PutEventsRequest.class);
        verify(eventBridgeClient).putEvents(requestCaptor.capture());
        
        PutEventsRequestEntry entry = requestCaptor.getValue().entries().get(0);
        assertEquals("OrganizationCreated", entry.detailType());
    }
    
    @Test
    void shouldIncludeEventJsonAsDetail() {
        // Given
        OrganizationCreated event = new OrganizationCreated(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            "Test Org",
            "test-org",
            UUID.randomUUID().toString(),
            Instant.now()
        );
        
        String eventJson = "{\"eventId\":\"123\",\"name\":\"Test Org\"}";
        when(eventMapper.toJson(event)).thenReturn(eventJson);
        
        PutEventsResponse response = PutEventsResponse.builder()
            .failedEntryCount(0)
            .build();
        when(eventBridgeClient.putEvents(any(PutEventsRequest.class))).thenReturn(response);
        
        // When
        publisher.publish(event);
        
        // Then
        ArgumentCaptor<PutEventsRequest> requestCaptor = ArgumentCaptor.forClass(PutEventsRequest.class);
        verify(eventBridgeClient).putEvents(requestCaptor.capture());
        
        PutEventsRequestEntry entry = requestCaptor.getValue().entries().get(0);
        assertEquals(eventJson, entry.detail());
    }
    
    @Test
    void shouldThrowExceptionWhenPublishingFails() {
        // Given
        OrganizationCreated event = new OrganizationCreated(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            "Test Org",
            "test-org",
            UUID.randomUUID().toString(),
            Instant.now()
        );
        
        when(eventMapper.toJson(event)).thenReturn("{}");
        
        PutEventsResultEntry failedEntry = PutEventsResultEntry.builder()
            .errorCode("InternalError")
            .errorMessage("Internal error")
            .build();
        
        PutEventsResponse response = PutEventsResponse.builder()
            .failedEntryCount(1)
            .entries(failedEntry)
            .build();
        when(eventBridgeClient.putEvents(any(PutEventsRequest.class))).thenReturn(response);
        
        // When/Then
        assertThrows(EventPublishException.class, () -> publisher.publish(event));
        
        verify(eventMapper).toJson(event);
        verify(eventBridgeClient).putEvents(any(PutEventsRequest.class));
    }
    
    @Test
    void shouldThrowExceptionWhenEventBridgeClientThrows() {
        // Given
        OrganizationCreated event = new OrganizationCreated(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            "Test Org",
            "test-org",
            UUID.randomUUID().toString(),
            Instant.now()
        );
        
        when(eventMapper.toJson(event)).thenReturn("{}");
        when(eventBridgeClient.putEvents(any(PutEventsRequest.class)))
            .thenThrow(new RuntimeException("EventBridge error"));
        
        // When/Then
        assertThrows(EventPublishException.class, () -> publisher.publish(event));
    }
    
    @Test
    void shouldPropagateEventMappingException() {
        // Given
        OrganizationCreated event = new OrganizationCreated(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            "Test Org",
            "test-org",
            UUID.randomUUID().toString(),
            Instant.now()
        );
        
        when(eventMapper.toJson(event))
            .thenThrow(new EventMappingException("Mapping failed"));
        
        // When/Then
        assertThrows(EventMappingException.class, () -> publisher.publish(event));
        
        verify(eventMapper).toJson(event);
        verify(eventBridgeClient, never()).putEvents(any(PutEventsRequest.class));
    }
}
