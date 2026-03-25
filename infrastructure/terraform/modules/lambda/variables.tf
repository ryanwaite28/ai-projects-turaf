# Lambda Module Variables

variable "environment" {
  description = "Environment name (dev, qa, prod)"
  type        = string
}

variable "region" {
  description = "AWS region"
  type        = string
}

variable "vpc_id" {
  description = "VPC ID for Lambda functions in VPC"
  type        = string
  default     = ""
}

variable "private_subnet_ids" {
  description = "Private subnet IDs for Lambda functions (optional - VPC mode only)"
  type        = list(string)
  default     = []
}

variable "lambda_security_group_id" {
  description = "Security group ID for Lambda functions in VPC"
  type        = string
  default     = ""
}

variable "lambda_execution_role_arn" {
  description = "IAM role ARN for Lambda execution"
  type        = string
}

# EventBridge Integration
variable "event_bus_name" {
  description = "EventBridge event bus name"
  type        = string
}

variable "event_bus_arn" {
  description = "EventBridge event bus ARN"
  type        = string
}

# SQS Integration
variable "events_queue_arn" {
  description = "SQS events queue ARN for Lambda triggers"
  type        = string
  default     = ""
}

variable "notifications_queue_arn" {
  description = "SQS notifications queue ARN"
  type        = string
  default     = ""
}

variable "reports_queue_arn" {
  description = "SQS reports queue ARN"
  type        = string
  default     = ""
}

# Storage
variable "reports_bucket_name" {
  description = "S3 bucket name for generated reports"
  type        = string
  default     = ""
}

variable "lambda_artifacts_bucket" {
  description = "S3 bucket for Lambda deployment packages"
  type        = string
  default     = ""
}

# SES Configuration
variable "from_email" {
  description = "From email address for notifications"
  type        = string
  default     = ""
}

# Cost Optimization Flags
variable "enable_event_processor" {
  description = "Enable event processor Lambda (disable for demo - use ECS instead)"
  type        = bool
  default     = false
}

variable "enable_notification_processor" {
  description = "Enable notification processor Lambda (disable for demo - use SES directly)"
  type        = bool
  default     = false
}

variable "enable_report_generator" {
  description = "Enable report generator Lambda (disable for demo - generate on-demand)"
  type        = bool
  default     = false
}

variable "use_vpc_mode" {
  description = "Run Lambda functions in VPC (additional cost for NAT - disable for demo)"
  type        = bool
  default     = false
}

# Lambda Configuration
variable "event_processor_memory" {
  description = "Memory for event processor Lambda in MB"
  type        = number
  default     = 512
  
  validation {
    condition     = var.event_processor_memory >= 128 && var.event_processor_memory <= 10240
    error_message = "Memory must be between 128 MB and 10240 MB."
  }
}

variable "event_processor_timeout" {
  description = "Timeout for event processor Lambda in seconds"
  type        = number
  default     = 60
  
  validation {
    condition     = var.event_processor_timeout >= 1 && var.event_processor_timeout <= 900
    error_message = "Timeout must be between 1 and 900 seconds."
  }
}

variable "notification_processor_memory" {
  description = "Memory for notification processor Lambda in MB"
  type        = number
  default     = 512
}

variable "notification_processor_timeout" {
  description = "Timeout for notification processor Lambda in seconds"
  type        = number
  default     = 60
}

variable "report_generator_memory" {
  description = "Memory for report generator Lambda in MB"
  type        = number
  default     = 1024
}

variable "report_generator_timeout" {
  description = "Timeout for report generator Lambda in seconds"
  type        = number
  default     = 300
}

# Lambda Versions
variable "event_processor_version" {
  description = "Version/tag of event processor Lambda package"
  type        = string
  default     = "latest"
}

variable "notification_processor_version" {
  description = "Version/tag of notification processor Lambda package"
  type        = string
  default     = "latest"
}

variable "report_generator_version" {
  description = "Version/tag of report generator Lambda package"
  type        = string
  default     = "latest"
}

# Runtime Configuration
variable "lambda_runtime" {
  description = "Lambda runtime (nodejs20.x, python3.11, java17)"
  type        = string
  default     = "nodejs20.x"
}

variable "log_retention_days" {
  description = "CloudWatch Logs retention in days (7 for demo, 30+ for production)"
  type        = number
  default     = 7
  
  validation {
    condition     = contains([1, 3, 5, 7, 14, 30, 60, 90, 120, 150, 180, 365, 400, 545, 731, 1827, 3653], var.log_retention_days)
    error_message = "CloudWatch log retention must be a valid value."
  }
}

variable "reserved_concurrent_executions" {
  description = "Reserved concurrent executions (set to limit costs, -1 for unreserved)"
  type        = number
  default     = -1
}

variable "tags" {
  description = "Additional tags for all resources"
  type        = map(string)
  default     = {}
}
