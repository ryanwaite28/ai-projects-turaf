# Local Development Guide

Complete guide for setting up and running the Turaf platform locally using Docker Compose.

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Quick Start](#quick-start)
3. [Architecture Overview](#architecture-overview)
4. [Environment Configuration](#environment-configuration)
5. [Running Services](#running-services)
6. [Database Management](#database-management)
7. [MiniStack (AWS Services)](#ministack-aws-services)
8. [Troubleshooting](#troubleshooting)
9. [Development Workflows](#development-workflows)

---

## Prerequisites

### Required Software

- **Docker Desktop** 4.0+ ([Download](https://www.docker.com/products/docker-desktop))
- **Docker Compose** 2.0+ (included with Docker Desktop)
- **Java JDK** 17+ ([Download](https://adoptium.net/))
- **Maven** 3.8+ ([Download](https://maven.apache.org/download.cgi))
- **Git** 2.30+ ([Download](https://git-scm.com/downloads))

### Optional Tools

- **AWS CLI** 2.0+ for MiniStack interaction ([Download](https://aws.amazon.com/cli/))
- **PostgreSQL Client** (psql) for direct database access
- **IntelliJ IDEA** or **VS Code** for development

### System Requirements

- **RAM**: 8GB minimum, 16GB recommended
- **Disk Space**: 10GB free space
- **OS**: macOS, Linux, or Windows with WSL2

---

## Quick Start

### 1. Clone Repository

```bash
git clone https://github.com/ryanwaite28/ai-projects-turaf.git
cd ai-projects-turaf
```

### 2. Setup Environment

```bash
# Copy environment template
cp .env.example .env

# Edit .env with your preferences (optional for local dev)
nano .env
```

### 3. Start All Services

**Option A: Run Everything in Docker (Recommended)**

```bash
# Build and start all services (first time)
docker-compose up --build

# Or start in detached mode
docker-compose up -d --build

# Verify all services are running
docker-compose ps

# Check logs for all services
docker-compose logs -f

# Check specific service logs
docker-compose logs -f bff-api
docker-compose logs -f identity-service
```

**Option B: Run Infrastructure Only (Hybrid Development)**

```bash
# Start only database and MiniStack
docker-compose up -d postgres ministack

# Run services locally via Maven
cd services/identity-service
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### 4. Verify Services

```bash
# Check BFF API
curl http://localhost:8080/actuator/health

# Check Identity Service
curl http://localhost:8081/actuator/health

# Check Organization Service
curl http://localhost:8082/actuator/health

# Check Experiment Service
curl http://localhost:8083/actuator/health

# Check Metrics Service
curl http://localhost:8084/actuator/health

# Verify database schemas
docker-compose exec postgres psql -U turaf_admin -d turaf -c "\dn"
```

### 5. Test End-to-End

```bash
# Test via BFF API
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'
```

---

## Architecture Overview

### Local Development Stack

```
┌─────────────────────────────────────────────────────────────┐
│                    Docker Compose Environment                │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────────────────────────────────────────────┐   │
│  │              BFF API Service (:8080)                 │   │
│  │  Orchestrates calls to all microservices             │   │
│  └────┬─────────────┬─────────────┬──────────────┬─────┘   │
│       │             │             │              │           │
│  ┌────▼────┐  ┌────▼────┐  ┌────▼─────┐  ┌────▼─────┐    │
│  │Identity │  │  Org    │  │Experiment│  │ Metrics  │    │
│  │Service  │  │Service  │  │ Service  │  │ Service  │    │
│  │  :8081  │  │  :8082  │  │  :8083   │  │  :8084   │    │
│  └────┬────┘  └────┬────┘  └────┬─────┘  └────┬─────┘    │
│       │            │            │             │            │
│       └────────────┴────────────┴─────────────┘            │
│                    │                                        │
│  ┌─────────────────▼────────────────────────────────┐      │
│  │         PostgreSQL 15.3 (:5432)                  │      │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐      │      │
│  │  │ identity │  │   org    │  │experiment│      │      │
│  │  │ _schema  │  │ _schema  │  │ _schema  │      │      │
│  │  └──────────┘  └──────────┘  └──────────┘      │      │
│  │  ┌──────────┐                                    │      │
│  │  │ metrics  │                                    │      │
│  │  │ _schema  │                                    │      │
│  │  └──────────┘                                    │      │
│  └──────────────────────────────────────────────────┘      │
│                                                               │
│  ┌──────────────────────────────────────────────────────┐   │
│  │     MiniStack (AWS Emulator) (:4566)                 │   │
│  │  • S3  • EventBridge  • SQS  • SNS  • Secrets Mgr   │   │
│  └──────────────────────────────────────────────────────┘   │
│                                                               │
│  ┌──────────────────────────────────────────────────────┐   │
│  │         PgAdmin (Optional) (:5050)                   │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘

Services communicate via Docker container names:
- BFF calls: http://identity-service:8081
- BFF calls: http://organization-service:8082
- BFF calls: http://experiment-service:8083
- BFF calls: http://metrics-service:8084
```

### Database Schema Isolation

Each microservice has its own PostgreSQL schema:

| Service | Schema | User | Port |
|---------|--------|------|------|
| identity-service | `identity_schema` | `identity_user` | 8081 |
| organization-service | `organization_schema` | `organization_user` | 8082 |
| experiment-service | `experiment_schema` | `experiment_user` | 8083 |
| metrics-service | `metrics_schema` | `metrics_user` | 8084 |

**Key Principles**:
- ✅ Each service connects only to its schema
- ✅ Database-level permissions enforce isolation
- ✅ No cross-schema foreign keys
- ✅ Services communicate via APIs, not database joins

---

## Environment Configuration

### Default Configuration (.env)

The `.env.example` file provides sensible defaults for local development:

```bash
# Database
DB_NAME=turaf
DB_ADMIN_USER=turaf_admin
DB_ADMIN_PASSWORD=admin_password_change_me
DB_HOST=localhost
DB_PORT=5432

# Service Users (auto-created on startup)
IDENTITY_USER_PASSWORD=identity_password_change_me
ORGANIZATION_USER_PASSWORD=organization_password_change_me
EXPERIMENT_USER_PASSWORD=experiment_password_change_me
METRICS_USER_PASSWORD=metrics_password_change_me

# MiniStack
AWS_ENDPOINT_URL=http://localhost:4566
AWS_REGION=us-east-1
AWS_ACCESS_KEY_ID=test
AWS_SECRET_ACCESS_KEY=test

# Service Ports
IDENTITY_SERVICE_PORT=8081
ORGANIZATION_SERVICE_PORT=8082
EXPERIMENT_SERVICE_PORT=8083
METRICS_SERVICE_PORT=8084
```

### Service-Specific Configuration

Each service's `application.yml` is pre-configured for local development:

**Example: identity-service**
```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:turaf}?currentSchema=identity_schema
    username: ${DB_USERNAME:identity_user}
    password: ${DB_PASSWORD:postgres}
  jpa:
    properties:
      hibernate:
        default_schema: identity_schema
  flyway:
    schemas: identity_schema
    default-schema: identity_schema
```

---

## Running Services

### Option 1: All Services in Docker (Recommended)

**Start Everything**:
```bash
# First time: Build and start all services
docker-compose up --build

# Subsequent runs: Start without rebuilding
docker-compose up -d

# View logs
docker-compose logs -f

# View specific service logs
docker-compose logs -f bff-api
docker-compose logs -f identity-service
```

**Service Startup Order**:
1. PostgreSQL (with schema initialization)
2. MiniStack (with AWS resources)
3. Identity Service
4. Organization Service
5. Experiment Service
6. Metrics Service
7. BFF API (waits for all microservices)

**Verify All Services**:
```bash
# Check all services are healthy
docker-compose ps

# Expected output: All services "Up (healthy)"
```

### Option 2: Hybrid Development (Some in Docker, Some Local)

**Start Infrastructure Only**:
```bash
# Start database and MiniStack
docker-compose up -d postgres ministack

# Verify infrastructure is ready
docker-compose ps postgres ministack
```

**Run Services Locally via Maven**:
```bash
# Terminal 1: Identity Service
cd services/identity-service
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Terminal 2: Organization Service
cd services/organization-service
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Terminal 3: Experiment Service
cd services/experiment-service
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Terminal 4: Metrics Service
cd services/metrics-service
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Terminal 5: BFF API
cd services/bff-api
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### Option 3: Selective Service Restart

**Rebuild and Restart Single Service**:
```bash
# Rebuild specific service
docker-compose up -d --build identity-service

# View logs for debugging
docker-compose logs -f identity-service

# Restart without rebuilding
docker-compose restart identity-service
```

### Verify Services

```bash
# Check BFF API
curl http://localhost:8080/actuator/health

# Check all microservices
curl http://localhost:8081/actuator/health  # Identity
curl http://localhost:8082/actuator/health  # Organization
curl http://localhost:8083/actuator/health  # Experiment
curl http://localhost:8084/actuator/health  # Metrics

# Test via BFF
curl http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'
```

### Stop Services

```bash
# Stop all services
docker-compose down

# Stop and remove volumes (CAUTION: deletes all data)
docker-compose down -v

# Stop specific service
docker-compose stop identity-service
```

---

## Database Management

### Access Database via psql

```bash
# Connect as admin
docker-compose exec postgres psql -U turaf_admin -d turaf

# Connect as service user
docker-compose exec postgres psql -U identity_user -d turaf

# Common commands
\dn                          # List schemas
\dt identity_schema.*        # List tables in schema
\d identity_schema.users     # Describe table
\q                           # Quit
```

### Access via PgAdmin

1. Start PgAdmin:
   ```bash
   docker-compose --profile tools up -d pgadmin
   ```

2. Open browser: http://localhost:5050

3. Login:
   - Email: `admin@turaf.local`
   - Password: `admin`

4. Add server:
   - Host: `postgres` (container name)
   - Port: `5432`
   - Database: `turaf`
   - Username: `turaf_admin`
   - Password: (from .env)

### Run Migrations Manually

Migrations run automatically when services start, but you can run them manually:

```bash
cd services/identity-service
mvn flyway:migrate

cd services/organization-service
mvn flyway:migrate

cd services/experiment-service
mvn flyway:migrate

cd services/metrics-service
mvn flyway:migrate
```

### Reset Database

```bash
# Stop services
docker-compose down

# Remove volumes (CAUTION: deletes all data)
docker-compose down -v

# Restart
docker-compose up -d

# Migrations will run automatically when services start
```

---

## MiniStack (AWS Services)

### Available Services

[MiniStack](https://github.com/Nahuel990/ministack) (`nahuelnucera/ministack`) provides local AWS service emulation. It is a free, open-source, MIT-licensed alternative to LocalStack with a ~150MB image footprint:

| Service | Purpose | Endpoint |
|---------|---------|----------|
| S3 | Object storage | http://localhost:4566 |
| EventBridge | Event bus | http://localhost:4566 |
| SQS | Message queues | http://localhost:4566 |
| SNS | Pub/sub messaging | http://localhost:4566 |
| DynamoDB | NoSQL database | http://localhost:4566 |
| SES | Email service | http://localhost:4566 |
| Secrets Manager | Secret storage | http://localhost:4566 |

### Using AWS CLI with MiniStack

```bash
# Set endpoint for the session
export AWS_ENDPOINT_URL=http://localhost:4566
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_DEFAULT_REGION=us-east-1

# List S3 buckets
aws --endpoint-url=http://localhost:4566 s3 ls

# List SQS queues
aws --endpoint-url=http://localhost:4566 sqs list-queues

# List EventBridge buses
aws --endpoint-url=http://localhost:4566 events list-event-buses

# Get secret
aws --endpoint-url=http://localhost:4566 secretsmanager get-secret-value \
  --secret-id turaf/db/identity-user-password
```

### Pre-Created Resources

The initialization script creates:

**S3 Buckets**:
- `turaf-reports-local`
- `turaf-artifacts-local`
- `turaf-frontend-local`

**EventBridge**:
- Event bus: `turaf-events`

**SQS Queues**:
- `turaf-experiment-events`
- `turaf-metric-events`
- `turaf-notification-events`
- `turaf-report-events`
- `turaf-dlq` (dead letter queue)

**SNS Topics**:
- `turaf-notifications`
- `turaf-alerts`

**DynamoDB Tables**:
- `processed_notification_events` (with TTL enabled)
- `processed_events` (with TTL enabled)

**SES Identities**:
- `notifications@turaf.com`

**Secrets**:
- Database user passwords (all services)

### MiniStack Health Check

```bash
# Check MiniStack status
curl http://localhost:4566/_localstack/health

# Expected response shows available services
```

---

## Troubleshooting

### Database Connection Issues

**Problem**: Service can't connect to database

```bash
# Check if PostgreSQL is running
docker-compose ps postgres

# Check PostgreSQL logs
docker-compose logs postgres

# Verify schemas exist
docker-compose exec postgres psql -U turaf_admin -d turaf -c "\dn"

# Test connection
docker-compose exec postgres psql -U identity_user -d turaf \
  -c "SELECT current_schema();"
```

### Schema Permission Issues

**Problem**: Service user can't access schema

```bash
# Check permissions
docker-compose exec postgres psql -U turaf_admin -d turaf <<SQL
SELECT 
  n.nspname as schema_name,
  r.rolname as user_name,
  has_schema_privilege(r.rolname, n.nspname, 'USAGE') as has_usage,
  has_schema_privilege(r.rolname, n.nspname, 'CREATE') as has_create
FROM pg_namespace n
CROSS JOIN pg_roles r
WHERE n.nspname LIKE '%_schema'
  AND r.rolname LIKE '%_user';
SQL
```

### Migration Failures

**Problem**: Flyway migration fails

```bash
# Check migration history
docker-compose exec postgres psql -U identity_user -d turaf \
  -c "SELECT * FROM identity_schema.flyway_schema_history;"

# Repair Flyway (if needed)
cd services/identity-service
mvn flyway:repair

# Re-run migrations
mvn flyway:migrate
```

### MiniStack Not Responding

**Problem**: MiniStack services unavailable

```bash
# Check MiniStack status
docker-compose ps ministack

# Restart MiniStack
docker-compose restart ministack

# Check initialization logs
docker-compose logs ministack | grep "initialization complete"

# Verify health
curl http://localhost:4566/_localstack/health
```

### Port Conflicts

**Problem**: Port already in use

```bash
# Check what's using port 5432
lsof -i :5432

# Change port in .env
DB_PORT=5433

# Restart
docker-compose down
docker-compose up -d
```

### Clean Slate Reset

```bash
# Stop everything
docker-compose down -v

# Remove all Turaf containers and volumes
docker system prune -a --volumes

# Restart
docker-compose up -d

# Verify
docker-compose ps
```

---

## Development Workflows

### Daily Development

**Full Docker Workflow**:
```bash
# 1. Start all services
docker-compose up -d

# 2. Make code changes to a service

# 3. Rebuild and restart that service
docker-compose up -d --build identity-service

# 4. View logs
docker-compose logs -f identity-service

# 5. Stop when done
docker-compose down
```

**Hybrid Workflow** (Faster iteration):
```bash
# 1. Start infrastructure only
docker-compose up -d postgres ministack

# 2. Run service you're working on locally
cd services/identity-service
mvn spring-boot:run -Dspring-boot.run.profiles=local

# 3. Make changes, Spring Boot DevTools auto-reloads

# 4. Stop when done
# Ctrl+C to stop service
docker-compose down  # Stop infrastructure
```

### Testing Changes

```bash
# Run unit tests
cd services/identity-service
mvn test

# Run integration tests (requires running database)
mvn verify

# Run specific test
mvn test -Dtest=UserServiceTest
```

### Adding New Migration

```bash
# 1. Create migration file
cd services/identity-service/src/main/resources/db/migration
touch V003__add_user_preferences.sql

# 2. Write migration with idempotent guards
cat > V003__add_user_preferences.sql <<'EOF'
CREATE TABLE IF NOT EXISTS user_preferences (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    preferences JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_user_preferences_user_id 
  ON user_preferences(user_id);
EOF

# 3. Restart service (migration runs automatically)
mvn spring-boot:run
```

### Debugging Database Issues

```bash
# Enable SQL logging
# In application.yml, set:
spring:
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true

logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

### Working with MiniStack

```bash
# Upload file to S3
aws --endpoint-url=http://localhost:4566 s3 cp report.pdf s3://turaf-reports-local/

# Send message to SQS
aws --endpoint-url=http://localhost:4566 sqs send-message \
  --queue-url http://localhost:4566/000000000000/turaf-experiment-events \
  --message-body '{"event":"experiment.created","id":"123"}'

# Publish to SNS
aws --endpoint-url=http://localhost:4566 sns publish \
  --topic-arn arn:aws:sns:us-east-1:000000000000:turaf-notifications \
  --message "Test notification"
```

---

## Best Practices

### Environment Variables

- ✅ Use `.env` for local overrides
- ✅ Never commit `.env` to version control
- ✅ Keep `.env.example` updated with new variables
- ✅ Use meaningful defaults in `application.yml`

### Database Management

- ✅ Always use idempotent migrations (`IF NOT EXISTS`)
- ✅ Test migrations on clean database
- ✅ Never modify existing migrations
- ✅ Use descriptive migration names

### Service Development

- ✅ Run services individually during development
- ✅ Use Spring Boot DevTools for auto-reload
- ✅ Check health endpoints before testing
- ✅ Review logs for errors

### Docker Compose

- ✅ Use `docker-compose down` to stop cleanly
- ✅ Use `docker-compose logs -f` to monitor
- ✅ Use `docker-compose ps` to check status
- ✅ Use `--profile tools` for optional services

---

## Additional Resources

- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [MiniStack Documentation](https://github.com/Nahuel990/ministack)
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Flyway Documentation](https://flywaydb.org/documentation/)

---

## Getting Help

**Common Issues**: Check [Troubleshooting](#troubleshooting) section

**Database Questions**: See [Database Management](#database-management)

**MiniStack Questions**: See [MiniStack](#ministack-aws-services)

**Service Issues**: Check service logs and health endpoints

---

**Last Updated**: March 27, 2026  
**Version**: 1.0.0
