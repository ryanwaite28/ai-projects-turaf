# Production Environment Outputs

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

output "nat_gateway_ips" {
  description = "NAT Gateway public IPs"
  value       = module.networking.nat_gateway_ips
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
  value       = module.database.redis_endpoint
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

output "alb_dns_name" {
  description = "ALB DNS name"
  value       = module.compute.alb_dns_name
}

output "alb_zone_id" {
  description = "ALB zone ID"
  value       = module.compute.alb_zone_id
}

output "identity_service_name" {
  description = "Identity service name"
  value       = module.compute.identity_service_name
}

output "organization_service_name" {
  description = "Organization service name"
  value       = module.compute.organization_service_name
}

output "experiment_service_name" {
  description = "Experiment service name"
  value       = module.compute.experiment_service_name
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
  value       = module.monitoring.alerts_topic_arn
}

output "dashboard_name" {
  description = "CloudWatch dashboard name"
  value       = module.monitoring.dashboard_name
}

# Summary Output
output "environment_summary" {
  description = "Production environment deployment summary"
  value = {
    environment = var.environment
    region      = var.aws_region
    vpc_id      = module.networking.vpc_id
    cluster     = module.compute.cluster_name
    alb_dns     = module.compute.alb_dns_name
    services = {
      identity     = module.compute.identity_service_name
      organization = module.compute.organization_service_name
      experiment   = module.compute.experiment_service_name
    }
    high_availability = {
      multi_az_rds          = var.enable_multi_az
      nat_gateway_enabled   = var.enable_nat_gateway
      redis_enabled         = var.enable_redis
      autoscaling_enabled   = var.enable_autoscaling
      deletion_protection   = var.deletion_protection
    }
    monitoring = {
      alarms_enabled       = var.enable_alarms
      dashboard_enabled    = var.enable_dashboard
      xray_enabled         = var.enable_xray
      container_insights   = var.enable_container_insights
    }
  }
}
