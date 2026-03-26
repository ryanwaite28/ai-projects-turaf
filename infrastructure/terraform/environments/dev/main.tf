# Turaf Platform - Development Environment
# Cost-optimized configuration for demo/portfolio purposes

terraform {
  required_version = ">= 1.5.0"
  
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = var.tags
  }
}

# Data sources
data "aws_caller_identity" "current" {}
data "aws_availability_zones" "available" {
  state = "available"
}

# Networking Module
module "networking" {
  source = "../../modules/networking"

  environment        = var.environment
  vpc_cidr           = var.vpc_cidr
  availability_zones = var.availability_zones
  enable_nat_gateway = var.enable_nat_gateway
  single_nat_gateway = var.single_nat_gateway
  enable_flow_logs   = var.enable_flow_logs

  tags = var.tags
}

# Security Module
module "security" {
  source = "../../modules/security"

  environment = var.environment
  region      = var.aws_region
  account_id  = data.aws_caller_identity.current.account_id
  vpc_id      = module.networking.vpc_id
  vpc_cidr    = var.vpc_cidr

  tags = var.tags
}

# Database Module
module "database" {
  source = "../../modules/database"

  environment = var.environment

  # Networking
  database_subnet_ids = module.networking.database_subnet_ids

  # Security
  rds_security_group_id         = module.security.rds_security_group_id
  elasticache_security_group_id = module.security.elasticache_security_group_id
  documentdb_security_group_id  = module.security.documentdb_security_group_id
  kms_key_id                    = module.security.rds_kms_key_id
  rds_kms_key_arn               = module.security.rds_kms_key_arn

  # RDS Configuration
  db_instance_class             = var.db_instance_class
  db_allocated_storage          = var.db_allocated_storage
  db_max_allocated_storage      = var.db_max_allocated_storage
  backup_retention_days         = var.backup_retention_days
  enable_multi_az               = var.enable_multi_az
  deletion_protection           = var.deletion_protection
  enable_performance_insights   = var.enable_performance_insights

  # Cost Optimization
  enable_redis      = var.enable_redis
  enable_documentdb = var.enable_documentdb

  # Redis Configuration (if enabled)
  redis_node_type               = var.redis_node_type
  redis_num_cache_nodes         = var.redis_num_cache_nodes
  redis_snapshot_retention_days = var.redis_snapshot_retention_days

  # DocumentDB Configuration (if enabled)
  documentdb_instance_class         = var.documentdb_instance_class
  documentdb_instance_count         = var.documentdb_instance_count
  documentdb_backup_retention_days  = var.documentdb_backup_retention_days

  tags = var.tags
}

# Storage Module
module "storage" {
  source = "../../modules/storage"

  environment = var.environment

  # Cost Optimization
  enable_versioning     = var.enable_s3_versioning
  enable_separate_buckets = var.enable_separate_s3_buckets
  log_retention_days    = var.s3_log_retention_days
  backup_retention_days = var.s3_backup_retention_days

  tags = var.tags
}

# Messaging Module
module "messaging" {
  source = "../../modules/messaging"

  environment = var.environment

  # Cost Optimization
  enable_event_archive        = var.enable_event_archive
  enable_chat_queue           = var.enable_chat_queue
  enable_notification_queue   = var.enable_notification_queue
  enable_report_queue         = var.enable_report_queue
  enable_sns_topics           = var.enable_sns_topics
  enable_queue_alarms         = var.enable_queue_alarms

  # Configuration
  event_archive_retention_days = var.event_archive_retention_days
  message_retention_seconds    = var.message_retention_seconds

  tags = var.tags
}

module "compute" {
  source = "../../modules/compute"

  environment = var.environment
  region      = var.aws_region

  # Networking
  vpc_id             = module.networking.vpc_id
  private_subnet_ids = module.networking.private_subnet_ids
  public_subnet_ids  = module.networking.public_subnet_ids

  # Security
  ecs_security_group_id  = module.security.ecs_tasks_security_group_id
  alb_security_group_id  = module.security.alb_security_group_id
  ecs_execution_role_arn = module.security.ecs_execution_role_arn
  ecs_task_role_arn      = module.security.ecs_task_role_arn
  acm_certificate_arn    = var.acm_certificate_arn

  # Cost Optimization
  use_fargate_spot          = var.use_fargate_spot
  enable_container_insights = var.enable_container_insights

  tags = var.tags
}

# Lambda Module
module "lambda" {
  source = "../../modules/lambda"

  environment = var.environment
  region      = var.aws_region

  # IAM
  lambda_execution_role_arn = module.security.ecs_execution_role_arn

  # EventBridge
  event_bus_name = module.messaging.event_bus_name
  event_bus_arn  = module.messaging.event_bus_arn

  # SQS
  notifications_queue_arn = module.messaging.notifications_queue_arn
  reports_queue_arn       = module.messaging.reports_queue_arn

  # Storage
  reports_bucket_name     = module.storage.primary_bucket_id
  lambda_artifacts_bucket = var.lambda_artifacts_bucket

  # SES
  from_email = var.from_email

  # Cost Optimization - All disabled for demo
  enable_event_processor        = var.enable_event_processor
  enable_notification_processor = var.enable_notification_processor
  enable_report_generator       = var.enable_report_generator
  use_vpc_mode                  = var.lambda_use_vpc_mode

  # VPC Configuration (if enabled)
  vpc_id                    = module.networking.vpc_id
  private_subnet_ids        = module.networking.private_subnet_ids
  lambda_security_group_id  = module.security.ecs_tasks_security_group_id

  # Runtime
  lambda_runtime     = var.lambda_runtime
  log_retention_days = var.log_retention_days

  tags = var.tags
}

# Monitoring Module - TEMPORARILY DISABLED (fixing dashboard syntax)
# module "monitoring" {
#   source = "../../modules/monitoring"
#
#   environment = var.environment
#   region      = var.aws_region
#
#   # Resource Identifiers
#   cluster_name    = module.compute.cluster_name
#   alb_arn_suffix  = module.compute.alb_arn_suffix
#   rds_instance_id = module.database.rds_instance_id
#
#   # Cost Optimization - All disabled for demo
#   enable_alarms       = var.enable_alarms
#   enable_dashboard    = var.enable_dashboard
#   enable_sns_alerts   = var.enable_sns_alerts
#   enable_xray         = var.enable_xray
#   enable_log_insights = var.enable_log_insights
#
#   # Alert Configuration (if enabled)
#   alarm_email = var.alarm_email
#
#   # Alarm Thresholds
#   cpu_threshold           = var.cpu_threshold
#   memory_threshold        = var.memory_threshold
#   response_time_threshold = var.response_time_threshold
#
#   tags = var.tags
# }
