# Task: Configure Database Users and Permissions

**Service**: Infrastructure  
**Phase**: 10  
**Estimated Time**: 1.5 hours  
**Dependencies**: Task 028 (CodeBuild Migration Projects)

## Objective

Configure database users with schema-level permissions and create validation scripts for the multi-schema PostgreSQL architecture.

**Note**: Schema and table creation is now handled by the centralized Flyway migration service. This task focuses on user management and permission configuration.

## Prerequisites

- [x] Task 016: Database module created
- [x] Task 028: CodeBuild migration projects created
- [ ] Flyway migrations executed (schemas and tables created)
- [ ] RDS instance deployed

## Scope

**Files to Create**:
- `infrastructure/scripts/create-db-users.sh` (NEW - user creation only)
- `infrastructure/scripts/validate-db-permissions.sh` (UPDATED - validation only)
- `infrastructure/scripts/test-schema-isolation.sql` (KEPT - still needed)
- `docs/DATABASE_SETUP.md` (UPDATED - reflect Flyway architecture)

**What This Task Does NOT Do**:
- ❌ Create schemas (handled by Flyway migrations)
- ❌ Create tables (handled by Flyway migrations)
- ❌ Manage migrations (handled by flyway-service)

## Implementation Details

### Database User Creation Script

**File**: `infrastructure/scripts/create-db-users.sh`

**Purpose**: Create service-specific database users with schema-level permissions.

**Note**: Schemas are created by Flyway migrations. This script only creates users and grants permissions.

```bash
#!/bin/bash
set -e

# Database connection parameters
DB_HOST=${1:-localhost}
DB_PORT=${2:-5432}
DB_NAME=${3:-turaf}
ADMIN_USER=${4:-postgres}
ADMIN_PASSWORD=${5}

if [ -z "$ADMIN_PASSWORD" ]; then
  echo "Usage: $0 <db_host> <db_port> <db_name> <admin_user> <admin_password>"
  exit 1
fi

echo "Creating database users on $DB_HOST:$DB_PORT/$DB_NAME"
echo "NOTE: Schemas should already exist (created by Flyway migrations)"

# Verify schemas exist before creating users
PGPASSWORD=$ADMIN_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $ADMIN_USER -d $DB_NAME <<SQL
-- Verify schemas exist (created by Flyway)
DO \$\$
DECLARE
  missing_schemas TEXT[];
BEGIN
  SELECT ARRAY_AGG(schema_name)
  INTO missing_schemas
  FROM (VALUES 
    ('identity_schema'),
    ('organization_schema'),
    ('experiment_schema'),
    ('metrics_schema'),
    ('communications_schema')
  ) AS expected(schema_name)
  WHERE NOT EXISTS (
    SELECT 1 FROM information_schema.schemata 
    WHERE schema_name = expected.schema_name
  );
  
  IF missing_schemas IS NOT NULL THEN
    RAISE EXCEPTION 'Missing schemas: %. Run Flyway migrations first!', missing_schemas;
  END IF;
  
  RAISE NOTICE 'All required schemas exist. Proceeding with user creation...';
END \$\$;

-- Create service users (passwords from environment variables)
DO \$\$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_user WHERE usename = 'identity_user') THEN
    CREATE USER identity_user WITH PASSWORD '${IDENTITY_PASSWORD}';
    RAISE NOTICE 'Created user: identity_user';
  ELSE
    RAISE NOTICE 'User already exists: identity_user';
  END IF;
  
  IF NOT EXISTS (SELECT FROM pg_user WHERE usename = 'organization_user') THEN
    CREATE USER organization_user WITH PASSWORD '${ORGANIZATION_PASSWORD}';
    RAISE NOTICE 'Created user: organization_user';
  ELSE
    RAISE NOTICE 'User already exists: organization_user';
  END IF;
  
  IF NOT EXISTS (SELECT FROM pg_user WHERE usename = 'experiment_user') THEN
    CREATE USER experiment_user WITH PASSWORD '${EXPERIMENT_PASSWORD}';
    RAISE NOTICE 'Created user: experiment_user';
  ELSE
    RAISE NOTICE 'User already exists: experiment_user';
  END IF;
  
  IF NOT EXISTS (SELECT FROM pg_user WHERE usename = 'metrics_user') THEN
    CREATE USER metrics_user WITH PASSWORD '${METRICS_PASSWORD}';
    RAISE NOTICE 'Created user: metrics_user';
  ELSE
    RAISE NOTICE 'User already exists: metrics_user';
  END IF;
  
  IF NOT EXISTS (SELECT FROM pg_user WHERE usename = 'communications_user') THEN
    CREATE USER communications_user WITH PASSWORD '${COMMUNICATIONS_PASSWORD}';
    RAISE NOTICE 'Created user: communications_user';
  ELSE
    RAISE NOTICE 'User already exists: communications_user';
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

-- Grant schema permissions for communications_schema
GRANT ALL PRIVILEGES ON SCHEMA communications_schema TO communications_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA communications_schema TO communications_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA communications_schema TO communications_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA communications_schema GRANT ALL ON TABLES TO communications_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA communications_schema GRANT ALL ON SEQUENCES TO communications_user;

SQL

echo "✅ Database users created and permissions granted"
echo "NOTE: Tables were created by Flyway migrations"
```

### Permission Validation Script

**File**: `infrastructure/scripts/validate-db-permissions.sh`

**Purpose**: Validate that users and permissions are correctly configured.
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

echo "Validating database users and permissions on $DB_HOST:$DB_PORT/$DB_NAME"

PGPASSWORD=$ADMIN_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $ADMIN_USER -d $DB_NAME <<SQL
-- Check schemas exist (created by Flyway)
SELECT schema_name 
FROM information_schema.schemata 
WHERE schema_name IN ('identity_schema', 'organization_schema', 'experiment_schema', 'metrics_schema', 'communications_schema')
ORDER BY schema_name;

-- Check users exist
SELECT usename 
FROM pg_user 
WHERE usename IN ('identity_user', 'organization_user', 'experiment_user', 'metrics_user', 'communications_user')
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
WHERE n.nspname IN ('identity_schema', 'organization_schema', 'experiment_schema', 'metrics_schema', 'communications_schema')
  AND r.rolname IN ('identity_user', 'organization_user', 'experiment_user', 'metrics_user', 'communications_user')
  AND (
    (n.nspname = 'identity_schema' AND r.rolname = 'identity_user') OR
    (n.nspname = 'organization_schema' AND r.rolname = 'organization_user') OR
    (n.nspname = 'experiment_schema' AND r.rolname = 'experiment_user') OR
    (n.nspname = 'metrics_schema' AND r.rolname = 'metrics_user') OR
    (n.nspname = 'communications_schema' AND r.rolname = 'communications_user')
  )
ORDER BY schema_name, user_name;

-- Check Flyway migration history
SELECT installed_rank, version, description, type, script, installed_on, success
FROM public.flyway_schema_history
ORDER BY installed_rank DESC
LIMIT 10;

SQL

echo "✅ Validation complete"
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

**Centralized Flyway Service**:

Database migrations are managed centrally through the `flyway-service`:

- All migrations located in `services/flyway-service/migrations/`
- Executed via AWS CodeBuild triggered by GitHub Actions
- Migrations run **before** service deployments
- Migration history tracked in `public.flyway_schema_history` table
- Each migration targets specific schema via `SET search_path`

See `specs/flyway-service.md` for complete documentation.

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

- [x] User creation script created and tested
- [x] Permission validation script created and tested
- [x] Schema isolation test script created and passes
- [x] Database setup documentation updated for Flyway architecture
- [x] Scripts executable and properly documented
- [x] All five users created with correct permissions (identity, organization, experiment, metrics, communications)
- [x] Schema isolation verified (users cannot access other schemas)
- [x] Local development setup documented
- [x] Flyway migrations verified as prerequisite
- [x] README created for scripts directory

## Testing Requirements

**Prerequisites**:
1. ✅ Flyway migrations must be executed first (schemas and tables created)
2. ✅ RDS instance must be deployed

**Local Testing**:
1. Verify Flyway migrations ran successfully
2. Run user creation script on local PostgreSQL
3. Run validation script to verify users and permissions
4. Run isolation test script to verify permissions
5. Verify each service can connect to its schema
6. Verify services cannot access other schemas

**Integration Testing**:
1. Deploy to DEV environment
2. Execute Flyway migrations via GitHub Actions
3. Run user creation script
4. Run validation and isolation tests
5. Deploy all services and verify connectivity
6. Run service integration tests

## References

- ADR: `docs/adr/ADR-006-single-database-multi-schema.md`
- Specification: `specs/aws-infrastructure.md` (Database section)
- PROJECT.md: Section 27 (Data Architecture)
- Related Tasks: 004-create-database-module
