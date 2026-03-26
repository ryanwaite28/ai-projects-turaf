# Messaging Module Outputs

# EventBridge Outputs
output "event_bus_name" {
  description = "Name of the EventBridge event bus"
  value       = aws_cloudwatch_event_bus.main.name
}

output "event_bus_arn" {
  description = "ARN of the EventBridge event bus"
  value       = aws_cloudwatch_event_bus.main.arn
}

output "event_archive_arn" {
  description = "ARN of the EventBridge event archive (null if disabled)"
  value       = var.enable_event_archive ? aws_cloudwatch_event_archive.main.arn : null
}

# SQS Queue Outputs
output "dlq_url" {
  description = "URL of the dead letter queue"
  value       = aws_sqs_queue.dlq.url
}

output "dlq_arn" {
  description = "ARN of the dead letter queue"
  value       = aws_sqs_queue.dlq.arn
}

output "events_queue_url" {
  description = "URL of the events queue"
  value       = aws_sqs_queue.events.url
}

output "events_queue_arn" {
  description = "ARN of the events queue"
  value       = aws_sqs_queue.events.arn
}

output "chat_messages_queue_url" {
  description = "URL of the chat messages queue (null if disabled)"
  value       = length(aws_sqs_queue.chat_messages) > 0 ? aws_sqs_queue.chat_messages[0].url : null
}

output "chat_messages_queue_arn" {
  description = "ARN of the chat messages queue (null if disabled)"
  value       = length(aws_sqs_queue.chat_messages) > 0 ? aws_sqs_queue.chat_messages[0].arn : null
}

output "notifications_queue_url" {
  description = "URL of the notifications queue (null if disabled)"
  value       = length(aws_sqs_queue.notifications) > 0 ? aws_sqs_queue.notifications[0].url : null
}

output "notifications_queue_arn" {
  description = "ARN of the notifications queue (null if disabled)"
  value       = length(aws_sqs_queue.notifications) > 0 ? aws_sqs_queue.notifications[0].arn : null
}

output "reports_queue_url" {
  description = "URL of the reports queue (null if disabled)"
  value       = length(aws_sqs_queue.reports) > 0 ? aws_sqs_queue.reports[0].url : null
}

output "reports_queue_arn" {
  description = "ARN of the reports queue (null if disabled)"
  value       = length(aws_sqs_queue.reports) > 0 ? aws_sqs_queue.reports[0].arn : null
}

# DynamoDB Idempotency Table Output
output "idempotency_table_name" {
  description = "Name of the DynamoDB idempotency table"
  value       = aws_dynamodb_table.idempotency.name
}

output "idempotency_table_arn" {
  description = "ARN of the DynamoDB idempotency table"
  value       = aws_dynamodb_table.idempotency.arn
}
