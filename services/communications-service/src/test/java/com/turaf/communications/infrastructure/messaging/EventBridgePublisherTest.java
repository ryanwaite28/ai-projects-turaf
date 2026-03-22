package com.turaf.communications.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turaf.communications.domain.event.MessageDeliveredEvent;
import com.turaf.communications.domain.model.ConversationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventBridgePublisherTest {
    
    @Mock
    private EventBridgeClient eventBridgeClient;
    
    private ObjectMapper objectMapper;
    
    private EventBridgePublisher publisher;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        publisher = new EventBridgePublisher(eventBridgeClient, objectMapper);
        ReflectionTestUtils.setField(publisher, "eventBusName", "turaf-event-bus");
    }
    
    @Test
    void publishMessageDelivered_shouldPublishEventSuccessfully() {
        MessageDeliveredEvent event = new MessageDeliveredEvent(
            "msg-1",
            "conv-1",
            "user-1",
            ConversationType.DIRECT,
            List.of("user-2"),
            "Hello",
            Instant.now()
        );
        
        PutEventsResponse response = PutEventsResponse.builder()
            .failedEntryCount(0)
            .build();
        
        when(eventBridgeClient.putEvents(any(PutEventsRequest.class)))
            .thenReturn(response);
        
        publisher.publishMessageDelivered(event);
        
        verify(eventBridgeClient).putEvents(any(PutEventsRequest.class));
    }
    
    @Test
    void publishMessageDelivered_shouldThrowExceptionOnFailure() {
        MessageDeliveredEvent event = new MessageDeliveredEvent(
            "msg-1",
            "conv-1",
            "user-1",
            ConversationType.DIRECT,
            List.of("user-2"),
            "Hello",
            Instant.now()
        );
        
        PutEventsResponse response = PutEventsResponse.builder()
            .failedEntryCount(1)
            .build();
        
        when(eventBridgeClient.putEvents(any(PutEventsRequest.class)))
            .thenReturn(response);
        
        assertThrows(RuntimeException.class, () -> 
            publisher.publishMessageDelivered(event)
        );
    }
    
    @Test
    void publishMessageDelivered_shouldIncludeCorrectEventDetails() {
        MessageDeliveredEvent event = new MessageDeliveredEvent(
            "msg-1",
            "conv-1",
            "user-1",
            ConversationType.GROUP,
            List.of("user-2", "user-3"),
            "Hello Group",
            Instant.now()
        );
        
        PutEventsResponse response = PutEventsResponse.builder()
            .failedEntryCount(0)
            .build();
        
        when(eventBridgeClient.putEvents(any(PutEventsRequest.class)))
            .thenReturn(response);
        
        publisher.publishMessageDelivered(event);
        
        verify(eventBridgeClient).putEvents(argThat((PutEventsRequest request) -> {
            var entry = request.entries().get(0);
            return entry.source().equals("communications-service") &&
                   entry.detailType().equals("MessageDelivered") &&
                   entry.eventBusName().equals("turaf-event-bus");
        }));
    }
    
    @Test
    void publishMessageDelivered_shouldHandleClientException() {
        MessageDeliveredEvent event = new MessageDeliveredEvent(
            "msg-1",
            "conv-1",
            "user-1",
            ConversationType.DIRECT,
            List.of("user-2"),
            "Hello",
            Instant.now()
        );
        
        when(eventBridgeClient.putEvents(any(PutEventsRequest.class)))
            .thenThrow(new RuntimeException("EventBridge error"));
        
        assertThrows(RuntimeException.class, () -> 
            publisher.publishMessageDelivered(event)
        );
    }
    
    @Test
    void publishMessageDelivered_shouldSerializeEventToJson() throws Exception {
        MessageDeliveredEvent event = new MessageDeliveredEvent(
            "msg-1",
            "conv-1",
            "user-1",
            ConversationType.DIRECT,
            List.of("user-2"),
            "Hello",
            Instant.now()
        );
        
        PutEventsResponse response = PutEventsResponse.builder()
            .failedEntryCount(0)
            .build();
        
        when(eventBridgeClient.putEvents(any(PutEventsRequest.class)))
            .thenReturn(response);
        
        publisher.publishMessageDelivered(event);
        
        verify(eventBridgeClient).putEvents(argThat((PutEventsRequest request) -> {
            String detail = request.entries().get(0).detail();
            return detail.contains("msg-1") && 
                   detail.contains("conv-1") && 
                   detail.contains("user-1");
        }));
    }
}
