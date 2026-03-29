package com.turaf.identity.infrastructure.events;

import com.turaf.common.domain.DomainEvent;
import com.turaf.common.event.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class EventBridgePublisher implements EventPublisher {
    
    private static final Logger logger = LoggerFactory.getLogger(EventBridgePublisher.class);
    
    private final EventBridgeClient eventBridgeClient;
    private final EventMapper eventMapper;
    private final String eventBusName;

    public EventBridgePublisher(
            EventBridgeClient eventBridgeClient,
            EventMapper eventMapper,
            @Value("${aws.eventbridge.bus-name:turaf-event-bus}") String eventBusName) {
        this.eventBridgeClient = eventBridgeClient;
        this.eventMapper = eventMapper;
        this.eventBusName = eventBusName;
    }

    @Override
    public void publish(DomainEvent event) {
        try {
            PutEventsRequestEntry entry = eventMapper.toEventBridgeEntry(event, eventBusName);
            
            PutEventsRequest request = PutEventsRequest.builder()
                .entries(entry)
                .build();
            
            PutEventsResponse response = eventBridgeClient.putEvents(request);
            
            if (response.failedEntryCount() > 0) {
                logger.error("Failed to publish event: {}", response.entries().get(0).errorMessage());
                throw new RuntimeException("Failed to publish event to EventBridge");
            }
            
            logger.info("Successfully published event: {} to EventBridge", event.getClass().getSimpleName());
        } catch (Exception e) {
            logger.error("Error publishing event to EventBridge", e);
            throw new RuntimeException("Failed to publish event", e);
        }
    }

    @Override
    public void publishBatch(List<DomainEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }

        try {
            List<PutEventsRequestEntry> entries = events.stream()
                .map(event -> eventMapper.toEventBridgeEntry(event, eventBusName))
                .collect(Collectors.toList());

            PutEventsRequest request = PutEventsRequest.builder()
                .entries(entries)
                .build();

            PutEventsResponse response = eventBridgeClient.putEvents(request);

            if (response.failedEntryCount() > 0) {
                logger.error("Failed to publish {} events to EventBridge", response.failedEntryCount());
                throw new RuntimeException("Failed to publish batch events to EventBridge");
            }

            logger.info("Successfully published {} events to EventBridge", events.size());
        } catch (Exception e) {
            logger.error("Error publishing batch events to EventBridge", e);
            throw new RuntimeException("Failed to publish batch events", e);
        }
    }
}
