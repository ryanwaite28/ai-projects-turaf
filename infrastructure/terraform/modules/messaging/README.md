# Messaging Module

This Terraform module configures the event-driven messaging infrastructure for the Turaf platform using AWS EventBridge.

## Overview

The messaging module sets up:
- **EventBridge Event Bus**: Central event bus for all domain events
- **EventBridge Rules**: Route events to appropriate Lambda functions
- **Dead Letter Queue**: SQS queue for failed event deliveries
- **Lambda Permissions**: Allow EventBridge to invoke Lambda functions
- **Event Archive**: 30-day archive for event replay and debugging
- **CloudWatch Alarms**: Monitor DLQ for failed events

## Architecture

```
┌─────────────────┐
│  Microservices  │
│  (Publishers)   │
└────────┬────────┘
         │ publish events
         ▼
┌─────────────────────────┐
│  EventBridge Event Bus  │
│  turaf-event-bus-{env}  │
└────────┬────────────────┘
         │
         ├─────────────────────────────────┐
         │                                 │
         ▼                                 ▼
┌──────────────────┐            ┌──────────────────┐
│  Event Rules     │            │  Event Archive   │
│  (Pattern Match) │            │  (30 days)       │
└────────┬─────────┘            └──────────────────┘
         │
         ├──────────┬──────────┬──────────┐
         ▼          ▼          ▼          ▼
    ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐
    │Reporting│ │Notif.  │ │Notif.  │ │Notif.  │
    │Lambda   │ │Lambda  │ │Lambda  │ │Lambda  │
    └────────┘ └────────┘ └────────┘ └────────┘
         │
         │ (on failure)
         ▼
    ┌─────────┐
    │   DLQ   │
    │  (SQS)  │
    └─────────┘
```

## Event Routing

### ExperimentCompleted
- **Source**: `turaf.experiment-service`
- **Targets**: 
  - Reporting Lambda (generates experiment reports)
  - Notification Lambda (sends completion notifications)
- **Retry**: 3 attempts, 1 hour max age
- **DLQ**: Enabled

### ReportGenerated
- **Source**: `turaf.reporting-service`
- **Targets**: 
  - Notification Lambda (sends report ready notifications)
- **Retry**: 3 attempts, 1 hour max age
- **DLQ**: Enabled

### OrganizationCreated
- **Source**: `turaf.organization-service`
- **Targets**: 
  - Notification Lambda (sends welcome notifications)
- **Retry**: 3 attempts, 1 hour max age
- **DLQ**: Enabled

### OrganizationUpdated
- **Source**: `turaf.organization-service`
- **Targets**: 
  - Notification Lambda (sends update notifications)
- **Retry**: 3 attempts, 1 hour max age

### UserCreated
- **Source**: `turaf.identity-service`
- **Targets**: 
  - Notification Lambda (sends welcome emails)
- **Retry**: 3 attempts, 1 hour max age
- **DLQ**: Enabled

### MetricsCalculated
- **Source**: `turaf.metrics-service`
- **Targets**: 
  - Notification Lambda (sends metrics alerts)
- **Retry**: 3 attempts, 1 hour max age

## Event Pattern Format

All events follow the standardized EventEnvelope schema:

```json
{
  "source": "turaf.{service-name}",
  "detail-type": "{EventType}",
  "detail": {
    "eventId": "uuid",
    "eventType": "EventType",
    "eventVersion": 1,
    "timestamp": "2024-03-20T12:00:00Z",
    "sourceService": "service-name",
    "organizationId": "org-123",
    "payload": { ... },
    "metadata": {
      "correlationId": "uuid",
      "causationId": "uuid",
      "customMetadata": {}
    }
  }
}
```

## Retry Policy

All event targets use the following retry configuration:
- **Maximum Event Age**: 3600 seconds (1 hour)
- **Maximum Retry Attempts**: 3
- **Backoff**: Exponential (managed by EventBridge)

After exhausting retries, events are sent to the Dead Letter Queue.

## Dead Letter Queue

Failed events are sent to an SQS queue for manual inspection and replay:
- **Name**: `turaf-eventbridge-dlq-{environment}`
- **Retention**: 14 days
- **Alarm**: Triggers when messages appear in DLQ

## Variables

| Name | Description | Type | Required |
|------|-------------|------|----------|
| environment | Environment name (dev, staging, prod) | string | yes |
| reporting_lambda_arn | ARN of the reporting Lambda function | string | yes |
| reporting_lambda_name | Name of the reporting Lambda function | string | yes |
| notification_lambda_arn | ARN of the notification Lambda function | string | yes |
| notification_lambda_name | Name of the notification Lambda function | string | yes |

## Outputs

| Name | Description |
|------|-------------|
| event_bus_name | Name of the EventBridge event bus |
| event_bus_arn | ARN of the EventBridge event bus |
| event_archive_arn | ARN of the event archive |
| experiment_completed_rule_arn | ARN of the ExperimentCompleted rule |
| report_generated_rule_arn | ARN of the ReportGenerated rule |
| organization_created_rule_arn | ARN of the OrganizationCreated rule |
| user_created_rule_arn | ARN of the UserCreated rule |
| dlq_url | URL of the dead letter queue |
| dlq_arn | ARN of the dead letter queue |

## Usage

```hcl
module "messaging" {
  source = "./modules/messaging"
  
  environment = var.environment
  
  reporting_lambda_arn  = module.reporting_lambda.function_arn
  reporting_lambda_name = module.reporting_lambda.function_name
  
  notification_lambda_arn  = module.notification_lambda.function_arn
  notification_lambda_name = module.notification_lambda.function_name
}
```

## Monitoring

### CloudWatch Alarms
- **DLQ Messages**: Alerts when events fail and land in DLQ
- **Threshold**: > 0 messages

### CloudWatch Logs
- **Log Group**: `/aws/events/turaf-{environment}`
- **Retention**: 7 days
- **Purpose**: Debugging event routing issues

## Event Replay

The event archive allows replaying events for:
- **Disaster Recovery**: Replay events after system failures
- **Testing**: Replay production events in staging
- **Debugging**: Investigate event processing issues

To replay events:
1. Create a replay in AWS Console or via CLI
2. Specify time range and destination
3. Events are re-sent to the event bus

## Best Practices

1. **Idempotency**: All Lambda handlers should use IdempotencyService
2. **Monitoring**: Monitor DLQ for failed events
3. **Testing**: Test event patterns before deploying
4. **Versioning**: Use eventVersion for schema evolution
5. **Correlation**: Use correlationId for distributed tracing

## Related Documentation

- [Event Flow Specification](../../../specs/event-flow.md)
- [AWS Infrastructure](../../../specs/aws-infrastructure.md)
- [Event Envelope Implementation](../../../services/common/src/main/java/com/turaf/common/events/EventEnvelope.java)
