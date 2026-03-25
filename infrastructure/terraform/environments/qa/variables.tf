# QA Environment Variables

variable "environment" {
  description = "Environment name"
  type        = string
  default     = "qa"
}

variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "us-east-1"
}

# Networking Variables
variable "vpc_cidr" {
  description = "CIDR block for VPC"
  type        = string
  default     = "10.1.0.0/16"
}

variable "availability_zones" {
  description = "Availability zones"
  type        = list(string)
  default     = ["us-east-1a", "us-east-1b"]
}

variable "enable_nat_gateway" {
  description = "Enable NAT Gateway"
  type        = bool
  default     = false
}

variable "single_nat_gateway" {
  description = "Use single NAT Gateway"
  type        = bool
  default     = false
}

variable "enable_flow_logs" {
  description = "Enable VPC Flow Logs"
  type        = bool
  default     = false
}

# Database Variables
variable "db_instance_class" {
  description = "RDS instance class"
  type        = string
  default     = "db.t3.small"
}

variable "db_allocated_storage" {
  description = "RDS allocated storage in GB"
  type        = number
  default     = 50
}

variable "db_max_allocated_storage" {
  description = "RDS max allocated storage in GB"
  type        = number
  default     = 100
}

variable "backup_retention_days" {
  description = "Database backup retention days"
  type        = number
  default     = 7
}

variable "enable_multi_az" {
  description = "Enable Multi-AZ deployment"
  type        = bool
  default     = false
}

variable "deletion_protection" {
  description = "Enable deletion protection"
  type        = bool
  default     = false
}

variable "enable_performance_insights" {
  description = "Enable Performance Insights"
  type        = bool
  default     = false
}

variable "enable_redis" {
  description = "Enable ElastiCache Redis"
  type        = bool
  default     = false
}

variable "redis_node_type" {
  description = "Redis node type"
  type        = string
  default     = "cache.t3.small"
}

variable "redis_num_cache_nodes" {
  description = "Number of Redis cache nodes"
  type        = number
  default     = 1
}

variable "redis_snapshot_retention_days" {
  description = "Redis snapshot retention days"
  type        = number
  default     = 3
}

variable "enable_documentdb" {
  description = "Enable DocumentDB"
  type        = bool
  default     = false
}

variable "documentdb_instance_class" {
  description = "DocumentDB instance class"
  type        = string
  default     = "db.t3.medium"
}

variable "documentdb_instance_count" {
  description = "Number of DocumentDB instances"
  type        = number
  default     = 1
}

variable "documentdb_backup_retention_days" {
  description = "DocumentDB backup retention days"
  type        = number
  default     = 3
}

# Storage Variables
variable "enable_s3_versioning" {
  description = "Enable S3 versioning"
  type        = bool
  default     = false
}

variable "enable_separate_s3_buckets" {
  description = "Use separate S3 buckets for different purposes"
  type        = bool
  default     = false
}

variable "s3_log_retention_days" {
  description = "S3 log retention days"
  type        = number
  default     = 14
}

variable "s3_backup_retention_days" {
  description = "S3 backup retention days"
  type        = number
  default     = 14
}

# Messaging Variables
variable "enable_event_archive" {
  description = "Enable EventBridge event archive"
  type        = bool
  default     = false
}

variable "enable_chat_queue" {
  description = "Enable chat messages queue"
  type        = bool
  default     = false
}

variable "enable_notification_queue" {
  description = "Enable notifications queue"
  type        = bool
  default     = false
}

variable "enable_report_queue" {
  description = "Enable reports queue"
  type        = bool
  default     = false
}

variable "enable_sns_topics" {
  description = "Enable SNS topics"
  type        = bool
  default     = false
}

variable "enable_queue_alarms" {
  description = "Enable queue alarms"
  type        = bool
  default     = false
}

variable "event_archive_retention_days" {
  description = "EventBridge archive retention days"
  type        = number
  default     = 14
}

variable "message_retention_seconds" {
  description = "SQS message retention in seconds"
  type        = number
  default     = 345600
}

# Compute Variables
variable "acm_certificate_arn" {
  description = "ACM certificate ARN for HTTPS"
  type        = string
}

variable "identity_service_image" {
  description = "Identity service ECR image URL"
  type        = string
}

variable "organization_service_image" {
  description = "Organization service ECR image URL"
  type        = string
}

variable "experiment_service_image" {
  description = "Experiment service ECR image URL"
  type        = string
}

variable "image_tag" {
  description = "Docker image tag"
  type        = string
  default     = "latest"
}

variable "use_fargate_spot" {
  description = "Use Fargate Spot"
  type        = bool
  default     = true
}

variable "enable_container_insights" {
  description = "Enable Container Insights"
  type        = bool
  default     = false
}

variable "enable_autoscaling" {
  description = "Enable auto-scaling"
  type        = bool
  default     = false
}

variable "identity_service_cpu" {
  description = "Identity service CPU units"
  type        = number
  default     = 512
}

variable "identity_service_memory" {
  description = "Identity service memory in MB"
  type        = number
  default     = 1024
}

variable "identity_service_desired_count" {
  description = "Identity service desired task count"
  type        = number
  default     = 2
}

variable "organization_service_cpu" {
  description = "Organization service CPU units"
  type        = number
  default     = 512
}

variable "organization_service_memory" {
  description = "Organization service memory in MB"
  type        = number
  default     = 1024
}

variable "organization_service_desired_count" {
  description = "Organization service desired task count"
  type        = number
  default     = 2
}

variable "experiment_service_cpu" {
  description = "Experiment service CPU units"
  type        = number
  default     = 512
}

variable "experiment_service_memory" {
  description = "Experiment service memory in MB"
  type        = number
  default     = 1024
}

variable "experiment_service_desired_count" {
  description = "Experiment service desired task count"
  type        = number
  default     = 2
}

variable "enable_metrics_service" {
  description = "Enable metrics service"
  type        = bool
  default     = false
}

variable "enable_reporting_service" {
  description = "Enable reporting service"
  type        = bool
  default     = false
}

variable "enable_notification_service" {
  description = "Enable notification service"
  type        = bool
  default     = false
}

variable "log_retention_days" {
  description = "CloudWatch log retention days"
  type        = number
  default     = 14
}

variable "enable_execute_command" {
  description = "Enable ECS Exec"
  type        = bool
  default     = false
}

# Lambda Variables
variable "lambda_artifacts_bucket" {
  description = "S3 bucket for Lambda artifacts"
  type        = string
  default     = ""
}

variable "from_email" {
  description = "From email address for notifications"
  type        = string
  default     = ""
}

variable "enable_event_processor" {
  description = "Enable event processor Lambda"
  type        = bool
  default     = false
}

variable "enable_notification_processor" {
  description = "Enable notification processor Lambda"
  type        = bool
  default     = false
}

variable "enable_report_generator" {
  description = "Enable report generator Lambda"
  type        = bool
  default     = false
}

variable "lambda_use_vpc_mode" {
  description = "Run Lambda in VPC"
  type        = bool
  default     = false
}

variable "lambda_runtime" {
  description = "Lambda runtime"
  type        = string
  default     = "nodejs20.x"
}

# Monitoring Variables
variable "enable_alarms" {
  description = "Enable CloudWatch alarms"
  type        = bool
  default     = false
}

variable "enable_dashboard" {
  description = "Enable CloudWatch dashboard"
  type        = bool
  default     = false
}

variable "enable_sns_alerts" {
  description = "Enable SNS alerts"
  type        = bool
  default     = false
}

variable "enable_xray" {
  description = "Enable X-Ray tracing"
  type        = bool
  default     = false
}

variable "enable_log_insights" {
  description = "Enable Log Insights queries"
  type        = bool
  default     = false
}

variable "alarm_email" {
  description = "Email for alarm notifications"
  type        = string
  default     = ""
}

variable "cpu_threshold" {
  description = "CPU alarm threshold"
  type        = number
  default     = 80
}

variable "memory_threshold" {
  description = "Memory alarm threshold"
  type        = number
  default     = 80
}

variable "response_time_threshold" {
  description = "Response time alarm threshold in seconds"
  type        = number
  default     = 2
}

# Tags
variable "tags" {
  description = "Common tags for all resources"
  type        = map(string)
  default = {
    Project     = "turaf"
    Environment = "qa"
    ManagedBy   = "terraform"
  }
}
