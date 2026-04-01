# Task 005: Create Terraform Infrastructure for Test Reports

**Status**: ✅ Completed  
**Assignee**: AI Assistant  
**Estimated Time**: 4 hours  
**Actual Time**: < 1 hour  
**Completed**: 2026-03-31  
**Related Spec**: [Architecture Testing](../../specs/architecture-testing.md)  
**Note**: All Terraform configuration files created. Manual deployment required with actual ACM certificate ARNs.

---

## Objective

Create Terraform configuration for S3 bucket and CloudFront distribution to store and serve architecture test reports.

---

## Prerequisites

- Task 004 completed (IAM permissions updated)
- Terraform 1.5+ installed
- Understanding of S3 and CloudFront

---

## Tasks

### 1. Create Terraform Directory Structure

```bash
cd services/architecture-tests
mkdir -p terraform
```

### 2. Create main.tf

Create `terraform/main.tf`:

```hcl
terraform {
  required_version = ">= 1.5.0"
  
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
  
  backend "s3" {
    bucket = "turaf-terraform-state"
    key    = "architecture-tests/terraform.tfstate"
    region = "us-east-1"
  }
}

provider "aws" {
  region = var.aws_region
  
  default_tags {
    tags = {
      Project     = "Turaf"
      Component   = "ArchitectureTests"
      ManagedBy   = "Terraform"
      Environment = var.environment
    }
  }
}
```

### 3. Create variables.tf

Create `terraform/variables.tf`:

```hcl
variable "environment" {
  description = "Environment name (dev, qa, prod)"
  type        = string
}

variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "us-east-1"
}

variable "acm_certificate_arn" {
  description = "ACM certificate ARN for CloudFront"
  type        = string
}

variable "report_retention_days" {
  description = "Number of days to retain test reports"
  type        = number
  default     = 90
}
```

### 4. Create s3.tf

Create `terraform/s3.tf`:

```hcl
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
```

### 5. Create cloudfront.tf

Create `terraform/cloudfront.tf`:

```hcl
resource "aws_cloudfront_origin_access_identity" "test_reports" {
  comment = "OAI for architecture test reports - ${var.environment}"
}

resource "aws_cloudfront_distribution" "test_reports" {
  enabled             = true
  is_ipv6_enabled     = true
  comment             = "Architecture test reports CDN - ${var.environment}"
  default_root_object = "index.html"
  
  aliases = ["reports.${var.environment}.turafapp.com"]
  
  origin {
    domain_name = aws_s3_bucket.test_reports.bucket_regional_domain_name
    origin_id   = "S3-test-reports"
    
    s3_origin_config {
      origin_access_identity = aws_cloudfront_origin_access_identity.test_reports.cloudfront_access_identity_path
    }
  }
  
  default_cache_behavior {
    allowed_methods  = ["GET", "HEAD", "OPTIONS"]
    cached_methods   = ["GET", "HEAD"]
    target_origin_id = "S3-test-reports"
    
    forwarded_values {
      query_string = false
      cookies {
        forward = "none"
      }
    }
    
    viewer_protocol_policy = "redirect-to-https"
    min_ttl                = 0
    default_ttl            = 3600
    max_ttl                = 86400
    compress               = true
  }
  
  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }
  
  viewer_certificate {
    acm_certificate_arn      = var.acm_certificate_arn
    ssl_support_method       = "sni-only"
    minimum_protocol_version = "TLSv1.2_2021"
  }
  
  tags = {
    Name = "turaf-architecture-test-reports-${var.environment}"
  }
}

resource "aws_s3_bucket_policy" "test_reports" {
  bucket = aws_s3_bucket.test_reports.id
  
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "AllowCloudFrontAccess"
        Effect = "Allow"
        Principal = {
          AWS = aws_cloudfront_origin_access_identity.test_reports.iam_arn
        }
        Action   = "s3:GetObject"
        Resource = "${aws_s3_bucket.test_reports.arn}/*"
      }
    ]
  })
}
```

### 6. Create route53.tf

Create `terraform/route53.tf`:

```hcl
data "aws_route53_zone" "main" {
  name         = "turafapp.com"
  private_zone = false
}

resource "aws_route53_record" "reports" {
  zone_id = data.aws_route53_zone.main.zone_id
  name    = "reports.${var.environment}.turafapp.com"
  type    = "A"
  
  alias {
    name                   = aws_cloudfront_distribution.test_reports.domain_name
    zone_id                = aws_cloudfront_distribution.test_reports.hosted_zone_id
    evaluate_target_health = false
  }
}
```

### 7. Create outputs.tf

Create `terraform/outputs.tf`:

```hcl
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
```

### 8. Create Environment-Specific tfvars

Create `terraform/dev.tfvars`:

```hcl
environment           = "dev"
aws_region            = "us-east-1"
acm_certificate_arn   = "arn:aws:acm:us-east-1:801651112319:certificate/YOUR_CERT_ID"
report_retention_days = 90
```

Create `terraform/qa.tfvars`:

```hcl
environment           = "qa"
aws_region            = "us-east-1"
acm_certificate_arn   = "arn:aws:acm:us-east-1:965932217544:certificate/YOUR_CERT_ID"
report_retention_days = 90
```

Create `terraform/prod.tfvars`:

```hcl
environment           = "prod"
aws_region            = "us-east-1"
acm_certificate_arn   = "arn:aws:acm:us-east-1:811783768245:certificate/YOUR_CERT_ID"
report_retention_days = 180
```

### 9. Deploy to DEV

```bash
cd terraform

# Initialize
terraform init

# Plan
terraform plan -var-file=dev.tfvars

# Apply
terraform apply -var-file=dev.tfvars
```

---

## Acceptance Criteria

- [x] All Terraform files created (main.tf, variables.tf, s3.tf, cloudfront.tf, route53.tf, outputs.tf)
- [x] Environment-specific tfvars files created (dev, qa, prod)
- [x] .gitignore created for Terraform files
- [x] Comprehensive README created with deployment instructions
- [ ] **Manual Step Required**: Update ACM certificate ARNs in tfvars files
- [ ] **Manual Step Required**: Terraform init and plan
- [ ] **Manual Step Required**: Deploy infrastructure to DEV
- [ ] **Manual Step Required**: Deploy infrastructure to QA
- [ ] **Manual Step Required**: Deploy infrastructure to PROD
- [ ] **Manual Step Required**: Verify S3 buckets and CloudFront distributions

---

## Verification

```bash
# Verify S3 bucket
aws s3 ls s3://turaf-architecture-test-reports-dev/

# Verify CloudFront distribution
aws cloudfront list-distributions \
  --query "DistributionList.Items[?Aliases.Items[?contains(@, 'reports.dev.turafapp.com')]]"

# Test HTTPS access
curl -I https://reports.dev.turafapp.com
```

---

## Notes

- ACM certificate ARNs need to be obtained from AWS Console
- CloudFront deployment takes 15-20 minutes
- Route53 propagation may take a few minutes
- Ensure wildcard certificate covers reports subdomain
