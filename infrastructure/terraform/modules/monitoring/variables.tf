# Monitoring Module Variables

variable "environment" {
  description = "Environment name (dev, qa, prod)"
  type        = string
}

variable "region" {
  description = "AWS region"
  type        = string
}

# ECS Resources
variable "cluster_name" {
  description = "ECS cluster name for monitoring"
  type        = string
  default     = ""
}

variable "service_names" {
  description = "List of ECS service names to monitor"
  type        = list(string)
  default     = []
}

# ALB Resources
variable "alb_arn_suffix" {
  description = "ALB ARN suffix for monitoring"
  type        = string
  default     = ""
}

variable "target_group_arn_suffixes" {
  description = "Map of service names to target group ARN suffixes"
  type        = map(string)
  default     = {}
}

# RDS Resources
variable "rds_instance_id" {
  description = "RDS instance identifier for monitoring"
  type        = string
  default     = ""
}

# Cost Optimization Flags
variable "enable_alarms" {
  description = "Enable CloudWatch alarms (additional cost - disable for demo)"
  type        = bool
  default     = false
}

variable "enable_dashboard" {
  description = "Enable CloudWatch dashboard (free, but disable for simplicity in demo)"
  type        = bool
  default     = false
}

variable "enable_sns_alerts" {
  description = "Enable SNS topic for alerts (additional cost - disable for demo)"
  type        = bool
  default     = false
}

variable "enable_xray" {
  description = "Enable X-Ray tracing (additional cost - disable for demo)"
  type        = bool
  default     = false
}

variable "enable_log_insights" {
  description = "Enable CloudWatch Logs Insights queries (free, but disable for demo)"
  type        = bool
  default     = false
}

# Alarm Configuration
variable "alarm_email" {
  description = "Email address for alarm notifications (only used if SNS enabled)"
  type        = string
  default     = ""
}

variable "cpu_threshold" {
  description = "CPU utilization threshold for alarms (percentage)"
  type        = number
  default     = 80
  
  validation {
    condition     = var.cpu_threshold >= 0 && var.cpu_threshold <= 100
    error_message = "CPU threshold must be between 0 and 100."
  }
}

variable "memory_threshold" {
  description = "Memory utilization threshold for alarms (percentage)"
  type        = number
  default     = 80
  
  validation {
    condition     = var.memory_threshold >= 0 && var.memory_threshold <= 100
    error_message = "Memory threshold must be between 0 and 100."
  }
}

variable "error_rate_threshold" {
  description = "Error rate threshold for alarms (percentage)"
  type        = number
  default     = 5
  
  validation {
    condition     = var.error_rate_threshold >= 0 && var.error_rate_threshold <= 100
    error_message = "Error rate threshold must be between 0 and 100."
  }
}

variable "response_time_threshold" {
  description = "Response time threshold for alarms (seconds)"
  type        = number
  default     = 2
}

variable "alarm_evaluation_periods" {
  description = "Number of periods to evaluate for alarms"
  type        = number
  default     = 2
  
  validation {
    condition     = var.alarm_evaluation_periods >= 1
    error_message = "Evaluation periods must be at least 1."
  }
}

variable "alarm_period" {
  description = "Period for alarm evaluation (seconds)"
  type        = number
  default     = 300
  
  validation {
    condition     = contains([60, 300, 900, 3600], var.alarm_period)
    error_message = "Alarm period must be 60, 300, 900, or 3600 seconds."
  }
}

# X-Ray Configuration
variable "xray_sampling_rate" {
  description = "X-Ray sampling rate (0.0 to 1.0)"
  type        = number
  default     = 0.05
  
  validation {
    condition     = var.xray_sampling_rate >= 0 && var.xray_sampling_rate <= 1
    error_message = "Sampling rate must be between 0 and 1."
  }
}

variable "xray_reservoir_size" {
  description = "X-Ray reservoir size (minimum traces per second)"
  type        = number
  default     = 1
}

# Dashboard Configuration
variable "dashboard_period" {
  description = "Default period for dashboard widgets (seconds)"
  type        = number
  default     = 300
}

variable "tags" {
  description = "Additional tags for all resources"
  type        = map(string)
  default     = {}
}
