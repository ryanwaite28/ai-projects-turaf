#!/bin/bash
set -e

# Database User Creation Script
# Purpose: Create service-specific database users with schema-level permissions
# Note: Schemas must already exist (created by Flyway migrations)

# Database connection parameters
DB_HOST=${1:-localhost}
DB_PORT=${2:-5432}
DB_NAME=${3:-turaf}
ADMIN_USER=${4:-postgres}
ADMIN_PASSWORD=${5}

if [ -z "$ADMIN_PASSWORD" ]; then
  echo "Usage: $0 <db_host> <db_port> <db_name> <admin_user> <admin_password>"
  echo ""
  echo "Environment variables required:"
  echo "  IDENTITY_PASSWORD"
  echo "  ORGANIZATION_PASSWORD"
  echo "  EXPERIMENT_PASSWORD"
  echo "  METRICS_PASSWORD"
  echo "  COMMUNICATIONS_PASSWORD"
  exit 1
fi

# Verify required environment variables
if [ -z "$IDENTITY_PASSWORD" ] || [ -z "$ORGANIZATION_PASSWORD" ] || \
   [ -z "$EXPERIMENT_PASSWORD" ] || [ -z "$METRICS_PASSWORD" ] || \
   [ -z "$COMMUNICATIONS_PASSWORD" ]; then
  echo "Error: Missing required password environment variables"
  echo "Please set: IDENTITY_PASSWORD, ORGANIZATION_PASSWORD, EXPERIMENT_PASSWORD, METRICS_PASSWORD, COMMUNICATIONS_PASSWORD"
  exit 1
fi

echo "=========================================="
echo "Database User Creation"
echo "=========================================="
echo "Host: $DB_HOST:$DB_PORT"
echo "Database: $DB_NAME"
echo "Admin User: $ADMIN_USER"
echo ""
echo "NOTE: Schemas should already exist (created by Flyway migrations)"
echo "=========================================="

# Execute SQL to create users and grant permissions
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
GRANT USAGE ON SCHEMA identity_schema TO identity_user;
GRANT ALL PRIVILEGES ON SCHEMA identity_schema TO identity_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA identity_schema TO identity_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA identity_schema TO identity_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA identity_schema GRANT ALL ON TABLES TO identity_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA identity_schema GRANT ALL ON SEQUENCES TO identity_user;

-- Grant schema permissions for organization_schema
GRANT USAGE ON SCHEMA organization_schema TO organization_user;
GRANT ALL PRIVILEGES ON SCHEMA organization_schema TO organization_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA organization_schema TO organization_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA organization_schema TO organization_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA organization_schema GRANT ALL ON TABLES TO organization_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA organization_schema GRANT ALL ON SEQUENCES TO organization_user;

-- Grant schema permissions for experiment_schema
GRANT USAGE ON SCHEMA experiment_schema TO experiment_user;
GRANT ALL PRIVILEGES ON SCHEMA experiment_schema TO experiment_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA experiment_schema TO experiment_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA experiment_schema TO experiment_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA experiment_schema GRANT ALL ON TABLES TO experiment_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA experiment_schema GRANT ALL ON SEQUENCES TO experiment_user;

-- Grant schema permissions for metrics_schema
GRANT USAGE ON SCHEMA metrics_schema TO metrics_user;
GRANT ALL PRIVILEGES ON SCHEMA metrics_schema TO metrics_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA metrics_schema TO metrics_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA metrics_schema TO metrics_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA metrics_schema GRANT ALL ON TABLES TO metrics_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA metrics_schema GRANT ALL ON SEQUENCES TO metrics_user;

-- Grant schema permissions for communications_schema
GRANT USAGE ON SCHEMA communications_schema TO communications_user;
GRANT ALL PRIVILEGES ON SCHEMA communications_schema TO communications_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA communications_schema TO communications_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA communications_schema TO communications_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA communications_schema GRANT ALL ON TABLES TO communications_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA communications_schema GRANT ALL ON SEQUENCES TO communications_user;

SQL

echo ""
echo "=========================================="
echo "✅ Database users created and permissions granted"
echo "=========================================="
echo "Users created:"
echo "  - identity_user (identity_schema)"
echo "  - organization_user (organization_schema)"
echo "  - experiment_user (experiment_schema)"
echo "  - metrics_user (metrics_schema)"
echo "  - communications_user (communications_schema)"
echo ""
echo "NOTE: Tables were created by Flyway migrations"
echo "=========================================="
