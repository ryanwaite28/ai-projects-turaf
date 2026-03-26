# EventBridge Rules for routing domain events to appropriate targets
# This configuration sets up event routing for the Turaf platform's event-driven architecture

# ============================================================================
# Experiment Service Events
# ============================================================================

# Rule: ExperimentCompleted events
resource "aws_cloudwatch_event_rule" "experiment_completed" {
  name           = "experiment-completed-${var.environment}"
  description    = "Routes ExperimentCompleted events to reporting and notification services"
  event_bus_name = aws_cloudwatch_event_bus.main.name

  event_pattern = jsonencode({
    source      = ["turaf.experiment-service"]
    detail-type = ["ExperimentCompleted"]
  })

  tags = {
    Name        = "experiment-completed-${var.environment}"
    Environment = var.environment
    Service     = "messaging"
    EventType   = "ExperimentCompleted"
  }
}

# Target: Reporting Lambda
resource "aws_cloudwatch_event_target" "experiment_completed_reporting" {
  count = var.reporting_lambda_arn != "" ? 1 : 0
  
  rule      = aws_cloudwatch_event_rule.experiment_completed.name
  event_bus_name = aws_cloudwatch_event_bus.main.name
  target_id = "ReportingLambda"
  arn       = var.reporting_lambda_arn

  retry_policy {
    maximum_event_age_in_seconds = 3600
    maximum_retry_attempts       = 2
  }

  dead_letter_config {
    arn = aws_sqs_queue.dlq.arn
  }

  input_transformer {
    input_paths = {
      eventId      = "$.detail.eventId"
      eventType    = "$.detail.eventType"
      timestamp    = "$.detail.timestamp"
      organization = "$.detail.organizationId"
    }

    input_template = <<EOF
{
  "eventId": <eventId>,
  "eventType": <eventType>,
  "timestamp": <timestamp>,
  "organizationId": <organization>,
  "detail": <aws.events.event.json>
}
EOF
  }
}

# Target: Notification Lambda
resource "aws_cloudwatch_event_target" "experiment_completed_notification" {
  count = var.notification_lambda_arn != "" ? 1 : 0
  
  rule           = aws_cloudwatch_event_rule.experiment_completed.name
  event_bus_name = aws_cloudwatch_event_bus.main.name
  arn            = var.notification_lambda_arn

  retry_policy {
    maximum_retry_attempts = 3
  }

  dead_letter_config {
    arn = aws_sqs_queue.dlq.arn
  }
}

# ============================================================================
# Reporting Service Events
# ============================================================================

# Rule: ReportGenerated events
resource "aws_cloudwatch_event_rule" "report_generated" {
  name           = "report-generated-${var.environment}"
  description    = "Routes ReportGenerated events to notification service"
  event_bus_name = aws_cloudwatch_event_bus.main.name

  event_pattern = jsonencode({
    source      = ["turaf.reporting-service"]
    detail-type = ["ReportGenerated"]
  })

  tags = {
    Name        = "report-generated-${var.environment}"
    Environment = var.environment
    Service     = "messaging"
    EventType   = "ReportGenerated"
  }
}

# Target: Notification Lambda
resource "aws_cloudwatch_event_target" "report_generated_notification" {
  count = var.notification_lambda_arn != "" ? 1 : 0
  
  rule           = aws_cloudwatch_event_rule.report_generated.name
  event_bus_name = aws_cloudwatch_event_bus.main.name
  arn            = var.notification_lambda_arn

  retry_policy {
    maximum_retry_attempts = 3
  }

  dead_letter_config {
    arn = aws_sqs_queue.dlq.arn
  }
}

# ============================================================================
# Organization Service Events
# ============================================================================

# Rule: OrganizationCreated events
resource "aws_cloudwatch_event_rule" "organization_created" {
  name           = "organization-created-${var.environment}"
  description    = "Routes OrganizationCreated events to notification service"
  event_bus_name = aws_cloudwatch_event_bus.main.name

  event_pattern = jsonencode({
    source      = ["turaf.organization-service"]
    detail-type = ["OrganizationCreated"]
  })

  tags = {
    Name        = "organization-created-${var.environment}"
    Environment = var.environment
    Service     = "messaging"
    EventType   = "OrganizationCreated"
  }
}

# Target: Notification Lambda
resource "aws_cloudwatch_event_target" "organization_created_notification" {
  count = var.notification_lambda_arn != "" ? 1 : 0
  
  rule           = aws_cloudwatch_event_rule.organization_created.name
  event_bus_name = aws_cloudwatch_event_bus.main.name
  arn            = var.notification_lambda_arn

  retry_policy {
    maximum_retry_attempts = 3
  }

  dead_letter_config {
    arn = aws_sqs_queue.dlq.arn
  }
}

# Rule: OrganizationUpdated events
resource "aws_cloudwatch_event_rule" "organization_updated" {
  name           = "organization-updated-${var.environment}"
  description    = "Routes OrganizationUpdated events to notification service"
  event_bus_name = aws_cloudwatch_event_bus.main.name

  event_pattern = jsonencode({
    source      = ["turaf.organization-service"]
    detail-type = ["OrganizationUpdated"]
  })

  tags = {
    Name        = "organization-updated-${var.environment}"
    Environment = var.environment
    Service     = "messaging"
    EventType   = "OrganizationUpdated"
  }
}

# Target: Notification Lambda
resource "aws_cloudwatch_event_target" "organization_updated_notification" {
  count = var.notification_lambda_arn != "" ? 1 : 0
  
  rule           = aws_cloudwatch_event_rule.organization_updated.name
  event_bus_name = aws_cloudwatch_event_bus.main.name
  arn            = var.notification_lambda_arn

  retry_policy {
    maximum_retry_attempts = 3
  }
}

# ============================================================================
# Identity Service Events
# ============================================================================

# Rule: UserCreated events
resource "aws_cloudwatch_event_rule" "user_created" {
  name           = "user-created-${var.environment}"
  description    = "Routes UserCreated events to notification service"
  event_bus_name = aws_cloudwatch_event_bus.main.name

  event_pattern = jsonencode({
    source      = ["turaf.identity-service"]
    detail-type = ["UserCreated"]
  })

  tags = {
    Name        = "user-created-${var.environment}"
    Environment = var.environment
    Service     = "messaging"
    EventType   = "UserCreated"
  }
}

# Target: Notification Lambda
resource "aws_cloudwatch_event_target" "user_created_notification" {
  count = var.notification_lambda_arn != "" ? 1 : 0
  
  rule           = aws_cloudwatch_event_rule.user_created.name
  event_bus_name = aws_cloudwatch_event_bus.main.name
  arn            = var.notification_lambda_arn

  retry_policy {
    maximum_retry_attempts = 3
  }

  dead_letter_config {
    arn = aws_sqs_queue.dlq.arn
  }
}

# ============================================================================
# Metrics Service Events
# ============================================================================

# Rule: MetricsCalculated events
resource "aws_cloudwatch_event_rule" "metrics_calculated" {
  name           = "metrics-calculated-${var.environment}"
  description    = "Routes MetricsCalculated events to notification service"
  event_bus_name = aws_cloudwatch_event_bus.main.name

  event_pattern = jsonencode({
    source      = ["turaf.metrics-service"]
    detail-type = ["MetricsCalculated"]
  })

  tags = {
    Name        = "metrics-calculated-${var.environment}"
    Environment = var.environment
    Service     = "messaging"
    EventType   = "MetricsCalculated"
  }
}

# Target: Notification Lambda
resource "aws_cloudwatch_event_target" "metrics_calculated_notification" {
  count = var.notification_lambda_arn != "" ? 1 : 0
  
  rule           = aws_cloudwatch_event_rule.metrics_calculated.name
  event_bus_name = aws_cloudwatch_event_bus.main.name
  arn            = var.notification_lambda_arn

  retry_policy {
    maximum_retry_attempts = 3
  }
}

# ============================================================================
# Dead Letter Queue for Failed Events
# ============================================================================
# Note: DLQ resource is defined in main.tf to avoid duplication

# DLQ Policy to allow EventBridge to send messages
resource "aws_sqs_queue_policy" "dlq_policy" {
  queue_url = aws_sqs_queue.dlq.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Service = "events.amazonaws.com"
        }
        Action   = "sqs:SendMessage"
        Resource = aws_sqs_queue.dlq.arn
      }
    ]
  })
}

# CloudWatch alarm for DLQ messages
resource "aws_cloudwatch_metric_alarm" "dlq_messages" {
  alarm_name          = "eventbridge-dlq-messages-${var.environment}"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  metric_name         = "ApproximateNumberOfMessagesVisible"
  namespace           = "AWS/SQS"
  period              = "300"
  statistic           = "Average"
  threshold           = "0"
  alarm_description   = "Alert when events fail and land in DLQ"
  treat_missing_data  = "notBreaching"

  dimensions = {
    QueueName = aws_sqs_queue.dlq.name
  }

  tags = {
    Name        = "eventbridge-dlq-messages-${var.environment}"
    Environment = var.environment
  }
}

# ============================================================================
# Lambda Permissions
# ============================================================================

# Permission for EventBridge to invoke Reporting Lambda
resource "aws_lambda_permission" "allow_eventbridge_reporting" {
  count = var.reporting_lambda_name != "" ? 1 : 0
  
  statement_id  = "AllowExecutionFromEventBridge"
  action        = "lambda:InvokeFunction"
  function_name = var.reporting_lambda_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.experiment_completed.arn
}

# Permission for EventBridge to invoke Notification Lambda (ExperimentCompleted)
resource "aws_lambda_permission" "allow_eventbridge_notification_experiment" {
  count = var.notification_lambda_name != "" ? 1 : 0
  
  statement_id  = "AllowExecutionFromEventBridgeExperiment"
  action        = "lambda:InvokeFunction"
  function_name = var.notification_lambda_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.experiment_completed.arn
}

# Permission for EventBridge to invoke Notification Lambda (ReportGenerated)
resource "aws_lambda_permission" "allow_eventbridge_notification_report" {
  count = var.notification_lambda_name != "" ? 1 : 0
  
  statement_id  = "AllowExecutionFromEventBridgeReport"
  action        = "lambda:InvokeFunction"
  function_name = var.notification_lambda_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.report_generated.arn
}

# Permission for EventBridge to invoke Notification Lambda (OrganizationCreated)
resource "aws_lambda_permission" "allow_eventbridge_notification_org_created" {
  count = var.notification_lambda_name != "" ? 1 : 0
  
  statement_id  = "AllowExecutionFromEventBridgeOrgCreated"
  action        = "lambda:InvokeFunction"
  function_name = var.notification_lambda_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.organization_created.arn
}

# Permission for EventBridge to invoke Notification Lambda (OrganizationUpdated)
resource "aws_lambda_permission" "allow_eventbridge_notification_org_updated" {
  count = var.notification_lambda_name != "" ? 1 : 0
  
  statement_id  = "AllowExecutionFromEventBridgeOrgUpdated"
  action        = "lambda:InvokeFunction"
  function_name = var.notification_lambda_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.organization_updated.arn
}

# Permission for EventBridge to invoke Notification Lambda (UserCreated)
resource "aws_lambda_permission" "allow_eventbridge_notification_user" {
  count = var.notification_lambda_name != "" ? 1 : 0
  
  statement_id  = "AllowExecutionFromEventBridgeUser"
  action        = "lambda:InvokeFunction"
  function_name = var.notification_lambda_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.user_created.arn
}

# Permission for EventBridge to invoke Notification Lambda (MetricsCalculated)
resource "aws_lambda_permission" "allow_eventbridge_notification_metrics" {
  count = var.notification_lambda_name != "" ? 1 : 0
  
  statement_id  = "AllowExecutionFromEventBridgeMetrics"
  action        = "lambda:InvokeFunction"
  function_name = var.notification_lambda_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.metrics_calculated.arn
}

# ============================================================================
# Outputs
# ============================================================================

output "experiment_completed_rule_arn" {
  description = "ARN of the ExperimentCompleted event rule"
  value       = aws_cloudwatch_event_rule.experiment_completed.arn
}

output "report_generated_rule_arn" {
  description = "ARN of the ReportGenerated event rule"
  value       = aws_cloudwatch_event_rule.report_generated.arn
}

output "organization_created_rule_arn" {
  description = "ARN of the OrganizationCreated event rule"
  value       = aws_cloudwatch_event_rule.organization_created.arn
}

output "user_created_rule_arn" {
  description = "ARN of the UserCreated event rule"
  value       = aws_cloudwatch_event_rule.user_created.arn
}

# Note: dlq_url and dlq_arn outputs are defined in outputs.tf to avoid duplication
