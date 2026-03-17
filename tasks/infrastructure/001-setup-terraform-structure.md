# Task: Setup Terraform Structure

**Service**: Infrastructure  
**Phase**: 10  
**Estimated Time**: 3 hours  

## Objective

Setup Terraform project structure with modules, environments, and state management configuration.

## Prerequisites

- [ ] Terraform installed (v1.5+)
- [ ] AWS CLI configured
- [ ] Understanding of Terraform best practices

## Scope

**Files to Create**:
- `infrastructure/terraform/main.tf`
- `infrastructure/terraform/variables.tf`
- `infrastructure/terraform/outputs.tf`
- `infrastructure/terraform/backend.tf`
- `infrastructure/terraform/versions.tf`
- `infrastructure/terraform/environments/dev/terraform.tfvars`
- `infrastructure/terraform/environments/qa/terraform.tfvars`
- `infrastructure/terraform/environments/prod/terraform.tfvars`

## Implementation Details

### Backend Configuration

```hcl
# backend.tf
terraform {
  backend "s3" {
    bucket         = "turaf-terraform-state"
    key            = "turaf/terraform.tfstate"
    region         = "us-east-1"
    encrypt        = true
    dynamodb_table = "turaf-terraform-locks"
  }
}
```

### Main Configuration

```hcl
# main.tf
module "networking" {
  source = "./modules/networking"
  
  environment = var.environment
  vpc_cidr    = var.vpc_cidr
}

module "compute" {
  source = "./modules/compute"
  
  environment = var.environment
  vpc_id      = module.networking.vpc_id
  subnet_ids  = module.networking.private_subnet_ids
}

module "database" {
  source = "./modules/database"
  
  environment = var.environment
  vpc_id      = module.networking.vpc_id
  subnet_ids  = module.networking.database_subnet_ids
}
```

### Directory Structure

```
infrastructure/terraform/
├── main.tf
├── variables.tf
├── outputs.tf
├── backend.tf
├── versions.tf
├── modules/
│   ├── networking/
│   ├── compute/
│   ├── database/
│   ├── storage/
│   ├── messaging/
│   ├── lambda/
│   ├── security/
│   └── monitoring/
└── environments/
    ├── dev/
    ├── qa/
    └── prod/
```

## Acceptance Criteria

- [ ] Terraform directory structure created
- [ ] Backend configuration for S3 state storage
- [ ] Environment-specific tfvars files created
- [ ] Module structure established
- [ ] Version constraints defined
- [ ] terraform init succeeds

## Testing Requirements

**Validation**:
- Run `terraform init`
- Run `terraform validate`
- Run `terraform plan` (dry run)

## References

- Specification: `specs/terraform-structure.md`
- Specification: `specs/aws-infrastructure.md`
- PROJECT.md: Section 51 (Terraform Structure)
- Related Tasks: 002-create-networking-module
