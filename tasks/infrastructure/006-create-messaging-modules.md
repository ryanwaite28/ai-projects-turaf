# Task: Create Messaging Modules

**Service**: Infrastructure  
**Phase**: 10  
**Estimated Time**: 3 hours  

## Objective

Create Terraform module for EventBridge event bus, rules, and SQS queues.

## Prerequisites

- [x] Task 001: Terraform structure setup

## Scope

**Files to Create**:
- `infrastructure/terraform/modules/messaging/eventbridge.tf`
- `infrastructure/terraform/modules/messaging/sqs.tf`
- `infrastructure/terraform/modules/messaging/variables.tf`
- `infrastructure/terraform/modules/messaging/outputs.tf`

## Implementation Details

### EventBridge

```hcl
resource "aws_cloudwatch_event_bus" "main" {
  name = "turaf-event-bus-${var.environment}"
  
  tags = {
    Name        = "turaf-event-bus-${var.environment}"
    Environment = var.environment
  }
}

resource "aws_cloudwatch_event_rule" "experiment_completed" {
  name           = "experiment-completed-${var.environment}"
  event_bus_name = aws_cloudwatch_event_bus.main.name
  
  event_pattern = jsonencode({
    source      = ["turaf.experiment-service"]
    detail-type = ["ExperimentCompleted"]
  })
}

resource "aws_cloudwatch_event_target" "reporting_lambda" {
  rule           = aws_cloudwatch_event_rule.experiment_completed.name
  event_bus_name = aws_cloudwatch_event_bus.main.name
  arn            = var.reporting_lambda_arn
}

resource "aws_cloudwatch_event_target" "notification_lambda" {
  rule           = aws_cloudwatch_event_rule.experiment_completed.name
  event_bus_name = aws_cloudwatch_event_bus.main.name
  arn            = var.notification_lambda_arn
}
```

### SQS Queues

```hcl
resource "aws_sqs_queue" "dlq" {
  name                      = "turaf-dlq-${var.environment}"
  message_retention_seconds = 1209600  # 14 days
  
  tags = {
    Name        = "turaf-dlq-${var.environment}"
    Environment = var.environment
  }
}

resource "aws_sqs_queue" "events" {
  name                       = "turaf-events-${var.environment}"
  visibility_timeout_seconds = 300
  message_retention_seconds  = 345600  # 4 days
  
  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.dlq.arn
    maxReceiveCount     = 3
  })
  
  tags = {
    Name        = "turaf-events-${var.environment}"
    Environment = var.environment
  }
}
```

## Acceptance Criteria

- [ ] EventBridge event bus created
- [ ] Event rules configured
- [ ] Event targets set
- [ ] SQS queues created
- [ ] DLQ configured
- [ ] terraform plan succeeds

## Testing Requirements

**Validation**:
- Run `terraform plan`
- Verify event patterns
- Check queue configurations

## References

- Specification: `specs/aws-infrastructure.md` (Messaging section)
- Related Tasks: 007-create-lambda-module
