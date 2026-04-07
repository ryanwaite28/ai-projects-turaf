resource "aws_s3_bucket" "test_reports" {
  bucket = "turaf-architecture-test-reports-${var.environment}"
}

resource "aws_s3_bucket_versioning" "test_reports" {
  bucket = aws_s3_bucket.test_reports.id
  
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_lifecycle_configuration" "test_reports" {
  bucket = aws_s3_bucket.test_reports.id
  
  rule {
    id     = "delete-old-reports"
    status = "Enabled"
    
    expiration {
      days = var.report_retention_days
    }
    
    noncurrent_version_expiration {
      noncurrent_days = 30
    }
  }
}

resource "aws_s3_bucket_public_access_block" "test_reports" {
  bucket = aws_s3_bucket.test_reports.id
  
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "test_reports" {
  bucket = aws_s3_bucket.test_reports.id
  
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}
