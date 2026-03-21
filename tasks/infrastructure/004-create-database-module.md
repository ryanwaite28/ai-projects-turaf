# Task: Create Database Module

**Service**: Infrastructure  
**Phase**: 10  
**Estimated Time**: 4 hours  

## Objective

Create Terraform module for single RDS PostgreSQL database with multi-schema isolation per microservice.

## Prerequisites

- [x] Task 002: Networking module created

## Scope

**Files to Create**:
- `infrastructure/terraform/modules/database/main.tf`
- `infrastructure/terraform/modules/database/variables.tf`
- `infrastructure/terraform/modules/database/outputs.tf`
- `infrastructure/terraform/modules/database/schema-init.sql` (template)

## Implementation Details

### Architecture

**Single Database, Multi-Schema Design**:
- One RDS PostgreSQL instance per environment
- Four isolated schemas: `identity_schema`, `organization_schema`, `experiment_schema`, `metrics_schema`
- One database user per service with schema-scoped permissions
- No cross-schema foreign keys or references

### RDS PostgreSQL Instance

```hcl
resource "aws_db_subnet_group" "main" {
  name       = "turaf-db-subnet-group-${var.environment}"
  subnet_ids = var.database_subnet_ids
  
  tags = {
    Name = "turaf-db-subnet-group-${var.environment}"
  }
}

resource "aws_db_instance" "postgres" {
  identifier     = "turaf-db-${var.environment}"
  engine         = "postgres"
  engine_version = "15.3"
  instance_class = var.db_instance_class
  
  allocated_storage     = var.db_allocated_storage
  max_allocated_storage = var.db_max_allocated_storage
  storage_type          = "gp3"
  storage_encrypted     = true
  kms_key_id            = aws_kms_key.rds.arn
  
  db_name  = "turaf"
  username = "turaf_admin"
  password = random_password.admin_password.result
  
  multi_az               = var.environment == "prod"
  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.database.id]
  
  backup_retention_period = var.backup_retention_days
  backup_window           = "03:00-04:00"
  maintenance_window      = "mon:04:00-mon:05:00"
  
  enabled_cloudwatch_logs_exports = ["postgresql", "upgrade"]
  
  deletion_protection = var.environment == "prod"
  skip_final_snapshot = var.environment != "prod"
  
  tags = {
    Name        = "turaf-db-${var.environment}"
    Environment = var.environment
  }
}
```

### Schema and User Creation

```hcl
# Generate passwords for service users
resource "random_password" "admin_password" {
  length  = 32
  special = true
}

resource "random_password" "identity_user" {
  length  = 32
  special = true
}

resource "random_password" "organization_user" {
  length  = 32
  special = true
}

resource "random_password" "experiment_user" {
  length  = 32
  special = true
}

resource "random_password" "metrics_user" {
  length  = 32
  special = true
}

# Initialize schemas and users
resource "null_resource" "database_schemas" {
  depends_on = [aws_db_instance.postgres]
  
  provisioner "local-exec" {
    command = <<-EOT
      PGPASSWORD=${random_password.admin_password.result} psql \
        -h ${aws_db_instance.postgres.endpoint} \
        -U turaf_admin \
        -d turaf \
        -f ${path.module}/schema-init.sql \
        -v identity_password='${random_password.identity_user.result}' \
        -v organization_password='${random_password.organization_user.result}' \
        -v experiment_password='${random_password.experiment_user.result}' \
        -v metrics_password='${random_password.metrics_user.result}'
    EOT
  }
}
```

### Schema Initialization Script

**File**: `schema-init.sql`
```sql
-- Create schemas
CREATE SCHEMA IF NOT EXISTS identity_schema;
CREATE SCHEMA IF NOT EXISTS organization_schema;
CREATE SCHEMA IF NOT EXISTS experiment_schema;
CREATE SCHEMA IF NOT EXISTS metrics_schema;

-- Create users
CREATE USER identity_user WITH PASSWORD :'identity_password';
CREATE USER organization_user WITH PASSWORD :'organization_password';
CREATE USER experiment_user WITH PASSWORD :'experiment_password';
CREATE USER metrics_user WITH PASSWORD :'metrics_password';

-- Grant schema permissions
GRANT ALL PRIVILEGES ON SCHEMA identity_schema TO identity_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA identity_schema TO identity_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA identity_schema GRANT ALL ON TABLES TO identity_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA identity_schema GRANT ALL ON SEQUENCES TO identity_user;

GRANT ALL PRIVILEGES ON SCHEMA organization_schema TO organization_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA organization_schema TO organization_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA organization_schema GRANT ALL ON TABLES TO organization_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA organization_schema GRANT ALL ON SEQUENCES TO organization_user;

GRANT ALL PRIVILEGES ON SCHEMA experiment_schema TO experiment_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA experiment_schema TO experiment_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA experiment_schema GRANT ALL ON TABLES TO experiment_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA experiment_schema GRANT ALL ON SEQUENCES TO experiment_user;

GRANT ALL PRIVILEGES ON SCHEMA metrics_schema TO metrics_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA metrics_schema TO metrics_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA metrics_schema GRANT ALL ON TABLES TO metrics_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA metrics_schema GRANT ALL ON SEQUENCES TO metrics_user;
```

### Secrets Management

```hcl
# Store admin password
resource "aws_secretsmanager_secret" "admin_password" {
  name = "turaf/db/admin-password-${var.environment}"
}

resource "aws_secretsmanager_secret_version" "admin_password" {
  secret_id     = aws_secretsmanager_secret.admin_password.id
  secret_string = random_password.admin_password.result
}

# Store service user passwords
resource "aws_secretsmanager_secret" "identity_user_password" {
  name = "turaf/db/identity-user-password-${var.environment}"
}

resource "aws_secretsmanager_secret_version" "identity_user_password" {
  secret_id     = aws_secretsmanager_secret.identity_user_password.id
  secret_string = random_password.identity_user.result
}

resource "aws_secretsmanager_secret" "organization_user_password" {
  name = "turaf/db/organization-user-password-${var.environment}"
}

resource "aws_secretsmanager_secret_version" "organization_user_password" {
  secret_id     = aws_secretsmanager_secret.organization_user_password.id
  secret_string = random_password.organization_user.result
}

resource "aws_secretsmanager_secret" "experiment_user_password" {
  name = "turaf/db/experiment-user-password-${var.environment}"
}

resource "aws_secretsmanager_secret_version" "experiment_user_password" {
  secret_id     = aws_secretsmanager_secret.experiment_user_password.id
  secret_string = random_password.experiment_user.result
}

resource "aws_secretsmanager_secret" "metrics_user_password" {
  name = "turaf/db/metrics-user-password-${var.environment}"
}

resource "aws_secretsmanager_secret_version" "metrics_user_password" {
  secret_id     = aws_secretsmanager_secret.metrics_user_password.id
  secret_string = random_password.metrics_user.result
}
```

## Acceptance Criteria

- [ ] Single RDS instance created per environment
- [ ] Four schemas created (identity, organization, experiment, metrics)
- [ ] Four service users created with schema-scoped permissions
- [ ] Database subnet group configured
- [ ] Security groups configured
- [ ] All passwords stored in Secrets Manager
- [ ] Backup configuration set
- [ ] KMS encryption enabled
- [ ] terraform plan succeeds
- [ ] Schema initialization script executes successfully

## Testing Requirements

**Validation**:
- Run `terraform plan`
- Verify database configurations
- Check security group rules

## References

- Specification: `specs/aws-infrastructure.md` (Database section)
- ADR: `docs/adr/ADR-006-single-database-multi-schema.md`
- PROJECT.md: Section 27 (Data Architecture)
- Related Tasks: 005-create-storage-modules, 013-setup-database-schemas
