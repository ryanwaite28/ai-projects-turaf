# Development Environment Outputs

# Networking Outputs
output "vpc_id" {
  description = "VPC ID"
  value       = module.networking.vpc_id
}

output "public_subnet_ids" {
  description = "Public subnet IDs"
  value       = module.networking.public_subnet_ids
}

output "private_subnet_ids" {
  description = "Private subnet IDs"
  value       = module.networking.private_subnet_ids
}

output "database_subnet_ids" {
  description = "Database subnet IDs"
  value       = module.networking.database_subnet_ids
}

# Database Outputs
output "rds_endpoint" {
  description = "RDS endpoint"
  value       = module.database.rds_endpoint
  sensitive   = true
}

output "rds_instance_id" {
  description = "RDS instance ID"
  value       = module.database.rds_instance_id
}

output "redis_endpoint" {
  description = "Redis endpoint"
  value       = module.database.redis_primary_endpoint
  sensitive   = true
}

output "documentdb_endpoint" {
  description = "DocumentDB endpoint"
  value       = module.database.documentdb_endpoint
  sensitive   = true
}

# Storage Outputs
output "primary_bucket_id" {
  description = "Primary S3 bucket ID"
  value       = module.storage.primary_bucket_id
}

output "primary_bucket_arn" {
  description = "Primary S3 bucket ARN"
  value       = module.storage.primary_bucket_arn
}

# Messaging Outputs
output "event_bus_name" {
  description = "EventBridge event bus name"
  value       = module.messaging.event_bus_name
}

output "events_queue_url" {
  description = "Events SQS queue URL"
  value       = module.messaging.events_queue_url
}

# Compute Outputs
output "cluster_name" {
  description = "ECS cluster name"
  value       = module.compute.cluster_name
}

output "cluster_arn" {
  description = "ECS cluster ARN"
  value       = module.compute.cluster_arn
}

output "alb_dns_name" {
  description = "ALB DNS name"
  value       = module.compute.alb_dns_name
}

output "alb_zone_id" {
  description = "ALB zone ID"
  value       = module.compute.alb_zone_id
}

output "alb_arn" {
  description = "ALB ARN"
  value       = module.compute.alb_arn
}

output "alb_listener_http_arn" {
  description = "ALB HTTP listener ARN"
  value       = module.compute.alb_listener_http_arn
}

output "alb_listener_https_arn" {
  description = "ALB HTTPS listener ARN"
  value       = module.compute.alb_listener_https_arn
}

output "internal_alb_dns_name" {
  description = "Internal ALB DNS name"
  value       = module.compute.internal_alb_dns_name
}

output "internal_alb_arn" {
  description = "Internal ALB ARN"
  value       = module.compute.internal_alb_arn
}

output "ecs_security_group_id" {
  description = "ECS tasks security group ID"
  value       = module.security.ecs_tasks_security_group_id
}

output "ecs_execution_role_arn" {
  description = "ECS execution role ARN"
  value       = module.security.ecs_execution_role_arn
}

output "ecs_task_role_arn" {
  description = "ECS task role ARN"
  value       = module.security.ecs_task_role_arn
}

# Lambda Outputs
output "event_processor_function_arn" {
  description = "Event processor Lambda ARN"
  value       = module.lambda.event_processor_function_arn
}

output "notification_processor_function_arn" {
  description = "Notification processor Lambda ARN"
  value       = module.lambda.notification_processor_function_arn
}

output "report_generator_function_arn" {
  description = "Report generator Lambda ARN"
  value       = module.lambda.report_generator_function_arn
}

# Monitoring Outputs
output "alerts_topic_arn" {
  description = "SNS alerts topic ARN"
  value       = var.enable_sns_alerts ? module.monitoring.alerts_topic_arn : ""
}

output "dashboard_name" {
  description = "CloudWatch dashboard name"
  value       = var.enable_dashboard ? module.monitoring.dashboard_name : ""
}

# Summary Output
output "environment_summary" {
  description = "Environment deployment summary"
  value = {
    environment = var.environment
    region      = var.aws_region
    vpc_id      = module.networking.vpc_id
    cluster     = module.compute.cluster_name
    alb_dns     = module.compute.alb_dns_name
    cost_optimization = {
      nat_gateway_disabled      = !var.enable_nat_gateway
      redis_disabled            = !var.enable_redis
      documentdb_disabled       = !var.enable_documentdb
      fargate_spot_enabled      = var.use_fargate_spot
      container_insights_disabled = !var.enable_container_insights
      monitoring_disabled       = !var.enable_alarms
    }
  }
}
