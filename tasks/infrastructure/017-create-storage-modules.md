# Task: Create Storage Modules

**Service**: Infrastructure  
**Phase**: 10  
**Estimated Time**: 2 hours  

## Objective

Create Terraform module for S3 buckets for reports and application data.

## Prerequisites

- [x] Task 001: Terraform structure setup

## Scope

**Files to Create**:
- `infrastructure/terraform/modules/storage/main.tf`
- `infrastructure/terraform/modules/storage/variables.tf`
- `infrastructure/terraform/modules/storage/outputs.tf`

## Implementation Details

### S3 Buckets

```hcl
resource "aws_s3_bucket" "reports" {
  bucket = "turaf-reports-${var.environment}-${data.aws_caller_identity.current.account_id}"
  
  tags = {
    Name        = "turaf-reports-${var.environment}"
    Environment = var.environment
  }
}

resource "aws_s3_bucket_versioning" "reports" {
  bucket = aws_s3_bucket.reports.id
  
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_encryption" "reports" {
  bucket = aws_s3_bucket.reports.id
  
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_lifecycle_configuration" "reports" {
  bucket = aws_s3_bucket.reports.id
  
  rule {
    id     = "archive-old-reports"
    status = "Enabled"
    
    transition {
      days          = 90
      storage_class = "GLACIER"
    }
    
    expiration {
      days = 365
    }
  }
}
```

## Acceptance Criteria

- [ ] S3 buckets created
- [ ] Versioning enabled
- [ ] Encryption configured
- [ ] Lifecycle policies set
- [ ] Bucket policies configured
- [ ] terraform plan succeeds

## Testing Requirements

**Validation**:
- Run `terraform plan`
- Verify bucket configurations
- Check lifecycle rules

## References

- Specification: `specs/aws-infrastructure.md` (Storage section)
- Related Tasks: 006-create-messaging-modules
