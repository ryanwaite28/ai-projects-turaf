# Compute Module Variables

variable "environment" {
  description = "Environment name (dev, qa, prod)"
  type        = string
}

variable "region" {
  description = "AWS region"
  type        = string
}

variable "vpc_id" {
  description = "VPC ID"
  type        = string
}

variable "private_subnet_ids" {
  description = "List of private subnet IDs for ECS tasks"
  type        = list(string)
}

variable "public_subnet_ids" {
  description = "List of public subnet IDs for ALB"
  type        = list(string)
}

variable "ecs_security_group_id" {
  description = "Security group ID for ECS tasks"
  type        = string
}

variable "alb_security_group_id" {
  description = "Security group ID for ALB"
  type        = string
}

variable "ecs_execution_role_arn" {
  description = "IAM role ARN for ECS task execution"
  type        = string
}

variable "ecs_task_role_arn" {
  description = "IAM role ARN for ECS tasks"
  type        = string
}

variable "acm_certificate_arn" {
  description = "ACM certificate ARN for HTTPS"
  type        = string
}

variable "db_secrets_arn" {
  description = "Secrets Manager ARN for database credentials"
  type        = string
  default     = ""
}

# Service Images
variable "identity_service_image" {
  description = "ECR image URL for identity service"
  type        = string
}

variable "organization_service_image" {
  description = "ECR image URL for organization service"
  type        = string
}

variable "experiment_service_image" {
  description = "ECR image URL for experiment service"
  type        = string
}

variable "metrics_service_image" {
  description = "ECR image URL for metrics service (optional)"
  type        = string
  default     = ""
}

variable "reporting_service_image" {
  description = "ECR image URL for reporting service (optional)"
  type        = string
  default     = ""
}

variable "notification_service_image" {
  description = "ECR image URL for notification service (optional)"
  type        = string
  default     = ""
}

variable "image_tag" {
  description = "Docker image tag to deploy"
  type        = string
  default     = "latest"
}

# Cost Optimization Flags
variable "use_fargate_spot" {
  description = "Use Fargate Spot for cost savings (70% cheaper, suitable for demo)"
  type        = bool
  default     = true
}

variable "enable_container_insights" {
  description = "Enable Container Insights (additional cost - disable for demo)"
  type        = bool
  default     = false
}

variable "enable_autoscaling" {
  description = "Enable auto-scaling (disable for demo)"
  type        = bool
  default     = false
}

variable "enable_metrics_service" {
  description = "Enable metrics service (disable for demo)"
  type        = bool
  default     = false
}

variable "enable_reporting_service" {
  description = "Enable reporting service (disable for demo)"
  type        = bool
  default     = false
}

variable "enable_notification_service" {
  description = "Enable notification service (disable for demo)"
  type        = bool
  default     = false
}

# Identity Service Configuration
variable "identity_service_cpu" {
  description = "CPU units for identity service (256 = 0.25 vCPU)"
  type        = number
  default     = 256
}

variable "identity_service_memory" {
  description = "Memory for identity service in MB"
  type        = number
  default     = 512
}

variable "identity_service_desired_count" {
  description = "Desired task count for identity service"
  type        = number
  default     = 1
}

variable "identity_service_min_capacity" {
  description = "Minimum tasks for auto-scaling"
  type        = number
  default     = 1
}

variable "identity_service_max_capacity" {
  description = "Maximum tasks for auto-scaling"
  type        = number
  default     = 1
}

# Organization Service Configuration
variable "organization_service_cpu" {
  description = "CPU units for organization service"
  type        = number
  default     = 256
}

variable "organization_service_memory" {
  description = "Memory for organization service in MB"
  type        = number
  default     = 512
}

variable "organization_service_desired_count" {
  description = "Desired task count for organization service"
  type        = number
  default     = 1
}

variable "organization_service_min_capacity" {
  description = "Minimum tasks for auto-scaling"
  type        = number
  default     = 1
}

variable "organization_service_max_capacity" {
  description = "Maximum tasks for auto-scaling"
  type        = number
  default     = 1
}

# Experiment Service Configuration
variable "experiment_service_cpu" {
  description = "CPU units for experiment service"
  type        = number
  default     = 256
}

variable "experiment_service_memory" {
  description = "Memory for experiment service in MB"
  type        = number
  default     = 512
}

variable "experiment_service_desired_count" {
  description = "Desired task count for experiment service"
  type        = number
  default     = 1
}

variable "experiment_service_min_capacity" {
  description = "Minimum tasks for auto-scaling"
  type        = number
  default     = 1
}

variable "experiment_service_max_capacity" {
  description = "Maximum tasks for auto-scaling"
  type        = number
  default     = 1
}

# Optional Services Configuration
variable "metrics_service_cpu" {
  description = "CPU units for metrics service"
  type        = number
  default     = 1024
}

variable "metrics_service_memory" {
  description = "Memory for metrics service in MB"
  type        = number
  default     = 2048
}

variable "metrics_service_desired_count" {
  description = "Desired task count for metrics service"
  type        = number
  default     = 1
}

variable "reporting_service_cpu" {
  description = "CPU units for reporting service"
  type        = number
  default     = 512
}

variable "reporting_service_memory" {
  description = "Memory for reporting service in MB"
  type        = number
  default     = 1024
}

variable "reporting_service_desired_count" {
  description = "Desired task count for reporting service"
  type        = number
  default     = 1
}

variable "notification_service_cpu" {
  description = "CPU units for notification service"
  type        = number
  default     = 256
}

variable "notification_service_memory" {
  description = "Memory for notification service in MB"
  type        = number
  default     = 512
}

variable "notification_service_desired_count" {
  description = "Desired task count for notification service"
  type        = number
  default     = 1
}

# Logging Configuration
variable "log_retention_days" {
  description = "CloudWatch Logs retention in days (7 for demo, 30+ for production)"
  type        = number
  default     = 7
  
  validation {
    condition     = contains([1, 3, 5, 7, 14, 30, 60, 90, 120, 150, 180, 365, 400, 545, 731, 1827, 3653], var.log_retention_days)
    error_message = "CloudWatch log retention must be a valid value."
  }
}

variable "enable_execute_command" {
  description = "Enable ECS Exec for debugging (additional CloudWatch Logs costs)"
  type        = bool
  default     = false
}

variable "tags" {
  description = "Additional tags for all resources"
  type        = map(string)
  default     = {}
}
