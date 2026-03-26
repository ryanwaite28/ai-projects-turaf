# Event Flow Specification

**Source**: PROJECT.md (Sections 13, 27)  
**Last Updated**: March 25, 2026  
**Status**: Current  
**Related Documents**: [Event Schemas](event-schemas.md), [Architecture](architecture.md), [AWS Infrastructure](aws-infrastructure.md)

This specification defines the event-driven architecture, event flow patterns, and event schemas for the Turaf platform.

---

## Event-Driven Architecture Overview

### Core Concept
The system uses **domain events** to enable asynchronous, loosely-coupled communication between services.

**Benefits**:
- Services don't need direct knowledge of each other
- New consumers can be added without modifying producers
- Enables audit trails and event sourcing
- Supports eventual consistency
- Facilitates scalability

---

## Event Envelope Standard

All events must follow this standardized envelope structure:

```json
{
  "eventId": "uuid",
  "eventType": "string",
  "eventVersion": 1,
  "timestamp": "ISO-8601",
  "sourceService": "string",
  "organizationId": "string",
  "payload": {}
}
```

### Envelope Fields

**eventId** (UUID):
- Unique identifier for this event instance
- Used for deduplication
- Used for event tracking and debugging

**eventType** (String):
- Event name (e.g., "ExperimentCompleted")
- Used for routing and filtering
- PascalCase naming convention

**eventVersion** (Integer):
- Schema version number
- Enables event schema evolution
- Consumers can handle multiple versions

**timestamp** (ISO-8601 String):
- When the event occurred
- UTC timezone
- Format: `2024-03-14T22:30:00.000Z`

**sourceService** (String):
- Service that published the event
- Used for tracing and debugging
- Examples: "experiment-service", "metrics-service"

**organizationId** (String):
- Tenant context for multi-tenancy
- Used for filtering and authorization
- All events are scoped to an organization

**payload** (Object):
- Event-specific data
- Schema varies by event type
- Contains domain-relevant information

---

## Domain Events

### ProblemCreated

**Published By**: Experiment Service  
**Triggered When**: A new problem is created

**Payload**:
```json
{
  "problemId": "uuid",
  "title": "string",
  "description": "string",
  "createdBy": "userId",
  "createdAt": "ISO-8601"
}
```

**Consumers**: None currently (future: analytics service)

---

### HypothesisCreated

**Published By**: Experiment Service  
**Triggered When**: A new hypothesis is created for a problem

**Payload**:
```json
{
  "hypothesisId": "uuid",
  "problemId": "uuid",
  "statement": "string",
  "createdBy": "userId",
  "createdAt": "ISO-8601"
}
```

**Consumers**: None currently (future: analytics service)

---

### ExperimentStarted

**Published By**: Experiment Service  
**Triggered When**: An experiment transitions to RUNNING state

**Payload**:
```json
{
  "experimentId": "uuid",
  "hypothesisId": "uuid",
  "name": "string",
  "startTime": "ISO-8601",
  "expectedEndTime": "ISO-8601"
}
```

**Consumers**: 
- Notification Service (send start notification)

---

### MetricRecorded

**Published By**: Metrics Service  
**Triggered When**: A metric is recorded for an experiment

**Payload**:
```json
{
  "metricId": "uuid",
  "experimentId": "uuid",
  "metricName": "string",
  "metricValue": "number",
  "unit": "string",
  "recordedAt": "ISO-8601",
  "metadata": {}
}
```

**Consumers**: None currently (future: real-time analytics)

---

### ExperimentCompleted

**Published By**: Experiment Service  
**Triggered When**: An experiment transitions to COMPLETED state

**Payload**:
```json
{
  "experimentId": "uuid",
  "hypothesisId": "uuid",
  "endTime": "ISO-8601",
  "resultSummary": "string",
  "outcome": "string"
}
```

**Consumers**:
- Reporting Service (generate report)
- Notification Service (send completion notification)

**Critical Event**: Triggers multiple downstream processes

---

### ReportGenerated

**Published By**: Reporting Service  
**Triggered When**: A report is successfully generated and stored

**Payload**:
```json
{
  "reportId": "uuid",
  "experimentId": "uuid",
  "reportLocation": "s3://bucket/path",
  "reportFormat": "PDF",
  "generatedAt": "ISO-8601"
}
```

**Consumers**:
- Notification Service (send report ready notification)

---

### OrganizationCreated

**Published By**: Organization Service  
**Triggered When**: A new organization is created

**Payload**:
```json
{
  "organizationId": "uuid",
  "name": "string",
  "slug": "string",
  "createdBy": "userId",
  "createdAt": "ISO-8601"
}
```

**Consumers**: None currently (future: onboarding service)

---

### MemberAdded

**Published By**: Organization Service  
**Triggered When**: A user is added to an organization

**Payload**:
```json
{
  "organizationId": "uuid",
  "userId": "uuid",
  "role": "string",
  "addedBy": "userId",
  "addedAt": "ISO-8601"
}
```

**Consumers**:
- Notification Service (send welcome email)

---

## Event Flow Patterns

### Pattern 1: Simple Event Flow

```
Service → Domain Event → Event Bus → Single Consumer
```

**Example**: ProblemCreated event (no consumers currently)

---

### Pattern 2: Fan-Out Event Flow

```
Service → Domain Event → Event Bus → Multiple Consumers
```

**Example**: ExperimentCompleted event
- Event Bus routes to Reporting Service
- Event Bus routes to Notification Service
- Both consumers process independently

---

### Pattern 3: Event Chain Flow

```
Service A → Event 1 → Event Bus → Service B → Event 2 → Event Bus → Service C
```

**Example**: Experiment completion workflow
1. Experiment Service publishes ExperimentCompleted
2. Reporting Service consumes, generates report
3. Reporting Service publishes ReportGenerated
4. Notification Service consumes, sends notification

---

## Event Processing Guarantees

### At-Least-Once Delivery
- EventBridge guarantees at-least-once delivery
- Events may be delivered multiple times
- **Requirement**: All event handlers must be idempotent

### Idempotency Implementation
- Use `eventId` for deduplication
- Store processed event IDs in database
- Check before processing: "Have I seen this eventId before?"
- If yes, skip processing; if no, process and record

### Retry Policy
- Failed events automatically retried
- Exponential backoff (1s, 2s, 4s, 8s, 16s)
- Maximum 5 retry attempts
- After max retries, sent to Dead Letter Queue

---

## Event Bus Configuration

### AWS EventBridge Setup

**Event Bus Name**: `turaf-event-bus-{env}`

**Event Rules**:
- Rule per event type
- Pattern matching on `eventType` field
- Routes to appropriate Lambda functions or SQS queues

**Example Rule Pattern**:
```json
{
  "source": ["turaf.experiment-service"],
  "detail-type": ["ExperimentCompleted"]
}
```

---

## Event Publishing

### From Spring Boot Services

**Implementation**:
```java
// Pseudocode - actual implementation in infrastructure layer
@Service
public class EventPublisher {
    private final EventBridgeClient eventBridge;
    
    public void publish(DomainEvent event) {
        PutEventsRequestEntry entry = PutEventsRequestEntry.builder()
            .eventBusName("turaf-event-bus")
            .source("turaf." + event.getSourceService())
            .detailType(event.getEventType())
            .detail(toJson(event))
            .build();
            
        eventBridge.putEvents(r -> r.entries(entry));
    }
}
```

**Best Practices**:
- Publish events in same transaction as domain changes
- Use outbox pattern for guaranteed delivery
- Include all necessary context in payload
- Never include sensitive data (passwords, tokens)

---

## Event Consumption

### Lambda Function Handlers

**Implementation**:
```java
// Pseudocode - actual implementation in Lambda functions
@Component
public class ExperimentCompletedHandler implements Function<EventBridgeEvent, Void> {
    
    @Override
    public Void apply(EventBridgeEvent event) {
        String eventId = event.getEventId();
        
        // Check idempotency
        if (alreadyProcessed(eventId)) {
            return null;
        }
        
        // Process event
        ExperimentCompletedPayload payload = parsePayload(event);
        generateReport(payload.getExperimentId());
        
        // Record processing
        markAsProcessed(eventId);
        
        return null;
    }
}
```

**Best Practices**:
- Implement idempotency checks
- Handle errors gracefully
- Log all event processing
- Emit metrics for monitoring

---

## Event Versioning Strategy

### Version Evolution

**Adding Fields** (Non-Breaking):
- Add new optional fields to payload
- Increment `eventVersion` to 2
- Old consumers ignore new fields
- New consumers use new fields

**Removing Fields** (Breaking):
- Deprecate old event type
- Create new event type with different name
- Run both versions in parallel during migration
- Eventually remove old version

**Changing Field Types** (Breaking):
- Treat as new event type
- Follow removal strategy above

---

## Event Monitoring

### Metrics to Track
- Events published per type
- Event processing latency
- Failed event count
- Dead letter queue depth
- Event replay count

### Alerting
- Alert on high failure rate (>5%)
- Alert on DLQ depth (>10 messages)
- Alert on processing latency (>30 seconds)

---

## Event Replay

### Use Cases
- Recover from processing failures
- Rebuild read models
- Backfill analytics
- Test new consumers

### Implementation
- EventBridge archives all events
- Replay from archive by time range
- Replay to specific event bus or target
- Idempotent handlers prevent duplicate side effects

---

## Dead Letter Queue Handling

### DLQ Strategy
- One DLQ per event consumer
- Failed events sent after max retries
- Manual investigation required
- Options: fix and replay, or discard

### DLQ Monitoring
- CloudWatch alarm on queue depth
- Daily review of DLQ messages
- Root cause analysis for failures
- Update code to prevent recurrence

---

## Event Ordering

### No Guaranteed Order
- EventBridge does not guarantee order
- Events may arrive out of sequence
- **Design Requirement**: Handlers must be order-independent

### Handling Out-of-Order Events
- Use timestamps for logical ordering
- Use version numbers on entities
- Implement optimistic locking
- Design idempotent operations

---

## Event Security

### Authorization
- Events include `organizationId`
- Consumers validate organization context
- Cross-organization events blocked
- Audit log all event processing

### Data Privacy
- No PII in event payloads (use IDs)
- Encrypt sensitive event data
- Comply with data retention policies
- Support GDPR right to erasure

---

## Testing Events

### Unit Testing
- Test event serialization/deserialization
- Test event handler logic in isolation
- Mock event bus interactions

### Integration Testing
- Use LocalStack for EventBridge
- Publish events to local bus
- Verify consumers process correctly
- Test idempotency behavior

### End-to-End Testing
- Test complete event chains
- Verify all side effects occur
- Test error scenarios and retries
- Validate monitoring and alerting

---

## References

- PROJECT.md: Event definitions and workflows
- AWS EventBridge Documentation
- Event-Driven Architecture patterns
- Domain-Driven Design events
