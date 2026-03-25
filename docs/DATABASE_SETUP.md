# Database Setup Guide

## Architecture

The Turaf platform uses a **single PostgreSQL database with multi-schema isolation** per microservice.

### Database Structure

- **Instance**: Single RDS PostgreSQL instance per environment
- **Database**: `turaf`
- **Schemas**:
  - `identity_schema` - User authentication and authorization
  - `organization_schema` - Organization and membership management
  - `experiment_schema` - Problems, hypotheses, and experiments
  - `metrics_schema` - Metrics and aggregations
  - `communications_schema` - Conversations, messages, and participants

### Users and Permissions

Each service has its own database user with schema-scoped permissions:

| User | Schema | Permissions |
|------|--------|-------------|
| `identity_user` | `identity_schema` | Full access (CREATE, SELECT, INSERT, UPDATE, DELETE) |
| `organization_user` | `organization_schema` | Full access |
| `experiment_user` | `experiment_schema` | Full access |
| `metrics_user` | `metrics_schema` | Full access |
| `communications_user` | `communications_schema` | Full access |

**Isolation**: Users cannot access other schemas (enforced by PostgreSQL permissions).

---

## Migration Management

### Centralized Flyway Service

Database migrations are managed centrally through the `flyway-service`:

- **Location**: All migrations in `services/flyway-service/migrations/`
- **Execution**: Via AWS CodeBuild triggered by GitHub Actions
- **Timing**: Migrations run **before** service deployments
- **History**: Tracked in `public.flyway_schema_history` table
- **Schema Targeting**: Each migration uses `SET search_path` to target specific schema

**Migration Naming Convention**: `V{NNN}__{service}_{description}.sql`

Examples:
- `V001__identity_create_users_table.sql`
- `V002__identity_create_refresh_tokens_table.sql`
- `V003__organization_create_organizations_table.sql`

See `specs/flyway-service.md` for complete documentation.

---

## Local Development Setup

### Prerequisites

- PostgreSQL 15.3+ installed
- `psql` command-line tool
- Docker (for running Flyway migrations locally)

### Step 1: Create Local Database

```bash
createdb turaf
```

### Step 2: Run Flyway Migrations

**Option A: Using Docker (Recommended)**

```bash
cd services/flyway-service

# Set environment variables
export DB_HOST=localhost
export DB_NAME=turaf
export DB_USER=postgres
export DB_PASSWORD=your-postgres-password

# Run migrations
./scripts/run-migrations.sh
```

**Option B: Using Flyway CLI**

```bash
cd services/flyway-service

# Download Flyway CLI if not installed
# https://flywaydb.org/documentation/usage/commandline/

# Configure flyway.conf with local database connection
# Run migrations
flyway migrate
```

### Step 3: Create Database Users

Set user passwords as environment variables:

```bash
export IDENTITY_PASSWORD="your-identity-password"
export ORGANIZATION_PASSWORD="your-organization-password"
export EXPERIMENT_PASSWORD="your-experiment-password"
export METRICS_PASSWORD="your-metrics-password"
export COMMUNICATIONS_PASSWORD="your-communications-password"
```

Run user creation script:

```bash
./infrastructure/scripts/create-db-users.sh localhost 5432 turaf postgres <postgres-password>
```

### Step 4: Validate Setup

```bash
./infrastructure/scripts/validate-db-permissions.sh localhost 5432 turaf postgres <postgres-password>
```

### Step 5: Test Schema Isolation

```bash
psql -h localhost -p 5432 -U postgres -d turaf -f infrastructure/scripts/test-schema-isolation.sql
```

---

## Service Configuration

Each service connects to its own schema using the `currentSchema` parameter:

### identity-service

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/turaf?currentSchema=identity_schema
    username: identity_user
    password: ${IDENTITY_PASSWORD}
```

### organization-service

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/turaf?currentSchema=organization_schema
    username: organization_user
    password: ${ORGANIZATION_PASSWORD}
```

### experiment-service

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/turaf?currentSchema=experiment_schema
    username: experiment_user
    password: ${EXPERIMENT_PASSWORD}
```

### metrics-service

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/turaf?currentSchema=metrics_schema
    username: metrics_user
    password: ${METRICS_PASSWORD}
```

### communications-service

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/turaf?currentSchema=communications_schema
    username: communications_user
    password: ${COMMUNICATIONS_PASSWORD}
```

---

## AWS Deployment

### Prerequisites

1. RDS PostgreSQL instance deployed (via Terraform)
2. Flyway IAM roles created (Task 026)
3. CodeBuild security groups configured (Task 027)
4. CodeBuild migration projects created (Task 028)

### Deployment Flow

```
1. Push migration script to services/flyway-service/migrations/
   ↓
2. GitHub Actions detects change
   ↓
3. Workflow assumes GitHubActionsFlywayRole (OIDC)
   ↓
4. Triggers AWS CodeBuild project
   ↓
5. CodeBuild executes Flyway migrations
   ↓
6. Migrations applied to RDS PostgreSQL
   ↓
7. Run create-db-users.sh on RDS (one-time setup)
   ↓
8. Services deploy and connect using service users
```

### One-Time RDS Setup

After Flyway migrations complete, create database users:

```bash
# Get RDS endpoint
RDS_ENDPOINT=$(aws rds describe-db-instances \
  --db-instance-identifier turaf-db-dev \
  --query 'DBInstances[0].Endpoint.Address' \
  --output text)

# Set passwords (store in AWS Secrets Manager)
export IDENTITY_PASSWORD="$(aws secretsmanager get-secret-value --secret-id turaf/db/identity-user --query SecretString --output text)"
export ORGANIZATION_PASSWORD="$(aws secretsmanager get-secret-value --secret-id turaf/db/organization-user --query SecretString --output text)"
export EXPERIMENT_PASSWORD="$(aws secretsmanager get-secret-value --secret-id turaf/db/experiment-user --query SecretString --output text)"
export METRICS_PASSWORD="$(aws secretsmanager get-secret-value --secret-id turaf/db/metrics-user --query SecretString --output text)"
export COMMUNICATIONS_PASSWORD="$(aws secretsmanager get-secret-value --secret-id turaf/db/communications-user --query SecretString --output text)"

# Get master password
MASTER_PASSWORD=$(aws secretsmanager get-secret-value \
  --secret-id turaf/db/master \
  --query SecretString \
  --output text)

# Create users
./infrastructure/scripts/create-db-users.sh $RDS_ENDPOINT 5432 turaf postgres $MASTER_PASSWORD

# Validate
./infrastructure/scripts/validate-db-permissions.sh $RDS_ENDPOINT 5432 turaf postgres $MASTER_PASSWORD
```

---

## Troubleshooting

### Check schemas exist

```sql
SELECT schema_name 
FROM information_schema.schemata 
WHERE schema_name LIKE '%_schema'
ORDER BY schema_name;
```

### Check users exist

```sql
SELECT usename 
FROM pg_user 
WHERE usename LIKE '%_user'
ORDER BY usename;
```

### Check user permissions

```sql
SELECT 
  n.nspname as schema_name,
  r.rolname as user_name,
  has_schema_privilege(r.rolname, n.nspname, 'USAGE') as has_usage,
  has_schema_privilege(r.rolname, n.nspname, 'CREATE') as has_create
FROM pg_namespace n
CROSS JOIN pg_roles r
WHERE n.nspname LIKE '%_schema'
  AND r.rolname LIKE '%_user';
```

### Check Flyway migration history

```sql
SELECT 
  installed_rank,
  version,
  description,
  installed_on,
  success
FROM public.flyway_schema_history
ORDER BY installed_rank DESC
LIMIT 20;
```

### Check tables per schema

```sql
SELECT 
  schemaname,
  tablename
FROM pg_tables
WHERE schemaname LIKE '%_schema'
ORDER BY schemaname, tablename;
```

### Connection test

```bash
# Test connection as service user
PGPASSWORD=$IDENTITY_PASSWORD psql \
  -h localhost \
  -p 5432 \
  -U identity_user \
  -d turaf \
  -c "SET search_path TO identity_schema; SELECT * FROM users LIMIT 1;"
```

### Reset schema (CAUTION: Deletes all data)

```sql
-- Drop all schemas
DROP SCHEMA IF EXISTS identity_schema CASCADE;
DROP SCHEMA IF EXISTS organization_schema CASCADE;
DROP SCHEMA IF EXISTS experiment_schema CASCADE;
DROP SCHEMA IF EXISTS metrics_schema CASCADE;
DROP SCHEMA IF EXISTS communications_schema CASCADE;

-- Drop all users
DROP USER IF EXISTS identity_user;
DROP USER IF EXISTS organization_user;
DROP USER IF EXISTS experiment_user;
DROP USER IF EXISTS metrics_user;
DROP USER IF EXISTS communications_user;

-- Clear Flyway history
DELETE FROM public.flyway_schema_history;
```

Then re-run Flyway migrations and user creation scripts.

---

## Security Best Practices

### Password Management

1. **Never hardcode passwords** in application code or configuration files
2. **Use environment variables** for local development
3. **Use AWS Secrets Manager** for production environments
4. **Rotate passwords regularly** (every 90 days recommended)
5. **Use strong passwords** (minimum 16 characters, mixed case, numbers, symbols)

### Network Security

1. **RDS in private subnets** - No public internet access
2. **Security groups** - Restrict access to application servers only
3. **SSL/TLS connections** - Enforce encrypted connections
4. **VPC isolation** - Separate VPCs per environment

### Access Control

1. **Principle of least privilege** - Each user has access only to their schema
2. **No cross-schema access** - Enforced by PostgreSQL permissions
3. **Read-only users** - Create separate read-only users for reporting/analytics
4. **Audit logging** - Enable PostgreSQL audit logging for compliance

---

## Monitoring

### Key Metrics to Monitor

1. **Connection count** per user
2. **Query performance** per schema
3. **Table sizes** and growth rates
4. **Index usage** and effectiveness
5. **Lock contention** and deadlocks
6. **Replication lag** (if using read replicas)

### CloudWatch Metrics

- `DatabaseConnections`
- `CPUUtilization`
- `FreeableMemory`
- `ReadLatency` / `WriteLatency`
- `DiskQueueDepth`

### Alerts

Set up CloudWatch alarms for:
- High connection count (> 80% of max)
- High CPU usage (> 80%)
- Low free memory (< 20%)
- Slow queries (> 5 seconds)
- Failed login attempts

---

## References

- **ADR-006**: Single Database Multi-Schema Architecture
- **PROJECT.md**: Section 27 (Data Architecture)
- **specs/aws-infrastructure.md**: Database section
- **specs/flyway-service.md**: Centralized migration service
- **PostgreSQL Documentation**: https://www.postgresql.org/docs/15/
- **Flyway Documentation**: https://flywaydb.org/documentation/

---

## Quick Reference

### Scripts

| Script | Purpose |
|--------|---------|
| `create-db-users.sh` | Create database users with schema permissions |
| `validate-db-permissions.sh` | Validate users and permissions |
| `test-schema-isolation.sql` | Test schema isolation security |

### Environment Variables

| Variable | Purpose |
|----------|---------|
| `IDENTITY_PASSWORD` | Password for identity_user |
| `ORGANIZATION_PASSWORD` | Password for organization_user |
| `EXPERIMENT_PASSWORD` | Password for experiment_user |
| `METRICS_PASSWORD` | Password for metrics_user |
| `COMMUNICATIONS_PASSWORD` | Password for communications_user |

### Connection Strings

```
identity-service:      jdbc:postgresql://host:5432/turaf?currentSchema=identity_schema
organization-service:  jdbc:postgresql://host:5432/turaf?currentSchema=organization_schema
experiment-service:    jdbc:postgresql://host:5432/turaf?currentSchema=experiment_schema
metrics-service:       jdbc:postgresql://host:5432/turaf?currentSchema=metrics_schema
communications-service: jdbc:postgresql://host:5432/turaf?currentSchema=communications_schema
```
