# Architecture Test Reports - Terraform Infrastructure

**Created**: 2026-03-31  
**Purpose**: S3 bucket and CloudFront distribution for hosting architecture test reports

---

## Overview

This Terraform configuration creates the infrastructure needed to store and serve architecture test reports:

- **S3 Bucket**: Stores test report HTML files and assets
- **CloudFront Distribution**: CDN for fast global access to reports
- **Route53 Record**: DNS alias pointing to CloudFront
- **Security**: Encryption at rest, private bucket with CloudFront OAI access

---

## Prerequisites

1. **Terraform 1.5+** installed
2. **AWS CLI** configured with appropriate profiles:
   - `turaf-dev` for DEV account (801651112319)
   - `turaf-qa` for QA account (965932217544)
   - `turaf-prod` for PROD account (811783768245)
3. **ACM Certificates** created in each account for `*.turafapp.com` or `reports.{env}.turafapp.com`
4. **Route53 Hosted Zone** for `turafapp.com`
5. **S3 Backend Bucket** `turaf-terraform-state` must exist

---

## Configuration

### Environment Variables

Each environment has its own `.tfvars` file:

- `dev.tfvars` - DEV environment
- `qa.tfvars` - QA environment
- `prod.tfvars` - PROD environment

**Important**: Update the `acm_certificate_arn` in each `.tfvars` file with the actual certificate ARN from AWS.

### Getting ACM Certificate ARN

```bash
# DEV
aws acm list-certificates --profile turaf-dev \
  --query "CertificateSummaryList[?DomainName=='*.turafapp.com'].CertificateArn" \
  --output text

# QA
aws acm list-certificates --profile turaf-qa \
  --query "CertificateSummaryList[?DomainName=='*.turafapp.com'].CertificateArn" \
  --output text

# PROD
aws acm list-certificates --profile turaf-prod \
  --query "CertificateSummaryList[?DomainName=='*.turafapp.com'].CertificateArn" \
  --output text
```

---

## Deployment

### Deploy to DEV

```bash
# Set AWS profile
export AWS_PROFILE=turaf-dev

# Initialize Terraform
terraform init

# Review plan
terraform plan -var-file=dev.tfvars

# Apply changes
terraform apply -var-file=dev.tfvars
```

### Deploy to QA

```bash
export AWS_PROFILE=turaf-qa
terraform init
terraform plan -var-file=qa.tfvars
terraform apply -var-file=qa.tfvars
```

### Deploy to PROD

```bash
export AWS_PROFILE=turaf-prod
terraform init
terraform plan -var-file=prod.tfvars
terraform apply -var-file=prod.tfvars
```

---

## Resources Created

### S3 Bucket
- **Name**: `turaf-architecture-test-reports-{environment}`
- **Versioning**: Enabled
- **Encryption**: AES256 (SSE-S3)
- **Lifecycle**: Delete reports after retention period (90 days for dev/qa, 180 days for prod)
- **Public Access**: Blocked (CloudFront OAI only)

### CloudFront Distribution
- **Domain**: `reports.{environment}.turafapp.com`
- **Origin**: S3 bucket via Origin Access Identity
- **HTTPS**: Required (redirect HTTP to HTTPS)
- **Caching**: 1 hour default TTL
- **Compression**: Enabled

### Route53 Record
- **Type**: A (Alias)
- **Name**: `reports.{environment}.turafapp.com`
- **Target**: CloudFront distribution

---

## Outputs

After successful deployment, Terraform outputs:

```hcl
s3_bucket_name           = "turaf-architecture-test-reports-dev"
s3_bucket_arn            = "arn:aws:s3:::turaf-architecture-test-reports-dev"
cloudfront_distribution_id = "E1234567890ABC"
cloudfront_domain_name   = "d111111abcdef8.cloudfront.net"
reports_url              = "https://reports.dev.turafapp.com"
```

---

## Verification

### Verify S3 Bucket

```bash
aws s3 ls s3://turaf-architecture-test-reports-dev/ --profile turaf-dev
```

### Verify CloudFront Distribution

```bash
aws cloudfront list-distributions --profile turaf-dev \
  --query "DistributionList.Items[?Aliases.Items[?contains(@, 'reports.dev.turafapp.com')]]"
```

### Test HTTPS Access

```bash
curl -I https://reports.dev.turafapp.com
```

### Upload Test Report

```bash
echo "<html><body><h1>Test Report</h1></body></html>" > test.html

aws s3 cp test.html \
  s3://turaf-architecture-test-reports-dev/test.html \
  --profile turaf-dev

# Wait for CloudFront propagation (or invalidate cache)
aws cloudfront create-invalidation \
  --distribution-id E1234567890ABC \
  --paths "/*" \
  --profile turaf-dev

# Access via CloudFront
curl https://reports.dev.turafapp.com/test.html
```

---

## Updating Infrastructure

To update the infrastructure:

```bash
# Make changes to .tf files
# Review changes
terraform plan -var-file=dev.tfvars

# Apply changes
terraform apply -var-file=dev.tfvars
```

---

## Destroying Infrastructure

**Warning**: This will delete all test reports!

```bash
# DEV
terraform destroy -var-file=dev.tfvars

# QA
terraform destroy -var-file=qa.tfvars

# PROD
terraform destroy -var-file=prod.tfvars
```

---

## Troubleshooting

### Issue: "Error creating S3 bucket: BucketAlreadyExists"

**Cause**: Bucket name already exists globally

**Solution**: S3 bucket names must be globally unique. If the bucket exists in another account, choose a different name.

### Issue: "Error creating CloudFront distribution: InvalidViewerCertificate"

**Cause**: ACM certificate ARN is invalid or doesn't exist

**Solution**: 
1. Verify certificate exists in the account
2. Ensure certificate is in `us-east-1` region (required for CloudFront)
3. Update `acm_certificate_arn` in `.tfvars` file

### Issue: "Error creating Route53 record: NoSuchHostedZone"

**Cause**: Route53 hosted zone for `turafapp.com` doesn't exist

**Solution**: Create the hosted zone first or update `route53.tf` to use the correct zone name.

### Issue: CloudFront distribution takes too long

**Expected**: CloudFront distributions take 15-20 minutes to deploy. This is normal AWS behavior.

---

## Security Considerations

1. **Private Bucket**: S3 bucket blocks all public access
2. **CloudFront OAI**: Only CloudFront can access S3 objects
3. **HTTPS Only**: HTTP requests are redirected to HTTPS
4. **Encryption**: All objects encrypted at rest with AES256
5. **Lifecycle Policies**: Old reports automatically deleted
6. **Versioning**: Enabled for accidental deletion protection

---

## Cost Estimation

**Monthly costs (approximate)**:

- **S3 Storage**: $0.023/GB (~$2-5/month for typical usage)
- **CloudFront**: $0.085/GB for first 10TB + $0.01 per 10,000 requests (~$5-20/month)
- **Route53**: $0.50/month per hosted zone (shared across all records)

**Total**: ~$10-30/month per environment

---

## Related Documentation

- [Task 005: Create Terraform Infrastructure](../../../tasks/architecture-tests/005-create-terraform-infrastructure.md)
- [Architecture Testing Specification](../../../specs/architecture-testing.md)
- [IAM Permissions](../../../infrastructure/github-oidc-roles.md)

---

## Maintenance

### Updating Retention Policy

Edit the `report_retention_days` variable in the appropriate `.tfvars` file and apply:

```bash
terraform apply -var-file=dev.tfvars
```

### Invalidating CloudFront Cache

```bash
aws cloudfront create-invalidation \
  --distribution-id $(terraform output -raw cloudfront_distribution_id) \
  --paths "/*" \
  --profile turaf-dev
```

### Viewing Terraform State

```bash
terraform show
```

### Listing Resources

```bash
terraform state list
```
