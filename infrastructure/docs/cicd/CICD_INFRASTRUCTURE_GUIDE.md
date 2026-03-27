# CI/CD Infrastructure Guide for Turaf DEV Environment

**Date**: March 25, 2026  
**Environment**: DEV  
**Purpose**: Infrastructure prerequisites and configuration for GitHub Actions CI/CD pipelines

---

## Current Infrastructure Status

### ✅ Deployed and Ready for CI/CD

**Networking** (100%):
- VPC: `vpc-04b562ab3eebfb8b5` (10.0.0.0/16)
- Subnets: 9 total (3 public, 3 private, 3 database)
- NAT Gateways: 3 (one per AZ for high availability)
- VPC Endpoints: 6 interface endpoints + S3 gateway endpoint

**Security** (100%):
- Security Groups: 5 (ALB, ECS, RDS, ElastiCache, DocumentDB)
- KMS Keys: 3 (RDS, S3, Secrets Manager)
- IAM Roles:
  - ECS Execution Role: For pulling images and accessing secrets
  - ECS Task Role: For application runtime permissions

**Database** (In Progress):
- ✅ Redis: `turaf-redis-dev` (cache.t3.micro, 1 node)
- ✅ DB Subnet Group: `turaf-db-subnet-group-dev`
- 🔄 RDS PostgreSQL: Being deployed (PostgreSQL 15.17, db.t3.micro)

**Storage** (100%):
- S3 Bucket: `turaf-dev-801651112319`
- Encryption: AES256
- Lifecycle policies configured

**Secrets Manager** (100%):
- DB Admin Password: `turaf/dev/db/admin-password`
- Service User Passwords:
  - `turaf/dev/db/identity-user`
  - `turaf/dev/db/organization-user`
  - `turaf/dev/db/experiment-user`
  - `turaf/dev/db/metrics-user`
- Redis Auth Token: `turaf/dev/redis/auth-token`

**Messaging** (In Progress):
- 🔄 EventBridge Event Bus: `turaf-events-dev`
- 🔄 SQS Queues:
  - Dead Letter Queue
  - Events Queue
  - Notifications Queue
  - Reports Queue
- 🔄 EventBridge Rules: For event routing

**Lambda** (Disabled - Optional):
- Event Processor: Disabled
- Notification Processor: Disabled
- Report Generator: Disabled

### ❌ Not Yet Deployed (Pending Fixes)

**Compute** (ECS):
- ECS Cluster
- ECS Services (identity, organization, experiment)
- Application Load Balancer
- Target Groups
- **Issue**: `deployment_configuration` syntax needs AWS provider compatibility fix

**Monitoring**:
- CloudWatch Dashboards
- CloudWatch Alarms
- X-Ray Tracing
- **Issue**: Dashboard conditional logic needs fix

---

## CI/CD Prerequisites

### 1. AWS Account Configuration

**Account ID**: `801651112319`  
**Region**: `us-east-1`  
**Environment**: `dev`

### 2. IAM Roles for GitHub Actions

**Required Role**: `github-actions-deploy-role`

**Permissions Needed**:
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ecr:GetAuthorizationToken",
        "ecr:BatchCheckLayerAvailability",
        "ecr:GetDownloadUrlForLayer",
        "ecr:BatchGetImage",
        "ecr:PutImage",
        "ecr:InitiateLayerUpload",
        "ecr:UploadLayerPart",
        "ecr:CompleteLayerUpload"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "ecs:UpdateService",
        "ecs:DescribeServices",
        "ecs:DescribeTaskDefinition",
        "ecs:RegisterTaskDefinition"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "iam:PassRole"
      ],
      "Resource": [
        "arn:aws:iam::801651112319:role/turaf-ecs-execution-role-dev",
        "arn:aws:iam::801651112319:role/turaf-ecs-task-role-dev"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "secretsmanager:GetSecretValue"
      ],
      "Resource": "arn:aws:secretsmanager:us-east-1:801651112319:secret:turaf/dev/*"
    }
  ]
}
```

**Trust Policy** (for OIDC):
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::801651112319:oidc-provider/token.actions.githubusercontent.com"
      },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringEquals": {
          "token.actions.githubusercontent.com:aud": "sts.amazonaws.com"
        },
        "StringLike": {
          "token.actions.githubusercontent.com:sub": "repo:YOUR_ORG/Turaf:*"
        }
      }
    }
  ]
}
```

### 3. ECR Repositories

**Required Repositories**:
- `turaf-identity-service`
- `turaf-organization-service`
- `turaf-experiment-service`
- `turaf-metrics-service` (optional)
- `turaf-reporting-service` (optional)
- `turaf-notification-service` (optional)

**Repository URIs**:
```
801651112319.dkr.ecr.us-east-1.amazonaws.com/turaf-identity-service
801651112319.dkr.ecr.us-east-1.amazonaws.com/turaf-organization-service
801651112319.dkr.ecr.us-east-1.amazonaws.com/turaf-experiment-service
```

**Check if repositories exist**:
```bash
aws ecr describe-repositories --region us-east-1 --query 'repositories[?starts_with(repositoryName, `turaf-`)].repositoryName'
```

### 4. GitHub Secrets Configuration

**Required Secrets**:
```yaml
AWS_ACCOUNT_ID: "801651112319"
AWS_REGION: "us-east-1"
AWS_ROLE_TO_ASSUME: "arn:aws:iam::801651112319:role/github-actions-deploy-role"
```

**Optional Secrets** (for enhanced security):
```yaml
SONAR_TOKEN: "<sonarcloud-token>"
SONAR_HOST_URL: "<sonarcloud-host-url>"
CODECOV_TOKEN: "<codecov-token>"
SLACK_WEBHOOK_URL: "<slack-webhook-for-notifications>"
```

### 5. Terraform Backend Configuration

**S3 Bucket**: `turaf-terraform-state`  
**DynamoDB Table**: `turaf-terraform-locks`  
**Region**: `us-east-1`

**Backend Config**:
```hcl
terraform {
  backend "s3" {
    bucket         = "turaf-terraform-state"
    key            = "dev/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "turaf-terraform-locks"
    encrypt        = true
  }
}
```

---

## GitHub Actions Workflow Structure

### Recommended Workflows

**1. Build and Test** (`.github/workflows/build-test.yml`):
```yaml
name: Build and Test

on:
  pull_request:
    branches: [main, develop]
  push:
    branches: [develop]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          
      - name: Run Tests
        run: mvn test -Dtest="!*IntegrationTest"
        
      - name: Upload Coverage
        uses: codecov/codecov-action@v4
        with:
          token: ${{ secrets.CODECOV_TOKEN }}
```

**2. Build and Push Images** (`.github/workflows/build-push.yml`):
```yaml
name: Build and Push Docker Images

on:
  push:
    branches: [main, develop]
    tags: ['v*']

jobs:
  build-push:
    runs-on: ubuntu-latest
    permissions:
      id-token: write
      contents: read
      
    strategy:
      matrix:
        service: [identity, organization, experiment]
        
    steps:
      - uses: actions/checkout@v4
      
      - name: Configure AWS Credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: ${{ secrets.AWS_ROLE_TO_ASSUME }}
          aws-region: ${{ secrets.AWS_REGION }}
          
      - name: Login to Amazon ECR
        id: login-ecr
        uses: aws-actions/amazon-ecr-login@v2
        
      - name: Build and Push
        env:
          ECR_REGISTRY: ${{ steps.login-ecr.outputs.registry }}
          IMAGE_TAG: ${{ github.sha }}
        run: |
          docker build -t $ECR_REGISTRY/turaf-${{ matrix.service }}-service:$IMAGE_TAG \
            -f services/${{ matrix.service }}-service/Dockerfile .
          docker push $ECR_REGISTRY/turaf-${{ matrix.service }}-service:$IMAGE_TAG
          docker tag $ECR_REGISTRY/turaf-${{ matrix.service }}-service:$IMAGE_TAG \
            $ECR_REGISTRY/turaf-${{ matrix.service }}-service:latest
          docker push $ECR_REGISTRY/turaf-${{ matrix.service }}-service:latest
```

**3. Deploy to DEV** (`.github/workflows/deploy-dev.yml`):
```yaml
name: Deploy to DEV

on:
  workflow_run:
    workflows: ["Build and Push Docker Images"]
    types: [completed]
    branches: [develop]

jobs:
  deploy:
    runs-on: ubuntu-latest
    if: ${{ github.event.workflow_run.conclusion == 'success' }}
    permissions:
      id-token: write
      contents: read
      
    steps:
      - uses: actions/checkout@v4
      
      - name: Configure AWS Credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: ${{ secrets.AWS_ROLE_TO_ASSUME }}
          aws-region: ${{ secrets.AWS_REGION }}
          
      - name: Update ECS Services
        run: |
          for service in identity organization experiment; do
            aws ecs update-service \
              --cluster turaf-cluster-dev \
              --service ${service}-service-dev \
              --force-new-deployment \
              --region us-east-1
          done
          
      - name: Wait for Deployment
        run: |
          for service in identity organization experiment; do
            aws ecs wait services-stable \
              --cluster turaf-cluster-dev \
              --services ${service}-service-dev \
              --region us-east-1
          done
```

---

## Infrastructure Outputs for CI/CD

### Terraform Outputs (Available after full deployment)

```hcl
# VPC and Networking
vpc_id                = "vpc-04b562ab3eebfb8b5"
public_subnet_ids     = ["subnet-xxx", "subnet-yyy", "subnet-zzz"]
private_subnet_ids    = ["subnet-aaa", "subnet-bbb", "subnet-ccc"]
database_subnet_ids   = ["subnet-ddd", "subnet-eee", "subnet-fff"]

# Compute (Pending deployment)
cluster_name          = "turaf-cluster-dev"
alb_dns_name          = "<pending>"
alb_zone_id           = "<pending>"

# Database
rds_endpoint          = "<sensitive>"
rds_instance_id       = "<pending>"
redis_endpoint        = "<sensitive>"

# Storage
primary_bucket_id     = "turaf-dev-801651112319"
primary_bucket_arn    = "arn:aws:s3:::turaf-dev-801651112319"

# Security
ecs_execution_role_arn = "arn:aws:iam::801651112319:role/turaf-ecs-execution-role-dev"
ecs_task_role_arn      = "arn:aws:iam::801651112319:role/turaf-ecs-task-role-dev"
```

### Environment Variables for Applications

**Database Connection**:
```bash
DB_HOST=$(aws secretsmanager get-secret-value --secret-id turaf/dev/db/admin-password --query SecretString --output text | jq -r .host)
DB_PORT=5432
DB_NAME=turaf
DB_USERNAME=turaf_admin
DB_PASSWORD=$(aws secretsmanager get-secret-value --secret-id turaf/dev/db/admin-password --query SecretString --output text | jq -r .password)
```

**Redis Connection**:
```bash
REDIS_HOST=$(aws secretsmanager get-secret-value --secret-id turaf/dev/redis/auth-token --query SecretString --output text | jq -r .endpoint)
REDIS_PORT=6379
REDIS_AUTH_TOKEN=$(aws secretsmanager get-secret-value --secret-id turaf/dev/redis/auth-token --query SecretString --output text | jq -r .auth_token)
```

**EventBridge**:
```bash
EVENT_BUS_NAME=turaf-events-dev
AWS_REGION=us-east-1
```

---

## Deployment Checklist

### Before First Deployment

- [ ] Create GitHub OIDC provider in AWS IAM
- [ ] Create `github-actions-deploy-role` IAM role with proper permissions
- [ ] Create ECR repositories for all services
- [ ] Configure GitHub repository secrets
- [ ] Complete Terraform infrastructure deployment (compute module)
- [ ] Run database migrations via Flyway
- [ ] Verify all secrets are populated in Secrets Manager

### For Each Deployment

- [ ] Build passes all unit tests
- [ ] Integration tests pass (if applicable)
- [ ] Docker images build successfully
- [ ] Images pushed to ECR
- [ ] ECS services updated with new task definitions
- [ ] Health checks pass
- [ ] Smoke tests pass

---

## Current Blockers for CI/CD

### 1. Compute Module Not Deployed
**Issue**: ECS cluster and services not yet deployed due to `deployment_configuration` syntax issue  
**Impact**: Cannot deploy containers until ECS infrastructure exists  
**Resolution**: Fix AWS provider compatibility in compute module

### 2. ACM Certificate
**Issue**: Using placeholder certificate ARN  
**Impact**: HTTPS won't work on ALB  
**Resolution**: Create valid ACM certificate for domain or use HTTP for dev

### 3. Database Migrations
**Issue**: Database schemas not yet created  
**Impact**: Applications will fail to start  
**Resolution**: Run Flyway migrations after RDS is available

---

## Next Steps

1. **Complete Infrastructure Deployment**:
   - Fix compute module `deployment_configuration` syntax
   - Deploy ECS cluster and services
   - Verify all resources healthy

2. **Setup Database**:
   - Wait for RDS to be available (~10 minutes)
   - Run Flyway migrations to create schemas
   - Create service-specific database users

3. **Configure GitHub Actions**:
   - Create OIDC provider
   - Create IAM role for GitHub Actions
   - Add repository secrets
   - Create workflow files

4. **Build and Deploy First Service**:
   - Build identity-service Docker image
   - Push to ECR
   - Deploy to ECS
   - Verify health checks

5. **Enable Monitoring**:
   - Fix monitoring module dashboard syntax
   - Deploy CloudWatch dashboards
   - Configure alarms
   - Set up log aggregation

---

## Cost Estimate (Current + Planned)

**Current Monthly Cost**: ~$60-80
- NAT Gateways: $96/month
- VPC Endpoints: $42/month
- Redis: $12/month
- S3 + Secrets + KMS: ~$7/month

**After Full Deployment**: ~$180-220/month
- Add ECS Fargate: ~$30-50/month (3 services, minimal)
- Add RDS: ~$15/month (db.t3.micro)
- Add ALB: ~$20/month
- Add CloudWatch: ~$10/month

---

## Support and Troubleshooting

**Terraform State**:
```bash
cd infrastructure/terraform/environments/dev
source ../../../scripts/assume-role.sh dev
terraform state list
terraform output
```

**Check ECS Services** (after deployment):
```bash
aws ecs list-services --cluster turaf-cluster-dev --region us-east-1
aws ecs describe-services --cluster turaf-cluster-dev --services identity-service-dev --region us-east-1
```

**Check ECR Images**:
```bash
aws ecr list-images --repository-name turaf-identity-service --region us-east-1
```

**View Logs**:
```bash
aws logs tail /aws/ecs/turaf-identity-service-dev --follow --region us-east-1
```

---

**Last Updated**: March 25, 2026 2:30 AM  
**Status**: Infrastructure 60% Complete, CI/CD Prerequisites Documented  
**Next Review**: After compute module deployment
