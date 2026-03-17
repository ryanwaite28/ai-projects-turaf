# Task: Implement Event Publishing

**Service**: Reporting Service  
**Phase**: 7  
**Estimated Time**: 2 hours  

## Objective

Implement EventBridge event publishing for ReportGenerated events.

## Prerequisites

- [x] Task 007: S3 storage implemented

## Scope

**Files to Create**:
- `services/reporting-service/src/main/java/com/turaf/reporting/event/EventPublisher.java`
- `services/reporting-service/src/main/java/com/turaf/reporting/event/ReportGeneratedEvent.java`

## Implementation Details

### Event Publisher

```java
public class EventPublisher {
    private final EventBridgeClient eventBridgeClient;
    private final String eventBusName;
    
    public EventPublisher() {
        this.eventBridgeClient = EventBridgeClient.builder()
            .region(Region.US_EAST_1)
            .build();
        this.eventBusName = System.getenv("EVENT_BUS_NAME");
    }
    
    public void publishReportGenerated(String organizationId, String experimentId, String reportUrl) {
        ReportGeneratedEvent event = new ReportGeneratedEvent(
            UUID.randomUUID().toString(),
            organizationId,
            experimentId,
            reportUrl,
            Instant.now()
        );
        
        String eventJson = serializeEvent(event);
        
        PutEventsRequestEntry entry = PutEventsRequestEntry.builder()
            .eventBusName(eventBusName)
            .source("turaf.reporting-service")
            .detailType("ReportGenerated")
            .detail(eventJson)
            .build();
        
        PutEventsRequest request = PutEventsRequest.builder()
            .entries(entry)
            .build();
        
        eventBridgeClient.putEvents(request);
    }
    
    private String serializeEvent(ReportGeneratedEvent event) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event", e);
        }
    }
}
```

## Acceptance Criteria

- [ ] Event publisher implemented
- [ ] ReportGenerated events published
- [ ] Event structure correct
- [ ] Error handling implemented
- [ ] Unit tests pass

## Testing Requirements

**Unit Tests**:
- Test event publishing
- Test event serialization

**Test Files to Create**:
- `EventPublisherTest.java`

## References

- Specification: `specs/reporting-service.md` (Event Publishing section)
- Specification: `specs/event-schemas.md`
- Related Tasks: 009-add-idempotency
