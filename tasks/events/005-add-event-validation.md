# Task: Add Event Validation

**Service**: Events Infrastructure  
**Phase**: 6  
**Estimated Time**: 2 hours  

## Objective

Implement event validation to ensure all published events conform to the standardized schema.

## Prerequisites

- [x] Task 001: Event envelope implemented
- [x] Task 002: Event publisher implemented

## Scope

**Files to Create**:
- `services/common/src/main/java/com/turaf/common/events/EventValidator.java`
- `services/common/src/main/java/com/turaf/common/events/EventValidationException.java`
- `services/common/src/main/java/com/turaf/common/events/EventSchema.java`

## Implementation Details

### Event Validator

```java
@Component
public class EventValidator {
    
    public void validate(DomainEvent event) {
        validateEventId(event.getEventId());
        validateEventType(event.getEventType());
        validateTimestamp(event.getTimestamp());
        validateOrganizationId(event.getOrganizationId());
    }
    
    public void validate(EventEnvelope envelope) {
        if (envelope.getEventId() == null || envelope.getEventId().isBlank()) {
            throw new EventValidationException("Event ID is required");
        }
        
        if (envelope.getEventType() == null || envelope.getEventType().isBlank()) {
            throw new EventValidationException("Event type is required");
        }
        
        if (envelope.getEventVersion() < 1) {
            throw new EventValidationException("Event version must be >= 1");
        }
        
        if (envelope.getTimestamp() == null) {
            throw new EventValidationException("Timestamp is required");
        }
        
        if (envelope.getTimestamp().isAfter(Instant.now().plusSeconds(60))) {
            throw new EventValidationException("Timestamp cannot be in the future");
        }
        
        if (envelope.getSourceService() == null || envelope.getSourceService().isBlank()) {
            throw new EventValidationException("Source service is required");
        }
        
        if (envelope.getOrganizationId() == null || envelope.getOrganizationId().isBlank()) {
            throw new EventValidationException("Organization ID is required");
        }
        
        if (envelope.getPayload() == null) {
            throw new EventValidationException("Payload is required");
        }
    }
    
    private void validateEventId(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            throw new EventValidationException("Event ID is required");
        }
        
        try {
            UUID.fromString(eventId);
        } catch (IllegalArgumentException e) {
            throw new EventValidationException("Event ID must be a valid UUID");
        }
    }
    
    private void validateEventType(String eventType) {
        if (eventType == null || eventType.isBlank()) {
            throw new EventValidationException("Event type is required");
        }
        
        if (!eventType.matches("^[A-Z][a-zA-Z]+$")) {
            throw new EventValidationException("Event type must be PascalCase");
        }
    }
    
    private void validateTimestamp(Instant timestamp) {
        if (timestamp == null) {
            throw new EventValidationException("Timestamp is required");
        }
        
        if (timestamp.isAfter(Instant.now().plusSeconds(60))) {
            throw new EventValidationException("Timestamp cannot be in the future");
        }
    }
    
    private void validateOrganizationId(String organizationId) {
        if (organizationId == null || organizationId.isBlank()) {
            throw new EventValidationException("Organization ID is required");
        }
    }
}
```

### Updated Event Publisher

```java
@Component
public class EventBridgeEventPublisher implements EventPublisher {
    private final EventBridgeClient eventBridgeClient;
    private final EventSerializer serializer;
    private final EventValidator validator;
    private final String eventBusName;
    private final String sourceService;
    
    @Override
    public void publish(DomainEvent event) {
        // Validate before publishing
        validator.validate(event);
        
        EventEnvelope envelope = EventEnvelope.wrap(event, sourceService);
        validator.validate(envelope);
        
        // ... rest of publishing logic
    }
}
```

## Acceptance Criteria

- [ ] Event validator implemented
- [ ] All required fields validated
- [ ] Event ID format validated (UUID)
- [ ] Event type format validated (PascalCase)
- [ ] Timestamp validated (not in future)
- [ ] Validation integrated into publisher
- [ ] Unit tests pass

## Testing Requirements

**Unit Tests**:
- Test valid events pass validation
- Test invalid event ID fails
- Test invalid event type fails
- Test future timestamp fails
- Test missing required fields fail

**Test Files to Create**:
- `EventValidatorTest.java`

## References

- Specification: `specs/event-schemas.md` (Validation section)
- Specification: `specs/event-flow.md`
- Related Tasks: All events tasks
