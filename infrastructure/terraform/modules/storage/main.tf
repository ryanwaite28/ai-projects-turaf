# Storage Module - S3 Buckets for Turaf Platform
# Cost-optimized configuration for demo/portfolio purposes

data "aws_caller_identity" "current" {}

locals {
  bucket_name = "turaf-${var.environment}-${data.aws_caller_identity.current.account_id}"
}

# Primary Application Bucket (Multi-purpose for cost optimization)
resource "aws_s3_bucket" "primary" {
  bucket = local.bucket_name

  tags = merge(
    var.tags,
    {
      Name        = "turaf-primary-${var.environment}"
      Environment = var.environment
      Purpose     = "multi-purpose"
    }
  )
}

# Block public access
resource "aws_s3_bucket_public_access_block" "primary" {
  bucket = aws_s3_bucket.primary.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# Versioning (disabled for cost optimization in demo)
resource "aws_s3_bucket_versioning" "primary" {
  bucket = aws_s3_bucket.primary.id

  versioning_configuration {
    status = var.enable_versioning ? "Enabled" : "Disabled"
  }
}

# Server-side encryption (AES-256, no KMS costs)
resource "aws_s3_bucket_server_side_encryption_configuration" "primary" {
  bucket = aws_s3_bucket.primary.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
    bucket_key_enabled = true
  }
}

# Lifecycle policies for cost optimization
resource "aws_s3_bucket_lifecycle_configuration" "primary" {
  bucket = aws_s3_bucket.primary.id

  # Delete old logs
  rule {
    id     = "delete-old-logs"
    status = "Enabled"

    filter {
      prefix = "logs/"
    }

    expiration {
      days = var.log_retention_days
    }

    noncurrent_version_expiration {
      noncurrent_days = 1
    }
  }

  # Delete old backups
  rule {
    id     = "delete-old-backups"
    status = "Enabled"

    filter {
      prefix = "backups/"
    }

    expiration {
      days = var.backup_retention_days
    }

    noncurrent_version_expiration {
      noncurrent_days = 1
    }
  }

  # Delete incomplete multipart uploads
  rule {
    id     = "cleanup-multipart-uploads"
    status = "Enabled"

    abort_incomplete_multipart_upload {
      days_after_initiation = 7
    }
  }

  # Transition old reports to Intelligent-Tiering (optional)
  dynamic "rule" {
    for_each = var.enable_intelligent_tiering ? [1] : []

    content {
      id     = "intelligent-tiering-reports"
      status = "Enabled"

      filter {
        prefix = "reports/"
      }

      transition {
        days          = 30
        storage_class = "INTELLIGENT_TIERING"
      }
    }
  }
}

# CORS configuration for static assets
resource "aws_s3_bucket_cors_configuration" "primary" {
  count  = var.enable_cors ? 1 : 0
  bucket = aws_s3_bucket.primary.id

  cors_rule {
    allowed_headers = ["*"]
    allowed_methods = ["GET", "HEAD"]
    allowed_origins = var.cors_allowed_origins
    expose_headers  = ["ETag"]
    max_age_seconds = 3000
  }
}

# Bucket policy for CloudFront access (if needed)
resource "aws_s3_bucket_policy" "primary" {
  count  = var.enable_cloudfront_access ? 1 : 0
  bucket = aws_s3_bucket.primary.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "AllowCloudFrontAccess"
        Effect = "Allow"
        Principal = {
          Service = "cloudfront.amazonaws.com"
        }
        Action   = "s3:GetObject"
        Resource = "${aws_s3_bucket.primary.arn}/static/*"
        Condition = {
          StringEquals = {
            "AWS:SourceArn" = var.cloudfront_distribution_arn
          }
        }
      }
    ]
  })
}

# Optional: Separate buckets for production (if enabled)
resource "aws_s3_bucket" "reports" {
  count  = var.enable_separate_buckets ? 1 : 0
  bucket = "turaf-reports-${var.environment}-${data.aws_caller_identity.current.account_id}"

  tags = merge(
    var.tags,
    {
      Name        = "turaf-reports-${var.environment}"
      Environment = var.environment
      Purpose     = "reports"
    }
  )
}

resource "aws_s3_bucket_public_access_block" "reports" {
  count  = var.enable_separate_buckets ? 1 : 0
  bucket = aws_s3_bucket.reports[0].id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "reports" {
  count  = var.enable_separate_buckets ? 1 : 0
  bucket = aws_s3_bucket.reports[0].id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
    bucket_key_enabled = true
  }
}

# Optional: Static assets bucket for production
resource "aws_s3_bucket" "static" {
  count  = var.enable_separate_buckets ? 1 : 0
  bucket = "turaf-static-${var.environment}-${data.aws_caller_identity.current.account_id}"

  tags = merge(
    var.tags,
    {
      Name        = "turaf-static-${var.environment}"
      Environment = var.environment
      Purpose     = "static-assets"
    }
  )
}

resource "aws_s3_bucket_public_access_block" "static" {
  count  = var.enable_separate_buckets ? 1 : 0
  bucket = aws_s3_bucket.static[0].id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "static" {
  count  = var.enable_separate_buckets ? 1 : 0
  bucket = aws_s3_bucket.static[0].id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
    bucket_key_enabled = true
  }
}

# CloudWatch Logs for S3 access (optional, additional cost)
resource "aws_s3_bucket_logging" "primary" {
  count  = var.enable_access_logging ? 1 : 0
  bucket = aws_s3_bucket.primary.id

  target_bucket = aws_s3_bucket.primary.id
  target_prefix = "access-logs/"
}
