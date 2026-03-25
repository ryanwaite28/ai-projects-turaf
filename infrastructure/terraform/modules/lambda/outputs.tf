# Lambda Module Outputs

# Event Processor Outputs
output "event_processor_function_name" {
  description = "Name of the event processor Lambda function (null if disabled)"
  value       = var.enable_event_processor ? aws_lambda_function.event_processor[0].function_name : null
}

output "event_processor_function_arn" {
  description = "ARN of the event processor Lambda function (null if disabled)"
  value       = var.enable_event_processor ? aws_lambda_function.event_processor[0].arn : null
}

output "event_processor_invoke_arn" {
  description = "Invoke ARN of the event processor Lambda function (null if disabled)"
  value       = var.enable_event_processor ? aws_lambda_function.event_processor[0].invoke_arn : null
}

# Notification Processor Outputs
output "notification_processor_function_name" {
  description = "Name of the notification processor Lambda function (null if disabled)"
  value       = var.enable_notification_processor ? aws_lambda_function.notification_processor[0].function_name : null
}

output "notification_processor_function_arn" {
  description = "ARN of the notification processor Lambda function (null if disabled)"
  value       = var.enable_notification_processor ? aws_lambda_function.notification_processor[0].arn : null
}

output "notification_processor_invoke_arn" {
  description = "Invoke ARN of the notification processor Lambda function (null if disabled)"
  value       = var.enable_notification_processor ? aws_lambda_function.notification_processor[0].invoke_arn : null
}

# Report Generator Outputs
output "report_generator_function_name" {
  description = "Name of the report generator Lambda function (null if disabled)"
  value       = var.enable_report_generator ? aws_lambda_function.report_generator[0].function_name : null
}

output "report_generator_function_arn" {
  description = "ARN of the report generator Lambda function (null if disabled)"
  value       = var.enable_report_generator ? aws_lambda_function.report_generator[0].arn : null
}

output "report_generator_invoke_arn" {
  description = "Invoke ARN of the report generator Lambda function (null if disabled)"
  value       = var.enable_report_generator ? aws_lambda_function.report_generator[0].invoke_arn : null
}

output "report_generator_function_url" {
  description = "Function URL for report generator (null if disabled)"
  value       = var.enable_report_generator ? aws_lambda_function_url.report_generator[0].function_url : null
}

# CloudWatch Log Groups
output "event_processor_log_group" {
  description = "CloudWatch log group for event processor (null if disabled)"
  value       = var.enable_event_processor ? aws_cloudwatch_log_group.event_processor[0].name : null
}

output "notification_processor_log_group" {
  description = "CloudWatch log group for notification processor (null if disabled)"
  value       = var.enable_notification_processor ? aws_cloudwatch_log_group.notification_processor[0].name : null
}

output "report_generator_log_group" {
  description = "CloudWatch log group for report generator (null if disabled)"
  value       = var.enable_report_generator ? aws_cloudwatch_log_group.report_generator[0].name : null
}

# EventBridge Rules
output "event_processor_rule_arn" {
  description = "ARN of the EventBridge rule for event processor (null if disabled)"
  value       = var.enable_event_processor ? aws_cloudwatch_event_rule.event_processor[0].arn : null
}

# Summary
output "lambda_summary" {
  description = "Summary of Lambda functions deployment"
  value = {
    event_processor_enabled        = var.enable_event_processor
    notification_processor_enabled = var.enable_notification_processor
    report_generator_enabled       = var.enable_report_generator
    vpc_mode_enabled              = var.use_vpc_mode
    runtime                       = var.lambda_runtime
    log_retention_days            = var.log_retention_days
    functions_deployed            = (
      (var.enable_event_processor ? 1 : 0) +
      (var.enable_notification_processor ? 1 : 0) +
      (var.enable_report_generator ? 1 : 0)
    )
  }
}
