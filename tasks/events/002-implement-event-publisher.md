# Task: Implement Event Publisher

**Service**: Events Infrastructure  
**Phase**: 6  
**Estimated Time**: 3 hours  

## Objective

Implement shared event publisher infrastructure for publishing events to EventBridge.

## Prerequisites

- [x] Task 001: Event envelope implemented

## Scope

**Files to Create**:
- `services/common/src/main/java/com/turaf/common/events/EventPublisher.java`
- `services/common/src/main/java/com/turaf/common/events/EventBridgeEventPublisher.java`
- `services/common/src/main/java/com/turaf/common/events/EventSerializer.java`
- `services/common/src/main/java/com/turaf/common/events/EventPublishException.java`

## Implementation Details

### Event Publisher Interface

```java
public interface EventPublisher {
    void publish(DomainEvent event);
    void publishBatch(List<DomainEvent> events);
}
```

### EventBridge Event Publisher

```java
@Component
public class EventBridgeEventPublisher implements EventPublisher {
    private final EventBridgeClient eventBridgeClient;
    private final EventSerializer serializer;
    private final String eventBusName;
    private final String sourceService;
    
    public EventBridgeEventPublisher(
            EventBridgeClient eventBridgeClient,
            EventSerializer serializer,
            @Value("${aws.eventbridge.bus-name}") String eventBusName,
            @Value("${spring.application.name}") String sourceService) {
        this.eventBridgeClient = eventBridgeClient;
        this.serializer = serializer;
        this.eventBusName = eventBusName;
        this.sourceService = sourceService;
    }
    
    @Override
    public void publish(DomainEvent event) {
        try {
            EventEnvelope envelope = EventEnvelope.wrap(event, sourceService);
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
                throw new EventPublishException("Failed to publish event: " + 
                    response.entries().get(0).errorMessage());
            }
            
            log.info("Published event: {} with ID: {}", event.getEventType(), event.getEventId());
        } catch (Exception e) {
            log.error("Error publishing event", e);
            throw new EventPublishException("Error publishing event", e);
        }
    }
    
    @Override
    public void publishBatch(List<DomainEvent> events) {
        if (events.isEmpty()) {
            return;
        }
        
        List<PutEventsRequestEntry> entries = events.stream()
            .map(event -> {
                EventEnvelope envelope = EventEnvelope.wrap(event, sourceService);
                String eventJson = serializer.serialize(envelope);
                
                return PutEventsRequestEntry.builder()
                    .eventBusName(eventBusName)
                    .source("turaf." + sourceService)
                    .detailType(event.getEventType())
                    .detail(eventJson)
                    .build();
            })
            .collect(Collectors.toList());
        
        // EventBridge supports max 10 entries per request
        Lists.partition(entries, 10).forEach(batch -> {
            PutEventsRequest request = PutEventsRequest.builder()
                .entries(batch)
                .build();
            
            PutEventsResponse response = eventBridgeClient.putEvents(request);
            
            if (response.failedEntryCount() > 0) {
                log.error("Failed to publish {} events", response.failedEntryCount());
            }
        });
    }
}
```

### Event Serializer

```java
@Component
public class EventSerializer {
    private final ObjectMapper objectMapper;
    
    public EventSerializer() {
        this.objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
    
    public String serialize(EventEnvelope envelope) {
        try {
            return objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException e) {
            throw new EventPublishException("Failed to serialize event", e);
        }
    }
    
    public EventEnvelope deserialize(String json) {
        try {
            return objectMapper.readValue(json, EventEnvelope.class);
        } catch (JsonProcessingException e) {
            throw new EventPublishException("Failed to deserialize event", e);
        }
    }
}
```

## Acceptance Criteria

- [ ] Event publisher interface defined
- [ ] EventBridge publisher implemented
- [ ] Batch publishing supported
- [ ] Event serialization works
- [ ] Error handling implemented
- [ ] Logging added
- [ ] Unit tests pass

## Testing Requirements

**Unit Tests**:
- Test single event publishing
- Test batch event publishing
- Test serialization
- Test error handling

**Test Files to Create**:
- `EventBridgeEventPublisherTest.java`
- `EventSerializerTest.java`

## References

- Specification: `specs/event-flow.md` (Event Publishing section)
- Related Tasks: 003-setup-eventbridge-rules
