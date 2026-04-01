# GitHub Actions OIDC Roles

**Configured**: 2024-03-23  
**Status**: ✅ Active in all accounts

---

## Overview

GitHub Actions uses OpenID Connect (OIDC) to authenticate with AWS without storing long-lived credentials. Each AWS account has an OIDC identity provider and an IAM role that GitHub Actions can assume.

---

## OIDC Providers

| Account | OIDC Provider ARN | Status |
|---------|-------------------|--------|
| Ops (146072879609) | arn:aws:iam::146072879609:oidc-provider/token.actions.githubusercontent.com | ✅ Verified |
| Dev (801651112319) | arn:aws:iam::801651112319:oidc-provider/token.actions.githubusercontent.com | ✅ Configured |
| QA (965932217544) | arn:aws:iam::965932217544:oidc-provider/token.actions.githubusercontent.com | ✅ Configured |
| Prod (811783768245) | arn:aws:iam::811783768245:oidc-provider/token.actions.githubusercontent.com | ✅ Configured |

**OIDC Configuration**:
- **Provider URL**: `https://token.actions.githubusercontent.com`
- **Audience**: `sts.amazonaws.com`
- **Thumbprint**: `6938fd4d98bab03faadb97b34396831e3780aea1`

---

## IAM Role ARNs

| Account | Role ARN | Environment |
|---------|----------|-------------|
| Ops | arn:aws:iam::146072879609:role/GitHubActionsDeploymentRole | ops |
| Dev | arn:aws:iam::801651112319:role/GitHubActionsDeploymentRole | dev |
| QA | arn:aws:iam::965932217544:role/GitHubActionsDeploymentRole | qa |
| Prod | arn:aws:iam::811783768245:role/GitHubActionsDeploymentRole | prod |

---

## Trust Policy

All roles use the following trust policy to allow GitHub Actions from the `ryanwaite28/ai-projects-turaf` repository:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::{ACCOUNT_ID}:oidc-provider/token.actions.githubusercontent.com"
      },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringEquals": {
          "token.actions.githubusercontent.com:aud": "sts.amazonaws.com"
        },
        "StringLike": {
          "token.actions.githubusercontent.com:sub": "repo:ryanwaite28/ai-projects-turaf:*"
        }
      }
    }
  ]
}
```

**Security Features**:
- ✅ Repository-scoped: Only `ryanwaite28/ai-projects-turaf` can assume roles
- ✅ Wildcard branch/tag: Supports all branches and tags (`*`)
- ✅ Audience validation: Ensures tokens are for AWS STS

---

## Permissions

Each role has the following permissions:

### ECR (Elastic Container Registry)
- `ecr:GetAuthorizationToken` - Authenticate to ECR
- `ecr:BatchCheckLayerAvailability` - Check image layers
- `ecr:GetDownloadUrlForLayer` - Download image layers
- `ecr:BatchGetImage` - Pull images
- `ecr:PutImage` - Push images
- `ecr:InitiateLayerUpload` - Start layer upload
- `ecr:UploadLayerPart` - Upload layer parts
- `ecr:CompleteLayerUpload` - Complete layer upload

### ECS (Elastic Container Service)
- `ecs:UpdateService` - Update ECS services
- `ecs:DescribeServices` - Get service details
- `ecs:DescribeTaskDefinition` - Get task definition details
- `ecs:RegisterTaskDefinition` - Register new task definitions
- `ecs:ListTasks` - List running tasks
- `ecs:DescribeTasks` - Get task details

### IAM (PassRole)
- `iam:PassRole` - Pass roles to ECS tasks
  - `arn:aws:iam::{ACCOUNT_ID}:role/ecsTaskExecutionRole`
  - `arn:aws:iam::{ACCOUNT_ID}:role/ecsTaskRole`

### S3 (Artifacts)
- `s3:PutObject` - Upload artifacts
- `s3:GetObject` - Download artifacts
- `s3:ListBucket` - List bucket contents
  - Resources: `turaf-artifacts-*` buckets

### CloudWatch Logs
- `logs:CreateLogGroup` - Create log groups
- `logs:CreateLogStream` - Create log streams
- `logs:PutLogEvents` - Write log events
  - Resources: `/aws/ecs/*` log groups

### S3 (Architecture Test Reports)
- `s3:PutObject` - Upload test reports
- `s3:PutObjectAcl` - Set object ACLs
- `s3:GetObject` - Download reports
- `s3:ListBucket` - List bucket contents
  - Resources: `turaf-architecture-test-reports-*` buckets

### CloudFront (Cache Invalidation)
- `cloudfront:CreateInvalidation` - Invalidate cache
- `cloudfront:GetInvalidation` - Check invalidation status
- `cloudfront:ListInvalidations` - List invalidations
  - Resources: All CloudFront distributions

---

## Usage in GitHub Actions

### Basic Workflow Configuration

```yaml
name: Deploy to AWS

on:
  push:
    branches: [main, develop]

permissions:
  id-token: write  # Required for OIDC
  contents: read

jobs:
  deploy-dev:
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
      
      - name: Build and push Docker image
        env:
          ECR_REGISTRY: ${{ steps.login-ecr.outputs.registry }}
          ECR_REPOSITORY: turaf-api
          IMAGE_TAG: ${{ github.sha }}
        run: |
          docker build -t $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG .
          docker push $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG
      
      - name: Deploy to ECS
        run: |
          aws ecs update-service \
            --cluster turaf-dev-cluster \
            --service turaf-api-service \
            --force-new-deployment
```

### Environment-Specific Deployments

```yaml
jobs:
  deploy-dev:
    environment: dev
    steps:
      - uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: arn:aws:iam::801651112319:role/GitHubActionsDeploymentRole
          aws-region: us-east-1

  deploy-qa:
    environment: qa
    needs: deploy-dev
    steps:
      - uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: arn:aws:iam::965932217544:role/GitHubActionsDeploymentRole
          aws-region: us-east-1

  deploy-prod:
    environment: prod
    needs: deploy-qa
    steps:
      - uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: arn:aws:iam::811783768245:role/GitHubActionsDeploymentRole
          aws-region: us-east-1
```

---

## GitHub Secrets Configuration

The following secrets should be configured in GitHub repository settings:

### Repository Secrets
- `AWS_REGION`: `us-east-1`

### Environment Secrets

**Dev Environment**:
- `AWS_ROLE_ARN`: `arn:aws:iam::801651112319:role/GitHubActionsDeploymentRole`
- `AWS_ACCOUNT_ID`: `801651112319`

**QA Environment**:
- `AWS_ROLE_ARN`: `arn:aws:iam::965932217544:role/GitHubActionsDeploymentRole`
- `AWS_ACCOUNT_ID`: `965932217544`

**Prod Environment**:
- `AWS_ROLE_ARN`: `arn:aws:iam::811783768245:role/GitHubActionsDeploymentRole`
- `AWS_ACCOUNT_ID`: `811783768245`

**Ops Environment**:
- `AWS_ROLE_ARN`: `arn:aws:iam::146072879609:role/GitHubActionsDeploymentRole`
- `AWS_ACCOUNT_ID`: `146072879609`

---

## Verification

### Test OIDC Authentication

Create a test workflow to verify OIDC authentication:

```yaml
name: Test OIDC Authentication

on:
  workflow_dispatch:

permissions:
  id-token: write
  contents: read

jobs:
  test-dev:
    runs-on: ubuntu-latest
    steps:
      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: arn:aws:iam::801651112319:role/GitHubActionsDeploymentRole
          aws-region: us-east-1
      
      - name: Test AWS access
        run: |
          echo "Testing AWS access..."
          aws sts get-caller-identity
          aws ecr describe-repositories || echo "No ECR repos yet"
          echo "✅ OIDC authentication successful!"
```

### Manual Verification Commands

```bash
# Verify OIDC provider exists (requires admin access)
aws iam list-open-id-connect-providers --profile turaf-dev

# Verify role exists (requires admin access)
aws iam get-role --role-name GitHubActionsDeploymentRole --profile turaf-dev

# Get role ARN
aws iam get-role \
  --role-name GitHubActionsDeploymentRole \
  --profile turaf-dev \
  --query 'Role.Arn' \
  --output text
```

---

## Security Best Practices

### 1. Least Privilege
- Roles have only the permissions needed for deployment
- No administrative or destructive permissions
- Scoped to specific resources where possible

### 2. Repository Scoping
- Trust policy limits access to specific GitHub repository
- Cannot be assumed by other repositories or organizations

### 3. No Long-Lived Credentials
- No AWS access keys or secret keys stored in GitHub
- Temporary credentials issued by AWS STS
- Credentials expire automatically after use

### 4. Audit Trail
- All role assumptions logged in CloudTrail
- GitHub Actions logs show authentication details
- Can track which workflow/run assumed which role

### 5. Environment Protection
- Use GitHub environment protection rules
- Require approvals for production deployments
- Limit who can trigger deployments

---

## Troubleshooting

### Issue: "Error: Not authorized to perform sts:AssumeRoleWithWebIdentity"

**Causes**:
- Trust policy doesn't match repository name
- OIDC provider not configured correctly
- Workflow missing `id-token: write` permission

**Solution**:
```yaml
# Ensure workflow has correct permissions
permissions:
  id-token: write
  contents: read

# Verify repository name in trust policy matches exactly
# repo:ryanwaite28/ai-projects-turaf:*
```

### Issue: "Error: User is not authorized to perform: ecr:PutImage"

**Cause**: Role permissions policy missing ECR permissions

**Solution**: Update role permissions policy to include ECR actions

### Issue: "Error: No basic auth credentials"

**Cause**: Not logged into ECR before pushing images

**Solution**:
```yaml
- name: Login to Amazon ECR
  uses: aws-actions/amazon-ecr-login@v2
```

---

## Maintenance

### Updating Permissions

To add new permissions to a role:

1. Log into AWS Console for the target account
2. Navigate to IAM > Roles > GitHubActionsDeploymentRole
3. Edit the inline policy `GitHubActionsDeploymentPolicy`
4. Add new permissions
5. Save changes

### Rotating OIDC Thumbprint

If GitHub updates their OIDC certificate:

```bash
# Get new thumbprint
echo | openssl s_client -servername token.actions.githubusercontent.com \
  -connect token.actions.githubusercontent.com:443 2>/dev/null | \
  openssl x509 -fingerprint -noout | \
  sed 's/://g' | \
  awk -F= '{print tolower($2)}'

# Update OIDC provider with new thumbprint (requires admin access)
aws iam update-open-id-connect-provider-thumbprint \
  --open-id-connect-provider-arn <PROVIDER_ARN> \
  --thumbprint-list <NEW_THUMBPRINT>
```

### Revoking Access

To revoke GitHub Actions access:

1. Delete the IAM role: `GitHubActionsDeploymentRole`
2. Delete the OIDC provider: `token.actions.githubusercontent.com`

---

## Cost

**OIDC Authentication**: Free
- No cost for OIDC providers
- No cost for role assumptions
- Only pay for AWS resources used by workflows

---

## References

- [GitHub OIDC with AWS](https://docs.github.com/en/actions/deployment/security-hardening-your-deployments/configuring-openid-connect-in-amazon-web-services)
- [AWS IAM OIDC Providers](https://docs.aws.amazon.com/IAM/latest/UserGuide/id_roles_providers_create_oidc.html)
- [aws-actions/configure-aws-credentials](https://github.com/aws-actions/configure-aws-credentials)
- specs/ci-cd-pipelines.md (Section: AWS OIDC Authentication)
- INFRASTRUCTURE_PLAN.md (Phase 2.2, 4.1)
