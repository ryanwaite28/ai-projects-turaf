output "vpc_id" {
  description = "ID of the VPC"
  value       = module.networking.vpc_id
}

output "vpc_cidr" {
  description = "CIDR block of the VPC"
  value       = module.networking.vpc_cidr
}

output "public_subnet_ids" {
  description = "IDs of public subnets"
  value       = module.networking.public_subnet_ids
}

output "private_subnet_ids" {
  description = "IDs of private subnets"
  value       = module.networking.private_subnet_ids
}

output "database_subnet_ids" {
  description = "IDs of database subnets"
  value       = module.networking.database_subnet_ids
}

output "alb_dns_name" {
  description = "DNS name of the Application Load Balancer"
  value       = module.compute.alb_dns_name
}

output "alb_zone_id" {
  description = "Zone ID of the Application Load Balancer"
  value       = module.compute.alb_zone_id
}

output "ecs_cluster_id" {
  description = "ID of the ECS cluster"
  value       = module.compute.ecs_cluster_id
}

output "ecs_cluster_name" {
  description = "Name of the ECS cluster"
  value       = module.compute.ecs_cluster_name
}

output "database_endpoint" {
  description = "Endpoint of the RDS database"
  value       = module.database.db_endpoint
  sensitive   = true
}

output "database_name" {
  description = "Name of the database"
  value       = module.database.db_name
}

output "s3_bucket_reports" {
  description = "Name of the S3 bucket for reports"
  value       = module.storage.reports_bucket_name
}

output "s3_bucket_uploads" {
  description = "Name of the S3 bucket for uploads"
  value       = module.storage.uploads_bucket_name
}

output "eventbridge_bus_name" {
  description = "Name of the EventBridge event bus"
  value       = module.messaging.eventbridge_bus_name
}

output "eventbridge_bus_arn" {
  description = "ARN of the EventBridge event bus"
  value       = module.messaging.eventbridge_bus_arn
}

output "sqs_direct_messages_queue_url" {
  description = "URL of the SQS queue for direct messages"
  value       = module.messaging.sqs_direct_messages_queue_url
}

output "sqs_group_messages_queue_url" {
  description = "URL of the SQS queue for group messages"
  value       = module.messaging.sqs_group_messages_queue_url
}

output "cloudwatch_log_group_name" {
  description = "Name of the CloudWatch log group"
  value       = module.monitoring.log_group_name
}
