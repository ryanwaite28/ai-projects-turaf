# Variables for the messaging module

variable "environment" {
  description = "Environment name (dev, staging, prod)"
  type        = string
}

variable "reporting_lambda_arn" {
  description = "ARN of the reporting Lambda function"
  type        = string
}

variable "reporting_lambda_name" {
  description = "Name of the reporting Lambda function"
  type        = string
}

variable "notification_lambda_arn" {
  description = "ARN of the notification Lambda function"
  type        = string
}

variable "notification_lambda_name" {
  description = "Name of the notification Lambda function"
  type        = string
}
