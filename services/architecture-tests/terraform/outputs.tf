output "s3_bucket_name" {
  description = "S3 bucket name for test reports"
  value       = aws_s3_bucket.test_reports.id
}

output "s3_bucket_arn" {
  description = "S3 bucket ARN"
  value       = aws_s3_bucket.test_reports.arn
}

output "cloudfront_distribution_id" {
  description = "CloudFront distribution ID for cache invalidation"
  value       = aws_cloudfront_distribution.test_reports.id
}

output "cloudfront_domain_name" {
  description = "CloudFront domain name"
  value       = aws_cloudfront_distribution.test_reports.domain_name
}

output "reports_url" {
  description = "Full URL for accessing test reports"
  value       = "https://reports.${var.environment}.turafapp.com"
}
