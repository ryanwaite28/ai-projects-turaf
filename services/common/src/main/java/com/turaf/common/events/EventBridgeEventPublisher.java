package com.turaf.common.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * EventBridge implementation of the EventPublisher interface.
 * 
 * This component publishes domain events to AWS EventBridge, enabling event-driven
 * communication between microservices. Events are wrapped in EventEnvelope with
 * metadata before publishing.
 * 
 * Key features:
 * - Single and batch event publishing
 * - Automatic batching for EventBridge's 10-event limit
 * - Comprehensive error handling and logging
 * - Source service identification for event routing
 * 
 * Following SOLID principles:
 * - Single Responsibility: Only handles EventBridge publishing
 * - Dependency Inversion: Depends on EventPublisher abstraction
 * - Open/Closed: Extensible through configuration
 * 
 * Following Spring Boot best practices:
 * - Configured via application properties
 * - Uses constructor injection for dependencies
 * - Comprehensive logging for observability
 */
@Component
public class EventBridgeEventPublisher implements EventPublisher {
    
    private static final Logger log = LoggerFactory.getLogger(EventBridgeEventPublisher.class);
    private static final int MAX_EVENTS_PER_REQUEST = 10;
    
    private final EventBridgeClient eventBridgeClient;
    private final EventSerializer serializer;
    private final EventValidator validator;
    private final String eventBusName;
    private final String sourceService;
    
    /**
     * Constructs an EventBridgeEventPublisher with required dependencies.
     * 
     * @param eventBridgeClient AWS EventBridge client for API calls
     * @param serializer serializer for converting events to JSON
     * @param validator validator for ensuring event schema compliance
     * @param eventBusName name of the EventBridge event bus
     * @param sourceService name of the service publishing events
     */
    public EventBridgeEventPublisher(
            EventBridgeClient eventBridgeClient,
            EventSerializer serializer,
            EventValidator validator,
            @Value("${aws.eventbridge.bus-name}") String eventBusName,
            @Value("${spring.application.name}") String sourceService) {
        this.eventBridgeClient = eventBridgeClient;
        this.serializer = serializer;
        this.validator = validator;
        this.eventBusName = eventBusName;
        this.sourceService = sourceService;
        
        log.info("EventBridge publisher initialized for service: {} on bus: {}", sourceService, eventBusName);
    }
    
    /**
     * Publishes a single domain event to EventBridge.
     * 
     * The event is wrapped in an EventEnvelope, serialized to JSON, and sent to EventBridge.
     * The source is set to "turaf.{service-name}" for consistent event routing.
     * 
     * @param event the domain event to publish
     * @throws EventPublishException if publishing fails
     */
    @Override
    public void publish(DomainEvent event) {
        try {
            log.debug("Publishing event: {} with ID: {}", event.getEventType(), event.getEventId());
            
            // Validate domain event before processing
            validator.validate(event);
            
            EventEnvelope envelope = EventEnvelope.wrap(event, sourceService);
            
            // Validate envelope before publishing
            validator.validate(envelope);
            
            String eventJson = serializer.serialize(envelope);
            
            PutEventsRequestEntry entry = PutEventsRequestEntry.builder()
                .eventBusName(eventBusName)
                .source("turaf." + sourceService)
                .detailType(event.getEventType())
                .detail(eventJson)
                .build();
            
            PutEventsRequest request = PutEventsRequest.builder()
                .entries(entry)
                .build();
            
            PutEventsResponse response = eventBridgeClient.putEvents(request);
            
            if (response.failedEntryCount() > 0) {
                String errorMessage = response.entries().get(0).errorMessage();
                String errorCode = response.entries().get(0).errorCode();
                log.error("Failed to publish event: {} - Error: {} ({})", 
                    event.getEventType(), errorMessage, errorCode);
                throw new EventPublishException(
                    String.format("Failed to publish event %s: %s (%s)", 
                        event.getEventType(), errorMessage, errorCode));
            }
            
            log.info("Successfully published event: {} with ID: {} to organization: {}", 
                event.getEventType(), event.getEventId(), event.getOrganizationId());
            
        } catch (EventPublishException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error publishing event: {}", event.getEventType(), e);
            throw new EventPublishException("Unexpected error publishing event: " + event.getEventType(), e);
        }
    }
    
    /**
     * Publishes multiple domain events to EventBridge in batches.
     * 
     * EventBridge supports a maximum of 10 events per PutEvents request.
     * This method automatically partitions large event lists into batches of 10.
     * 
     * @param events the list of domain events to publish
     * @throws EventPublishException if publishing fails
     */
    @Override
    public void publishBatch(List<DomainEvent> events) {
        if (events == null || events.isEmpty()) {
            log.debug("No events to publish in batch");
            return;
        }
        
        log.info("Publishing batch of {} events", events.size());
        
        try {
            List<PutEventsRequestEntry> entries = events.stream()
                .map(event -> {
                    // Validate each event in the batch
                    validator.validate(event);
                    
                    EventEnvelope envelope = EventEnvelope.wrap(event, sourceService);
                    
                    // Validate envelope
                    validator.validate(envelope);
                    
                    String eventJson = serializer.serialize(envelope);
                    
                    return PutEventsRequestEntry.builder()
                        .eventBusName(eventBusName)
                        .source("turaf." + sourceService)
                        .detailType(event.getEventType())
                        .detail(eventJson)
                        .build();
                })
                .collect(Collectors.toList());
            
            // Partition into batches of 10 (EventBridge limit)
            List<List<PutEventsRequestEntry>> batches = partition(entries, MAX_EVENTS_PER_REQUEST);
            
            int totalFailed = 0;
            for (int i = 0; i < batches.size(); i++) {
                List<PutEventsRequestEntry> batch = batches.get(i);
                log.debug("Publishing batch {}/{} with {} events", i + 1, batches.size(), batch.size());
                
                PutEventsRequest request = PutEventsRequest.builder()
                    .entries(batch)
                    .build();
                
                PutEventsResponse response = eventBridgeClient.putEvents(request);
                
                if (response.failedEntryCount() > 0) {
                    totalFailed += response.failedEntryCount();
                    log.error("Failed to publish {} events in batch {}/{}", 
                        response.failedEntryCount(), i + 1, batches.size());
                    
                    // Log details of failed entries
                    response.entries().stream()
                        .filter(entry -> entry.errorCode() != null)
                        .forEach(entry -> log.error("Event failed: {} - {}", 
                            entry.errorCode(), entry.errorMessage()));
                }
            }
            
            if (totalFailed > 0) {
                throw new EventPublishException(
                    String.format("Failed to publish %d out of %d events", totalFailed, events.size()));
            }
            
            log.info("Successfully published batch of {} events", events.size());
            
        } catch (EventPublishException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error publishing event batch", e);
            throw new EventPublishException("Unexpected error publishing event batch", e);
        }
    }
    
    /**
     * Partitions a list into smaller sublists of specified size.
     * 
     * @param list the list to partition
     * @param size the maximum size of each partition
     * @return list of partitioned sublists
     */
    private <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }
}
