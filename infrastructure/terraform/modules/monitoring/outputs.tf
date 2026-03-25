# Monitoring Module Outputs

# SNS Topic Outputs
output "alerts_topic_arn" {
  description = "ARN of the SNS alerts topic (null if disabled)"
  value       = var.enable_sns_alerts ? aws_sns_topic.alerts[0].arn : null
}

output "alerts_topic_name" {
  description = "Name of the SNS alerts topic (null if disabled)"
  value       = var.enable_sns_alerts ? aws_sns_topic.alerts[0].name : null
}

# CloudWatch Dashboard Outputs
output "dashboard_name" {
  description = "Name of the CloudWatch dashboard (null if disabled)"
  value       = var.enable_dashboard ? aws_cloudwatch_dashboard.main[0].dashboard_name : null
}

output "dashboard_arn" {
  description = "ARN of the CloudWatch dashboard (null if disabled)"
  value       = var.enable_dashboard ? aws_cloudwatch_dashboard.main[0].dashboard_arn : null
}

# X-Ray Outputs
output "xray_sampling_rule_name" {
  description = "Name of the X-Ray sampling rule (null if disabled)"
  value       = var.enable_xray ? aws_xray_sampling_rule.main[0].rule_name : null
}

output "xray_sampling_rule_arn" {
  description = "ARN of the X-Ray sampling rule (null if disabled)"
  value       = var.enable_xray ? aws_xray_sampling_rule.main[0].arn : null
}

# Alarm Outputs
output "ecs_cpu_alarm_arn" {
  description = "ARN of the ECS CPU alarm (null if disabled)"
  value       = var.enable_alarms && var.cluster_name != "" ? aws_cloudwatch_metric_alarm.ecs_cpu_high[0].arn : null
}

output "ecs_memory_alarm_arn" {
  description = "ARN of the ECS memory alarm (null if disabled)"
  value       = var.enable_alarms && var.cluster_name != "" ? aws_cloudwatch_metric_alarm.ecs_memory_high[0].arn : null
}

output "alb_5xx_alarm_arn" {
  description = "ARN of the ALB 5xx errors alarm (null if disabled)"
  value       = var.enable_alarms && var.alb_arn_suffix != "" ? aws_cloudwatch_metric_alarm.alb_5xx_errors[0].arn : null
}

output "alb_response_time_alarm_arn" {
  description = "ARN of the ALB response time alarm (null if disabled)"
  value       = var.enable_alarms && var.alb_arn_suffix != "" ? aws_cloudwatch_metric_alarm.alb_response_time[0].arn : null
}

output "rds_cpu_alarm_arn" {
  description = "ARN of the RDS CPU alarm (null if disabled)"
  value       = var.enable_alarms && var.rds_instance_id != "" ? aws_cloudwatch_metric_alarm.rds_cpu_high[0].arn : null
}

output "rds_storage_alarm_arn" {
  description = "ARN of the RDS storage alarm (null if disabled)"
  value       = var.enable_alarms && var.rds_instance_id != "" ? aws_cloudwatch_metric_alarm.rds_storage_low[0].arn : null
}

# Log Insights Query Outputs
output "error_logs_query_id" {
  description = "ID of the error logs query (null if disabled)"
  value       = var.enable_log_insights ? aws_cloudwatch_query_definition.error_logs[0].query_definition_id : null
}

output "slow_requests_query_id" {
  description = "ID of the slow requests query (null if disabled)"
  value       = var.enable_log_insights ? aws_cloudwatch_query_definition.slow_requests[0].query_definition_id : null
}

# Summary
output "monitoring_summary" {
  description = "Summary of monitoring configuration"
  value = {
    alarms_enabled       = var.enable_alarms
    dashboard_enabled    = var.enable_dashboard
    sns_alerts_enabled   = var.enable_sns_alerts
    xray_enabled         = var.enable_xray
    log_insights_enabled = var.enable_log_insights
    alarm_count = (
      (var.enable_alarms && var.cluster_name != "" ? 2 : 0) +
      (var.enable_alarms && var.alb_arn_suffix != "" ? 2 : 0) +
      (var.enable_alarms && var.rds_instance_id != "" ? 2 : 0)
    )
  }
}
