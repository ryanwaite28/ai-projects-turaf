# EventBridge Event Bus configuration
# This is the central event bus for the Turaf platform's event-driven architecture

resource "aws_cloudwatch_event_bus" "main" {
  name = "turaf-event-bus-${var.environment}"

  tags = {
    Name        = "turaf-event-bus-${var.environment}"
    Environment = var.environment
    Service     = "messaging"
    ManagedBy   = "terraform"
  }
}

# EventBridge archive for event replay capability
resource "aws_cloudwatch_event_archive" "main" {
  name             = "turaf-event-archive-${var.environment}"
  event_source_arn = aws_cloudwatch_event_bus.main.arn

  retention_days = 30

  description = "Archive of all events for replay and debugging"

  event_pattern = jsonencode({
    source = [{
      prefix = "turaf."
    }]
  })
}

# CloudWatch Logs group for EventBridge debugging
resource "aws_cloudwatch_log_group" "eventbridge" {
  name              = "/aws/events/turaf-${var.environment}"
  retention_in_days = 7

  tags = {
    Name        = "eventbridge-logs-${var.environment}"
    Environment = var.environment
    Service     = "messaging"
  }
}

# Note: Outputs are defined in outputs.tf to avoid duplication
