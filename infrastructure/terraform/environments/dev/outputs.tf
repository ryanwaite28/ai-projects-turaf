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

# Messaging Outputs - DISABLED (module commented out)
# output "event_bus_name" {
#   description = "EventBridge event bus name"
#   value       = module.messaging.event_bus_name
# }
#
# output "events_queue_url" {
#   description = "Events SQS queue URL"
#   value       = module.messaging.events_queue_url
# }

# Compute Outputs - DISABLED (module commented out)
# output "cluster_name" {
#   description = "ECS cluster name"
#   value       = module.compute.cluster_name
# }
#
# output "alb_dns_name" {
#   description = "ALB DNS name"
#   value       = module.compute.alb_dns_name
# }
#
# output "alb_zone_id" {
#   description = "ALB zone ID"
#   value       = module.compute.alb_zone_id
# }
#
# output "identity_service_name" {
#   description = "Identity service name"
#   value       = module.compute.identity_service_name
# }
#
# output "organization_service_name" {
#   description = "Organization service name"
#   value       = module.compute.organization_service_name
# }
#
# output "experiment_service_name" {
#   description = "Experiment service name"
#   value       = module.compute.experiment_service_name
# }

# Lambda Outputs - DISABLED (module commented out)
# output "event_processor_function_arn" {
#   description = "Event processor Lambda ARN"
#   value       = module.lambda.event_processor_function_arn
# }
#
# output "notification_processor_function_arn" {
#   description = "Notification processor Lambda ARN"
#   value       = module.lambda.notification_processor_function_arn
# }
#
# output "report_generator_function_arn" {
#   description = "Report generator Lambda ARN"
#   value       = module.lambda.report_generator_function_arn
# }

# Monitoring Outputs - DISABLED (module commented out)
# output "alerts_topic_arn" {
#   description = "SNS alerts topic ARN"
#   value       = module.monitoring.alerts_topic_arn
# }
#
# output "dashboard_name" {
#   description = "CloudWatch dashboard name"
#   value       = module.monitoring.dashboard_name
# }

# Summary Output
output "environment_summary" {
  description = "Environment deployment summary"
  value = {
    environment = var.environment
    region      = var.aws_region
    vpc_id      = module.networking.vpc_id
    # cluster     = module.compute.cluster_name
    # alb_dns     = module.compute.alb_dns_name
    # services = {
    #   identity     = module.compute.identity_service_name
    #   organization = module.compute.organization_service_name
    #   experiment   = module.compute.experiment_service_name
    # }
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
