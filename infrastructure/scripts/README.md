# Database Management Scripts

This directory contains scripts for managing database users and permissions in the Turaf multi-schema PostgreSQL architecture.

## Overview

The Turaf platform uses a single PostgreSQL database with schema-level isolation. Each microservice has its own schema and database user. These scripts help set up and validate that configuration.

**Important**: Schemas and tables are created by Flyway migrations. These scripts only manage database users and permissions.

---

## Scripts

### 1. create-db-users.sh

**Purpose**: Create service-specific database users with schema-level permissions.

**Prerequisites**:
- PostgreSQL database exists
- Flyway migrations have been executed (schemas and tables created)
- Environment variables set for user passwords

**Usage**:

```bash
# Set password environment variables
export IDENTITY_PASSWORD="your-password"
export ORGANIZATION_PASSWORD="your-password"
export EXPERIMENT_PASSWORD="your-password"
export METRICS_PASSWORD="your-password"
export COMMUNICATIONS_PASSWORD="your-password"

# Run script
./create-db-users.sh <db_host> <db_port> <db_name> <admin_user> <admin_password>

# Example (local)
./create-db-users.sh localhost 5432 turaf postgres mypassword

# Example (RDS)
./create-db-users.sh turaf-db-dev.abc123.us-east-1.rds.amazonaws.com 5432 turaf postgres mypassword
```

**What it does**:
1. Verifies all required schemas exist
2. Creates 5 database users (if they don't exist)
3. Grants schema-level permissions to each user
4. Grants table and sequence permissions
5. Sets default privileges for future objects

**Users created**:
- `identity_user` → `identity_schema`
- `organization_user` → `organization_schema`
- `experiment_user` → `experiment_schema`
- `metrics_user` → `metrics_schema`
- `communications_user` → `communications_schema`

---

### 2. validate-db-permissions.sh

**Purpose**: Validate that users and permissions are correctly configured.

**Usage**:

```bash
./validate-db-permissions.sh <db_host> <db_port> <db_name> <admin_user> <admin_password>

# Example
./validate-db-permissions.sh localhost 5432 turaf postgres mypassword
```

**What it checks**:
1. All 5 schemas exist
2. All 5 users exist
3. Schema permissions (USAGE, CREATE)
4. Table counts per schema
5. Flyway migration history (last 10 migrations)
6. Table-level permissions (SELECT, INSERT, UPDATE, DELETE)

**Expected output**:
- 5 schemas found
- 5 users found
- Each user has USAGE and CREATE on their schema
- Each user has full permissions on their tables
- Flyway migrations show successful execution

---

### 3. test-schema-isolation.sql

**Purpose**: Test that schema isolation is properly enforced.

**Usage**:

```bash
psql -h <db_host> -p <db_port> -U postgres -d turaf -f test-schema-isolation.sql

# Example
psql -h localhost -p 5432 -U postgres -d turaf -f test-schema-isolation.sql
```

**What it tests**:
1. Users cannot create tables in other schemas
2. Users cannot read from other schemas
3. Users can fully access their own schema
4. Cross-schema isolation is enforced

**Test cases**:
- Test 1-5: Verify users cannot access other schemas (should fail with permission error)
- Test 6-10: Verify users can access their own schema (should succeed)
- Test 11: Verify cross-schema read isolation

**Expected result**: All tests pass with ✅ PASS messages

---

## Workflow

### Local Development Setup

```bash
# 1. Create database
createdb turaf

# 2. Run Flyway migrations
cd services/flyway-service
./scripts/run-migrations.sh

# 3. Set passwords
export IDENTITY_PASSWORD="dev-password"
export ORGANIZATION_PASSWORD="dev-password"
export EXPERIMENT_PASSWORD="dev-password"
export METRICS_PASSWORD="dev-password"
export COMMUNICATIONS_PASSWORD="dev-password"

# 4. Create users
cd ../../infrastructure/scripts
./create-db-users.sh localhost 5432 turaf postgres postgres

# 5. Validate
./validate-db-permissions.sh localhost 5432 turaf postgres postgres

# 6. Test isolation
psql -h localhost -p 5432 -U postgres -d turaf -f test-schema-isolation.sql
```

### AWS RDS Setup

```bash
# 1. Get RDS endpoint
RDS_ENDPOINT=$(aws rds describe-db-instances \
  --db-instance-identifier turaf-db-dev \
  --query 'DBInstances[0].Endpoint.Address' \
  --output text)

# 2. Get passwords from Secrets Manager
export IDENTITY_PASSWORD="$(aws secretsmanager get-secret-value --secret-id turaf/db/identity-user --query SecretString --output text)"
export ORGANIZATION_PASSWORD="$(aws secretsmanager get-secret-value --secret-id turaf/db/organization-user --query SecretString --output text)"
export EXPERIMENT_PASSWORD="$(aws secretsmanager get-secret-value --secret-id turaf/db/experiment-user --query SecretString --output text)"
export METRICS_PASSWORD="$(aws secretsmanager get-secret-value --secret-id turaf/db/metrics-user --query SecretString --output text)"
export COMMUNICATIONS_PASSWORD="$(aws secretsmanager get-secret-value --secret-id turaf/db/communications-user --query SecretString --output text)"

MASTER_PASSWORD=$(aws secretsmanager get-secret-value \
  --secret-id turaf/db/master \
  --query SecretString \
  --output text)

# 3. Create users
./create-db-users.sh $RDS_ENDPOINT 5432 turaf postgres $MASTER_PASSWORD

# 4. Validate
./validate-db-permissions.sh $RDS_ENDPOINT 5432 turaf postgres $MASTER_PASSWORD
```

---

## Troubleshooting

### Error: "Missing schemas"

**Cause**: Flyway migrations have not been executed yet.

**Solution**: Run Flyway migrations first:
```bash
cd services/flyway-service
./scripts/run-migrations.sh
```

### Error: "User already exists"

**Cause**: Users were already created in a previous run.

**Solution**: This is normal. The script will skip user creation and only update permissions.

### Error: "Permission denied"

**Cause**: Admin user doesn't have sufficient privileges.

**Solution**: Use the `postgres` superuser or a user with `CREATEROLE` privilege.

### Validation shows "NO USAGE" or "NO CREATE"

**Cause**: Permissions were not granted correctly.

**Solution**: Re-run `create-db-users.sh` to fix permissions.

### Isolation test fails

**Cause**: Permissions are too permissive (users can access other schemas).

**Solution**: 
1. Check schema ownership
2. Revoke cross-schema permissions
3. Re-run `create-db-users.sh`

---

## Security Notes

### Password Management

- **Never commit passwords** to version control
- Use **environment variables** for local development
- Use **AWS Secrets Manager** for production
- **Rotate passwords** regularly (every 90 days)

### Principle of Least Privilege

- Each user has access **only to their schema**
- No cross-schema access is permitted
- Users cannot access `public` schema tables
- Admin access is separate from service access

### Audit Trail

- All user creation and permission changes should be logged
- Use PostgreSQL audit logging in production
- Monitor failed login attempts
- Track permission escalation attempts

---

## References

- **docs/DATABASE_SETUP.md**: Complete database setup guide
- **specs/flyway-service.md**: Centralized migration service documentation
- **PROJECT.md**: Section 27 (Data Architecture)
- **ADR-006**: Single Database Multi-Schema Architecture

---

## Quick Reference

| Script | Purpose | Prerequisites |
|--------|---------|---------------|
| `create-db-users.sh` | Create database users | Flyway migrations completed |
| `validate-db-permissions.sh` | Validate setup | Users created |
| `test-schema-isolation.sql` | Test security | Users created |

**Environment Variables Required**:
- `IDENTITY_PASSWORD`
- `ORGANIZATION_PASSWORD`
- `EXPERIMENT_PASSWORD`
- `METRICS_PASSWORD`
- `COMMUNICATIONS_PASSWORD`
