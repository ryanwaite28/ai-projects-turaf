# ECR Repositories

**Configured**: 2024-03-23  
**Region**: us-east-1  
**Status**: ✅ Active in all accounts

---

## Overview

Amazon Elastic Container Registry (ECR) repositories configured in all 4 AWS accounts for storing Docker images of microservices with automatic image scanning, encryption, and lifecycle-based cleanup.

---

## Repository Configuration

### Services

| Service | Description | Repositories |
|---------|-------------|--------------|
| identity-service | User authentication and authorization | Ops, Dev, QA, Prod |
| organization-service | Organization and team management | Ops, Dev, QA, Prod |
| experiment-service | Experiment tracking and management | Ops, Dev, QA, Prod |
| metrics-service | Metrics collection and analysis | Ops, Dev, QA, Prod |
| communications-service | Email and notification handling | Ops, Dev, QA, Prod |
| bff-api | Backend-for-Frontend API gateway | Ops, Dev, QA, Prod |
| ws-gateway | WebSocket gateway for real-time | Ops, Dev, QA, Prod |

**Total**: 7 services × 4 accounts = **28 repositories**

---

## Repository URIs

### Ops Account (146072879609)

```
146072879609.dkr.ecr.us-east-1.amazonaws.com/turaf/identity-service
146072879609.dkr.ecr.us-east-1.amazonaws.com/turaf/organization-service
146072879609.dkr.ecr.us-east-1.amazonaws.com/turaf/experiment-service
146072879609.dkr.ecr.us-east-1.amazonaws.com/turaf/metrics-service
146072879609.dkr.ecr.us-east-1.amazonaws.com/turaf/communications-service
146072879609.dkr.ecr.us-east-1.amazonaws.com/turaf/bff-api
146072879609.dkr.ecr.us-east-1.amazonaws.com/turaf/ws-gateway
```

### Dev Account (801651112319)

```
801651112319.dkr.ecr.us-east-1.amazonaws.com/turaf/identity-service
801651112319.dkr.ecr.us-east-1.amazonaws.com/turaf/organization-service
801651112319.dkr.ecr.us-east-1.amazonaws.com/turaf/experiment-service
801651112319.dkr.ecr.us-east-1.amazonaws.com/turaf/metrics-service
801651112319.dkr.ecr.us-east-1.amazonaws.com/turaf/communications-service
801651112319.dkr.ecr.us-east-1.amazonaws.com/turaf/bff-api
801651112319.dkr.ecr.us-east-1.amazonaws.com/turaf/ws-gateway
```

### QA Account (965932217544)

```
965932217544.dkr.ecr.us-east-1.amazonaws.com/turaf/identity-service
965932217544.dkr.ecr.us-east-1.amazonaws.com/turaf/organization-service
965932217544.dkr.ecr.us-east-1.amazonaws.com/turaf/experiment-service
965932217544.dkr.ecr.us-east-1.amazonaws.com/turaf/metrics-service
965932217544.dkr.ecr.us-east-1.amazonaws.com/turaf/communications-service
965932217544.dkr.ecr.us-east-1.amazonaws.com/turaf/bff-api
965932217544.dkr.ecr.us-east-1.amazonaws.com/turaf/ws-gateway
```

### Prod Account (811783768245)

```
811783768245.dkr.ecr.us-east-1.amazonaws.com/turaf/identity-service
811783768245.dkr.ecr.us-east-1.amazonaws.com/turaf/organization-service
811783768245.dkr.ecr.us-east-1.amazonaws.com/turaf/experiment-service
811783768245.dkr.ecr.us-east-1.amazonaws.com/turaf/metrics-service
811783768245.dkr.ecr.us-east-1.amazonaws.com/turaf/communications-service
811783768245.dkr.ecr.us-east-1.amazonaws.com/turaf/bff-api
811783768245.dkr.ecr.us-east-1.amazonaws.com/turaf/ws-gateway
```

---

## Features

### Image Scanning
- ✅ **Scan on push enabled** - Automatic vulnerability scanning when images are pushed
- Scans for CVEs (Common Vulnerabilities and Exposures)
- Integration with AWS Security Hub
- Scan results available via AWS Console and CLI

### Encryption
- ✅ **AES256 encryption** - All images encrypted at rest
- Server-side encryption using AWS-managed keys
- Automatic encryption for all pushed images

### Lifecycle Policies
- ✅ **Automatic cleanup** - Removes old images based on rules
- ✅ **Tag-based retention** - Different retention for prod/qa/dev
- ✅ **Untagged image cleanup** - Removes untagged images after 7 days

---

## Lifecycle Policy Rules

### Rule 1: Production Images
- **Priority**: 1
- **Tag prefix**: `prod-`, `v`
- **Retention**: Keep last 10 images
- **Action**: Expire older images

### Rule 2: Staging/QA Images
- **Priority**: 2
- **Tag prefix**: `qa-`, `staging-`
- **Retention**: Keep last 5 images
- **Action**: Expire older images

### Rule 3: Development Images
- **Priority**: 3
- **Tag prefix**: `dev-`
- **Retention**: Keep last 3 images
- **Action**: Expire older images

### Rule 4: Untagged Images
- **Priority**: 4
- **Tag status**: Untagged
- **Retention**: 7 days since push
- **Action**: Expire after 7 days

---

## Usage

### Authenticate Docker to ECR

```bash
# Dev account
aws ecr get-login-password --region us-east-1 --profile turaf-dev | \
docker login --username AWS --password-stdin \
  801651112319.dkr.ecr.us-east-1.amazonaws.com

# QA account
aws ecr get-login-password --region us-east-1 --profile turaf-qa | \
docker login --username AWS --password-stdin \
  965932217544.dkr.ecr.us-east-1.amazonaws.com

# Prod account
aws ecr get-login-password --region us-east-1 --profile turaf-prod | \
docker login --username AWS --password-stdin \
  811783768245.dkr.ecr.us-east-1.amazonaws.com
```

### Build and Push Image

```bash
# Build Docker image
docker build -t turaf/identity-service:v1.0.0 .

# Tag for ECR (dev example)
docker tag turaf/identity-service:v1.0.0 \
  801651112319.dkr.ecr.us-east-1.amazonaws.com/turaf/identity-service:dev-v1.0.0

# Push to ECR
docker push \
  801651112319.dkr.ecr.us-east-1.amazonaws.com/turaf/identity-service:dev-v1.0.0
```

### Pull Image

```bash
# Pull from ECR
docker pull \
  801651112319.dkr.ecr.us-east-1.amazonaws.com/turaf/identity-service:dev-v1.0.0
```

### List Images in Repository

```bash
# List all images
aws ecr list-images \
  --repository-name turaf/identity-service \
  --region us-east-1 \
  --profile turaf-dev

# List images with details
aws ecr describe-images \
  --repository-name turaf/identity-service \
  --region us-east-1 \
  --profile turaf-dev
```

---

## GitHub Actions Integration

### Workflow Example

```yaml
name: Build and Push to ECR

on:
  push:
    branches: [main, develop]

permissions:
  id-token: write
  contents: read

jobs:
  build-and-push:
    runs-on: ubuntu-latest
    environment: dev
    steps:
      - name: Checkout code
        uses: actions/checkout@v4
      
      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: ${{ secrets.AWS_ROLE_ARN }}
          aws-region: us-east-1
      
      - name: Login to Amazon ECR
        id: login-ecr
        uses: aws-actions/amazon-ecr-login@v2
      
      - name: Build, tag, and push image
        env:
          ECR_REGISTRY: ${{ steps.login-ecr.outputs.registry }}
          ECR_REPOSITORY: turaf/identity-service
          IMAGE_TAG: dev-${{ github.sha }}
        run: |
          docker build -t $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG .
          docker push $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG
          echo "image=$ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG" >> $GITHUB_OUTPUT
```

---

## Image Tagging Strategy

### Tag Format

```
<environment>-<version>
```

### Examples

**Development**:
- `dev-v1.0.0`
- `dev-abc123` (git commit SHA)
- `dev-latest`

**QA/Staging**:
- `qa-v1.0.0`
- `staging-v1.0.0`

**Production**:
- `prod-v1.0.0`
- `v1.0.0` (semantic version)
- `prod-latest`

---

## Monitoring

### View Scan Results

```bash
# Get scan findings for an image
aws ecr describe-image-scan-findings \
  --repository-name turaf/identity-service \
  --image-id imageTag=dev-v1.0.0 \
  --region us-east-1 \
  --profile turaf-dev
```

### Check Repository Metrics

```bash
# Get repository details
aws ecr describe-repositories \
  --repository-names turaf/identity-service \
  --region us-east-1 \
  --profile turaf-dev

# Count images in repository
aws ecr list-images \
  --repository-name turaf/identity-service \
  --region us-east-1 \
  --profile turaf-dev \
  --query 'length(imageIds)'
```

### CloudWatch Metrics

Available ECR metrics:
- `RepositoryPullCount` - Number of image pulls
- `RepositoryPushCount` - Number of image pushes

---

## Security Best Practices

### 1. Image Scanning
- ✅ Enabled on all repositories
- Review scan findings regularly
- Address critical and high vulnerabilities
- Block deployment of vulnerable images

### 2. Access Control
- Use IAM roles for ECR access (GitHub Actions OIDC)
- Limit cross-account access
- Enable CloudTrail logging for ECR operations
- Monitor unauthorized access attempts

### 3. Image Signing (Future)
- Consider implementing Docker Content Trust
- Use AWS Signer for image signing
- Verify image signatures before deployment

### 4. Least Privilege
- Grant minimum required ECR permissions
- Use repository-specific policies
- Separate read and write permissions

---

## Cost Optimization

### ECR Pricing (us-east-1)

**Storage**: $0.10 per GB/month
- First 500 MB: Free (per account)

**Data Transfer**:
- To ECS/Lambda in same region: Free
- To internet: Standard AWS data transfer rates

### Estimated Costs

**Without Lifecycle Policies**:
- 28 repositories × 10 images × 500 MB = 140 GB
- Cost: ~$14/month

**With Lifecycle Policies**:
- 28 repositories × 3 avg images × 500 MB = 42 GB
- Cost: ~$4/month
- **Savings**: ~$10/month (70% reduction)

### Lifecycle Policy Benefits

- Automatically removes old images
- Reduces storage by 60-80%
- Keeps only necessary images
- No manual cleanup required

---

## Troubleshooting

### Issue: Authentication Failed

**Error**: `no basic auth credentials`

**Solution**:
```bash
# Re-authenticate to ECR
aws ecr get-login-password --region us-east-1 --profile turaf-dev | \
docker login --username AWS --password-stdin \
  801651112319.dkr.ecr.us-east-1.amazonaws.com

# Verify Docker is running
docker info
```

### Issue: Image Push Denied

**Error**: `denied: User is not authorized`

**Solution**:
- Verify AWS credentials are correct
- Check IAM permissions include `ecr:PutImage`
- Ensure repository exists
- Verify repository name is correct

### Issue: Scan Findings

**Error**: Vulnerabilities detected in image

**Solution**:
```bash
# View scan findings
aws ecr describe-image-scan-findings \
  --repository-name turaf/identity-service \
  --image-id imageTag=dev-v1.0.0 \
  --region us-east-1

# Update base images
# Rebuild with patched dependencies
# Re-scan updated image
```

### Issue: Repository Not Found

**Error**: `RepositoryNotFoundException`

**Solution**:
```bash
# List all repositories
aws ecr describe-repositories --region us-east-1

# Verify repository name (case-sensitive)
# Check correct AWS account/profile
```

---

## Maintenance

### Regular Tasks

**Weekly**:
- Review image scan findings
- Check for critical vulnerabilities
- Verify lifecycle policies are working

**Monthly**:
- Review storage costs
- Audit repository access logs
- Clean up unused repositories

**Quarterly**:
- Update lifecycle policies if needed
- Review and update IAM permissions
- Test disaster recovery procedures

---

## References

- [Amazon ECR Documentation](https://docs.aws.amazon.com/ecr/)
- [ECR Lifecycle Policies](https://docs.aws.amazon.com/AmazonECR/latest/userguide/LifecyclePolicies.html)
- [ECR Image Scanning](https://docs.aws.amazon.com/AmazonECR/latest/userguide/image-scanning.html)
- [Docker Best Practices](https://docs.docker.com/develop/dev-best-practices/)
- specs/ci-cd-pipelines.md
- INFRASTRUCTURE_PLAN.md (Phase 2.4)
