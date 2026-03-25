# Messaging Module Variables

variable "environment" {
  description = "Environment name (dev, qa, prod)"
  type        = string
}

variable "reporting_lambda_arn" {
  description = "ARN of the reporting Lambda function (optional)"
  type        = string
  default     = ""
}

variable "reporting_lambda_name" {
  description = "Name of the reporting Lambda function (optional)"
  type        = string
  default     = ""
}

variable "notification_lambda_arn" {
  description = "ARN of the notification Lambda function (optional)"
  type        = string
  default     = ""
}

variable "notification_lambda_name" {
  description = "Name of the notification Lambda function (optional)"
  type        = string
  default     = ""
}

variable "enable_event_archive" {
  description = "Enable EventBridge event archive (additional cost - disable for demo)"
  type        = bool
  default     = false
}

variable "event_archive_retention_days" {
  description = "Number of days to retain events in archive"
  type        = number
  default     = 7
}

variable "dlq_message_retention_seconds" {
  description = "DLQ message retention in seconds (14 days max)"
  type        = number
  default     = 1209600
}

variable "message_retention_seconds" {
  description = "Message retention in seconds for main queues"
  type        = number
  default     = 345600  # 4 days
}

variable "visibility_timeout_seconds" {
  description = "Visibility timeout for SQS queues"
  type        = number
  default     = 300
}

variable "max_receive_count" {
  description = "Maximum receives before sending to DLQ"
  type        = number
  default     = 3
}

variable "enable_queue_alarms" {
  description = "Enable CloudWatch alarms for queues (additional cost)"
  type        = bool
  default     = false
}

variable "dlq_alarm_threshold" {
  description = "Alarm threshold for DLQ depth"
  type        = number
  default     = 1
}

variable "queue_depth_alarm_threshold" {
  description = "Alarm threshold for queue depth"
  type        = number
  default     = 100
}

variable "alarm_sns_topic_arn" {
  description = "SNS topic ARN for alarms (optional)"
  type        = string
  default     = ""
}

variable "enable_chat_queue" {
  description = "Enable chat messages queue (disable for demo)"
  type        = bool
  default     = false
}

variable "enable_notification_queue" {
  description = "Enable notifications queue (disable for demo)"
  type        = bool
  default     = false
}

variable "enable_report_queue" {
  description = "Enable reports queue (disable for demo)"
  type        = bool
  default     = false
}

variable "enable_sns_topics" {
  description = "Enable SNS topics (additional cost - disable for demo)"
  type        = bool
  default     = false
}

variable "kms_key_id" {
  description = "KMS key ID for SNS encryption (only used if SNS enabled)"
  type        = string
  default     = ""
}

variable "alarm_email" {
  description = "Email address for alarm notifications (only used if SNS enabled)"
  type        = string
  default     = ""
}

variable "tags" {
  description = "Additional tags for all resources"
  type        = map(string)
  default     = {}
}
