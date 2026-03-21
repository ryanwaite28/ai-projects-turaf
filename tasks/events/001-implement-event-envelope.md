# Task: Implement Event Envelope

**Service**: Events Infrastructure  
**Phase**: 6  
**Estimated Time**: 2 hours  

## Objective

Implement standardized event envelope structure for all domain events across the platform.

## Prerequisites

- [x] Task 002: DDD patterns implemented

## Scope

**Files to Create**:
- `services/common/src/main/java/com/turaf/common/events/EventEnvelope.java`
- `services/common/src/main/java/com/turaf/common/events/DomainEvent.java`
- `services/common/src/main/java/com/turaf/common/events/EventMetadata.java`

## Implementation Details

### Event Envelope

```java
public class EventEnvelope {
    private final String eventId;
    private final String eventType;
    private final int eventVersion;
    private final Instant timestamp;
    private final String sourceService;
    private final String organizationId;
    private final Object payload;
    private final EventMetadata metadata;
    
    public EventEnvelope(String eventId, String eventType, int eventVersion,
                        Instant timestamp, String sourceService, String organizationId,
                        Object payload, EventMetadata metadata) {
        this.eventId = Objects.requireNonNull(eventId);
        this.eventType = Objects.requireNonNull(eventType);
        this.eventVersion = eventVersion;
        this.timestamp = Objects.requireNonNull(timestamp);
        this.sourceService = Objects.requireNonNull(sourceService);
        this.organizationId = Objects.requireNonNull(organizationId);
        this.payload = Objects.requireNonNull(payload);
        this.metadata = metadata != null ? metadata : new EventMetadata();
    }
    
    public static EventEnvelope wrap(DomainEvent event, String sourceService) {
        return new EventEnvelope(
            event.getEventId(),
            event.getEventType(),
            1,
            event.getTimestamp(),
            sourceService,
            event.getOrganizationId(),
            event,
            new EventMetadata()
        );
    }
    
    // Getters
}
```

### Event Metadata

```java
public class EventMetadata {
    private String correlationId;
    private String causationId;
    private Map<String, String> customMetadata;
    
    public EventMetadata() {
        this.customMetadata = new HashMap<>();
    }
    
    public void addMetadata(String key, String value) {
        customMetadata.put(key, value);
    }
    
    // Getters, setters
}
```

### Domain Event Interface

```java
public interface DomainEvent {
    String getEventId();
    String getEventType();
    Instant getTimestamp();
    String getOrganizationId();
}
```

## Acceptance Criteria

- [x] Event envelope structure defined
- [x] All required fields present
- [x] Event metadata support added
- [x] Versioning supported
- [x] Correlation/causation tracking enabled
- [x] Unit tests pass

## Testing Requirements

**Unit Tests**:
- Test event envelope creation
- Test event wrapping
- Test metadata handling

**Test Files to Create**:
- `EventEnvelopeTest.java`

## References

- Specification: `specs/event-schemas.md` (Event Envelope section)
- Specification: `specs/event-flow.md`
- PROJECT.md: Section 39 (Event Schemas)
- Related Tasks: 002-implement-event-publisher
