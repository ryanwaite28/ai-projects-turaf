# Storage Module

Terraform module for creating S3 buckets for the Turaf platform with cost-optimized configuration for demo/portfolio purposes.

## Features

- **Single Multi-Purpose Bucket**: Cost-optimized approach using prefixes instead of separate buckets
- **Lifecycle Policies**: Automatic cleanup of old logs and backups
- **Encryption**: AES-256 server-side encryption (no KMS costs)
- **Public Access Block**: All buckets blocked from public access by default
- **Optional Separate Buckets**: Can enable separate buckets for production
- **CORS Support**: Optional CORS configuration for static assets
- **CloudFront Integration**: Optional bucket policy for CloudFront access

## Architecture

### Demo Configuration (Cost-Optimized)

```
Single S3 Bucket: turaf-{env}-{account_id}
├── /logs/           → Application and access logs (7-day retention)
├── /backups/        → Database and application backups (7-day retention)
├── /reports/        → Generated reports
├── /static/         → Static assets for CloudFront
├── /app-data/       → Application data
├── /uploads/        → User uploads
└── /temp/           → Temporary files
```

**Benefits**:
- Single bucket = lower costs
- Prefix-based organization
- Lifecycle policies per prefix
- ~$2/month for minimal usage

### Production Configuration (Optional)

```
Separate Buckets:
├── turaf-{env}-{account_id}        → Primary application data
├── turaf-reports-{env}-{account_id} → Reports (separate lifecycle)
└── turaf-static-{env}-{account_id}  → Static assets (CloudFront origin)
```

## Usage

### Minimal Demo Configuration (Recommended)

```hcl
module "storage" {
  source = "../../modules/storage"

  environment = "dev"

  # Cost optimization - single bucket
  enable_separate_buckets = false
  enable_versioning       = false
  
  # Short retention for demo
  log_retention_days    = 7
  backup_retention_days = 7

  # Disable optional features
  enable_intelligent_tiering = false
  enable_cors               = false
  enable_cloudfront_access  = false
  enable_access_logging     = false

  tags = {
    Project     = "turaf"
    Environment = "dev"
    ManagedBy   = "terraform"
  }
}
```

### Production Configuration

```hcl
module "storage" {
  source = "../../modules/storage"

  environment = "prod"

  # Separate buckets for better organization
  enable_separate_buckets = true
  enable_versioning       = true
  
  # Longer retention for production
  log_retention_days    = 90
  backup_retention_days = 30

  # Enable production features
  enable_intelligent_tiering = true
  enable_cors               = true
  enable_cloudfront_access  = true
  enable_access_logging     = true

  cors_allowed_origins = [
    "https://app.turafapp.com"
  ]

  cloudfront_distribution_arn = "arn:aws:cloudfront::123456789012:distribution/ABCDEFG"

  tags = {
    Project     = "turaf"
    Environment = "prod"
    ManagedBy   = "terraform"
  }
}
```

## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|----------|
| environment | Environment name (dev, qa, prod) | string | - | yes |
| enable_versioning | Enable S3 versioning | bool | false | no |
| enable_separate_buckets | Create separate buckets | bool | false | no |
| log_retention_days | Log retention in days | number | 7 | no |
| backup_retention_days | Backup retention in days | number | 7 | no |
| enable_intelligent_tiering | Enable Intelligent-Tiering | bool | false | no |
| enable_cors | Enable CORS configuration | bool | false | no |
| cors_allowed_origins | Allowed CORS origins | list(string) | [...] | no |
| enable_cloudfront_access | Enable CloudFront access | bool | false | no |
| cloudfront_distribution_arn | CloudFront distribution ARN | string | "" | no |
| enable_access_logging | Enable S3 access logging | bool | false | no |
| tags | Additional tags | map(string) | {} | no |

## Outputs

| Name | Description |
|------|-------------|
| primary_bucket_id | Primary S3 bucket ID |
| primary_bucket_arn | Primary S3 bucket ARN |
| primary_bucket_domain_name | Primary bucket domain name |
| reports_bucket_id | Reports bucket ID (if enabled) |
| static_bucket_id | Static assets bucket ID (if enabled) |
| bucket_prefixes | Recommended S3 prefixes |

## Cost Estimation

### Demo Configuration
- **Storage**: ~$0.50/month (assuming 20 GB)
- **Requests**: ~$0.10/month (minimal usage)
- **Data Transfer**: Free (first 100 GB out)
- **Total**: ~$2/month

### Production Configuration
- **Storage**: ~$5/month (assuming 200 GB)
- **Requests**: ~$1/month
- **Data Transfer**: ~$4/month (beyond 100 GB)
- **Intelligent-Tiering**: ~$0.50/month (monitoring fee)
- **Total**: ~$10/month

## Bucket Organization

### Recommended Prefix Structure

```
/logs/
  /application/
    /identity-service/
    /organization-service/
    /experiment-service/
  /access/
  /cloudtrail/

/backups/
  /database/
    /daily/
    /weekly/
  /application/

/reports/
  /experiments/
  /metrics/
  /analytics/

/static/
  /css/
  /js/
  /images/
  /fonts/

/app-data/
  /experiments/
  /organizations/

/uploads/
  /avatars/
  /documents/
  /exports/

/temp/
  /processing/
  /cache/
```

## Lifecycle Policies

### Logs Prefix
- **Expiration**: 7 days (demo) or 90 days (production)
- **Purpose**: Automatic cleanup of old logs

### Backups Prefix
- **Expiration**: 7 days (demo) or 30 days (production)
- **Purpose**: Automatic cleanup of old backups

### Reports Prefix (with Intelligent-Tiering)
- **Transition**: 30 days to Intelligent-Tiering
- **Purpose**: Cost optimization for infrequently accessed reports

### Multipart Uploads
- **Abort**: 7 days after initiation
- **Purpose**: Cleanup incomplete uploads

## Security

### Encryption
- **Server-Side**: AES-256 (SSE-S3)
- **In-Transit**: HTTPS enforced via bucket policy
- **Bucket Key**: Enabled for reduced KMS costs

### Access Control
- **Public Access**: Blocked by default
- **IAM Policies**: Least privilege access
- **Bucket Policies**: CloudFront access only (if enabled)
- **CORS**: Restricted to specified origins

### Best Practices
- ✅ All buckets encrypted at rest
- ✅ Public access blocked
- ✅ Versioning disabled for demo (enable for production)
- ✅ Lifecycle policies for cost control
- ✅ Access logging optional (additional cost)

## Usage Examples

### Upload File to S3

```typescript
import { S3Client, PutObjectCommand } from '@aws-sdk/client-s3';

const s3Client = new S3Client({ region: 'us-east-1' });

async function uploadReport(reportData: Buffer, reportId: string) {
  const command = new PutObjectCommand({
    Bucket: process.env.S3_BUCKET_NAME,
    Key: `reports/experiments/${reportId}.pdf`,
    Body: reportData,
    ContentType: 'application/pdf',
    ServerSideEncryption: 'AES256'
  });

  await s3Client.send(command);
}
```

### Generate Presigned URL

```typescript
import { S3Client, GetObjectCommand } from '@aws-sdk/client-s3';
import { getSignedUrl } from '@aws-sdk/s3-request-presigner';

async function getReportUrl(reportId: string): Promise<string> {
  const command = new GetObjectCommand({
    Bucket: process.env.S3_BUCKET_NAME,
    Key: `reports/experiments/${reportId}.pdf`
  });

  return await getSignedUrl(s3Client, command, { expiresIn: 3600 });
}
```

### List Objects with Prefix

```typescript
import { S3Client, ListObjectsV2Command } from '@aws-sdk/client-s3';

async function listBackups() {
  const command = new ListObjectsV2Command({
    Bucket: process.env.S3_BUCKET_NAME,
    Prefix: 'backups/database/',
    MaxKeys: 100
  });

  const response = await s3Client.send(command);
  return response.Contents || [];
}
```

## Monitoring

### CloudWatch Metrics
- **BucketSizeBytes**: Total bucket size
- **NumberOfObjects**: Object count
- **AllRequests**: Total request count
- **4xxErrors**: Client errors
- **5xxErrors**: Server errors

### Cost Monitoring
- **Storage**: Monitor GB-month usage
- **Requests**: Track PUT/GET request counts
- **Data Transfer**: Monitor outbound transfer

## Troubleshooting

### Issue: Access Denied

**Problem**: Cannot access S3 objects

**Solutions**:
1. Verify IAM role has s3:GetObject permission
2. Check bucket policy doesn't deny access
3. Ensure object exists at specified key
4. Verify encryption settings match

### Issue: High Costs

**Problem**: S3 costs higher than expected

**Solutions**:
1. Review lifecycle policies are working
2. Check for incomplete multipart uploads
3. Verify versioning is disabled for demo
4. Monitor request patterns
5. Enable Intelligent-Tiering for large datasets

### Issue: CORS Errors

**Problem**: Browser blocks S3 requests

**Solutions**:
1. Verify CORS is enabled
2. Check allowed origins include your domain
3. Ensure allowed methods include GET
4. Verify preflight requests succeed

## Migration Guide

### From Single Bucket to Separate Buckets

1. Set `enable_separate_buckets = true`
2. Run `terraform plan` to review changes
3. Run `terraform apply`
4. Copy data to new buckets:

```bash
# Copy reports
aws s3 sync s3://turaf-dev-123/reports/ s3://turaf-reports-dev-123/

# Copy static assets
aws s3 sync s3://turaf-dev-123/static/ s3://turaf-static-dev-123/
```

5. Update application configuration
6. Verify new buckets work
7. Clean up old prefixes

## References

- [S3 Pricing](https://aws.amazon.com/s3/pricing/)
- [S3 Best Practices](https://docs.aws.amazon.com/AmazonS3/latest/userguide/best-practices.html)
- [S3 Lifecycle Configuration](https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-lifecycle-mgmt.html)
- [S3 Intelligent-Tiering](https://aws.amazon.com/s3/storage-classes/intelligent-tiering/)
