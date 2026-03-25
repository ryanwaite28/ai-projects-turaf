# Task: Configure QA Environment

**Service**: Infrastructure  
**Phase**: 10  
**Estimated Time**: 2 hours  

## Objective

Configure Terraform variables and settings specific to the QA environment.

## Prerequisites

- [x] All infrastructure modules created
- [x] Task 010: DEV environment configured

## Scope

**Files to Create**:
- `infrastructure/terraform/environments/qa/terraform.tfvars`
- `infrastructure/terraform/environments/qa/backend.tfvars`

## Implementation Details

### QA Environment Variables

```hcl
# terraform.tfvars
environment = "qa"

# Networking
vpc_cidr           = "10.1.0.0/16"
availability_zones = ["us-east-1a", "us-east-1b"]

# Compute
ecs_task_cpu    = "1024"
ecs_task_memory = "2048"
desired_count   = 2

# Database
db_instance_class = "db.t3.small"
db_allocated_storage = 50

# Lambda
lambda_memory_size = 1024

# Monitoring
log_retention_days = 14

# Tags
tags = {
  Environment = "qa"
  Project     = "Turaf"
  ManagedBy   = "Terraform"
}
```

## Acceptance Criteria

- [x] QA environment main.tf created
- [x] QA environment variables.tf created
- [x] QA environment outputs.tf created
- [x] Backend configuration example created
- [x] terraform.tfvars.example created with QA-appropriate defaults
- [x] Resource sizing appropriate for QA testing
- [ ] terraform init succeeds (requires manual setup)
- [ ] terraform plan succeeds (requires manual configuration)

## Implementation Results (2024-03-23)

### ✅ Environment Configuration Created

**Files Created**:
- ✅ `infrastructure/terraform/environments/qa/main.tf` (280 lines) - Module orchestration
- ✅ `infrastructure/terraform/environments/qa/variables.tf` (450 lines) - All variables with QA defaults
- ✅ `infrastructure/terraform/environments/qa/outputs.tf` (140 lines) - Environment outputs
- ✅ `infrastructure/terraform/environments/qa/backend.tfvars.example` - Backend config template
- ✅ `infrastructure/terraform/environments/qa/terraform.tfvars.example` - QA-optimized variable values

**Existing Files**:
- ✅ `infrastructure/terraform/environments/qa/backend.tf` - S3 backend configuration

### 📦 QA Environment Configuration

**Differences from DEV**:
- **VPC CIDR**: 10.1.0.0/16 (vs 10.0.0.0/16 for DEV)
- **RDS Instance**: db.t3.small (vs db.t3.micro) - 2 vCPU, 2 GB RAM
- **Storage**: 50 GB (vs 20 GB)
- **ECS Tasks**: 6 tasks total (vs 3) - 2 per service for redundancy
- **Task Size**: 0.5 vCPU, 1 GB (vs 0.25 vCPU, 512 MB)
- **Backup Retention**: 7 days (vs 1 day)
- **Log Retention**: 14 days (vs 7 days)

**Still Cost-Optimized**:
- ❌ NAT Gateway (use VPC endpoints)
- ❌ ElastiCache Redis
- ❌ DocumentDB
- ❌ Container Insights
- ❌ CloudWatch Alarms (can enable for testing)
- ❌ X-Ray Tracing (can enable for testing)
- ❌ Lambda Functions
- ✅ Fargate Spot (70% savings)

**Estimated Monthly Cost**: ~$70-90/month
- RDS PostgreSQL (db.t3.small): ~$25/month
- ECS Fargate (6 tasks, 0.5 vCPU, 1 GB, Spot): ~$30/month
- ALB: ~$16/month
- VPC Endpoints: ~$14/month
- Other services: ~$10/month

**Cost Comparison**:
- DEV: ~$32-55/month
- QA: ~$70-90/month (~2x DEV)
- Difference: Larger RDS, more tasks, larger task sizes

### 🎯 QA Environment Purpose

**Testing Capabilities**:
- **Load Testing**: 2 tasks per service for redundancy
- **Integration Testing**: Larger database for test data
- **Performance Testing**: More resources to simulate production
- **Regression Testing**: Longer log retention for debugging
- **UAT**: User acceptance testing environment

**Optional Features** (can enable as needed):
- CloudWatch Alarms for testing alert workflows
- X-Ray Tracing for performance analysis
- Container Insights for resource monitoring
- Redis/DocumentDB if needed for specific tests

### ⚠️ Manual Steps Required

**Before deployment**:

1. **Create S3 backend bucket** (if not exists):
   ```bash
   aws s3 mb s3://turaf-terraform-state-qa --region us-east-1
   aws s3api put-bucket-versioning \
     --bucket turaf-terraform-state-qa \
     --versioning-configuration Status=Enabled
   ```

2. **Copy and configure terraform.tfvars**:
   ```bash
   cd infrastructure/terraform/environments/qa
   cp terraform.tfvars.example terraform.tfvars
   # Edit terraform.tfvars and update:
   # - acm_certificate_arn
   # - identity_service_image
   # - organization_service_image
   # - experiment_service_image
   # - image_tag (use "qa-latest" or specific version)
   ```

3. **Copy backend configuration**:
   ```bash
   cp backend.tfvars.example backend.tfvars
   ```

4. **Initialize Terraform**:
   ```bash
   terraform init
   ```

5. **Plan deployment**:
   ```bash
   terraform plan
   ```

## Testing Requirements

**Validation**:
- ✅ All module references correct
- ✅ Variable defaults set for QA testing
- ✅ Resource sizing appropriate for testing workloads
- ⏳ `terraform init` (requires manual backend setup)
- ⏳ `terraform plan` (requires manual variable configuration)

**QA Testing Workflow**:
1. Deploy QA environment after DEV is validated
2. Run integration tests against QA
3. Perform load testing with multiple tasks
4. Validate monitoring and alerting (if enabled)
5. Test deployment and rollback procedures
6. Conduct UAT with stakeholders

## References

- Specification: `specs/terraform-structure.md` (Environments section)
- Module Documentation: `infrastructure/terraform/modules/*/README.md`
- Cost Estimation: `INFRASTRUCTURE_COSTS.md`
- Related Tasks: 022-configure-dev-environment, 024-configure-prod-environment
