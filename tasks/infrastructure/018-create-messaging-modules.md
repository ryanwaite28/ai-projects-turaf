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

- [x] EventBridge event bus created
- [x] Event rules configured
- [x] Event targets set (in eventbridge-rules.tf)
- [x] SQS queues created
- [x] DLQ configured
- [x] Optional queues (chat, notifications, reports)
- [x] Optional SNS topics
- [x] CloudWatch alarms (optional)
- [x] Module outputs created
- [x] Cost optimization variables added
- [ ] terraform plan succeeds (requires environment configuration)

## Implementation Results (2024-03-23)

### ✅ Module Completed

**Files Present**:
- ✅ `infrastructure/terraform/modules/messaging/main.tf` (271 lines) - SQS queues, SNS topics, alarms
- ✅ `infrastructure/terraform/modules/messaging/eventbridge.tf` (58 lines) - Event bus and archive
- ✅ `infrastructure/terraform/modules/messaging/eventbridge-rules.tf` (existing) - Event routing rules
- ✅ `infrastructure/terraform/modules/messaging/idempotency-table.tf` (existing) - DynamoDB for idempotency
- ✅ `infrastructure/terraform/modules/messaging/variables.tf` (130 lines) - Cost-optimized variables
- ✅ `infrastructure/terraform/modules/messaging/outputs.tf` (76 lines) - All outputs
- ✅ `infrastructure/terraform/modules/messaging/README.md` (existing documentation)

### 📦 Messaging Configuration

**Demo Approach** (Cost-Optimized):
- EventBridge event bus (free)
- 2 SQS queues: events + DLQ
- Event archive disabled (save storage costs)
- SNS topics disabled (save $0.50/month)
- CloudWatch alarms disabled
- **Cost**: ~$0/month (SQS Free Tier: 1M requests)

**SQS Queues**:
1. **DLQ** (Dead Letter Queue)
   - 14-day retention
   - SSE encryption
   - Always enabled

2. **Events Queue**
   - 4-day retention
   - Long polling (cost optimization)
   - DLQ redrive policy
   - Always enabled

3. **Chat Messages Queue** (Optional - disabled by default)
   - 1-day retention
   - For real-time chat processing

4. **Notifications Queue** (Optional - disabled by default)
   - 4-day retention
   - For email/SMS notifications

5. **Reports Queue** (Optional - disabled by default)
   - 4-day retention
   - 15-minute visibility timeout

**EventBridge**:
- Central event bus for domain events
- Event rules for routing
- Optional 7-day archive (disabled by default)
- CloudWatch Logs integration

**Optional Features** (Disabled for Demo):
- ❌ SNS topics (save $0.50/month)
- ❌ Event archive (save storage costs)
- ❌ CloudWatch alarms (save $0.20/month)
- ❌ Extra queues (chat, notifications, reports)

### 💰 Cost Breakdown

**Demo Configuration**:
- EventBridge: Free (1M events/month)
- SQS: Free (1M requests/month)
- DynamoDB (idempotency): Free (25 GB, 25 WCU/RCU)
- **Total**: $0/month (within Free Tier)

**Production Configuration** (all features):
- EventBridge: ~$1/month
- SQS (5 queues): ~$0.50/month
- SNS (2 topics): ~$0.50/month
- Event Archive: ~$0.50/month
- CloudWatch Alarms: ~$0.20/month
- **Total**: ~$3/month

### 🎯 Module Inputs

| Variable | Default | Purpose |
|----------|---------|---------|
| enable_event_archive | false | Disable archive to save storage |
| enable_chat_queue | false | Disable for demo |
| enable_notification_queue | false | Disable for demo |
| enable_report_queue | false | Disable for demo |
| enable_sns_topics | false | Disable SNS to save costs |
| enable_queue_alarms | false | Disable alarms for demo |
| message_retention_seconds | 345600 | 4 days retention |

### 📤 Module Outputs

- Event bus name and ARN
- All queue URLs and ARNs
- Idempotency table name
- Optional outputs return null if disabled

## Testing Requirements

**Validation**:
- Run `terraform plan`
- Verify event patterns in eventbridge-rules.tf
- Check queue configurations
- Test DLQ redrive policy
- Verify encryption settings

## References

- Specification: `specs/aws-infrastructure.md` (Messaging section)
- Module Documentation: `infrastructure/terraform/modules/messaging/README.md`
- Related Tasks: 020-create-lambda-module, 019-create-compute-modules
