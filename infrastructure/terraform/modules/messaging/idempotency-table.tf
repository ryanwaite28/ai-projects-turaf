# DynamoDB table for event idempotency tracking
# Prevents duplicate event processing in at-least-once delivery systems

resource "aws_dynamodb_table" "idempotency" {
  name         = "turaf-event-idempotency-${var.environment}"
  billing_mode = "PAY_PER_REQUEST" # On-demand pricing for variable workloads
  hash_key     = "eventId"

  # Primary key: eventId (unique event identifier)
  attribute {
    name = "eventId"
    type = "S"
  }

  # TTL configuration for automatic cleanup
  # Records older than 30 days are automatically deleted
  ttl {
    attribute_name = "ttl"
    enabled        = true
  }

  # Point-in-time recovery for data protection
  point_in_time_recovery {
    enabled = true
  }

  # Server-side encryption
  server_side_encryption {
    enabled = true
  }

  tags = {
    Name        = "turaf-event-idempotency-${var.environment}"
    Environment = var.environment
    Service     = "messaging"
    ManagedBy   = "terraform"
    Purpose     = "event-idempotency-tracking"
  }
}

# CloudWatch alarm for throttled requests
resource "aws_cloudwatch_metric_alarm" "idempotency_throttled_requests" {
  alarm_name          = "idempotency-table-throttled-${var.environment}"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "UserErrors"
  namespace           = "AWS/DynamoDB"
  period              = "300"
  statistic           = "Sum"
  threshold           = "10"
  alarm_description   = "This metric monitors DynamoDB throttled requests"
  treat_missing_data  = "notBreaching"

  dimensions = {
    TableName = aws_dynamodb_table.idempotency.name
  }

  tags = {
    Name        = "idempotency-table-throttled-${var.environment}"
    Environment = var.environment
  }
}

# Output the table name for use in application configuration
output "idempotency_table_name" {
  description = "Name of the DynamoDB idempotency table"
  value       = aws_dynamodb_table.idempotency.name
}

output "idempotency_table_arn" {
  description = "ARN of the DynamoDB idempotency table"
  value       = aws_dynamodb_table.idempotency.arn
}
