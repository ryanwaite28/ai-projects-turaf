# Task: Create Database Module

**Service**: Infrastructure  
**Phase**: 10  
**Estimated Time**: 3 hours  

## Objective

Create Terraform module for RDS PostgreSQL databases for each microservice.

## Prerequisites

- [x] Task 002: Networking module created

## Scope

**Files to Create**:
- `infrastructure/terraform/modules/database/main.tf`
- `infrastructure/terraform/modules/database/variables.tf`
- `infrastructure/terraform/modules/database/outputs.tf`

## Implementation Details

### RDS PostgreSQL

```hcl
resource "aws_db_subnet_group" "main" {
  name       = "turaf-db-subnet-group-${var.environment}"
  subnet_ids = var.database_subnet_ids
  
  tags = {
    Name = "turaf-db-subnet-group-${var.environment}"
  }
}

resource "aws_db_instance" "identity_service" {
  identifier     = "identity-service-${var.environment}"
  engine         = "postgres"
  engine_version = "15.3"
  instance_class = var.db_instance_class
  
  allocated_storage     = 20
  max_allocated_storage = 100
  storage_encrypted     = true
  
  db_name  = "identitydb"
  username = "admin"
  password = random_password.db_password.result
  
  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.database.id]
  
  backup_retention_period = var.environment == "prod" ? 7 : 1
  backup_window           = "03:00-04:00"
  maintenance_window      = "mon:04:00-mon:05:00"
  
  skip_final_snapshot = var.environment != "prod"
  
  tags = {
    Name        = "identity-service-db-${var.environment}"
    Environment = var.environment
  }
}

resource "random_password" "db_password" {
  length  = 32
  special = true
}

resource "aws_secretsmanager_secret" "db_password" {
  name = "identity-service-db-password-${var.environment}"
}

resource "aws_secretsmanager_secret_version" "db_password" {
  secret_id     = aws_secretsmanager_secret.db_password.id
  secret_string = random_password.db_password.result
}
```

## Acceptance Criteria

- [ ] RDS instances created for each service
- [ ] Database subnet group configured
- [ ] Security groups configured
- [ ] Passwords stored in Secrets Manager
- [ ] Backup configuration set
- [ ] terraform plan succeeds

## Testing Requirements

**Validation**:
- Run `terraform plan`
- Verify database configurations
- Check security group rules

## References

- Specification: `specs/aws-infrastructure.md` (Database section)
- Related Tasks: 005-create-storage-modules
