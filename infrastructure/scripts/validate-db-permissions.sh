#!/bin/bash
set -e

# Database Permission Validation Script
# Purpose: Validate that users and permissions are correctly configured

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

echo "=========================================="
echo "Database Permission Validation"
echo "=========================================="
echo "Host: $DB_HOST:$DB_PORT"
echo "Database: $DB_NAME"
echo "Admin User: $ADMIN_USER"
echo "=========================================="
echo ""

PGPASSWORD=$ADMIN_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $ADMIN_USER -d $DB_NAME <<SQL

\echo '1. Checking schemas exist (created by Flyway)...'
\echo '================================================'
SELECT schema_name 
FROM information_schema.schemata 
WHERE schema_name IN ('identity_schema', 'organization_schema', 'experiment_schema', 'metrics_schema', 'communications_schema')
ORDER BY schema_name;

\echo ''
\echo '2. Checking users exist...'
\echo '================================================'
SELECT usename 
FROM pg_user 
WHERE usename IN ('identity_user', 'organization_user', 'experiment_user', 'metrics_user', 'communications_user')
ORDER BY usename;

\echo ''
\echo '3. Checking schema permissions...'
\echo '================================================'
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

\echo ''
\echo '4. Checking table counts per schema...'
\echo '================================================'
SELECT 
  schemaname,
  COUNT(*) as table_count
FROM pg_tables
WHERE schemaname IN ('identity_schema', 'organization_schema', 'experiment_schema', 'metrics_schema', 'communications_schema')
GROUP BY schemaname
ORDER BY schemaname;

\echo ''
\echo '5. Checking Flyway migration history (last 10)...'
\echo '================================================'
SELECT 
  installed_rank,
  version,
  description,
  type,
  script,
  installed_on,
  success
FROM public.flyway_schema_history
ORDER BY installed_rank DESC
LIMIT 10;

\echo ''
\echo '6. Checking table access per user...'
\echo '================================================'
SELECT 
  n.nspname as schema_name,
  c.relname as table_name,
  r.rolname as user_name,
  CASE 
    WHEN has_table_privilege(r.rolname, c.oid, 'SELECT') THEN 'SELECT'
    ELSE 'NO SELECT'
  END as select_privilege,
  CASE 
    WHEN has_table_privilege(r.rolname, c.oid, 'INSERT') THEN 'INSERT'
    ELSE 'NO INSERT'
  END as insert_privilege,
  CASE 
    WHEN has_table_privilege(r.rolname, c.oid, 'UPDATE') THEN 'UPDATE'
    ELSE 'NO UPDATE'
  END as update_privilege,
  CASE 
    WHEN has_table_privilege(r.rolname, c.oid, 'DELETE') THEN 'DELETE'
    ELSE 'NO DELETE'
  END as delete_privilege
FROM pg_class c
JOIN pg_namespace n ON n.oid = c.relnamespace
CROSS JOIN pg_roles r
WHERE n.nspname IN ('identity_schema', 'organization_schema', 'experiment_schema', 'metrics_schema', 'communications_schema')
  AND r.rolname IN ('identity_user', 'organization_user', 'experiment_user', 'metrics_user', 'communications_user')
  AND c.relkind = 'r'
  AND (
    (n.nspname = 'identity_schema' AND r.rolname = 'identity_user') OR
    (n.nspname = 'organization_schema' AND r.rolname = 'organization_user') OR
    (n.nspname = 'experiment_schema' AND r.rolname = 'experiment_user') OR
    (n.nspname = 'metrics_schema' AND r.rolname = 'metrics_user') OR
    (n.nspname = 'communications_schema' AND r.rolname = 'communications_user')
  )
ORDER BY schema_name, table_name, user_name;

SQL

echo ""
echo "=========================================="
echo "✅ Validation complete"
echo "=========================================="
echo ""
echo "Expected results:"
echo "  - 5 schemas should exist"
echo "  - 5 users should exist"
echo "  - Each user should have USAGE and CREATE on their schema"
echo "  - Each user should have SELECT, INSERT, UPDATE, DELETE on their tables"
echo "  - Flyway migrations should show successful execution"
echo "=========================================="
