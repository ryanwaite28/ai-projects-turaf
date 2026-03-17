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

- [ ] DEV environment variables configured
- [ ] Backend configuration set
- [ ] Resource sizing appropriate for dev
- [ ] Cost optimization applied
- [ ] terraform plan succeeds for dev

## Testing Requirements

**Validation**:
- Run `terraform init -backend-config=environments/dev/backend.tfvars`
- Run `terraform plan -var-file=environments/dev/terraform.tfvars`
- Verify resource configurations

## References

- Specification: `specs/terraform-structure.md` (Environments section)
- Related Tasks: 011-configure-qa-environment
