# Storage Module Outputs

output "primary_bucket_id" {
  description = "Primary S3 bucket ID"
  value       = aws_s3_bucket.primary.id
}

output "primary_bucket_arn" {
  description = "Primary S3 bucket ARN"
  value       = aws_s3_bucket.primary.arn
}

output "primary_bucket_domain_name" {
  description = "Primary S3 bucket domain name"
  value       = aws_s3_bucket.primary.bucket_domain_name
}

output "primary_bucket_regional_domain_name" {
  description = "Primary S3 bucket regional domain name"
  value       = aws_s3_bucket.primary.bucket_regional_domain_name
}

output "reports_bucket_id" {
  description = "Reports S3 bucket ID (null if separate buckets disabled)"
  value       = var.enable_separate_buckets ? aws_s3_bucket.reports[0].id : null
}

output "reports_bucket_arn" {
  description = "Reports S3 bucket ARN (null if separate buckets disabled)"
  value       = var.enable_separate_buckets ? aws_s3_bucket.reports[0].arn : null
}

output "static_bucket_id" {
  description = "Static assets S3 bucket ID (null if separate buckets disabled)"
  value       = var.enable_separate_buckets ? aws_s3_bucket.static[0].id : null
}

output "static_bucket_arn" {
  description = "Static assets S3 bucket ARN (null if separate buckets disabled)"
  value       = var.enable_separate_buckets ? aws_s3_bucket.static[0].arn : null
}

output "bucket_prefixes" {
  description = "Recommended S3 bucket prefixes for organizing data"
  value = {
    logs         = "logs/"
    backups      = "backups/"
    reports      = "reports/"
    static       = "static/"
    app_data     = "app-data/"
    uploads      = "uploads/"
    temp         = "temp/"
  }
}
