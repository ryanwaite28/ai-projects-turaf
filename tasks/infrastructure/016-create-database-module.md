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

- [x] Single RDS instance created per environment
- [x] Four schemas created (identity, organization, experiment, metrics)
- [x] Four service users created with schema-scoped permissions
- [x] Database subnet group configured
- [x] ElastiCache Redis cluster created
- [x] DocumentDB cluster created
- [x] All passwords stored in Secrets Manager
- [x] Backup configuration set
- [x] KMS encryption enabled
- [x] Schema initialization script created
- [x] Module documentation created
- [ ] terraform plan succeeds (requires environment configuration)
- [ ] Schema initialization script executed (manual step after apply)

## Testing Requirements

**Validation**:
- Run `terraform plan`
- Verify database configurations
- Check security group rules

## Implementation Results (2024-03-23)

### ✅ Module Created

**Files Created**:
- ✅ `infrastructure/terraform/modules/database/main.tf` (450 lines)
- ✅ `infrastructure/terraform/modules/database/variables.tf` (130 lines)
- ✅ `infrastructure/terraform/modules/database/outputs.tf` (100 lines)
- ✅ `infrastructure/terraform/modules/database/schema-init.sql` (schema initialization script)
- ✅ `infrastructure/terraform/modules/database/README.md` (comprehensive documentation)

### 🗄️ Database Components

**RDS PostgreSQL 15.4**:
- Single instance with multi-schema design
- Four isolated schemas: `identity_schema`, `organization_schema`, `experiment_schema`, `metrics_schema`
- Four service users with schema-scoped permissions
- Automated backups with configurable retention
- KMS encryption at rest
- Performance Insights support (optional)
- Multi-AZ support for production

**ElastiCache Redis 7.0**:
- In-memory caching and session storage
- TLS encryption in transit
- Auth token authentication
- Automated snapshots
- Multi-node support for HA

**DocumentDB 5.0**:
- MongoDB-compatible document database
- Cluster support with multiple instances
- Automated backups
- KMS encryption at rest
- CloudWatch Logs integration

### 📊 Features

- ✅ **Multi-Schema Isolation**: One database, four isolated schemas
- ✅ **Cost Optimization**: Single RDS instance vs. four separate instances
- ✅ **Security**: Schema-level permissions, no cross-schema access
- ✅ **Secrets Management**: All credentials in AWS Secrets Manager
- ✅ **Encryption**: KMS encryption for all databases
- ✅ **High Availability**: Multi-AZ support for production
- ✅ **Automated Backups**: Configurable retention periods
- ✅ **Monitoring**: CloudWatch Logs and metrics integration

### 🔐 Schema Isolation

**Identity Schema** → `identity_user`:
- User authentication and authorization data
- Search path: `identity_schema`

**Organization Schema** → `organization_user`:
- Organization and team management data
- Search path: `organization_schema`

**Experiment Schema** → `experiment_user`:
- Experiment tracking and results data
- Search path: `experiment_schema`

**Metrics Schema** → `metrics_user`:
- Metrics collection and analysis data
- Search path: `metrics_schema`

### 💰 Cost Estimation

**Demo/Development** (Cost-Optimized, ~$2-17/month):
- RDS (db.t3.micro, Free Tier): $0 or ~$15/month
- Redis: $0 (disabled by default)
- DocumentDB: $0 (disabled by default)
- Secrets Manager (5 secrets): ~$2/month

**Development** (All Services, ~$84/month):
- RDS (db.t3.micro): ~$15/month
- Redis (cache.t3.micro): ~$12/month
- DocumentDB (db.t3.medium): ~$54/month
- Secrets Manager: ~$3/month

**Production** (~$534/month):
- RDS (db.t3.small, Multi-AZ): ~$72/month
- Redis (cache.t3.small, 2 nodes): ~$50/month
- DocumentDB (db.r5.large, 2 instances): ~$409/month
- Secrets Manager: ~$3/month

**Cost Optimization for Demo**:
- ✅ Redis disabled by default (save $12/month)
- ✅ DocumentDB disabled by default (save $54/month)
- ✅ RDS Free Tier eligible (save $15/month for 12 months)
- ✅ Minimal backup retention (1 day vs 7 days)
- ✅ Performance Insights disabled (save $7/month)

### 🎯 Module Inputs

| Variable | Description | Default |
|----------|-------------|---------|
| db_instance_class | RDS instance class | db.t3.micro |
| enable_multi_az | Enable Multi-AZ | false |
| deletion_protection | Enable deletion protection | false |
| redis_node_type | Redis node type | cache.t3.micro |
| redis_num_cache_nodes | Number of Redis nodes | 1 |
| documentdb_instance_class | DocumentDB instance class | db.t3.medium |
| documentdb_instance_count | Number of DocumentDB instances | 1 |

### 📤 Module Outputs

- RDS endpoint, address, port
- Redis primary and reader endpoints
- DocumentDB cluster endpoint
- Secret ARNs for all credentials
- Schema names and service user mappings

### ⚠️ Manual Steps Required

After `terraform apply`, you must run the schema initialization script:

```bash
# Get credentials from Secrets Manager
ADMIN_PASSWORD=$(aws secretsmanager get-secret-value \
  --secret-id turaf/dev/db/admin-password \
  --query SecretString --output text | jq -r '.password')

# Get service user passwords
# ... (see README for full script)

# Run schema initialization
PGPASSWORD=$ADMIN_PASSWORD psql \
  -h <RDS_ENDPOINT> \
  -U turaf_admin \
  -d turaf \
  -f infrastructure/terraform/modules/database/schema-init.sql \
  -v identity_password="$IDENTITY_PASSWORD" \
  -v organization_password="$ORGANIZATION_PASSWORD" \
  -v experiment_password="$EXPERIMENT_PASSWORD" \
  -v metrics_password="$METRICS_PASSWORD"
```

### 📋 Next Steps

1. Create storage modules (Task 017)
2. Create messaging modules (Task 018)
3. Create compute modules (Task 019)
4. Configure dev environment (Task 022)
5. Run schema initialization after first apply

## References

- Specification: `specs/aws-infrastructure.md` (Database section)
- ADR: `docs/adr/ADR-006-single-database-multi-schema.md`
- PROJECT.md: Section 27 (Data Architecture)
- Related Tasks: 017-create-storage-modules, 022-configure-dev-environment
- Module Documentation: `infrastructure/terraform/modules/database/README.md`
