# Database Module

Terraform module for creating RDS PostgreSQL (multi-schema), ElastiCache Redis, and DocumentDB for the Turaf platform.

## Features

- **RDS PostgreSQL**: Single database with multi-schema isolation per microservice
- **ElastiCache Redis**: In-memory caching and session storage
- **DocumentDB**: MongoDB-compatible document database
- **Schema Isolation**: Four isolated schemas for microservices
- **Secrets Management**: All credentials stored in AWS Secrets Manager
- **Encryption**: KMS encryption at rest for all databases
- **Automated Backups**: Configurable retention periods
- **High Availability**: Multi-AZ support for production

## Architecture

### Single Database, Multi-Schema Design

```
RDS PostgreSQL Instance (turaf)
├── identity_schema → identity_user
├── organization_schema → organization_user
├── experiment_schema → experiment_user
└── metrics_schema → metrics_user
```

**Benefits**:
- Cost-effective (one RDS instance vs. four)
- Simplified management and backups
- Schema-level isolation prevents cross-service data access
- Centralized monitoring and maintenance

**Security**:
- Each service has its own database user
- Users can only access their assigned schema
- No cross-schema foreign keys
- Search path configured per user

### Database Stack

**RDS PostgreSQL 15.4**:
- Primary relational database
- Multi-schema isolation
- Automated backups
- Performance Insights (optional)
- CloudWatch Logs integration

**ElastiCache Redis 7.0**:
- Session storage
- Caching layer
- Pub/Sub messaging
- TLS encryption in transit
- Auth token authentication

**DocumentDB 5.0**:
- MongoDB-compatible
- Flexible schema storage
- Automated backups
- Cluster support

## Usage

### Minimal Demo Configuration (Recommended)

```hcl
module "database" {
  source = "../../modules/database"

  environment            = "dev"
  database_subnet_ids    = module.networking.database_subnet_ids
  rds_security_group_id  = module.security.rds_security_group_id
  elasticache_security_group_id = module.security.elasticache_security_group_id
  documentdb_security_group_id  = module.security.documentdb_security_group_id
  kms_key_id             = module.security.kms_key_id
  rds_kms_key_arn        = module.security.rds_kms_key_arn

  # RDS PostgreSQL (Free Tier)
  db_instance_class      = "db.t3.micro"
  db_allocated_storage   = 20
  backup_retention_days  = 1
  enable_multi_az        = false
  deletion_protection    = false
  enable_performance_insights = false

  # Redis and DocumentDB disabled for cost savings
  enable_redis           = false
  enable_documentdb      = false

  tags = {
    Project   = "turaf"
    ManagedBy = "terraform"
    Environment = "dev"
  }
}
```

### Full Configuration (All Services Enabled)

```hcl
module "database" {
  source = "../../modules/database"

  environment            = "dev"
  database_subnet_ids    = module.networking.database_subnet_ids
  rds_security_group_id  = module.security.rds_security_group_id
  elasticache_security_group_id = module.security.elasticache_security_group_id
  documentdb_security_group_id  = module.security.documentdb_security_group_id
  kms_key_id             = module.security.kms_key_id
  rds_kms_key_arn        = module.security.rds_kms_key_arn

  # RDS PostgreSQL
  db_instance_class      = "db.t3.micro"
  db_allocated_storage   = 20
  backup_retention_days  = 7
  enable_multi_az        = false
  deletion_protection    = false

  # Enable Redis
  enable_redis           = true
  redis_node_type        = "cache.t3.micro"
  redis_num_cache_nodes  = 1

  # Enable DocumentDB
  enable_documentdb      = true
  documentdb_instance_class = "db.t3.medium"
  documentdb_instance_count = 1

  tags = {
    Project   = "turaf"
    ManagedBy = "terraform"
  }
}
```

## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|----------|
| environment | Environment name (dev, qa, prod) | string | - | yes |
| database_subnet_ids | List of subnet IDs for databases | list(string) | - | yes |
| rds_security_group_id | Security group ID for RDS | string | - | yes |
| elasticache_security_group_id | Security group ID for ElastiCache | string | - | yes |
| documentdb_security_group_id | Security group ID for DocumentDB | string | - | yes |
| kms_key_id | KMS key ID for encryption | string | - | yes |
| rds_kms_key_arn | KMS key ARN for RDS | string | - | yes |
| db_instance_class | RDS instance class | string | db.t3.micro | no |
| db_allocated_storage | RDS allocated storage (GB) | number | 20 | no |
| db_max_allocated_storage | RDS max storage (GB) | number | 100 | no |
| backup_retention_days | Backup retention days | number | 7 | no |
| enable_multi_az | Enable Multi-AZ for RDS | bool | false | no |
| deletion_protection | Enable deletion protection | bool | false | no |
| enable_performance_insights | Enable Performance Insights | bool | false | no |
| redis_node_type | Redis node type | string | cache.t3.micro | no |
| redis_num_cache_nodes | Number of Redis nodes | number | 1 | no |
| redis_snapshot_retention_days | Redis snapshot retention | number | 5 | no |
| documentdb_instance_class | DocumentDB instance class | string | db.t3.medium | no |
| documentdb_instance_count | Number of DocumentDB instances | number | 1 | no |
| documentdb_backup_retention_days | DocumentDB backup retention | number | 7 | no |

## Outputs

| Name | Description |
|------|-------------|
| rds_endpoint | RDS instance endpoint |
| rds_address | RDS instance address |
| rds_port | RDS instance port |
| redis_primary_endpoint | Redis primary endpoint |
| redis_reader_endpoint | Redis reader endpoint |
| documentdb_endpoint | DocumentDB cluster endpoint |
| db_admin_secret_arn | Admin password secret ARN |
| identity_user_secret_arn | Identity user secret ARN |
| organization_user_secret_arn | Organization user secret ARN |
| experiment_user_secret_arn | Experiment user secret ARN |
| metrics_user_secret_arn | Metrics user secret ARN |
| redis_auth_secret_arn | Redis auth token secret ARN |
| documentdb_secret_arn | DocumentDB password secret ARN |

## Schema Initialization

After Terraform creates the RDS instance, you must manually run the schema initialization script:

```bash
# Get admin password from Secrets Manager
ADMIN_PASSWORD=$(aws secretsmanager get-secret-value \
  --secret-id turaf/dev/db/admin-password \
  --query SecretString \
  --output text | jq -r '.password')

# Get RDS endpoint
RDS_ENDPOINT=$(terraform output -raw rds_endpoint | cut -d: -f1)

# Get service user passwords
IDENTITY_PASSWORD=$(aws secretsmanager get-secret-value \
  --secret-id turaf/dev/db/identity-user \
  --query SecretString \
  --output text | jq -r '.password')

ORGANIZATION_PASSWORD=$(aws secretsmanager get-secret-value \
  --secret-id turaf/dev/db/organization-user \
  --query SecretString \
  --output text | jq -r '.password')

EXPERIMENT_PASSWORD=$(aws secretsmanager get-secret-value \
  --secret-id turaf/dev/db/experiment-user \
  --query SecretString \
  --output text | jq -r '.password')

METRICS_PASSWORD=$(aws secretsmanager get-secret-value \
  --secret-id turaf/dev/db/metrics-user \
  --query SecretString \
  --output text | jq -r '.password')

# Run schema initialization
PGPASSWORD=$ADMIN_PASSWORD psql \
  -h $RDS_ENDPOINT \
  -U turaf_admin \
  -d turaf \
  -f infrastructure/terraform/modules/database/schema-init.sql \
  -v identity_password="$IDENTITY_PASSWORD" \
  -v organization_password="$ORGANIZATION_PASSWORD" \
  -v experiment_password="$EXPERIMENT_PASSWORD" \
  -v metrics_password="$METRICS_PASSWORD"
```

## Service Database Connections

Each microservice retrieves its credentials from Secrets Manager:

```typescript
// Example: Identity Service
const secret = await secretsManager.getSecretValue({
  SecretId: 'turaf/dev/db/identity-user'
}).promise();

const credentials = JSON.parse(secret.SecretString);

const pool = new Pool({
  host: credentials.host,
  port: credentials.port,
  database: credentials.database,
  user: credentials.username,
  password: credentials.password,
  // Search path is automatically set to identity_schema
});
```

## Cost Estimation

### Demo/Development Environment (Cost-Optimized)

**RDS PostgreSQL** (db.t3.micro, Free Tier):
- Instance: $0/month (Free Tier: 750 hours) or $12.41/month after
- Storage: $0/month (Free Tier: 20 GB) or $2.30/month after
- Backups: $0/month (Free Tier: 20 GB) or $0.48/month after
- **Subtotal**: ~$0/month (Free Tier) or ~$15/month

**ElastiCache Redis** (DISABLED by default):
- **Cost**: $0/month (disabled)
- **Alternative**: Use in-memory cache or local Redis container
- **To Enable**: Set `enable_redis = true` (+$12/month)

**DocumentDB** (DISABLED by default):
- **Cost**: $0/month (disabled)
- **Alternative**: Use PostgreSQL JSON columns for document storage
- **To Enable**: Set `enable_documentdb = true` (+$54/month)

**Secrets Manager**:
- 5 secrets × $0.40/month = $2.00/month

**Total Demo**: ~$2/month (with Free Tier) or ~$17/month (after Free Tier)
**Total with All Services**: ~$84/month

### Production Environment

**RDS PostgreSQL** (db.t3.small, Multi-AZ):
- Instance: $0.034/hour × 2 × 730 = $49.64/month
- Storage: 100 GB × $0.115/GB = $11.50/month
- Backups: ~50 GB × $0.095/GB = $4.75/month
- Performance Insights: $0.009/hour × 730 = $6.57/month
- **Subtotal**: ~$72/month

**ElastiCache Redis** (cache.t3.small, 2 nodes):
- Instances: $0.034/hour × 2 × 730 = $49.64/month
- **Subtotal**: ~$50/month

**DocumentDB** (db.r5.large, 2 instances):
- Instances: $0.277/hour × 2 × 730 = $404.44/month
- Storage: 50 GB × $0.10/GB = $5.00/month
- **Subtotal**: ~$409/month

**Secrets Manager**:
- 7 secrets × $0.40/month = $2.80/month

**Total Prod**: ~$534/month

## Security Best Practices

### Schema Isolation

- ✅ Each service has dedicated schema
- ✅ Service users cannot access other schemas
- ✅ No cross-schema foreign keys
- ✅ Search path configured per user
- ✅ Public schema access revoked

### Encryption

- ✅ RDS encrypted with KMS
- ✅ Redis TLS in transit
- ✅ Redis auth token required
- ✅ DocumentDB encrypted at rest
- ✅ All passwords in Secrets Manager

### Network Security

- ✅ Databases in private subnets
- ✅ No public access
- ✅ Security groups restrict access to ECS tasks only
- ✅ Subnet groups span multiple AZs

### Backup & Recovery

- ✅ Automated daily backups
- ✅ Configurable retention periods
- ✅ Point-in-time recovery (RDS)
- ✅ Snapshot backups (Redis, DocumentDB)
- ✅ Deletion protection for production

## Monitoring

### CloudWatch Metrics

**RDS**:
- CPUUtilization
- DatabaseConnections
- FreeStorageSpace
- ReadLatency / WriteLatency
- FreeableMemory

**ElastiCache**:
- CPUUtilization
- CurrConnections
- Evictions
- CacheHits / CacheMisses
- NetworkBytesIn / NetworkBytesOut

**DocumentDB**:
- CPUUtilization
- DatabaseConnections
- FreeableMemory
- ReadLatency / WriteLatency
- VolumeBytesUsed

### CloudWatch Logs

- RDS: PostgreSQL logs, upgrade logs
- DocumentDB: Audit logs, profiler logs

## Troubleshooting

### Issue: Cannot connect to RDS

**Problem**: Connection timeout

**Solutions**:
1. Verify security group allows port 5432 from ECS
2. Check RDS is in database subnets
3. Verify route tables
4. Check NACL rules
5. Ensure VPC has DNS resolution enabled

### Issue: Schema initialization fails

**Problem**: psql command fails

**Solutions**:
1. Verify admin password is correct
2. Check RDS endpoint is accessible
3. Ensure psql client is installed
4. Verify database name is "turaf"
5. Check user passwords are properly escaped

### Issue: Service cannot access schema

**Problem**: Permission denied on schema

**Solutions**:
1. Verify schema initialization ran successfully
2. Check user is connecting with correct credentials
3. Verify search path is set correctly
4. Ensure user has USAGE on schema
5. Check table ownership

### Issue: Redis connection refused

**Problem**: Cannot connect to Redis

**Solutions**:
1. Verify auth token is correct
2. Check TLS is enabled in client
3. Verify security group allows port 6379
4. Ensure Redis cluster is available
5. Check endpoint address is correct

## Maintenance

### Regular Tasks

**Daily**:
- Automated backups run at 03:00 UTC
- Monitor CloudWatch alarms

**Weekly**:
- Review slow query logs
- Check storage usage
- Monitor connection counts

**Monthly**:
- Review backup retention
- Analyze Performance Insights
- Update minor versions if available
- Review and optimize queries

**Quarterly**:
- Test backup restoration
- Review and update instance sizes
- Audit user permissions
- Update PostgreSQL extensions

## Migration Guide

### Adding a New Service Schema

1. Update `schema-init.sql`:
```sql
CREATE SCHEMA IF NOT EXISTS new_service_schema;
CREATE USER new_service_user WITH PASSWORD :'new_service_password';
GRANT ALL PRIVILEGES ON SCHEMA new_service_schema TO new_service_user;
ALTER USER new_service_user SET search_path TO new_service_schema;
```

2. Add password generation in `main.tf`:
```hcl
resource "random_password" "new_service_user" {
  length  = 32
  special = true
}
```

3. Add Secrets Manager secret:
```hcl
resource "aws_secretsmanager_secret" "new_service_user_password" {
  name = "turaf/${var.environment}/db/new-service-user"
}
```

4. Run schema initialization script

## References

- [RDS PostgreSQL Documentation](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/CHAP_PostgreSQL.html)
- [ElastiCache Redis Documentation](https://docs.aws.amazon.com/AmazonElastiCache/latest/red-ug/)
- [DocumentDB Documentation](https://docs.aws.amazon.com/documentdb/)
- [PostgreSQL Multi-Schema Design](https://www.postgresql.org/docs/current/ddl-schemas.html)
- ADR-006: Single Database Multi-Schema Architecture
