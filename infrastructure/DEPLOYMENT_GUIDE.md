# Infrastructure Deployment Guide

This guide covers deploying the Turaf infrastructure to AWS using Terraform.

## Prerequisites

### Required Tools

- **Terraform** >= 1.5.0 ([Install](https://www.terraform.io/downloads))
- **AWS CLI** >= 2.0 ([Install](https://aws.amazon.com/cli/))
- **jq** (for JSON parsing) - `brew install jq`

### AWS Configuration

1. **AWS SSO Login**:
   ```bash
   aws sso login --profile turaf-dev
   ```

2. **Verify Credentials**:
   ```bash
   aws sts get-caller-identity --profile turaf-dev
   ```

---

## Quick Start - Development Environment

### Option 1: Automated Deployment (Recommended)

```bash
./infrastructure/scripts/deploy-dev-infrastructure.sh
```

This script will:
1. Check prerequisites (Terraform, AWS credentials)
2. Create `terraform.tfvars` from example if needed
3. Run `terraform init`
4. Run `terraform validate`
5. Generate and show execution plan
6. Prompt for confirmation
7. Apply infrastructure changes

### Option 2: Manual Deployment

```bash
# Navigate to dev environment
cd infrastructure/terraform/environments/dev

# Create terraform.tfvars
cp terraform.tfvars.example terraform.tfvars

# Edit terraform.tfvars with your values
# At minimum, update:
# - acm_certificate_arn (if you have one)
# - domain_name

# Initialize Terraform
AWS_PROFILE=turaf-dev terraform init

# Validate configuration
AWS_PROFILE=turaf-dev terraform validate

# Plan deployment
AWS_PROFILE=turaf-dev terraform plan -out=tfplan

# Review plan, then apply
AWS_PROFILE=turaf-dev terraform apply tfplan
```

---

## What Gets Deployed

### Networking (Module: networking)

- **VPC**: 10.0.0.0/16
- **Subnets** (2 AZs):
  - Public: 10.0.0.0/24, 10.0.1.0/24
  - Private: 10.0.10.0/24, 10.0.11.0/24
  - Database: 10.0.20.0/24, 10.0.21.0/24
- **Internet Gateway**
- **VPC Endpoints** (S3, ECR, CloudWatch, Secrets Manager, ECS)
- **NAT Gateway**: Disabled by default (cost optimization)

### Security (Module: security)

- **Security Groups**: RDS, ECS, ALB, Lambda, VPC Endpoints
- **IAM Roles**: ECS Task Execution, ECS Task, Lambda Execution
- **KMS Keys**: RDS encryption

### Database (Module: database)

- **RDS PostgreSQL 15.4**:
  - Instance: db.t3.micro (Free Tier eligible)
  - Storage: 20 GB (Free Tier eligible)
  - Multi-schema: identity, organization, experiment, metrics, communications
  - Automated backups: 1 day retention
  - Encryption: KMS at rest
- **Secrets Manager**: Database credentials
- **ElastiCache Redis**: Disabled by default
- **DocumentDB**: Disabled by default

### Storage (Module: storage)

- **S3 Buckets**:
  - Primary bucket (application data)
  - Logs bucket (access logs)
  - Backups bucket (database backups)
- **Lifecycle Policies**: Automatic archival and deletion

### Messaging (Module: messaging)

- **EventBridge**: Custom event bus
- **SQS Queues**: Notifications, reports, chat (optional)
- **Dead Letter Queues**: For failed messages

### Compute (Module: compute)

- **ECS Cluster**: Fargate-based
- **Application Load Balancer**: HTTPS/HTTP
- **Target Groups**: Per service
- **CloudWatch Log Groups**: Per service
- **Services**: Not deployed yet (requires Docker images)

---

## Cost Optimization

### Development Environment (Default)

**Estimated Cost**: ~$15-30/month (with Free Tier) or ~$55/month (after Free Tier)

**Cost-Saving Features**:
- ✅ NAT Gateway disabled (save ~$65/month)
- ✅ Redis disabled (save ~$12/month)
- ✅ DocumentDB disabled (save ~$54/month)
- ✅ RDS Free Tier (db.t3.micro, 20 GB)
- ✅ Single-AZ deployment
- ✅ Minimal backup retention (1 day)
- ✅ Performance Insights disabled
- ✅ Fargate Spot instances (70% cheaper)

**Cost Breakdown**:
- RDS PostgreSQL: $0 (Free Tier) or $15/month
- ECS Fargate (3 services, Spot): ~$15/month
- ALB: ~$16/month
- VPC Endpoints (2): ~$14/month
- S3, ECR, Secrets Manager: ~$5/month
- CloudWatch Logs: ~$2/month

### Production Environment

For production, enable:
- Multi-AZ RDS
- NAT Gateways
- Redis and DocumentDB
- Increased backup retention
- Performance Insights
- Regular Fargate (not Spot)

**Estimated Cost**: ~$500-700/month

---

## Post-Deployment Steps

After Terraform completes, you need to:

### 1. Run Flyway Migrations

```bash
# Navigate to flyway-service
cd services/flyway-service

# Set environment variables
export DB_HOST=<RDS_ENDPOINT>
export DB_NAME=turaf
export DB_USER=postgres
export DB_PASSWORD=<FROM_SECRETS_MANAGER>

# Run migrations
./scripts/run-migrations.sh
```

### 2. Create Database Users

```bash
# Set user passwords
export IDENTITY_PASSWORD="your-password"
export ORGANIZATION_PASSWORD="your-password"
export EXPERIMENT_PASSWORD="your-password"
export METRICS_PASSWORD="your-password"
export COMMUNICATIONS_PASSWORD="your-password"

# Run user creation script
./infrastructure/scripts/create-db-users.sh <RDS_ENDPOINT> 5432 turaf postgres <MASTER_PASSWORD>
```

### 3. Configure Network Security Groups

```bash
# Run network setup script
./infrastructure/scripts/setup-flyway-network-cross-account.sh

# Verify configuration
./infrastructure/scripts/verify-flyway-network-cross-account.sh
```

### 4. Verify Infrastructure

```bash
cd infrastructure/terraform/environments/dev

# Check outputs
AWS_PROFILE=turaf-dev terraform output

# Verify VPC
aws ec2 describe-vpcs --filters "Name=tag:Name,Values=turaf-vpc-dev" --profile turaf-dev

# Verify RDS
aws rds describe-db-instances --db-instance-identifier turaf-db-dev --profile turaf-dev

# Verify security groups
aws ec2 describe-security-groups --filters "Name=tag:Name,Values=turaf-rds-dev" --profile turaf-dev
```

---

## Terraform State Management

### Backend Configuration

The Terraform state is stored in S3 with DynamoDB locking:

- **S3 Bucket**: `turaf-terraform-state-<account-id>`
- **DynamoDB Table**: `turaf-terraform-locks`
- **State File**: `dev/terraform.tfstate`

### State Commands

```bash
# Show current state
terraform show

# List resources
terraform state list

# Show specific resource
terraform state show module.networking.aws_vpc.main

# Refresh state
terraform refresh
```

---

## Updating Infrastructure

### Making Changes

1. Update Terraform configuration files
2. Run `terraform plan` to preview changes
3. Review the plan carefully
4. Run `terraform apply` to apply changes

```bash
cd infrastructure/terraform/environments/dev

# Plan changes
AWS_PROFILE=turaf-dev terraform plan

# Apply changes
AWS_PROFILE=turaf-dev terraform apply
```

### Targeting Specific Resources

```bash
# Update only networking module
terraform apply -target=module.networking

# Update only database module
terraform apply -target=module.database
```

---

## Destroying Infrastructure

### ⚠️ WARNING: This will delete all resources

```bash
cd infrastructure/terraform/environments/dev

# Plan destruction
AWS_PROFILE=turaf-dev terraform plan -destroy

# Destroy all resources
AWS_PROFILE=turaf-dev terraform destroy
```

### Selective Destruction

```bash
# Destroy only compute resources
terraform destroy -target=module.compute

# Destroy only database
terraform destroy -target=module.database
```

---

## Troubleshooting

### Issue: Terraform Init Fails

**Error**: "Backend configuration changed"

**Solution**:
```bash
terraform init -reconfigure
```

### Issue: AWS Credentials Expired

**Error**: "The security token included in the request is invalid"

**Solution**:
```bash
aws sso login --profile turaf-dev
```

### Issue: Resource Already Exists

**Error**: "Resource already exists"

**Solution**:
```bash
# Import existing resource
terraform import module.networking.aws_vpc.main vpc-xxxxx

# Or remove from state and let Terraform recreate
terraform state rm module.networking.aws_vpc.main
```

### Issue: Insufficient Permissions

**Error**: "User is not authorized to perform: ..."

**Solution**: Use `turaf-root` profile or ensure your profile has admin permissions.

### Issue: RDS Creation Timeout

**Error**: "timeout while waiting for state to become 'available'"

**Solution**: RDS creation can take 10-15 minutes. Increase timeout or wait and retry.

---

## Environment-Specific Deployments

### QA Environment

```bash
cd infrastructure/terraform/environments/qa

# Copy and update tfvars
cp terraform.tfvars.example terraform.tfvars

# Deploy
AWS_PROFILE=turaf-qa terraform init
AWS_PROFILE=turaf-qa terraform plan
AWS_PROFILE=turaf-qa terraform apply
```

### Production Environment

```bash
cd infrastructure/terraform/environments/prod

# Copy and update tfvars
cp terraform.tfvars.example terraform.tfvars

# IMPORTANT: Review production settings
# - Enable Multi-AZ
# - Enable deletion protection
# - Increase backup retention
# - Enable NAT gateways
# - Enable Redis and DocumentDB

# Deploy
AWS_PROFILE=turaf-prod terraform init
AWS_PROFILE=turaf-prod terraform plan
AWS_PROFILE=turaf-prod terraform apply
```

---

## Next Steps

After infrastructure deployment:

1. ✅ **Task 025**: Configure Database Users and Permissions
2. ✅ **Task 026**: Configure Database Migration IAM Roles
3. ✅ **Task 027**: Configure Database Migration Network Access
4. ⏭️ **Task 028**: Create CodeBuild Migration Projects
5. ⏭️ **Build and Deploy Services**: Build Docker images and deploy to ECS

---

## References

- **Terraform Modules**: `infrastructure/terraform/modules/`
- **Task Documentation**: `tasks/infrastructure/`
- **AWS Infrastructure Spec**: `specs/aws-infrastructure.md`
- **Terraform Documentation**: https://www.terraform.io/docs
