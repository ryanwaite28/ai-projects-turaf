# Messaging Module - SQS Queues and SNS Topics for Turaf Platform
# Cost-optimized configuration for demo/portfolio purposes

# SQS Dead Letter Queue (DLQ)
resource "aws_sqs_queue" "dlq" {
  name                      = "turaf-dlq-${var.environment}"
  message_retention_seconds = var.dlq_message_retention_seconds
  
  # Enable server-side encryption
  sqs_managed_sse_enabled = true

  tags = merge(
    var.tags,
    {
      Name        = "turaf-dlq-${var.environment}"
      Environment = var.environment
      Purpose     = "dead-letter-queue"
    }
  )
}

# Main Event Queue
resource "aws_sqs_queue" "events" {
  name                       = "turaf-events-${var.environment}"
  visibility_timeout_seconds = var.visibility_timeout_seconds
  message_retention_seconds  = var.message_retention_seconds
  receive_wait_time_seconds  = 20  # Long polling for cost optimization
  
  # Enable server-side encryption
  sqs_managed_sse_enabled = true

  # Dead letter queue configuration
  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.dlq.arn
    maxReceiveCount     = var.max_receive_count
  })

  tags = merge(
    var.tags,
    {
      Name        = "turaf-events-${var.environment}"
      Environment = var.environment
      Purpose     = "event-processing"
    }
  )
}

# Queue for Chat Messages
resource "aws_sqs_queue" "chat_messages" {
  count = var.enable_chat_queue ? 1 : 0
  
  name                       = "turaf-chat-messages-${var.environment}"
  visibility_timeout_seconds = 30
  message_retention_seconds  = 86400  # 1 day
  receive_wait_time_seconds  = 20
  
  sqs_managed_sse_enabled = true

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.dlq.arn
    maxReceiveCount     = 3
  })

  tags = merge(
    var.tags,
    {
      Name        = "turaf-chat-messages-${var.environment}"
      Environment = var.environment
      Purpose     = "chat-processing"
    }
  )
}

# Queue for Notifications
resource "aws_sqs_queue" "notifications" {
  count = var.enable_notification_queue ? 1 : 0
  
  name                       = "turaf-notifications-${var.environment}"
  visibility_timeout_seconds = 30
  message_retention_seconds  = 345600  # 4 days
  receive_wait_time_seconds  = 20
  
  sqs_managed_sse_enabled = true

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.dlq.arn
    maxReceiveCount     = 3
  })

  tags = merge(
    var.tags,
    {
      Name        = "turaf-notifications-${var.environment}"
      Environment = var.environment
      Purpose     = "notification-delivery"
    }
  )
}

# Queue for Report Generation
resource "aws_sqs_queue" "reports" {
  count = var.enable_report_queue ? 1 : 0
  
  name                       = "turaf-reports-${var.environment}"
  visibility_timeout_seconds = 900  # 15 minutes for long-running reports
  message_retention_seconds  = 345600  # 4 days
  receive_wait_time_seconds  = 20
  
  sqs_managed_sse_enabled = true

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.dlq.arn
    maxReceiveCount     = 2  # Fewer retries for expensive operations
  })

  tags = merge(
    var.tags,
    {
      Name        = "turaf-reports-${var.environment}"
      Environment = var.environment
      Purpose     = "report-generation"
    }
  )
}

# SNS Topic for System Events (Optional)
resource "aws_sns_topic" "system_events" {
  count = var.enable_sns_topics ? 1 : 0
  
  name              = "turaf-system-events-${var.environment}"
  display_name      = "Turaf System Events"
  fifo_topic        = false
  
  # Enable server-side encryption
  kms_master_key_id = var.kms_key_id

  tags = merge(
    var.tags,
    {
      Name        = "turaf-system-events-${var.environment}"
      Environment = var.environment
      Purpose     = "system-notifications"
    }
  )
}

# SNS Topic for Alarms (Optional)
resource "aws_sns_topic" "alarms" {
  count = var.enable_sns_topics ? 1 : 0
  
  name              = "turaf-alarms-${var.environment}"
  display_name      = "Turaf Alarms"
  fifo_topic        = false
  
  kms_master_key_id = var.kms_key_id

  tags = merge(
    var.tags,
    {
      Name        = "turaf-alarms-${var.environment}"
      Environment = var.environment
      Purpose     = "alarm-notifications"
    }
  )
}

# SNS Subscription for Email Alerts (Optional)
resource "aws_sns_topic_subscription" "alarm_email" {
  count = var.enable_sns_topics && var.alarm_email != "" ? 1 : 0
  
  topic_arn = aws_sns_topic.alarms[0].arn
  protocol  = "email"
  endpoint  = var.alarm_email
}

# SQS Queue Policy for SNS (if SNS enabled)
resource "aws_sqs_queue_policy" "events_sns_policy" {
  count     = var.enable_sns_topics ? 1 : 0
  queue_url = aws_sqs_queue.events.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "AllowSNSPublish"
        Effect = "Allow"
        Principal = {
          Service = "sns.amazonaws.com"
        }
        Action   = "sqs:SendMessage"
        Resource = aws_sqs_queue.events.arn
        Condition = {
          ArnEquals = {
            "aws:SourceArn" = aws_sns_topic.system_events[0].arn
          }
        }
      }
    ]
  })
}

# SNS to SQS Subscription (if SNS enabled)
resource "aws_sns_topic_subscription" "events_queue" {
  count = var.enable_sns_topics ? 1 : 0
  
  topic_arn = aws_sns_topic.system_events[0].arn
  protocol  = "sqs"
  endpoint  = aws_sqs_queue.events.arn
  
  raw_message_delivery = true
}

# CloudWatch Alarms for Queue Depth (Optional)
resource "aws_cloudwatch_metric_alarm" "dlq_depth" {
  count = var.enable_queue_alarms ? 1 : 0
  
  alarm_name          = "turaf-dlq-depth-${var.environment}"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "ApproximateNumberOfMessagesVisible"
  namespace           = "AWS/SQS"
  period              = 300
  statistic           = "Average"
  threshold           = var.dlq_alarm_threshold
  alarm_description   = "Alert when DLQ has messages"
  treat_missing_data  = "notBreaching"

  dimensions = {
    QueueName = aws_sqs_queue.dlq.name
  }

  alarm_actions = var.enable_sns_topics ? [aws_sns_topic.alarms[0].arn] : []

  tags = merge(
    var.tags,
    {
      Name        = "turaf-dlq-alarm-${var.environment}"
      Environment = var.environment
    }
  )
}

resource "aws_cloudwatch_metric_alarm" "events_queue_depth" {
  count = var.enable_queue_alarms ? 1 : 0
  
  alarm_name          = "turaf-events-queue-depth-${var.environment}"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "ApproximateNumberOfMessagesVisible"
  namespace           = "AWS/SQS"
  period              = 300
  statistic           = "Average"
  threshold           = var.queue_depth_alarm_threshold
  alarm_description   = "Alert when events queue is backing up"
  treat_missing_data  = "notBreaching"

  dimensions = {
    QueueName = aws_sqs_queue.events.name
  }

  alarm_actions = var.enable_sns_topics ? [aws_sns_topic.alarms[0].arn] : []

  tags = merge(
    var.tags,
    {
      Name        = "turaf-events-queue-alarm-${var.environment}"
      Environment = var.environment
    }
  )
}
