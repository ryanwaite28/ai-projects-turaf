# Task: Implement Event Publishing

**Service**: Organization Service  
**Phase**: 3  
**Estimated Time**: 2-3 hours  

## Objective

Implement EventBridge event publishing infrastructure for domain events (OrganizationCreated, MemberAdded, MemberRemoved).

## Prerequisites

- [x] Task 001: Domain model with events created
- [x] Task 003: Organization service implemented
- [x] Task 004: Membership service implemented

## Scope

**Files to Create**:
- `services/organization-service/src/main/java/com/turaf/organization/infrastructure/events/EventPublisher.java`
- `services/organization-service/src/main/java/com/turaf/organization/infrastructure/events/EventBridgePublisher.java`
- `services/organization-service/src/main/java/com/turaf/organization/infrastructure/events/EventMapper.java`
- `services/organization-service/src/main/java/com/turaf/organization/infrastructure/config/EventBridgeConfig.java`

## Implementation Details

### Event Publisher Interface

```java
public interface EventPublisher {
    void publish(DomainEvent event);
}
```

### EventBridge Publisher Implementation

```java
@Service
public class EventBridgePublisher implements EventPublisher {
    private final EventBridgeClient eventBridgeClient;
    private final EventMapper eventMapper;
    
    @Value("${aws.eventbridge.bus-name}")
    private String eventBusName;
    
    @Override
    public void publish(DomainEvent event) {
        try {
            PutEventsRequestEntry entry = PutEventsRequestEntry.builder()
                .eventBusName(eventBusName)
                .source("turaf.organization-service")
                .detailType(event.getEventType())
                .detail(eventMapper.toJson(event))
                .build();
            
            PutEventsRequest request = PutEventsRequest.builder()
                .entries(entry)
                .build();
            
            PutEventsResponse response = eventBridgeClient.putEvents(request);
            
            if (response.failedEntryCount() > 0) {
                log.error("Failed to publish event: {}", response.entries());
                throw new EventPublishException("Failed to publish event");
            }
            
            log.info("Published event: {} with ID: {}", event.getEventType(), event.getEventId());
        } catch (Exception e) {
            log.error("Error publishing event", e);
            throw new EventPublishException("Error publishing event", e);
        }
    }
}
```

### Event Mapper

```java
@Component
public class EventMapper {
    private final ObjectMapper objectMapper;
    
    public EventMapper() {
        this.objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
    
    public String toJson(DomainEvent event) {
        try {
            Map<String, Object> envelope = new HashMap<>();
            envelope.put("eventId", event.getEventId());
            envelope.put("eventType", event.getEventType());
            envelope.put("eventVersion", 1);
            envelope.put("timestamp", event.getTimestamp());
            envelope.put("sourceService", "organization-service");
            envelope.put("organizationId", event.getOrganizationId());
            envelope.put("payload", event);
            
            return objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException e) {
            throw new EventMappingException("Failed to serialize event", e);
        }
    }
}
```

### EventBridge Configuration

```java
@Configuration
public class EventBridgeConfig {
    
    @Bean
    public EventBridgeClient eventBridgeClient() {
        return EventBridgeClient.builder()
            .region(Region.US_EAST_1)
            .build();
    }
}
```

### Application Configuration

```yaml
# application.yml
aws:
  eventbridge:
    bus-name: ${EVENT_BUS_NAME:turaf-event-bus-dev}
```

## Acceptance Criteria

- [ ] EventPublisher interface defined
- [ ] EventBridge publisher implementation works
- [ ] Events published with correct envelope structure
- [ ] Event mapper serializes events correctly
- [ ] EventBridge client configured
- [ ] Failed events logged
- [ ] Integration tests pass

## Testing Requirements

**Unit Tests**:
- Test event mapper serialization
- Test event publishing success
- Test event publishing failure handling

**Integration Tests**:
- Test event published to EventBridge
- Test event envelope structure
- Test event contains all required fields

**Test Files to Create**:
- `EventBridgePublisherTest.java`
- `EventMapperTest.java`

## References

- Specification: `specs/organization-service.md` (Event Publishing section)
- Specification: `specs/event-schemas.md`
- Related Tasks: 007-add-tenant-context-filter
