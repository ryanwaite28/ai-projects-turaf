# Production Environment Variables

variable "environment" {
  description = "Environment name"
  type        = string
  default     = "prod"
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
  default     = "10.2.0.0/16"
}

variable "availability_zones" {
  description = "Availability zones"
  type        = list(string)
  default     = ["us-east-1a", "us-east-1b", "us-east-1c"]
}

variable "enable_nat_gateway" {
  description = "Enable NAT Gateway"
  type        = bool
  default     = true
}

variable "single_nat_gateway" {
  description = "Use single NAT Gateway"
  type        = bool
  default     = false
}

variable "enable_flow_logs" {
  description = "Enable VPC Flow Logs"
  type        = bool
  default     = true
}

# Database Variables
variable "db_instance_class" {
  description = "RDS instance class"
  type        = string
  default     = "db.t3.medium"
}

variable "db_allocated_storage" {
  description = "RDS allocated storage in GB"
  type        = number
  default     = 100
}

variable "db_max_allocated_storage" {
  description = "RDS max allocated storage in GB"
  type        = number
  default     = 500
}

variable "backup_retention_days" {
  description = "Database backup retention days"
  type        = number
  default     = 30
}

variable "enable_multi_az" {
  description = "Enable Multi-AZ deployment"
  type        = bool
  default     = true
}

variable "deletion_protection" {
  description = "Enable deletion protection"
  type        = bool
  default     = true
}

variable "enable_performance_insights" {
  description = "Enable Performance Insights"
  type        = bool
  default     = true
}

variable "enable_redis" {
  description = "Enable ElastiCache Redis"
  type        = bool
  default     = true
}

variable "redis_node_type" {
  description = "Redis node type"
  type        = string
  default     = "cache.t3.medium"
}

variable "redis_num_cache_nodes" {
  description = "Number of Redis cache nodes"
  type        = number
  default     = 2
}

variable "redis_snapshot_retention_days" {
  description = "Redis snapshot retention days"
  type        = number
  default     = 7
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
  default     = 3
}

variable "documentdb_backup_retention_days" {
  description = "DocumentDB backup retention days"
  type        = number
  default     = 7
}

# Storage Variables
variable "enable_s3_versioning" {
  description = "Enable S3 versioning"
  type        = bool
  default     = true
}

variable "enable_separate_s3_buckets" {
  description = "Use separate S3 buckets for different purposes"
  type        = bool
  default     = true
}

variable "s3_log_retention_days" {
  description = "S3 log retention days"
  type        = number
  default     = 90
}

variable "s3_backup_retention_days" {
  description = "S3 backup retention days"
  type        = number
  default     = 90
}

# Messaging Variables
variable "enable_event_archive" {
  description = "Enable EventBridge event archive"
  type        = bool
  default     = true
}

variable "enable_chat_queue" {
  description = "Enable chat messages queue"
  type        = bool
  default     = true
}

variable "enable_notification_queue" {
  description = "Enable notifications queue"
  type        = bool
  default     = true
}

variable "enable_report_queue" {
  description = "Enable reports queue"
  type        = bool
  default     = true
}

variable "enable_sns_topics" {
  description = "Enable SNS topics"
  type        = bool
  default     = true
}

variable "enable_queue_alarms" {
  description = "Enable queue alarms"
  type        = bool
  default     = true
}

variable "event_archive_retention_days" {
  description = "EventBridge archive retention days"
  type        = number
  default     = 30
}

variable "message_retention_seconds" {
  description = "SQS message retention in seconds"
  type        = number
  default     = 1209600
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
  default     = false
}

variable "enable_container_insights" {
  description = "Enable Container Insights"
  type        = bool
  default     = true
}

variable "enable_autoscaling" {
  description = "Enable auto-scaling"
  type        = bool
  default     = true
}

variable "identity_service_cpu" {
  description = "Identity service CPU units"
  type        = number
  default     = 1024
}

variable "identity_service_memory" {
  description = "Identity service memory in MB"
  type        = number
  default     = 2048
}

variable "identity_service_desired_count" {
  description = "Identity service desired task count"
  type        = number
  default     = 3
}

variable "identity_service_min_capacity" {
  description = "Identity service minimum capacity"
  type        = number
  default     = 2
}

variable "identity_service_max_capacity" {
  description = "Identity service maximum capacity"
  type        = number
  default     = 10
}

variable "organization_service_cpu" {
  description = "Organization service CPU units"
  type        = number
  default     = 1024
}

variable "organization_service_memory" {
  description = "Organization service memory in MB"
  type        = number
  default     = 2048
}

variable "organization_service_desired_count" {
  description = "Organization service desired task count"
  type        = number
  default     = 3
}

variable "organization_service_min_capacity" {
  description = "Organization service minimum capacity"
  type        = number
  default     = 2
}

variable "organization_service_max_capacity" {
  description = "Organization service maximum capacity"
  type        = number
  default     = 10
}

variable "experiment_service_cpu" {
  description = "Experiment service CPU units"
  type        = number
  default     = 2048
}

variable "experiment_service_memory" {
  description = "Experiment service memory in MB"
  type        = number
  default     = 4096
}

variable "experiment_service_desired_count" {
  description = "Experiment service desired task count"
  type        = number
  default     = 3
}

variable "experiment_service_min_capacity" {
  description = "Experiment service minimum capacity"
  type        = number
  default     = 2
}

variable "experiment_service_max_capacity" {
  description = "Experiment service maximum capacity"
  type        = number
  default     = 10
}

variable "enable_metrics_service" {
  description = "Enable metrics service"
  type        = bool
  default     = true
}

variable "enable_reporting_service" {
  description = "Enable reporting service"
  type        = bool
  default     = true
}

variable "enable_notification_service" {
  description = "Enable notification service"
  type        = bool
  default     = true
}

variable "log_retention_days" {
  description = "CloudWatch log retention days"
  type        = number
  default     = 30
}

variable "enable_execute_command" {
  description = "Enable ECS Exec"
  type        = bool
  default     = true
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
  default     = true
}

variable "enable_notification_processor" {
  description = "Enable notification processor Lambda"
  type        = bool
  default     = true
}

variable "enable_report_generator" {
  description = "Enable report generator Lambda"
  type        = bool
  default     = true
}

variable "lambda_use_vpc_mode" {
  description = "Run Lambda in VPC"
  type        = bool
  default     = true
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
  default     = true
}

variable "enable_dashboard" {
  description = "Enable CloudWatch dashboard"
  type        = bool
  default     = true
}

variable "enable_sns_alerts" {
  description = "Enable SNS alerts"
  type        = bool
  default     = true
}

variable "enable_xray" {
  description = "Enable X-Ray tracing"
  type        = bool
  default     = true
}

variable "enable_log_insights" {
  description = "Enable Log Insights queries"
  type        = bool
  default     = true
}

variable "alarm_email" {
  description = "Email for alarm notifications"
  type        = string
}

variable "cpu_threshold" {
  description = "CPU alarm threshold"
  type        = number
  default     = 70
}

variable "memory_threshold" {
  description = "Memory alarm threshold"
  type        = number
  default     = 70
}

variable "response_time_threshold" {
  description = "Response time alarm threshold in seconds"
  type        = number
  default     = 1
}

# Tags
variable "tags" {
  description = "Common tags for all resources"
  type        = map(string)
  default = {
    Project     = "turaf"
    Environment = "prod"
    ManagedBy   = "terraform"
    Compliance  = "required"
  }
}
