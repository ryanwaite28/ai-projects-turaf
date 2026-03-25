-- Turaf Database Schema Initialization
-- Single database, multi-schema design for service isolation

-- Create schemas for each microservice
CREATE SCHEMA IF NOT EXISTS identity_schema;
CREATE SCHEMA IF NOT EXISTS organization_schema;
CREATE SCHEMA IF NOT EXISTS experiment_schema;
CREATE SCHEMA IF NOT EXISTS metrics_schema;

-- Create service-specific database users
-- Note: Passwords are passed as variables from Terraform
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'identity_user') THEN
        EXECUTE format('CREATE USER identity_user WITH PASSWORD %L', :'identity_password');
    END IF;
    
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'organization_user') THEN
        EXECUTE format('CREATE USER organization_user WITH PASSWORD %L', :'organization_password');
    END IF;
    
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'experiment_user') THEN
        EXECUTE format('CREATE USER experiment_user WITH PASSWORD %L', :'experiment_password');
    END IF;
    
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'metrics_user') THEN
        EXECUTE format('CREATE USER metrics_user WITH PASSWORD %L', :'metrics_password');
    END IF;
END
$$;

-- Grant schema-level permissions to identity_user
GRANT ALL PRIVILEGES ON SCHEMA identity_schema TO identity_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA identity_schema TO identity_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA identity_schema TO identity_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA identity_schema GRANT ALL ON TABLES TO identity_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA identity_schema GRANT ALL ON SEQUENCES TO identity_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA identity_schema GRANT ALL ON FUNCTIONS TO identity_user;

-- Set search path for identity_user
ALTER USER identity_user SET search_path TO identity_schema;

-- Grant schema-level permissions to organization_user
GRANT ALL PRIVILEGES ON SCHEMA organization_schema TO organization_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA organization_schema TO organization_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA organization_schema TO organization_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA organization_schema GRANT ALL ON TABLES TO organization_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA organization_schema GRANT ALL ON SEQUENCES TO organization_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA organization_schema GRANT ALL ON FUNCTIONS TO organization_user;

-- Set search path for organization_user
ALTER USER organization_user SET search_path TO organization_schema;

-- Grant schema-level permissions to experiment_user
GRANT ALL PRIVILEGES ON SCHEMA experiment_schema TO experiment_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA experiment_schema TO experiment_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA experiment_schema TO experiment_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA experiment_schema GRANT ALL ON TABLES TO experiment_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA experiment_schema GRANT ALL ON SEQUENCES TO experiment_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA experiment_schema GRANT ALL ON FUNCTIONS TO experiment_user;

-- Set search path for experiment_user
ALTER USER experiment_user SET search_path TO experiment_schema;

-- Grant schema-level permissions to metrics_user
GRANT ALL PRIVILEGES ON SCHEMA metrics_schema TO metrics_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA metrics_schema TO metrics_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA metrics_schema TO metrics_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA metrics_schema GRANT ALL ON TABLES TO metrics_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA metrics_schema GRANT ALL ON SEQUENCES TO metrics_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA metrics_schema GRANT ALL ON FUNCTIONS TO metrics_user;

-- Set search path for metrics_user
ALTER USER metrics_user SET search_path TO metrics_schema;

-- Revoke public schema access from service users (security hardening)
REVOKE ALL ON SCHEMA public FROM identity_user;
REVOKE ALL ON SCHEMA public FROM organization_user;
REVOKE ALL ON SCHEMA public FROM experiment_user;
REVOKE ALL ON SCHEMA public FROM metrics_user;

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";
CREATE EXTENSION IF NOT EXISTS "btree_gin";

-- Grant extension usage to all service users
GRANT USAGE ON SCHEMA public TO identity_user, organization_user, experiment_user, metrics_user;

-- Output confirmation
DO $$
BEGIN
    RAISE NOTICE 'Schema initialization completed successfully';
    RAISE NOTICE 'Created schemas: identity_schema, organization_schema, experiment_schema, metrics_schema';
    RAISE NOTICE 'Created users: identity_user, organization_user, experiment_user, metrics_user';
    RAISE NOTICE 'Permissions granted and search paths configured';
END
$$;
