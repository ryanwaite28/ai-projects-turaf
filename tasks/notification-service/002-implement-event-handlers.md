# Task: Implement Event Handlers

**Service**: Notification Service  
**Phase**: 8  
**Estimated Time**: 3 hours  

## Objective

Implement EventBridge event handlers for various domain events that trigger notifications.

## Prerequisites

- [x] Task 001: Lambda project setup

## Scope

**Files to Create**:
- `services/notification-service/src/main/java/com/turaf/notification/handler/EventRouter.java`
- `services/notification-service/src/main/java/com/turaf/notification/handler/ExperimentCompletedHandler.java`
- `services/notification-service/src/main/java/com/turaf/notification/handler/ReportGeneratedHandler.java`
- `services/notification-service/src/main/java/com/turaf/notification/handler/MemberAddedHandler.java`

## Implementation Details

### Event Router

```java
public class EventRouter implements RequestHandler<EventBridgeEvent, Void> {
    private final Map<String, EventHandler> handlers;
    private final IdempotencyService idempotencyService;
    
    public EventRouter() {
        this.handlers = Map.of(
            "ExperimentCompleted", new ExperimentCompletedHandler(),
            "ReportGenerated", new ReportGeneratedHandler(),
            "MemberAdded", new MemberAddedHandler()
        );
        this.idempotencyService = new IdempotencyService();
    }
    
    @Override
    public Void handleRequest(EventBridgeEvent event, Context context) {
        LambdaLogger logger = context.getLogger();
        
        try {
            String eventType = event.getDetailType();
            String eventId = extractEventId(event);
            
            // Check idempotency
            if (idempotencyService.isProcessed(eventId)) {
                logger.log("Event already processed: " + eventId);
                return null;
            }
            
            // Route to appropriate handler
            EventHandler handler = handlers.get(eventType);
            if (handler != null) {
                handler.handle(event, context);
                idempotencyService.markProcessed(eventId);
            } else {
                logger.log("No handler for event type: " + eventType);
            }
        } catch (Exception e) {
            logger.log("Error processing event: " + e.getMessage());
            throw new RuntimeException("Failed to process event", e);
        }
        
        return null;
    }
    
    private String extractEventId(EventBridgeEvent event) {
        Map<String, Object> detail = (Map<String, Object>) event.getDetail();
        return (String) detail.get("eventId");
    }
}
```

### Experiment Completed Handler

```java
public class ExperimentCompletedHandler implements EventHandler {
    private final EmailService emailService;
    
    @Override
    public void handle(EventBridgeEvent event, Context context) {
        ExperimentCompletedEvent experimentEvent = parseEvent(event);
        
        // Send notification to experiment owner
        emailService.sendExperimentCompletedEmail(
            experimentEvent.getOrganizationId(),
            experimentEvent.getExperimentId()
        );
    }
}
```

## Acceptance Criteria

- [ ] Event router dispatches to correct handlers
- [ ] All event handlers implemented
- [ ] Idempotency check works
- [ ] Error handling implemented
- [ ] Unit tests pass

## Testing Requirements

**Unit Tests**:
- Test event routing
- Test each handler
- Test idempotency

**Test Files to Create**:
- `EventRouterTest.java`
- `ExperimentCompletedHandlerTest.java`

## References

- Specification: `specs/notification-service.md` (Event Handlers section)
- Related Tasks: 003-implement-email-service
