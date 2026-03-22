package com.turaf.communications.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turaf.communications.domain.event.MessageDeliveredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventBridgePublisher {
    
    private final EventBridgeClient eventBridgeClient;
    private final ObjectMapper objectMapper;
    
    @Value("${aws.eventbridge.bus-name}")
    private String eventBusName;
    
    public void publishMessageDelivered(MessageDeliveredEvent event) {
        try {
            String eventDetail = objectMapper.writeValueAsString(event);
            
            PutEventsRequestEntry entry = PutEventsRequestEntry.builder()
                .source("communications-service")
                .detailType("MessageDelivered")
                .detail(eventDetail)
                .eventBusName(eventBusName)
                .build();
            
            PutEventsRequest request = PutEventsRequest.builder()
                .entries(entry)
                .build();
            
            PutEventsResponse response = eventBridgeClient.putEvents(request);
            
            if (response.failedEntryCount() > 0) {
                log.error("Failed to publish event: {}", response.entries());
                throw new RuntimeException("Failed to publish MessageDelivered event");
            }
            
            log.info("MessageDelivered event published successfully: messageId={}, conversationId={}", 
                     event.getMessageId(), event.getConversationId());
            
        } catch (Exception e) {
            log.error("Error publishing MessageDelivered event", e);
            throw new RuntimeException("Failed to publish event", e);
        }
    }
}
