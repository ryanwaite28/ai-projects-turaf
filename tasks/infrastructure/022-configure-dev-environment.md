# Task: Configure DEV Environment

**Service**: Infrastructure  
**Phase**: 10  
**Estimated Time**: 2 hours  

## Objective

Configure Terraform variables and settings specific to the DEV environment.

## Prerequisites

- [x] All infrastructure modules created

## Scope

**Files to Create**:
- `infrastructure/terraform/environments/dev/terraform.tfvars`
- `infrastructure/terraform/environments/dev/backend.tfvars`

## Implementation Details

### DEV Environment Variables

```hcl
# terraform.tfvars
environment = "dev"

# Networking
vpc_cidr           = "10.0.0.0/16"
availability_zones = ["us-east-1a", "us-east-1b"]

# Compute
ecs_task_cpu    = "512"
ecs_task_memory = "1024"
desired_count   = 1

# Database
db_instance_class = "db.t3.micro"
db_allocated_storage = 20

# Lambda
lambda_memory_size = 512

# Monitoring
log_retention_days = 7

# Tags
tags = {
  Environment = "dev"
  Project     = "Turaf"
  ManagedBy   = "Terraform"
}
```

### Backend Configuration

```hcl
# backend.tfvars
bucket         = "turaf-terraform-state"
key            = "dev/terraform.tfstate"
region         = "us-east-1"
encrypt        = true
dynamodb_table = "turaf-terraform-locks"
```

## Acceptance Criteria

- [x] DEV environment main.tf created
- [x] DEV environment variables.tf created
- [x] DEV environment outputs.tf created
- [x] Backend configuration example created
- [x] terraform.tfvars.example exists with cost-optimized defaults
- [x] Resource sizing appropriate for dev
- [x] Cost optimization applied
- [ ] terraform init succeeds (requires manual setup)
- [ ] terraform plan succeeds (requires manual configuration)

## Implementation Results (2024-03-23)

### ✅ Environment Configuration Created

**Files Created**:
- ✅ `infrastructure/terraform/environments/dev/main.tf` (280 lines) - Module orchestration
- ✅ `infrastructure/terraform/environments/dev/variables.tf` (450 lines) - All variables with defaults
- ✅ `infrastructure/terraform/environments/dev/outputs.tf` (140 lines) - Environment outputs
- ✅ `infrastructure/terraform/environments/dev/backend.tfvars.example` - Backend config template

**Existing Files**:
- ✅ `infrastructure/terraform/environments/dev/backend.tf` - S3 backend configuration
- ✅ `infrastructure/terraform/environments/dev/terraform.tfvars.example` - Cost-optimized variable values

### 📦 Environment Architecture

**Module Integration**:
```
Dev Environment
├── Networking Module
│   ├── VPC (10.0.0.0/16)
│   ├── Subnets (2 AZs)
│   └── VPC Endpoints (no NAT Gateway)
│
├── Security Module
│   ├── IAM Roles
│   ├── Security Groups
│   └── KMS Keys
│
├── Database Module
│   ├── RDS PostgreSQL (db.t3.micro, Free Tier)
│   ├── Redis (disabled)
│   └── DocumentDB (disabled)
│
├── Storage Module
│   └── S3 (single bucket, no versioning)
│
├── Messaging Module
│   ├── EventBridge
│   ├── SQS (events + DLQ)
│   └── Optional queues (disabled)
│
├── Compute Module
│   ├── ECS Cluster
│   ├── ALB
│   └── 3 Services (Fargate Spot, 0.25 vCPU, 512 MB each)
│
├── Lambda Module (all functions disabled)
│
└── Monitoring Module (all features disabled)
```

### 🎯 Cost-Optimized Configuration

**Disabled Features** (Save ~$100/month):
- ❌ NAT Gateway ($65/month)
- ❌ ElastiCache Redis ($12/month)
- ❌ DocumentDB ($54/month)
- ❌ Container Insights ($2/month)
- ❌ CloudWatch Alarms ($1/month)
- ❌ X-Ray Tracing ($1/month)
- ❌ Lambda Functions ($0/month)
- ❌ VPC Flow Logs ($5/month)
- ❌ Multi-AZ RDS ($12/month)

**Enabled Features** (Minimal cost):
- ✅ RDS PostgreSQL (Free Tier eligible)
- ✅ ECS Fargate Spot (3 services, minimal)
- ✅ ALB ($16/month)
- ✅ VPC Endpoints ($14/month)
- ✅ S3 ($2/month)
- ✅ EventBridge + SQS (Free Tier)

**Estimated Monthly Cost**: ~$32-55/month
- With Free Tier: ~$32/month
- After Free Tier: ~$55/month

### 🎯 Key Variables

**Networking**:
- `enable_nat_gateway = false` - Use VPC endpoints
- `enable_flow_logs = false` - Save costs

**Database**:
- `db_instance_class = "db.t3.micro"` - Free Tier
- `enable_redis = false` - Disabled
- `enable_documentdb = false` - Disabled

**Compute**:
- `use_fargate_spot = true` - 70% savings
- `*_service_cpu = 256` - Minimal (0.25 vCPU)
- `*_service_memory = 512` - Minimal
- `enable_container_insights = false` - Save costs

**Monitoring**:
- `enable_alarms = false` - Use AWS Console
- `enable_dashboard = false` - Manual monitoring
- `enable_xray = false` - Save costs

### ⚠️ Manual Steps Required

**Before running terraform init/plan**:

1. **Create S3 Backend Bucket** (if not exists):
   ```bash
   aws s3 mb s3://turaf-terraform-state-dev --region us-east-1
   aws s3api put-bucket-versioning \
     --bucket turaf-terraform-state-dev \
     --versioning-configuration Status=Enabled
   ```

2. **Create DynamoDB Lock Table** (if not exists):
   ```bash
   aws dynamodb create-table \
     --table-name turaf-terraform-locks \
     --attribute-definitions AttributeName=LockID,AttributeType=S \
     --key-schema AttributeName=LockID,KeyType=HASH \
     --billing-mode PAY_PER_REQUEST \
     --region us-east-1
   ```

3. **Copy and configure terraform.tfvars**:
   ```bash
   cd infrastructure/terraform/environments/dev
   cp terraform.tfvars.example terraform.tfvars
   # Edit terraform.tfvars and update:
   # - acm_certificate_arn (from Task 005)
   # - identity_service_image (ECR URL from Task 011)
   # - organization_service_image (ECR URL)
   # - experiment_service_image (ECR URL)
   ```

4. **Copy backend configuration**:
   ```bash
   cp backend.tfvars.example backend.tfvars
   # Edit if using different bucket/table names
   ```

5. **Initialize Terraform**:
   ```bash
   terraform init
   ```

6. **Plan deployment**:
   ```bash
   terraform plan
   ```

**Note**: Do NOT run `terraform apply` yet. This is just configuration setup. Actual deployment should be done carefully after reviewing the plan.

## Testing Requirements

**Validation**:
- ✅ All module references correct
- ✅ Variable defaults set appropriately
- ✅ Cost optimization applied
- ⏳ `terraform init` (requires manual backend setup)
- ⏳ `terraform plan` (requires manual variable configuration)

**Manual Testing Steps**:
1. Set up S3 backend and DynamoDB table
2. Configure terraform.tfvars with actual values
3. Run `terraform init`
4. Run `terraform plan`
5. Review plan output for correctness
6. Verify estimated costs match expectations

## References

- Specification: `specs/terraform-structure.md` (Environments section)
- Module Documentation: `infrastructure/terraform/modules/*/README.md`
- Cost Estimation: `INFRASTRUCTURE_COSTS.md`
- Related Tasks: 023-configure-qa-environment, 024-configure-prod-environment
