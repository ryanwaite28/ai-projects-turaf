-- Schema Isolation Test Script
-- Purpose: Verify that service users cannot access other schemas
-- This ensures proper security boundaries in the multi-schema architecture

\echo '=========================================='
\echo 'Schema Isolation Test Suite'
\echo '=========================================='
\echo ''

-- Test 1: identity_user should NOT be able to access organization_schema
\echo 'Test 1: identity_user cannot access organization_schema'
\echo '================================================'
\c turaf identity_user
DO $$
BEGIN
  -- This should fail
  CREATE TABLE organization_schema.test_table (id INT);
  RAISE EXCEPTION 'ISOLATION VIOLATION: identity_user can create tables in organization_schema';
EXCEPTION
  WHEN insufficient_privilege THEN
    RAISE NOTICE '✅ PASS: identity_user cannot access organization_schema';
END $$;

-- Test 2: organization_user should NOT be able to access experiment_schema
\echo ''
\echo 'Test 2: organization_user cannot access experiment_schema'
\echo '================================================'
\c turaf organization_user
DO $$
BEGIN
  -- This should fail
  CREATE TABLE experiment_schema.test_table (id INT);
  RAISE EXCEPTION 'ISOLATION VIOLATION: organization_user can create tables in experiment_schema';
EXCEPTION
  WHEN insufficient_privilege THEN
    RAISE NOTICE '✅ PASS: organization_user cannot access experiment_schema';
END $$;

-- Test 3: experiment_user should NOT be able to access metrics_schema
\echo ''
\echo 'Test 3: experiment_user cannot access metrics_schema'
\echo '================================================'
\c turaf experiment_user
DO $$
BEGIN
  -- This should fail
  CREATE TABLE metrics_schema.test_table (id INT);
  RAISE EXCEPTION 'ISOLATION VIOLATION: experiment_user can create tables in metrics_schema';
EXCEPTION
  WHEN insufficient_privilege THEN
    RAISE NOTICE '✅ PASS: experiment_user cannot access metrics_schema';
END $$;

-- Test 4: metrics_user should NOT be able to access communications_schema
\echo ''
\echo 'Test 4: metrics_user cannot access communications_schema'
\echo '================================================'
\c turaf metrics_user
DO $$
BEGIN
  -- This should fail
  CREATE TABLE communications_schema.test_table (id INT);
  RAISE EXCEPTION 'ISOLATION VIOLATION: metrics_user can create tables in communications_schema';
EXCEPTION
  WHEN insufficient_privilege THEN
    RAISE NOTICE '✅ PASS: metrics_user cannot access communications_schema';
END $$;

-- Test 5: communications_user should NOT be able to access identity_schema
\echo ''
\echo 'Test 5: communications_user cannot access identity_schema'
\echo '================================================'
\c turaf communications_user
DO $$
BEGIN
  -- This should fail
  CREATE TABLE identity_schema.test_table (id INT);
  RAISE EXCEPTION 'ISOLATION VIOLATION: communications_user can create tables in identity_schema';
EXCEPTION
  WHEN insufficient_privilege THEN
    RAISE NOTICE '✅ PASS: communications_user cannot access identity_schema';
END $$;

-- Test 6: Each user CAN access their own schema
\echo ''
\echo 'Test 6: identity_user can access identity_schema'
\echo '================================================'
\c turaf identity_user
CREATE TABLE identity_schema.test_table (id INT);
DROP TABLE identity_schema.test_table;
\echo '✅ PASS: identity_user can access identity_schema'

\echo ''
\echo 'Test 7: organization_user can access organization_schema'
\echo '================================================'
\c turaf organization_user
CREATE TABLE organization_schema.test_table (id INT);
DROP TABLE organization_schema.test_table;
\echo '✅ PASS: organization_user can access organization_schema'

\echo ''
\echo 'Test 8: experiment_user can access experiment_schema'
\echo '================================================'
\c turaf experiment_user
CREATE TABLE experiment_schema.test_table (id INT);
DROP TABLE experiment_schema.test_table;
\echo '✅ PASS: experiment_user can access experiment_schema'

\echo ''
\echo 'Test 9: metrics_user can access metrics_schema'
\echo '================================================'
\c turaf metrics_user
CREATE TABLE metrics_schema.test_table (id INT);
DROP TABLE metrics_schema.test_table;
\echo '✅ PASS: metrics_user can access metrics_schema'

\echo ''
\echo 'Test 10: communications_user can access communications_schema'
\echo '================================================'
\c turaf communications_user
CREATE TABLE communications_schema.test_table (id INT);
DROP TABLE communications_schema.test_table;
\echo '✅ PASS: communications_user can access communications_schema'

-- Test 11: Users cannot read from other schemas
\echo ''
\echo 'Test 11: Cross-schema read isolation'
\echo '================================================'
\c turaf identity_user
DO $$
DECLARE
  result INTEGER;
BEGIN
  -- Try to read from organization_schema (should fail)
  SELECT COUNT(*) INTO result FROM organization_schema.organizations;
  RAISE EXCEPTION 'ISOLATION VIOLATION: identity_user can read from organization_schema';
EXCEPTION
  WHEN insufficient_privilege OR undefined_table THEN
    RAISE NOTICE '✅ PASS: identity_user cannot read from organization_schema';
END $$;

\echo ''
\echo '=========================================='
\echo '✅ All schema isolation tests passed!'
\echo '=========================================='
\echo ''
\echo 'Summary:'
\echo '  - Users cannot create tables in other schemas'
\echo '  - Users cannot read from other schemas'
\echo '  - Users can fully access their own schema'
\echo '  - Schema isolation is properly enforced'
\echo '=========================================='
