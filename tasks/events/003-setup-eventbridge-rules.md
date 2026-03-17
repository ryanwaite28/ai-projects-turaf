# Task: Setup EventBridge Rules

**Service**: Events Infrastructure  
**Phase**: 6  
**Estimated Time**: 2 hours  

## Objective

Configure EventBridge rules for routing events to appropriate targets (Lambda functions, SQS queues).

## Prerequisites

- [x] Task 002: Event publisher implemented
- [ ] Lambda functions deployed

## Scope

**Files to Create**:
- `infrastructure/terraform/modules/messaging/eventbridge-rules.tf`

## Implementation Details

### EventBridge Rules

```hcl
# Rule for ExperimentCompleted events
resource "aws_cloudwatch_event_rule" "experiment_completed" {
  name           = "experiment-completed-${var.environment}"
  event_bus_name = aws_cloudwatch_event_bus.main.name
  
  event_pattern = jsonencode({
    source      = ["turaf.experiment-service"]
    detail-type = ["ExperimentCompleted"]
  })
  
  tags = {
    Name = "experiment-completed-${var.environment}"
  }
}

# Target: Reporting Lambda
resource "aws_cloudwatch_event_target" "experiment_completed_reporting" {
  rule           = aws_cloudwatch_event_rule.experiment_completed.name
  event_bus_name = aws_cloudwatch_event_bus.main.name
  arn            = var.reporting_lambda_arn
  
  retry_policy {
    maximum_event_age       = 3600
    maximum_retry_attempts  = 3
  }
  
  dead_letter_config {
    arn = aws_sqs_queue.dlq.arn
  }
}

# Target: Notification Lambda
resource "aws_cloudwatch_event_target" "experiment_completed_notification" {
  rule           = aws_cloudwatch_event_rule.experiment_completed.name
  event_bus_name = aws_cloudwatch_event_bus.main.name
  arn            = var.notification_lambda_arn
  
  retry_policy {
    maximum_event_age       = 3600
    maximum_retry_attempts  = 3
  }
}

# Rule for ReportGenerated events
resource "aws_cloudwatch_event_rule" "report_generated" {
  name           = "report-generated-${var.environment}"
  event_bus_name = aws_cloudwatch_event_bus.main.name
  
  event_pattern = jsonencode({
    source      = ["turaf.reporting-service"]
    detail-type = ["ReportGenerated"]
  })
}

resource "aws_cloudwatch_event_target" "report_generated_notification" {
  rule           = aws_cloudwatch_event_rule.report_generated.name
  event_bus_name = aws_cloudwatch_event_bus.main.name
  arn            = var.notification_lambda_arn
}

# Rule for OrganizationCreated events
resource "aws_cloudwatch_event_rule" "organization_created" {
  name           = "organization-created-${var.environment}"
  event_bus_name = aws_cloudwatch_event_bus.main.name
  
  event_pattern = jsonencode({
    source      = ["turaf.organization-service"]
    detail-type = ["OrganizationCreated"]
  })
}

# Lambda permissions
resource "aws_lambda_permission" "allow_eventbridge_reporting" {
  statement_id  = "AllowExecutionFromEventBridge"
  action        = "lambda:InvokeFunction"
  function_name = var.reporting_lambda_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.experiment_completed.arn
}

resource "aws_lambda_permission" "allow_eventbridge_notification" {
  statement_id  = "AllowExecutionFromEventBridge"
  action        = "lambda:InvokeFunction"
  function_name = var.notification_lambda_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.experiment_completed.arn
}
```

## Acceptance Criteria

- [ ] EventBridge rules created for all event types
- [ ] Rules route to correct targets
- [ ] Retry policies configured
- [ ] DLQ configured for failed events
- [ ] Lambda permissions granted
- [ ] terraform apply succeeds

## Testing Requirements

**Validation**:
- Publish test events
- Verify routing to targets
- Check DLQ for failures

## References

- Specification: `specs/event-flow.md` (EventBridge Configuration section)
- Specification: `specs/aws-infrastructure.md` (Messaging section)
- Related Tasks: 004-implement-idempotency-tracking
