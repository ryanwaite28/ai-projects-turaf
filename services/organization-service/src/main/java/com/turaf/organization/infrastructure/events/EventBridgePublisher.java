package com.turaf.organization.infrastructure.events;

import com.turaf.common.domain.DomainEvent;
import com.turaf.organization.application.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;

/**
 * EventBridge implementation of EventPublisher.
 * Publishes domain events to AWS EventBridge.
 */
@Service
public class EventBridgePublisher implements EventPublisher {
    
    private static final Logger log = LoggerFactory.getLogger(EventBridgePublisher.class);
    private static final String EVENT_SOURCE = "turaf.organization-service";
    
    private final EventBridgeClient eventBridgeClient;
    private final EventMapper eventMapper;
    
    @Value("${aws.eventbridge.bus-name:turaf-event-bus-dev}")
    private String eventBusName;
    
    public EventBridgePublisher(EventBridgeClient eventBridgeClient, EventMapper eventMapper) {
        this.eventBridgeClient = eventBridgeClient;
        this.eventMapper = eventMapper;
    }
    
    @Override
    public void publish(DomainEvent event) {
        try {
            String eventJson = eventMapper.toJson(event);
            
            PutEventsRequestEntry entry = PutEventsRequestEntry.builder()
                .eventBusName(eventBusName)
                .source(EVENT_SOURCE)
                .detailType(event.getEventType())
                .detail(eventJson)
                .build();
            
            PutEventsRequest request = PutEventsRequest.builder()
                .entries(entry)
                .build();
            
            PutEventsResponse response = eventBridgeClient.putEvents(request);
            
            if (response.failedEntryCount() > 0) {
                log.error("Failed to publish event: {} - Failures: {}", 
                    event.getEventType(), 
                    response.entries());
                throw new EventPublishException(
                    "Failed to publish event: " + event.getEventType() + 
                    " - Failed count: " + response.failedEntryCount()
                );
            }
            
            log.info("Successfully published event: {} with ID: {} to bus: {}", 
                event.getEventType(), 
                event.getEventId(), 
                eventBusName);
            
        } catch (EventMappingException e) {
            log.error("Failed to map event: {}", event.getEventType(), e);
            throw e;
        } catch (Exception e) {
            log.error("Error publishing event: {} with ID: {}", 
                event.getEventType(), 
                event.getEventId(), 
                e);
            throw new EventPublishException(
                "Error publishing event: " + event.getEventType(), 
                e
            );
        }
    }
    
    /**
     * Get the configured event bus name.
     * Useful for testing and diagnostics.
     */
    public String getEventBusName() {
        return eventBusName;
    }
}
