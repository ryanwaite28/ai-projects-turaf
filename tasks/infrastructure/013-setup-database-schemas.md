# Task: Setup Database Schemas

**Service**: Infrastructure  
**Phase**: 10  
**Estimated Time**: 2 hours  

## Objective

Create database schema initialization and validation scripts for the multi-schema PostgreSQL architecture.

## Prerequisites

- [x] Task 004: Database module created
- [ ] RDS instance deployed

## Scope

**Files to Create**:
- `infrastructure/scripts/init-schemas.sh`
- `infrastructure/scripts/validate-schemas.sh`
- `infrastructure/scripts/test-schema-isolation.sql`
- `docs/DATABASE_SETUP.md`

## Implementation Details

### Schema Initialization Script

**File**: `infrastructure/scripts/init-schemas.sh`
```bash
#!/bin/bash
set -e

# Database connection parameters
DB_HOST=${1:-localhost}
DB_PORT=${2:-5432}
DB_NAME=${3:-turaf}
ADMIN_USER=${4:-turaf_admin}
ADMIN_PASSWORD=${5}

if [ -z "$ADMIN_PASSWORD" ]; then
  echo "Usage: $0 <db_host> <db_port> <db_name> <admin_user> <admin_password>"
  exit 1
fi

echo "Initializing database schemas on $DB_HOST:$DB_PORT/$DB_NAME"

# Create schemas
PGPASSWORD=$ADMIN_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $ADMIN_USER -d $DB_NAME <<SQL
-- Create schemas
CREATE SCHEMA IF NOT EXISTS identity_schema;
CREATE SCHEMA IF NOT EXISTS organization_schema;
CREATE SCHEMA IF NOT EXISTS experiment_schema;
CREATE SCHEMA IF NOT EXISTS metrics_schema;

-- Create users (passwords should be provided via environment variables)
DO \$\$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_user WHERE usename = 'identity_user') THEN
    CREATE USER identity_user WITH PASSWORD '${IDENTITY_PASSWORD}';
  END IF;
  IF NOT EXISTS (SELECT FROM pg_user WHERE usename = 'organization_user') THEN
    CREATE USER organization_user WITH PASSWORD '${ORGANIZATION_PASSWORD}';
  END IF;
  IF NOT EXISTS (SELECT FROM pg_user WHERE usename = 'experiment_user') THEN
    CREATE USER experiment_user WITH PASSWORD '${EXPERIMENT_PASSWORD}';
  END IF;
  IF NOT EXISTS (SELECT FROM pg_user WHERE usename = 'metrics_user') THEN
    CREATE USER metrics_user WITH PASSWORD '${METRICS_PASSWORD}';
  END IF;
END
\$\$;

-- Grant schema permissions for identity_schema
GRANT ALL PRIVILEGES ON SCHEMA identity_schema TO identity_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA identity_schema TO identity_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA identity_schema TO identity_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA identity_schema GRANT ALL ON TABLES TO identity_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA identity_schema GRANT ALL ON SEQUENCES TO identity_user;

-- Grant schema permissions for organization_schema
GRANT ALL PRIVILEGES ON SCHEMA organization_schema TO organization_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA organization_schema TO organization_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA organization_schema TO organization_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA organization_schema GRANT ALL ON TABLES TO organization_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA organization_schema GRANT ALL ON SEQUENCES TO organization_user;

-- Grant schema permissions for experiment_schema
GRANT ALL PRIVILEGES ON SCHEMA experiment_schema TO experiment_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA experiment_schema TO experiment_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA experiment_schema TO experiment_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA experiment_schema GRANT ALL ON TABLES TO experiment_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA experiment_schema GRANT ALL ON SEQUENCES TO experiment_user;

-- Grant schema permissions for metrics_schema
GRANT ALL PRIVILEGES ON SCHEMA metrics_schema TO metrics_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA metrics_schema TO metrics_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA metrics_schema TO metrics_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA metrics_schema GRANT ALL ON TABLES TO metrics_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA metrics_schema GRANT ALL ON SEQUENCES TO metrics_user;

SQL

echo "Schema initialization complete"
```

### Schema Validation Script

**File**: `infrastructure/scripts/validate-schemas.sh`
```bash
#!/bin/bash
set -e

DB_HOST=${1:-localhost}
DB_PORT=${2:-5432}
DB_NAME=${3:-turaf}
ADMIN_USER=${4:-turaf_admin}
ADMIN_PASSWORD=${5}

if [ -z "$ADMIN_PASSWORD" ]; then
  echo "Usage: $0 <db_host> <db_port> <db_name> <admin_user> <admin_password>"
  exit 1
fi

echo "Validating database schemas on $DB_HOST:$DB_PORT/$DB_NAME"

PGPASSWORD=$ADMIN_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $ADMIN_USER -d $DB_NAME <<SQL
-- Check schemas exist
SELECT schema_name 
FROM information_schema.schemata 
WHERE schema_name IN ('identity_schema', 'organization_schema', 'experiment_schema', 'metrics_schema')
ORDER BY schema_name;

-- Check users exist
SELECT usename 
FROM pg_user 
WHERE usename IN ('identity_user', 'organization_user', 'experiment_user', 'metrics_user')
ORDER BY usename;

-- Check permissions
SELECT 
  n.nspname as schema_name,
  r.rolname as user_name,
  CASE 
    WHEN has_schema_privilege(r.rolname, n.nspname, 'USAGE') THEN 'USAGE'
    ELSE 'NO USAGE'
  END as usage_privilege,
  CASE 
    WHEN has_schema_privilege(r.rolname, n.nspname, 'CREATE') THEN 'CREATE'
    ELSE 'NO CREATE'
  END as create_privilege
FROM pg_namespace n
CROSS JOIN pg_roles r
WHERE n.nspname IN ('identity_schema', 'organization_schema', 'experiment_schema', 'metrics_schema')
  AND r.rolname IN ('identity_user', 'organization_user', 'experiment_user', 'metrics_user')
  AND (
    (n.nspname = 'identity_schema' AND r.rolname = 'identity_user') OR
    (n.nspname = 'organization_schema' AND r.rolname = 'organization_user') OR
    (n.nspname = 'experiment_schema' AND r.rolname = 'experiment_user') OR
    (n.nspname = 'metrics_schema' AND r.rolname = 'metrics_user')
  )
ORDER BY schema_name, user_name;

SQL

echo "Validation complete"
```

### Schema Isolation Test

**File**: `infrastructure/scripts/test-schema-isolation.sql`
```sql
-- Test schema isolation
-- This script verifies that service users cannot access other schemas

-- Test 1: identity_user should NOT be able to access organization_schema
\c turaf identity_user
DO $$
BEGIN
  -- This should fail
  CREATE TABLE organization_schema.test_table (id INT);
  RAISE EXCEPTION 'ISOLATION VIOLATION: identity_user can create tables in organization_schema';
EXCEPTION
  WHEN insufficient_privilege THEN
    RAISE NOTICE 'PASS: identity_user cannot access organization_schema';
END $$;

-- Test 2: organization_user should NOT be able to access experiment_schema
\c turaf organization_user
DO $$
BEGIN
  -- This should fail
  CREATE TABLE experiment_schema.test_table (id INT);
  RAISE EXCEPTION 'ISOLATION VIOLATION: organization_user can create tables in experiment_schema';
EXCEPTION
  WHEN insufficient_privilege THEN
    RAISE NOTICE 'PASS: organization_user cannot access experiment_schema';
END $$;

-- Test 3: experiment_user should NOT be able to access metrics_schema
\c turaf experiment_user
DO $$
BEGIN
  -- This should fail
  CREATE TABLE metrics_schema.test_table (id INT);
  RAISE EXCEPTION 'ISOLATION VIOLATION: experiment_user can create tables in metrics_schema';
EXCEPTION
  WHEN insufficient_privilege THEN
    RAISE NOTICE 'PASS: experiment_user cannot access metrics_schema';
END $$;

-- Test 4: metrics_user should NOT be able to access identity_schema
\c turaf metrics_user
DO $$
BEGIN
  -- This should fail
  CREATE TABLE identity_schema.test_table (id INT);
  RAISE EXCEPTION 'ISOLATION VIOLATION: metrics_user can create tables in identity_schema';
EXCEPTION
  WHEN insufficient_privilege THEN
    RAISE NOTICE 'PASS: metrics_user cannot access identity_schema';
END $$;

-- Test 5: Each user CAN access their own schema
\c turaf identity_user
CREATE TABLE identity_schema.test_table (id INT);
DROP TABLE identity_schema.test_table;
RAISE NOTICE 'PASS: identity_user can access identity_schema';

\c turaf organization_user
CREATE TABLE organization_schema.test_table (id INT);
DROP TABLE organization_schema.test_table;
RAISE NOTICE 'PASS: organization_user can access organization_schema';

\c turaf experiment_user
CREATE TABLE experiment_schema.test_table (id INT);
DROP TABLE experiment_schema.test_table;
RAISE NOTICE 'PASS: experiment_user can access experiment_schema';

\c turaf metrics_user
CREATE TABLE metrics_schema.test_table (id INT);
DROP TABLE metrics_schema.test_table;
RAISE NOTICE 'PASS: metrics_user can access metrics_schema';

\echo 'All schema isolation tests passed!'
```

### Database Setup Documentation

**File**: `docs/DATABASE_SETUP.md`
```markdown
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

### Users and Permissions

Each service has its own database user with schema-scoped permissions:

| User | Schema | Permissions |
|------|--------|-------------|
| `identity_user` | `identity_schema` | Full access (CREATE, SELECT, INSERT, UPDATE, DELETE) |
| `organization_user` | `organization_schema` | Full access |
| `experiment_user` | `experiment_schema` | Full access |
| `metrics_user` | `metrics_schema` | Full access |

**Isolation**: Users cannot access other schemas (enforced by PostgreSQL permissions).

## Local Development Setup

### Prerequisites

- PostgreSQL 15.3+ installed
- `psql` command-line tool

### Initialize Local Database

1. Create database:
```bash
createdb turaf
```

2. Set user passwords as environment variables:
```bash
export IDENTITY_PASSWORD="your-identity-password"
export ORGANIZATION_PASSWORD="your-organization-password"
export EXPERIMENT_PASSWORD="your-experiment-password"
export METRICS_PASSWORD="your-metrics-password"
```

3. Run initialization script:
```bash
./infrastructure/scripts/init-schemas.sh localhost 5432 turaf postgres <postgres-password>
```

4. Validate setup:
```bash
./infrastructure/scripts/validate-schemas.sh localhost 5432 turaf postgres <postgres-password>
```

5. Test schema isolation:
```bash
psql -h localhost -p 5432 -U postgres -d turaf -f infrastructure/scripts/test-schema-isolation.sql
```

## Service Configuration

Each service connects to its own schema:

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

## Migration Management

Each service manages its own schema migrations using Flyway:

- Migrations are located in `src/main/resources/db/migration/`
- Flyway automatically targets the configured schema
- Migration history is tracked per schema
- No migration conflicts between services

## Troubleshooting

### Check schema exists
```sql
SELECT schema_name FROM information_schema.schemata WHERE schema_name LIKE '%_schema';
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

### Reset schema (CAUTION: Deletes all data)
```sql
DROP SCHEMA IF EXISTS identity_schema CASCADE;
DROP SCHEMA IF EXISTS organization_schema CASCADE;
DROP SCHEMA IF EXISTS experiment_schema CASCADE;
DROP SCHEMA IF EXISTS metrics_schema CASCADE;
```

Then re-run initialization script.

## References

- ADR-006: Single Database Multi-Schema Architecture
- PROJECT.md: Section 27 (Data Architecture)
- specs/aws-infrastructure.md: Database section
```

## Acceptance Criteria

- [ ] Schema initialization script created and tested
- [ ] Schema validation script created and tested
- [ ] Schema isolation test script created and passes
- [ ] Database setup documentation created
- [ ] Scripts executable and properly documented
- [ ] All four schemas can be created successfully
- [ ] All four users can be created with correct permissions
- [ ] Schema isolation is verified (users cannot access other schemas)
- [ ] Local development setup documented

## Testing Requirements

**Local Testing**:
1. Run initialization script on local PostgreSQL
2. Run validation script to verify setup
3. Run isolation test script to verify permissions
4. Verify each service can connect to its schema
5. Verify services cannot access other schemas

**Integration Testing**:
1. Deploy to DEV environment
2. Run validation and isolation tests
3. Deploy all services and verify connectivity
4. Run service integration tests

## References

- ADR: `docs/adr/ADR-006-single-database-multi-schema.md`
- Specification: `specs/aws-infrastructure.md` (Database section)
- PROJECT.md: Section 27 (Data Architecture)
- Related Tasks: 004-create-database-module
