#!/bin/bash
set -e

echo "=========================================="
echo "Initializing Turaf Multi-Schema Database"
echo "=========================================="

# Create schemas and users
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    -- ==========================================
    -- Create Schemas
    -- ==========================================
    CREATE SCHEMA IF NOT EXISTS identity_schema;
    CREATE SCHEMA IF NOT EXISTS organization_schema;
    CREATE SCHEMA IF NOT EXISTS experiment_schema;
    CREATE SCHEMA IF NOT EXISTS metrics_schema;

    -- ==========================================
    -- Create Service Users
    -- ==========================================
    DO \$\$
    BEGIN
        -- Identity User
        IF NOT EXISTS (SELECT FROM pg_user WHERE usename = 'identity_user') THEN
            CREATE USER identity_user WITH PASSWORD '${IDENTITY_USER_PASSWORD}';
            RAISE NOTICE 'Created user: identity_user';
        ELSE
            RAISE NOTICE 'User already exists: identity_user';
        END IF;

        -- Organization User
        IF NOT EXISTS (SELECT FROM pg_user WHERE usename = 'organization_user') THEN
            CREATE USER organization_user WITH PASSWORD '${ORGANIZATION_USER_PASSWORD}';
            RAISE NOTICE 'Created user: organization_user';
        ELSE
            RAISE NOTICE 'User already exists: organization_user';
        END IF;

        -- Experiment User
        IF NOT EXISTS (SELECT FROM pg_user WHERE usename = 'experiment_user') THEN
            CREATE USER experiment_user WITH PASSWORD '${EXPERIMENT_USER_PASSWORD}';
            RAISE NOTICE 'Created user: experiment_user';
        ELSE
            RAISE NOTICE 'User already exists: experiment_user';
        END IF;

        -- Metrics User
        IF NOT EXISTS (SELECT FROM pg_user WHERE usename = 'metrics_user') THEN
            CREATE USER metrics_user WITH PASSWORD '${METRICS_USER_PASSWORD}';
            RAISE NOTICE 'Created user: metrics_user';
        ELSE
            RAISE NOTICE 'User already exists: metrics_user';
        END IF;
    END
    \$\$;

    -- ==========================================
    -- Grant Permissions: identity_schema
    -- ==========================================
    GRANT ALL PRIVILEGES ON SCHEMA identity_schema TO identity_user;
    GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA identity_schema TO identity_user;
    GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA identity_schema TO identity_user;
    ALTER DEFAULT PRIVILEGES IN SCHEMA identity_schema GRANT ALL ON TABLES TO identity_user;
    ALTER DEFAULT PRIVILEGES IN SCHEMA identity_schema GRANT ALL ON SEQUENCES TO identity_user;

    -- ==========================================
    -- Grant Permissions: organization_schema
    -- ==========================================
    GRANT ALL PRIVILEGES ON SCHEMA organization_schema TO organization_user;
    GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA organization_schema TO organization_user;
    GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA organization_schema TO organization_user;
    ALTER DEFAULT PRIVILEGES IN SCHEMA organization_schema GRANT ALL ON TABLES TO organization_user;
    ALTER DEFAULT PRIVILEGES IN SCHEMA organization_schema GRANT ALL ON SEQUENCES TO organization_user;

    -- ==========================================
    -- Grant Permissions: experiment_schema
    -- ==========================================
    GRANT ALL PRIVILEGES ON SCHEMA experiment_schema TO experiment_user;
    GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA experiment_schema TO experiment_user;
    GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA experiment_schema TO experiment_user;
    ALTER DEFAULT PRIVILEGES IN SCHEMA experiment_schema GRANT ALL ON TABLES TO experiment_user;
    ALTER DEFAULT PRIVILEGES IN SCHEMA experiment_schema GRANT ALL ON SEQUENCES TO experiment_user;

    -- ==========================================
    -- Grant Permissions: metrics_schema
    -- ==========================================
    GRANT ALL PRIVILEGES ON SCHEMA metrics_schema TO metrics_user;
    GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA metrics_schema TO metrics_user;
    GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA metrics_schema TO metrics_user;
    ALTER DEFAULT PRIVILEGES IN SCHEMA metrics_schema GRANT ALL ON TABLES TO metrics_user;
    ALTER DEFAULT PRIVILEGES IN SCHEMA metrics_schema GRANT ALL ON SEQUENCES TO metrics_user;

    -- ==========================================
    -- Verify Setup
    -- ==========================================
    SELECT schema_name FROM information_schema.schemata 
    WHERE schema_name LIKE '%_schema' 
    ORDER BY schema_name;
EOSQL

echo "=========================================="
echo "Database initialization complete!"
echo "Schemas created: identity_schema, organization_schema, experiment_schema, metrics_schema"
echo "Users created: identity_user, organization_user, experiment_user, metrics_user"
echo "=========================================="
