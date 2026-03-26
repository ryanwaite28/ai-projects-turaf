# Docker Compose Local Development Setup

Create a comprehensive Docker Compose environment for local development that replicates the AWS production architecture using PostgreSQL with multi-schema isolation and LocalStack for AWS service mocking, with all migration scripts updated to include schema creation and idempotent guards.

---

## Overview

**Objective**: Set up a complete local development environment that mirrors the production architecture as closely as possible while using free-tier alternatives.

**Components**:
- PostgreSQL 15.3 with multi-schema architecture (identity, organization, experiment, metrics)
- LocalStack for AWS service mocking (S3, EventBridge, SQS, SNS, Secrets Manager)
- Schema auto-initialization via volume-mounted migration scripts
- Environment variable configuration via `.env` files

---

## Files to Create

### 1. Docker Compose Configuration
- **`docker-compose.yml`** - Main orchestration file
- **`.env.example`** - Template for environment variables
- **`.env`** - Local environment configuration (gitignored)
- **`.dockerignore`** - Exclude unnecessary files from context

### 2. Database Initialization
- **`infrastructure/docker/postgres/init-db.sh`** - Schema and user creation script
- **`infrastructure/docker/postgres/Dockerfile`** (optional) - Custom PostgreSQL image if needed

### 3. LocalStack Configuration
- **`infrastructure/docker/localstack/init-aws.sh`** - Initialize AWS resources (S3 buckets, EventBridge, etc.)
- **`infrastructure/docker/localstack/ready.d/init.sh`** - Auto-run initialization on container ready

### 4. Documentation
- **`docs/LOCAL_DEVELOPMENT.md`** - Complete local setup guide
- Update **`README.md`** - Add local development quick start

---

## Migration Script Updates

All migration scripts will be updated to include:

### Schema Creation Guards
Each service's first migration (V001) will include:
```sql
-- Create schema if not exists
CREATE SCHEMA IF NOT EXISTS {service}_schema;
```

### Idempotent Table Creation
Replace `CREATE TABLE` with:
```sql
CREATE TABLE IF NOT EXISTS {table_name} (
    ...
);
```

### Idempotent Index Creation
Replace `CREATE INDEX` with:
```sql
CREATE INDEX IF NOT EXISTS {index_name} ON {table_name}({columns});
```

### Idempotent Foreign Key Constraints
Use `DO $$ ... END $$` blocks to check constraint existence before adding.

### Files to Update (8 migration files)
1. **identity-service**:
   - `V001__create_users_table.sql`
   - `V002__create_refresh_tokens_table.sql`

2. **organization-service**:
   - `V001__create_organizations_table.sql`
   - `V002__create_organization_members_table.sql`

3. **experiment-service**:
   - `V001__create_problems_table.sql`
   - `V002__create_hypotheses_table.sql`
   - `V003__create_experiments_table.sql`

4. **metrics-service**:
   - `V001__create_metrics_table.sql`

---

## Docker Compose Architecture

### Services

#### 1. PostgreSQL Database
```yaml
postgres:
  image: postgres:15.3-alpine
  container_name: turaf-postgres
  environment:
    - POSTGRES_DB=turaf
    - POSTGRES_USER=turaf_admin
    - POSTGRES_PASSWORD=${DB_ADMIN_PASSWORD}
  volumes:
    - postgres_data:/var/lib/postgresql/data
    - ./infrastructure/docker/postgres/init-db.sh:/docker-entrypoint-initdb.d/init-db.sh
  ports:
    - "5432:5432"
  healthcheck:
    test: ["CMD-EXEC", "pg_isready -U turaf_admin -d turaf"]
    interval: 10s
    timeout: 5s
    retries: 5
```

#### 2. LocalStack (AWS Services)
```yaml
localstack:
  image: localstack/localstack:latest
  container_name: turaf-localstack
  environment:
    - SERVICES=s3,sqs,sns,events,secretsmanager
    - DEBUG=1
    - DATA_DIR=/tmp/localstack/data
    - DOCKER_HOST=unix:///var/run/docker.sock
  volumes:
    - localstack_data:/tmp/localstack
    - ./infrastructure/docker/localstack/init-aws.sh:/etc/localstack/init/ready.d/init-aws.sh
    - /var/run/docker.sock:/var/run/docker.sock
  ports:
    - "4566:4566"  # LocalStack gateway
    - "4571:4571"  # LocalStack web UI (optional)
```

#### 3. Optional: PgAdmin (Database Management UI)
```yaml
pgadmin:
  image: dpage/pgadmin4:latest
  container_name: turaf-pgadmin
  environment:
    - PGADMIN_DEFAULT_EMAIL=${PGADMIN_EMAIL}
    - PGADMIN_DEFAULT_PASSWORD=${PGADMIN_PASSWORD}
  ports:
    - "5050:80"
  depends_on:
    - postgres
```

### Networks
```yaml
networks:
  turaf-network:
    driver: bridge
```

### Volumes
```yaml
volumes:
  postgres_data:
  localstack_data:
```

---

## Database Initialization Script

**File**: `infrastructure/docker/postgres/init-db.sh`

```bash
#!/bin/bash
set -e

echo "Creating schemas and users for Turaf multi-tenant architecture..."

# Create schemas
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    -- Create schemas
    CREATE SCHEMA IF NOT EXISTS identity_schema;
    CREATE SCHEMA IF NOT EXISTS organization_schema;
    CREATE SCHEMA IF NOT EXISTS experiment_schema;
    CREATE SCHEMA IF NOT EXISTS metrics_schema;

    -- Create users
    DO \$\$
    BEGIN
        IF NOT EXISTS (SELECT FROM pg_user WHERE usename = 'identity_user') THEN
            CREATE USER identity_user WITH PASSWORD '${IDENTITY_USER_PASSWORD}';
        END IF;
        IF NOT EXISTS (SELECT FROM pg_user WHERE usename = 'organization_user') THEN
            CREATE USER organization_user WITH PASSWORD '${ORGANIZATION_USER_PASSWORD}';
        END IF;
        IF NOT EXISTS (SELECT FROM pg_user WHERE usename = 'experiment_user') THEN
            CREATE USER experiment_user WITH PASSWORD '${EXPERIMENT_USER_PASSWORD}';
        END IF;
        IF NOT EXISTS (SELECT FROM pg_user WHERE usename = 'metrics_user') THEN
            CREATE USER metrics_user WITH PASSWORD '${METRICS_USER_PASSWORD}';
        END IF;
    END
    \$\$;

    -- Grant permissions for identity_schema
    GRANT ALL PRIVILEGES ON SCHEMA identity_schema TO identity_user;
    GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA identity_schema TO identity_user;
    GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA identity_schema TO identity_user;
    ALTER DEFAULT PRIVILEGES IN SCHEMA identity_schema GRANT ALL ON TABLES TO identity_user;
    ALTER DEFAULT PRIVILEGES IN SCHEMA identity_schema GRANT ALL ON SEQUENCES TO identity_user;

    -- Grant permissions for organization_schema
    GRANT ALL PRIVILEGES ON SCHEMA organization_schema TO organization_user;
    GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA organization_schema TO organization_user;
    GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA organization_schema TO organization_user;
    ALTER DEFAULT PRIVILEGES IN SCHEMA organization_schema GRANT ALL ON TABLES TO organization_user;
    ALTER DEFAULT PRIVILEGES IN SCHEMA organization_schema GRANT ALL ON SEQUENCES TO organization_user;

    -- Grant permissions for experiment_schema
    GRANT ALL PRIVILEGES ON SCHEMA experiment_schema TO experiment_user;
    GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA experiment_schema TO experiment_user;
    GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA experiment_schema TO experiment_user;
    ALTER DEFAULT PRIVILEGES IN SCHEMA experiment_schema GRANT ALL ON TABLES TO experiment_user;
    ALTER DEFAULT PRIVILEGES IN SCHEMA experiment_schema GRANT ALL ON SEQUENCES TO experiment_user;

    -- Grant permissions for metrics_schema
    GRANT ALL PRIVILEGES ON SCHEMA metrics_schema TO metrics_user;
    GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA metrics_schema TO metrics_user;
    GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA metrics_schema TO metrics_user;
    ALTER DEFAULT PRIVILEGES IN SCHEMA metrics_schema GRANT ALL ON TABLES TO metrics_user;
    ALTER DEFAULT PRIVILEGES IN SCHEMA metrics_schema GRANT ALL ON SEQUENCES TO metrics_user;
EOSQL

echo "Database initialization complete!"
```

---

## LocalStack Initialization Script

**File**: `infrastructure/docker/localstack/init-aws.sh`

```bash
#!/bin/bash

echo "Initializing LocalStack AWS resources..."

# Wait for LocalStack to be ready
awslocal s3 ls || sleep 5

# Create S3 buckets
awslocal s3 mb s3://turaf-reports-local || true
awslocal s3 mb s3://turaf-artifacts-local || true

# Create EventBridge event bus
awslocal events create-event-bus --name turaf-events || true

# Create SQS queues
awslocal sqs create-queue --queue-name turaf-experiment-events || true
awslocal sqs create-queue --queue-name turaf-metric-events || true
awslocal sqs create-queue --queue-name turaf-notification-events || true

# Create SNS topics
awslocal sns create-topic --name turaf-notifications || true

# Create Secrets Manager secrets
awslocal secretsmanager create-secret \
    --name turaf/db/identity-user-password \
    --secret-string "${IDENTITY_USER_PASSWORD}" || true

awslocal secretsmanager create-secret \
    --name turaf/db/organization-user-password \
    --secret-string "${ORGANIZATION_USER_PASSWORD}" || true

awslocal secretsmanager create-secret \
    --name turaf/db/experiment-user-password \
    --secret-string "${EXPERIMENT_USER_PASSWORD}" || true

awslocal secretsmanager create-secret \
    --name turaf/db/metrics-user-password \
    --secret-string "${METRICS_USER_PASSWORD}" || true

echo "LocalStack initialization complete!"
```

---

## Environment Variables

**File**: `.env.example`

```bash
# Database Configuration
DB_ADMIN_PASSWORD=admin_password_change_me
IDENTITY_USER_PASSWORD=identity_password_change_me
ORGANIZATION_USER_PASSWORD=organization_password_change_me
EXPERIMENT_USER_PASSWORD=experiment_password_change_me
METRICS_USER_PASSWORD=metrics_password_change_me

# Database Connection (for services)
DB_HOST=localhost
DB_PORT=5432
DB_NAME=turaf

# PgAdmin Configuration (optional)
PGADMIN_EMAIL=admin@turaf.local
PGADMIN_PASSWORD=admin

# LocalStack Configuration
AWS_ENDPOINT_URL=http://localhost:4566
AWS_REGION=us-east-1
AWS_ACCESS_KEY_ID=test
AWS_SECRET_ACCESS_KEY=test

# Service Ports
IDENTITY_SERVICE_PORT=8081
ORGANIZATION_SERVICE_PORT=8082
EXPERIMENT_SERVICE_PORT=8083
METRICS_SERVICE_PORT=8084

# JWT Configuration
JWT_SECRET=your-256-bit-secret-key-for-local-development-only-change-in-production
```

---

## Migration Script Update Pattern

### Example: V001__create_users_table.sql

**Before**:
```sql
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    ...
);

CREATE INDEX idx_users_email ON users(email);
```

**After**:
```sql
-- Create schema if not exists
CREATE SCHEMA IF NOT EXISTS identity_schema;

-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Create indexes (idempotent)
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- Add comments (safe to re-run)
COMMENT ON TABLE users IS 'Stores user account information';
...
```

### Foreign Key Constraint Pattern

**Before**:
```sql
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
```

**After**:
```sql
-- Add foreign key constraint if not exists
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'fk_refresh_tokens_user_id'
    ) THEN
        ALTER TABLE refresh_tokens 
        ADD CONSTRAINT fk_refresh_tokens_user_id 
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
    END IF;
END $$;
```

---

## Usage Instructions

### Initial Setup
```bash
# 1. Copy environment template
cp .env.example .env

# 2. Edit .env with your local passwords
nano .env

# 3. Start all services
docker-compose up -d

# 4. Verify services are running
docker-compose ps

# 5. Check database schemas
docker-compose exec postgres psql -U turaf_admin -d turaf -c "\dn"

# 6. Check LocalStack services
curl http://localhost:4566/_localstack/health
```

### Running Services
Services will connect to the containerized database and LocalStack:
```bash
# Run identity-service
cd services/identity-service
mvn spring-boot:run

# Run organization-service
cd services/organization-service
mvn spring-boot:run

# etc.
```

### Stopping Environment
```bash
# Stop all containers
docker-compose down

# Stop and remove volumes (CAUTION: deletes all data)
docker-compose down -v
```

---

## LocalStack Free Tier Services

**Included** (Free):
- ✅ S3 - Object storage
- ✅ SQS - Message queues
- ✅ SNS - Pub/sub messaging
- ✅ EventBridge (Events) - Event bus
- ✅ Secrets Manager - Secret storage
- ✅ Lambda - Serverless functions (basic)
- ✅ DynamoDB - NoSQL database (if needed)

**Not Included** (Pro tier):
- ❌ ECS - Container orchestration (services run locally via Maven/IDE)
- ❌ RDS - Managed databases (using direct PostgreSQL container)
- ❌ CloudFront - CDN (not needed for local dev)
- ❌ API Gateway - (services expose ports directly)

---

## Testing & Validation

### Database Schema Validation
```bash
# Connect to database
docker-compose exec postgres psql -U turaf_admin -d turaf

# List schemas
\dn

# List tables in identity_schema
\dt identity_schema.*

# Test user permissions
\c turaf identity_user
CREATE TABLE identity_schema.test (id INT);
DROP TABLE identity_schema.test;
```

### LocalStack Validation
```bash
# List S3 buckets
aws --endpoint-url=http://localhost:4566 s3 ls

# List EventBridge buses
aws --endpoint-url=http://localhost:4566 events list-event-buses

# List SQS queues
aws --endpoint-url=http://localhost:4566 sqs list-queues

# Get secret
aws --endpoint-url=http://localhost:4566 secretsmanager get-secret-value \
    --secret-id turaf/db/identity-user-password
```

---

## Benefits

1. **Production Parity**: Mirrors AWS architecture locally
2. **Cost-Free**: No AWS charges for local development
3. **Fast Iteration**: No deployment delays
4. **Offline Development**: Works without internet
5. **Isolated Testing**: Each developer has independent environment
6. **Schema Isolation**: Maintains microservice boundaries locally
7. **Idempotent Migrations**: Safe to re-run migrations

---

## Files Summary

**New Files** (7):
- `docker-compose.yml`
- `.env.example`
- `.dockerignore`
- `infrastructure/docker/postgres/init-db.sh`
- `infrastructure/docker/localstack/init-aws.sh`
- `docs/LOCAL_DEVELOPMENT.md`

**Updated Files** (8 migration scripts):
- All V001, V002, V003 migration files across 4 services

**Updated Documentation** (1):
- `README.md` - Add local development section

**Total**: 7 new files, 9 updated files
