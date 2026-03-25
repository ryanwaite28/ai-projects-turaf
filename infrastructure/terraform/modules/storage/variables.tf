# Storage Module Variables

variable "environment" {
  description = "Environment name (dev, qa, prod)"
  type        = string
}

variable "enable_versioning" {
  description = "Enable S3 versioning (disable for demo to save on storage costs)"
  type        = bool
  default     = false
}

variable "enable_separate_buckets" {
  description = "Create separate buckets for reports and static assets (disable for demo to minimize costs)"
  type        = bool
  default     = false
}

variable "log_retention_days" {
  description = "Number of days to retain logs in S3 (7 days for demo, 90+ for production)"
  type        = number
  default     = 7

  validation {
    condition     = var.log_retention_days >= 1 && var.log_retention_days <= 365
    error_message = "Log retention must be between 1 and 365 days."
  }
}

variable "backup_retention_days" {
  description = "Number of days to retain backups in S3 (7 days for demo, 30+ for production)"
  type        = number
  default     = 7

  validation {
    condition     = var.backup_retention_days >= 1 && var.backup_retention_days <= 365
    error_message = "Backup retention must be between 1 and 365 days."
  }
}

variable "enable_intelligent_tiering" {
  description = "Enable Intelligent-Tiering for cost optimization (optional)"
  type        = bool
  default     = false
}

variable "enable_cors" {
  description = "Enable CORS configuration for static assets"
  type        = bool
  default     = false
}

variable "cors_allowed_origins" {
  description = "List of allowed origins for CORS (only used if enable_cors = true)"
  type        = list(string)
  default = [
    "https://app.turafapp.com",
    "https://app.dev.turafapp.com",
    "https://app.qa.turafapp.com"
  ]
}

variable "enable_cloudfront_access" {
  description = "Enable CloudFront access to static assets"
  type        = bool
  default     = false
}

variable "cloudfront_distribution_arn" {
  description = "CloudFront distribution ARN for bucket policy (only used if enable_cloudfront_access = true)"
  type        = string
  default     = ""
}

variable "enable_access_logging" {
  description = "Enable S3 access logging (additional storage costs)"
  type        = bool
  default     = false
}

variable "tags" {
  description = "Additional tags for all resources"
  type        = map(string)
  default     = {}
}
