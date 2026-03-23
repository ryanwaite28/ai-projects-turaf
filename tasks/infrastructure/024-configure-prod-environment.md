# Task: Configure PROD Environment

**Service**: Infrastructure  
**Phase**: 10  
**Estimated Time**: 2 hours  

## Objective

Configure Terraform variables and settings specific to the PROD environment with high availability and security.

## Prerequisites

- [x] All infrastructure modules created
- [x] Task 010: DEV environment configured
- [x] Task 011: QA environment configured

## Scope

**Files to Create**:
- `infrastructure/terraform/environments/prod/terraform.tfvars`
- `infrastructure/terraform/environments/prod/backend.tfvars`

## Implementation Details

### PROD Environment Variables

```hcl
# terraform.tfvars
environment = "prod"

# Networking
vpc_cidr           = "10.2.0.0/16"
availability_zones = ["us-east-1a", "us-east-1b", "us-east-1c"]

# Compute
ecs_task_cpu    = "2048"
ecs_task_memory = "4096"
desired_count   = 3

# Database
db_instance_class = "db.r6g.large"
db_allocated_storage = 100
multi_az = true

# Lambda
lambda_memory_size = 1024

# Monitoring
log_retention_days = 30

# Security
enable_deletion_protection = true
backup_retention_period = 7

# Tags
tags = {
  Environment = "prod"
  Project     = "Turaf"
  ManagedBy   = "Terraform"
  Compliance  = "Required"
}
```

## Acceptance Criteria

- [ ] PROD environment variables configured
- [ ] High availability enabled
- [ ] Multi-AZ database configured
- [ ] Deletion protection enabled
- [ ] Backup retention configured
- [ ] terraform plan succeeds for prod

## Testing Requirements

**Validation**:
- Run `terraform init -backend-config=environments/prod/backend.tfvars`
- Run `terraform plan -var-file=environments/prod/terraform.tfvars`
- Verify HA configuration
- Check security settings

## References

- Specification: `specs/terraform-structure.md` (Environments section)
- Specification: `specs/aws-infrastructure.md`
- Related Tasks: All infrastructure tasks
