# Frontend Deployment Specification

**Source**: PROJECT.md (Section 41 - Angular Frontend), aws-infrastructure.md  
**Last Updated**: 2026-03-26  
**Status**: Current  
**Related Documents**: [Angular Frontend](angular-frontend.md), [CI/CD Pipelines](ci-cd-pipelines.md), [Domain DNS Management](domain-dns-management.md)

This specification defines the deployment architecture for the Angular frontend application using AWS S3, CloudFront, and ACM certificates.

---

## Deployment Overview

**Technology Stack**:
- **Frontend Framework**: Angular 17
- **Build Tool**: Angular CLI
- **Static Hosting**: AWS S3
- **CDN**: AWS CloudFront
- **SSL/TLS**: AWS Certificate Manager (ACM)
- **DNS**: AWS Route 53
- **CI/CD**: GitHub Actions

**Architecture**:
```
User Browser
  ↓ HTTPS
CloudFront Distribution (app.{env}.turafapp.com)
  ↓ ACM Certificate (*.turafapp.com)
  ↓ Origin Request
S3 Bucket (turaf-frontend-{env})
  ↓ Static Files
  index.html, *.js, *.css, assets/
```

---

## S3 Bucket Configuration

### Bucket Structure

**Per Environment**:
- **DEV**: `turaf-frontend-dev-801651112319`
- **QA**: `turaf-frontend-qa-965932217544`
- **PROD**: `turaf-frontend-prod-811783768245`

### Bucket Settings

**Access Control**:
- **Public Access**: Blocked (CloudFront-only access)
- **Bucket Policy**: Allow CloudFront OAI (Origin Access Identity)
- **Versioning**: Enabled (for rollback capability)
- **Encryption**: AES-256 (SSE-S3)

**Static Website Hosting**:
- **Index Document**: `index.html`
- **Error Document**: `index.html` (for Angular routing)

**Lifecycle Policy**:
```json
{
  "Rules": [
    {
      "Id": "DeleteOldVersions",
      "Status": "Enabled",
      "NoncurrentVersionExpiration": {
        "NoncurrentDays": 30
      }
    }
  ]
}
```

### Terraform Configuration

```hcl
resource "aws_s3_bucket" "frontend" {
  bucket = "${var.project_name}-frontend-${var.environment}-${data.aws_caller_identity.current.account_id}"

  tags = {
    Name        = "${var.project_name}-frontend-${var.environment}"
    Environment = var.environment
    ManagedBy   = "Terraform"
  }
}

resource "aws_s3_bucket_versioning" "frontend" {
  bucket = aws_s3_bucket.frontend.id

  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "frontend" {
  bucket = aws_s3_bucket.frontend.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_public_access_block" "frontend" {
  bucket = aws_s3_bucket.frontend.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_website_configuration" "frontend" {
  bucket = aws_s3_bucket.frontend.id

  index_document {
    suffix = "index.html"
  }

  error_document {
    key = "index.html"
  }
}
```

---

## CloudFront Distribution

### Distribution Configuration

**General Settings**:
- **Price Class**: PriceClass_100 (US, Canada, Europe)
- **HTTP Version**: HTTP/2 and HTTP/3
- **IPv6**: Enabled
- **Default Root Object**: `index.html`

**Origin Settings**:
- **Origin Domain**: S3 bucket regional endpoint
- **Origin Access**: CloudFront Origin Access Identity (OAI)
- **Origin Protocol**: HTTPS only
- **Origin Path**: `/` (root)

**Cache Behavior**:
- **Viewer Protocol Policy**: Redirect HTTP to HTTPS
- **Allowed HTTP Methods**: GET, HEAD, OPTIONS
- **Cached HTTP Methods**: GET, HEAD
- **Cache Policy**: CachingOptimized
- **Compress Objects**: Enabled (Gzip, Brotli)

**Custom Error Responses**:
```
403 → /index.html (200) - For Angular routing
404 → /index.html (200) - For Angular routing
```

**SSL/TLS Configuration**:
- **Certificate**: Root account ACM certificate (us-east-1)
  - ARN: `arn:aws:acm:us-east-1:072456928432:certificate/c660ca8d-5584-4d6f-b75f-e5f10fc5a8ab`
- **Minimum Protocol Version**: TLSv1.2_2021
- **Security Policy**: TLSv1.2_2021
- **SNI**: Required

**Domain Names (CNAMEs)**:
- **DEV**: `app.dev.turafapp.com`
- **QA**: `app.qa.turafapp.com`
- **PROD**: `app.turafapp.com`

### Terraform Configuration

```hcl
# CloudFront Origin Access Identity
resource "aws_cloudfront_origin_access_identity" "frontend" {
  comment = "OAI for ${var.project_name} frontend ${var.environment}"
}

# S3 Bucket Policy for CloudFront
resource "aws_s3_bucket_policy" "frontend" {
  bucket = aws_s3_bucket.frontend.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "AllowCloudFrontOAI"
        Effect = "Allow"
        Principal = {
          AWS = aws_cloudfront_origin_access_identity.frontend.iam_arn
        }
        Action   = "s3:GetObject"
        Resource = "${aws_s3_bucket.frontend.arn}/*"
      }
    ]
  })
}

# Data source for ACM certificate (must be in us-east-1 for CloudFront)
data "aws_acm_certificate" "cloudfront" {
  provider    = aws.us-east-1
  domain      = "*.turafapp.com"
  statuses    = ["ISSUED"]
  most_recent = true
}

# CloudFront Distribution
resource "aws_cloudfront_distribution" "frontend" {
  enabled             = true
  is_ipv6_enabled     = true
  http_version        = "http2and3"
  price_class         = "PriceClass_100"
  default_root_object = "index.html"
  
  aliases = [
    var.environment == "prod" ? "app.${var.domain_name}" : "app.${var.environment}.${var.domain_name}"
  ]

  origin {
    domain_name = aws_s3_bucket.frontend.bucket_regional_domain_name
    origin_id   = "S3-${aws_s3_bucket.frontend.id}"

    s3_origin_config {
      origin_access_identity = aws_cloudfront_origin_access_identity.frontend.cloudfront_access_identity_path
    }
  }

  default_cache_behavior {
    allowed_methods        = ["GET", "HEAD", "OPTIONS"]
    cached_methods         = ["GET", "HEAD"]
    target_origin_id       = "S3-${aws_s3_bucket.frontend.id}"
    viewer_protocol_policy = "redirect-to-https"
    compress               = true

    cache_policy_id          = data.aws_cloudfront_cache_policy.caching_optimized.id
    origin_request_policy_id = data.aws_cloudfront_origin_request_policy.cors_s3.id
  }

  # Custom error responses for Angular routing
  custom_error_response {
    error_code         = 403
    response_code      = 200
    response_page_path = "/index.html"
  }

  custom_error_response {
    error_code         = 404
    response_code      = 200
    response_page_path = "/index.html"
  }

  viewer_certificate {
    acm_certificate_arn      = data.aws_acm_certificate.cloudfront.arn
    ssl_support_method       = "sni-only"
    minimum_protocol_version = "TLSv1.2_2021"
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  tags = {
    Name        = "${var.project_name}-frontend-${var.environment}"
    Environment = var.environment
    ManagedBy   = "Terraform"
  }
}

# CloudFront cache policies (data sources)
data "aws_cloudfront_cache_policy" "caching_optimized" {
  name = "Managed-CachingOptimized"
}

data "aws_cloudfront_origin_request_policy" "cors_s3" {
  name = "Managed-CORS-S3Origin"
}
```

---

## Route 53 DNS Configuration

### DNS Records

**DEV Environment**:
```hcl
resource "aws_route53_record" "frontend_dev" {
  zone_id = data.aws_route53_zone.main.zone_id
  name    = "app.dev.${var.domain_name}"
  type    = "A"

  alias {
    name                   = aws_cloudfront_distribution.frontend.domain_name
    zone_id                = aws_cloudfront_distribution.frontend.hosted_zone_id
    evaluate_target_health = false
  }
}
```

**QA Environment**:
```hcl
resource "aws_route53_record" "frontend_qa" {
  zone_id = data.aws_route53_zone.main.zone_id
  name    = "app.qa.${var.domain_name}"
  type    = "A"

  alias {
    name                   = aws_cloudfront_distribution.frontend.domain_name
    zone_id                = aws_cloudfront_distribution.frontend.hosted_zone_id
    evaluate_target_health = false
  }
}
```

**PROD Environment**:
```hcl
resource "aws_route53_record" "frontend_prod" {
  zone_id = data.aws_route53_zone.main.zone_id
  name    = "app.${var.domain_name}"
  type    = "A"

  alias {
    name                   = aws_cloudfront_distribution.frontend.domain_name
    zone_id                = aws_cloudfront_distribution.frontend.hosted_zone_id
    evaluate_target_health = false
  }
}
```

---

## CI/CD Deployment Pipeline

### Build Process

**Angular Build Configuration**:
```json
{
  "configurations": {
    "dev": {
      "fileReplacements": [
        {
          "replace": "src/environments/environment.ts",
          "with": "src/environments/environment.dev.ts"
        }
      ],
      "optimization": true,
      "outputHashing": "all",
      "sourceMap": false,
      "namedChunks": false,
      "aot": true,
      "extractLicenses": true,
      "buildOptimizer": true
    },
    "qa": {
      "fileReplacements": [
        {
          "replace": "src/environments/environment.ts",
          "with": "src/environments/environment.qa.ts"
        }
      ],
      "optimization": true,
      "outputHashing": "all",
      "sourceMap": false,
      "namedChunks": false,
      "aot": true,
      "extractLicenses": true,
      "buildOptimizer": true
    },
    "production": {
      "fileReplacements": [
        {
          "replace": "src/environments/environment.ts",
          "with": "src/environments/environment.prod.ts"
        }
      ],
      "optimization": true,
      "outputHashing": "all",
      "sourceMap": false,
      "namedChunks": false,
      "aot": true,
      "extractLicenses": true,
      "buildOptimizer": true,
      "budgets": [
        {
          "type": "initial",
          "maximumWarning": "2mb",
          "maximumError": "5mb"
        }
      ]
    }
  }
}
```

### Deployment Steps

1. **Build Angular Application**:
   ```bash
   npm ci
   npm run build:${ENVIRONMENT}
   ```

2. **Sync to S3**:
   ```bash
   aws s3 sync dist/turaf-frontend/ s3://turaf-frontend-${ENVIRONMENT}-${ACCOUNT_ID}/ \
     --delete \
     --cache-control "public, max-age=31536000, immutable" \
     --exclude "index.html" \
     --profile turaf-${ENVIRONMENT}
   
   # Upload index.html separately with no-cache
   aws s3 cp dist/turaf-frontend/index.html s3://turaf-frontend-${ENVIRONMENT}-${ACCOUNT_ID}/index.html \
     --cache-control "no-cache, no-store, must-revalidate" \
     --profile turaf-${ENVIRONMENT}
   ```

3. **Invalidate CloudFront Cache**:
   ```bash
   aws cloudfront create-invalidation \
     --distribution-id ${CLOUDFRONT_DISTRIBUTION_ID} \
     --paths "/*" \
     --profile turaf-${ENVIRONMENT}
   ```

4. **Verify Deployment**:
   ```bash
   curl -I https://app.${ENVIRONMENT}.turafapp.com
   ```

---

## Environment Configuration

### Environment Files

**`src/environments/environment.dev.ts`**:
```typescript
export const environment = {
  production: false,
  apiUrl: 'https://api.dev.turafapp.com/api/v1',
  wsUrl: 'wss://ws.dev.turafapp.com',
  environment: 'dev'
};
```

**`src/environments/environment.qa.ts`**:
```typescript
export const environment = {
  production: false,
  apiUrl: 'https://api.qa.turafapp.com/api/v1',
  wsUrl: 'wss://ws.qa.turafapp.com',
  environment: 'qa'
};
```

**`src/environments/environment.prod.ts`**:
```typescript
export const environment = {
  production: true,
  apiUrl: 'https://api.turafapp.com/api/v1',
  wsUrl: 'wss://ws.turafapp.com',
  environment: 'prod'
};
```

---

## Security Considerations

### Content Security Policy (CSP)

Add to `index.html`:
```html
<meta http-equiv="Content-Security-Policy" 
      content="default-src 'self'; 
               script-src 'self' 'unsafe-inline' 'unsafe-eval'; 
               style-src 'self' 'unsafe-inline'; 
               img-src 'self' data: https:; 
               font-src 'self' data:; 
               connect-src 'self' https://api.*.turafapp.com wss://ws.*.turafapp.com;">
```

### Security Headers (CloudFront Functions)

```javascript
function handler(event) {
  var response = event.response;
  var headers = response.headers;

  headers['strict-transport-security'] = { value: 'max-age=63072000; includeSubdomains; preload' };
  headers['x-content-type-options'] = { value: 'nosniff' };
  headers['x-frame-options'] = { value: 'DENY' };
  headers['x-xss-protection'] = { value: '1; mode=block' };
  headers['referrer-policy'] = { value: 'strict-origin-when-cross-origin' };

  return response;
}
```

---

## Monitoring and Logging

### CloudFront Access Logs

```hcl
resource "aws_s3_bucket" "cloudfront_logs" {
  bucket = "${var.project_name}-cloudfront-logs-${var.environment}"
}

resource "aws_cloudfront_distribution" "frontend" {
  # ... other configuration ...

  logging_config {
    include_cookies = false
    bucket          = aws_s3_bucket.cloudfront_logs.bucket_domain_name
    prefix          = "frontend/"
  }
}
```

### CloudWatch Metrics

Monitor:
- **Requests**: Total requests per minute
- **Bytes Downloaded**: Data transfer
- **Error Rate**: 4xx and 5xx errors
- **Cache Hit Ratio**: CloudFront cache effectiveness

---

## Cost Optimization

### Estimated Monthly Costs

| Component | DEV | QA | PROD |
|-----------|-----|-----|------|
| S3 Storage (5GB) | $0.12 | $0.12 | $0.12 |
| S3 Requests | $0.01 | $0.05 | $0.50 |
| CloudFront Data Transfer (10GB) | $0.85 | $4.25 | $42.50 |
| CloudFront Requests (1M) | $0.01 | $0.10 | $1.00 |
| **Total** | **~$1** | **~$5** | **~$44** |

### Optimization Strategies

1. **Enable Compression**: Reduce data transfer costs by 70%
2. **Cache Static Assets**: Reduce S3 requests by 95%
3. **Use Price Class 100**: Limit to US/EU edge locations
4. **Set Proper Cache Headers**: Maximize CloudFront cache hit ratio

---

## Rollback Strategy

### Version Rollback

```bash
# List S3 object versions
aws s3api list-object-versions \
  --bucket turaf-frontend-${ENVIRONMENT}-${ACCOUNT_ID} \
  --prefix index.html

# Restore previous version
aws s3api copy-object \
  --bucket turaf-frontend-${ENVIRONMENT}-${ACCOUNT_ID} \
  --copy-source turaf-frontend-${ENVIRONMENT}-${ACCOUNT_ID}/index.html?versionId=${VERSION_ID} \
  --key index.html

# Invalidate CloudFront
aws cloudfront create-invalidation \
  --distribution-id ${CLOUDFRONT_DISTRIBUTION_ID} \
  --paths "/*"
```

---

## Related Documentation

- [Angular Frontend Specification](angular-frontend.md)
- [AWS Infrastructure](aws-infrastructure.md)
- [Domain DNS Management](domain-dns-management.md)
- [CI/CD Pipelines](ci-cd-pipelines.md)
- [ACM Certificates](../infrastructure/acm-certificates.md)
