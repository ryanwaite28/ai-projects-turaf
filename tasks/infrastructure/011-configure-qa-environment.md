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

- [ ] QA environment variables configured
- [ ] Backend configuration set
- [ ] Resource sizing appropriate for qa
- [ ] terraform plan succeeds for qa

## Testing Requirements

**Validation**:
- Run `terraform init -backend-config=environments/qa/backend.tfvars`
- Run `terraform plan -var-file=environments/qa/terraform.tfvars`

## References

- Specification: `specs/terraform-structure.md` (Environments section)
- Related Tasks: 012-configure-prod-environment
