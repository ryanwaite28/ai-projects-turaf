# Task: Implement Event Handler

**Service**: Reporting Service  
**Phase**: 7  
**Estimated Time**: 2-3 hours  

## Objective

Implement EventBridge event handler for ExperimentCompleted events to trigger report generation.

## Prerequisites

- [x] Task 001: Lambda project setup

## Scope

**Files to Create**:
- `services/reporting-service/src/main/java/com/turaf/reporting/handler/ExperimentCompletedHandler.java`
- `services/reporting-service/src/main/java/com/turaf/reporting/model/ExperimentCompletedEvent.java`
- `services/reporting-service/src/main/java/com/turaf/reporting/service/ReportGenerationService.java`

## Implementation Details

### Event Handler

```java
public class ExperimentCompletedHandler implements RequestHandler<EventBridgeEvent, Void> {
    private final ReportGenerationService reportGenerationService;
    private final IdempotencyService idempotencyService;
    
    public ExperimentCompletedHandler() {
        this.reportGenerationService = new ReportGenerationService();
        this.idempotencyService = new IdempotencyService();
    }
    
    @Override
    public Void handleRequest(EventBridgeEvent event, Context context) {
        LambdaLogger logger = context.getLogger();
        
        try {
            ExperimentCompletedEvent experimentEvent = parseEvent(event);
            
            // Check idempotency
            if (idempotencyService.isProcessed(experimentEvent.getEventId())) {
                logger.log("Event already processed: " + experimentEvent.getEventId());
                return null;
            }
            
            // Generate report
            reportGenerationService.generateReport(experimentEvent);
            
            // Mark as processed
            idempotencyService.markProcessed(experimentEvent.getEventId());
            
            logger.log("Report generated for experiment: " + experimentEvent.getExperimentId());
        } catch (Exception e) {
            logger.log("Error processing event: " + e.getMessage());
            throw new RuntimeException("Failed to process event", e);
        }
        
        return null;
    }
    
    private ExperimentCompletedEvent parseEvent(EventBridgeEvent event) {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.convertValue(event.getDetail(), ExperimentCompletedEvent.class);
    }
}
```

## Acceptance Criteria

- [ ] Event handler processes ExperimentCompleted events
- [ ] Event parsing works correctly
- [ ] Idempotency check implemented
- [ ] Error handling implemented
- [ ] Logging implemented
- [ ] Unit tests pass

## Testing Requirements

**Unit Tests**:
- Test event parsing
- Test idempotency check
- Test error handling

**Test Files to Create**:
- `ExperimentCompletedHandlerTest.java`

## References

- Specification: `specs/reporting-service.md` (Event Handler section)
- Related Tasks: 003-implement-data-fetching
