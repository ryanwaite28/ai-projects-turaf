# AWS Prerequisites for Turaf Deployment

This document outlines all AWS resources that must be manually created before deploying the Turaf application infrastructure.

## Table of Contents
- [IAM Roles for GitHub Actions](#iam-roles-for-github-actions)
- [S3 Backend for Terraform State](#s3-backend-for-terraform-state)
- [DynamoDB for Terraform State Locking](#dynamodb-for-terraform-state-locking)
- [ECR Repositories](#ecr-repositories)
- [GitHub Secrets Configuration](#github-secrets-configuration)
- [Deployment Order](#deployment-order)

---

## IAM Roles for GitHub Actions

✅ **ALREADY CONFIGURED** - The following IAM roles already exist in each AWS account:

### Existing Roles

#### DEV Environment
- **Role Name:** `GitHubActionsDeploymentRole`
- **AWS Account ID:** `801651112319`
- **Role ARN:** `arn:aws:iam::801651112319:role/GitHubActionsDeploymentRole`

#### QA Environment
- **Role Name:** `GitHubActionsDeploymentRole`
- **AWS Account ID:** `965932217544`
- **Role ARN:** `arn:aws:iam::965932217544:role/GitHubActionsDeploymentRole`

#### PROD Environment
- **Role Name:** `GitHubActionsDeploymentRole`
- **AWS Account ID:** `811783768245`
- **Role ARN:** `arn:aws:iam::811783768245:role/GitHubActionsDeploymentRole`

### Role Configuration

These roles are configured with:
- OIDC-based authentication via GitHub Actions
- Trust policy allowing `repo:ryanwaite28/ai-projects-turaf:*`
- Appropriate permissions for Terraform deployments (EC2, ECS, VPC, ALB, RDS, S3, CloudFront, etc.)

---

## S3 Backend for Terraform State

Create S3 buckets to store Terraform state files for each environment.

### Bucket Names
- `turaf-terraform-state-dev`
- `turaf-terraform-state-qa`
- `turaf-terraform-state-prod`

### Creation Commands

```bash
# DEV
aws s3api create-bucket \
  --bucket turaf-terraform-state-dev \
  --region us-east-1

aws s3api put-bucket-versioning \
  --bucket turaf-terraform-state-dev \
  --versioning-configuration Status=Enabled

aws s3api put-bucket-encryption \
  --bucket turaf-terraform-state-dev \
  --server-side-encryption-configuration '{
    "Rules": [{
      "ApplyServerSideEncryptionByDefault": {
        "SSEAlgorithm": "AES256"
      }
    }]
  }'

aws s3api put-public-access-block \
  --bucket turaf-terraform-state-dev \
  --public-access-block-configuration \
    BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true

# QA
aws s3api create-bucket \
  --bucket turaf-terraform-state-qa \
  --region us-east-1

aws s3api put-bucket-versioning \
  --bucket turaf-terraform-state-qa \
  --versioning-configuration Status=Enabled

aws s3api put-bucket-encryption \
  --bucket turaf-terraform-state-qa \
  --server-side-encryption-configuration '{
    "Rules": [{
      "ApplyServerSideEncryptionByDefault": {
        "SSEAlgorithm": "AES256"
      }
    }]
  }'

aws s3api put-public-access-block \
  --bucket turaf-terraform-state-qa \
  --public-access-block-configuration \
    BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true

# PROD
aws s3api create-bucket \
  --bucket turaf-terraform-state-prod \
  --region us-east-1

aws s3api put-bucket-versioning \
  --bucket turaf-terraform-state-prod \
  --versioning-configuration Status=Enabled

aws s3api put-bucket-encryption \
  --bucket turaf-terraform-state-prod \
  --server-side-encryption-configuration '{
    "Rules": [{
      "ApplyServerSideEncryptionByDefault": {
        "SSEAlgorithm": "AES256"
      }
    }]
  }'

aws s3api put-public-access-block \
  --bucket turaf-terraform-state-prod \
  --public-access-block-configuration \
    BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true
```

---

## DynamoDB for Terraform State Locking

Create a DynamoDB table for Terraform state locking (shared across all environments).

### Table Name
`turaf-terraform-locks`

### Creation Command

```bash
aws dynamodb create-table \
  --table-name turaf-terraform-locks \
  --attribute-definitions AttributeName=LockID,AttributeType=S \
  --key-schema AttributeName=LockID,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --region us-east-1
```

---

## ECR Repositories

Create ECR repositories for each service.

### Repository Names
- `turaf-identity-service`
- `turaf-organization-service`
- `turaf-experiment-service`
- `turaf-metrics-service`
- `turaf-communications-service`
- `turaf-bff-api`
- `turaf-ws-gateway`

### Creation Commands

```bash
for service in identity-service organization-service experiment-service metrics-service communications-service bff-api ws-gateway; do
  aws ecr create-repository \
    --repository-name turaf-$service \
    --image-scanning-configuration scanOnPush=true \
    --encryption-configuration encryptionType=AES256 \
    --region us-east-1
done
```

### Set Lifecycle Policies

Keep only the last 10 images to save costs:

```bash
for service in identity-service organization-service experiment-service metrics-service communications-service bff-api ws-gateway; do
  aws ecr put-lifecycle-policy \
    --repository-name turaf-$service \
    --lifecycle-policy-text '{
      "rules": [{
        "rulePriority": 1,
        "description": "Keep last 10 images",
        "selection": {
          "tagStatus": "any",
          "countType": "imageCountMoreThan",
          "countNumber": 10
        },
        "action": {
          "type": "expire"
        }
      }]
    }' \
    --region us-east-1
done
```

---

## GitHub Secrets Configuration

✅ **ALREADY CONFIGURED** - The following secrets already exist in the GitHub repository:

### Existing Secrets (Per Environment)

#### DEV Environment Secrets
- `AWS_ROLE_ARN`: `arn:aws:iam::801651112319:role/GitHubActionsDeploymentRole`
- `AWS_ACCOUNT_ID`: `801651112319`

#### QA Environment Secrets
- `AWS_ROLE_ARN`: `arn:aws:iam::965932217544:role/GitHubActionsDeploymentRole`
- `AWS_ACCOUNT_ID`: `965932217544`

#### PROD Environment Secrets
- `AWS_ROLE_ARN`: `arn:aws:iam::811783768245:role/GitHubActionsDeploymentRole`
- `AWS_ACCOUNT_ID`: `811783768245`

**Note:** These secrets are configured as environment-specific secrets in GitHub Actions, accessible via the `dev`, `qa`, and `prod` environments.

---

## Deployment Order

Follow this order to deploy the Turaf application:

### 1. Prerequisites (Manual - One Time)
- [x] ~~Create OIDC Identity Provider~~ (Already exists)
- [x] ~~Create IAM Roles (DEV, QA, PROD)~~ (Already exists: `GitHubActionsDeploymentRole`)
- [x] ~~Configure GitHub Secrets~~ (Already configured)
- [ ] Create S3 buckets for Terraform state
- [ ] Create DynamoDB table for state locking
- [ ] Create ECR repositories

### 2. Shared Infrastructure (Automated via GitHub Actions)
- [ ] Push to `develop` branch to deploy DEV infrastructure
- [ ] Merge to `main` branch to deploy QA and PROD infrastructure
- [ ] Verify outputs: VPC, subnets, security groups, RDS, ALB, ECS cluster

### 3. Service Deployments (Automated via GitHub Actions)
- [ ] Deploy services individually by pushing changes to service directories
- [ ] Each service workflow will:
  - Build Docker image
  - Push to ECR
  - Deploy via Terraform
  - Update ECS service

### 4. Frontend Deployment (Automated via GitHub Actions)
- [ ] Push frontend changes to deploy
- [ ] Workflow will:
  - Build Angular application
  - Create S3 + CloudFront infrastructure
  - Deploy static files
  - Invalidate CloudFront cache

### 5. Database Migrations (Manual or Automated)
- [ ] Run Flyway migrations via ECS task or local execution
- [ ] Verify schema changes in RDS

---

## Verification Checklist

After completing prerequisites, verify:

- [ ] OIDC provider exists in IAM
- [ ] IAM roles created with correct trust policies
- [ ] S3 buckets created with versioning and encryption enabled
- [ ] DynamoDB table created
- [ ] ECR repositories created with lifecycle policies
- [ ] GitHub secrets configured
- [ ] GitHub Actions workflows can assume IAM roles

---

## Cost Optimization Notes

- **S3 State Buckets**: Minimal cost (~$0.01/month per bucket)
- **DynamoDB Locks Table**: Pay-per-request, minimal cost
- **ECR Repositories**: Free tier includes 500MB storage; lifecycle policies keep costs low
- **IAM Roles**: No cost

**Estimated Monthly Cost for Prerequisites**: < $1 USD

---

## Troubleshooting

### GitHub Actions Cannot Assume Role

**Error:** `An error occurred (AccessDenied) when calling the AssumeRoleWithWebIdentity operation`

**Solution:**
1. Verify OIDC provider thumbprint is correct
2. Check trust policy allows your GitHub repository
3. Ensure GitHub Actions has `id-token: write` permission

### Terraform State Lock Issues

**Error:** `Error locking state: ConditionalCheckFailedException`

**Solution:**
1. Check DynamoDB table exists and is accessible
2. Verify IAM role has DynamoDB permissions
3. If stuck, manually remove lock from DynamoDB table

### ECR Push Failures

**Error:** `denied: Your authorization token has expired`

**Solution:**
1. Re-authenticate: `aws ecr get-login-password | docker login --username AWS --password-stdin`
2. Verify IAM role has ECR permissions

---

## Additional Resources

- [AWS OIDC for GitHub Actions](https://docs.github.com/en/actions/deployment/security-hardening-your-deployments/configuring-openid-connect-in-amazon-web-services)
- [Terraform S3 Backend](https://developer.hashicorp.com/terraform/language/settings/backends/s3)
- [ECR Lifecycle Policies](https://docs.aws.amazon.com/AmazonECR/latest/userguide/LifecyclePolicies.html)
