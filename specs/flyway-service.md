# Flyway Service Specification

**Service**: flyway-service  
**Type**: Database Migration Management  
**Purpose**: Centralized database migration management for all Turaf microservices

---

## Overview

The Flyway Service is a dedicated component that manages all database schema migrations for the Turaf platform. Unlike traditional microservices, it does not run as a persistent application but executes migrations via AWS CodeBuild triggered by GitHub Actions.

---

## Architecture

### Execution Model

**Not a Running Service**: The flyway-service is a collection of migration scripts and configuration, not a deployed application.

**Execution Environment**: AWS CodeBuild

**Trigger Mechanism**: GitHub Actions workflow

**Execution Flow**:
1. Developer commits migration script to `services/flyway-service/migrations/`
2. GitHub Actions detects changes in `services/flyway-service/**`
3. Workflow assumes `GitHubActionsFlywayRole` via OIDC
4. Workflow triggers AWS CodeBuild project
5. CodeBuild executes Flyway migrations in VPC
6. Results logged to CloudWatch
7. Success/failure reported to GitHub Actions

---

## Directory Structure

```
services/flyway-service/
├── README.md                    # Service documentation
├── flyway.conf                  # Flyway configuration
├── buildspec.yml                # AWS CodeBuild specification
├── migrations/                  # SQL migration scripts
│   ├── V001__identity_create_users_table.sql
│   ├── V002__identity_create_refresh_tokens_table.sql
│   ├── V003__organization_create_organizations_table.sql
│   ├── V004__organization_create_members_table.sql
│   ├── V005__experiment_create_problems_table.sql
│   ├── V006__experiment_create_hypotheses_table.sql
│   ├── V007__experiment_create_experiments_table.sql
│   ├── V008__metrics_create_metrics_table.sql
│   └── V009__communications_create_tables.sql
└── scripts/
    ├── run-migrations.sh        # Migration execution script
    └── validate-migrations.sh   # Migration validation script
```

---

## Migration Naming Convention

### Format

`V{NNN}__{service}_{description}.sql`

### Components

- **V**: Version prefix (required by Flyway)
- **{NNN}**: 3-digit zero-padded sequential number (001, 002, 003, etc.)
- **__**: Double underscore separator (required by Flyway)
- **{service}**: Service name (identity, organization, experiment, metrics, communications)
- **_{description}**: Snake_case description of the migration

### Examples

- `V001__identity_create_users_table.sql`
- `V002__identity_create_refresh_tokens_table.sql`
- `V010__identity_add_email_verification.sql`
- `V011__experiment_add_status_column.sql`

### Rules

1. Sequential numbering across all services (global sequence)
2. Service name included for clarity
3. Descriptive names for maintainability
4. No gaps in version numbers

---

## Migration Script Template

```sql
-- Service: {service-name}
-- Schema: {schema_name}
-- Description: {what this migration does}

-- Set search path to target schema
SET search_path TO {schema_name};

-- Migration SQL
CREATE TABLE IF NOT EXISTS table_name (
    id VARCHAR(36) PRIMARY KEY,
    -- columns...
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_table_column ON table_name(column);

-- Add comments
COMMENT ON TABLE table_name IS 'Description of table';

-- Reset search path
SET search_path TO public;
```

---

## Schemas Managed

The flyway-service manages migrations for all microservice schemas:

| Schema | Service | Purpose |
|--------|---------|---------|
| `identity_schema` | identity-service | User authentication, refresh tokens |
| `organization_schema` | organization-service | Organizations, memberships |
| `experiment_schema` | experiment-service | Problems, hypotheses, experiments |
| `metrics_schema` | metrics-service | Time-series metrics data |
| `communications_schema` | communications-service | Conversations, messages, participants |

---

## Flyway Configuration

### flyway.conf

```properties
# Database connection
flyway.url=jdbc:postgresql://${DB_HOST}:5432/${DB_NAME}
flyway.user=${DB_USER}
flyway.password=${DB_PASSWORD}

# Schema configuration
flyway.schemas=identity_schema,organization_schema,experiment_schema,metrics_schema,communications_schema
flyway.defaultSchema=public

# Migration settings
flyway.locations=filesystem:./migrations
flyway.validateOnMigrate=true
flyway.outOfOrder=false
flyway.baselineOnMigrate=true
flyway.baselineVersion=0

# Placeholders
flyway.placeholders.identity_schema=identity_schema
flyway.placeholders.organization_schema=organization_schema
flyway.placeholders.experiment_schema=experiment_schema
flyway.placeholders.metrics_schema=metrics_schema
flyway.placeholders.communications_schema=communications_schema
```

---

## AWS CodeBuild Configuration

### buildspec.yml

Defines the build process for executing migrations:

**Phases**:
1. **Install**: Download and install Flyway CLI
2. **Pre-build**: Validate environment variables and connectivity
3. **Build**: Execute migrations via `run-migrations.sh`
4. **Post-build**: Report results

**Environment Variables**:
- `DB_HOST`: RDS endpoint (from Terraform output)
- `DB_NAME`: Database name (`turaf`)
- `DB_USER`: Database user (`postgres`)
- `DB_PASSWORD`: Database password (from Secrets Manager)

---

## GitHub Actions Workflow

### Workflow File

`.github/workflows/database-migrations.yml`

### Triggers

**Automatic**:
- Push to `develop` branch → DEV environment
- Push to `release/*` branches → QA environment

**Manual**:
- Workflow dispatch with environment selection (dev, qa, prod)

**Path Filter**:
- Only triggers when files in `services/flyway-service/**` change

### Workflow Steps

1. **Checkout code**: Clone repository
2. **Determine environment**: Based on branch or manual input
3. **Set AWS account ID**: Map environment to AWS account
4. **Configure AWS credentials**: Assume `GitHubActionsFlywayRole` via OIDC
5. **Trigger CodeBuild**: Start migration build
6. **Wait for completion**: Poll CodeBuild status
7. **Get build status**: Check success/failure
8. **Summary**: Report results in GitHub Actions UI

---

## IAM Roles

### GitHubActionsFlywayRole

**Purpose**: Allows GitHub Actions to trigger CodeBuild

**Permissions**:
- `codebuild:StartBuild`
- `codebuild:BatchGetBuilds`
- `logs:GetLogEvents`
- `logs:FilterLogEvents`

**Trust Policy**: GitHub OIDC provider for repository `ryanwaite28/ai-projects-turaf`

### CodeBuildFlywayRole

**Purpose**: Allows CodeBuild to execute migrations

**Permissions**:
- `secretsmanager:GetSecretValue` (database credentials)
- `logs:CreateLogGroup`, `logs:CreateLogStream`, `logs:PutLogEvents`
- `ec2:CreateNetworkInterface`, `ec2:DescribeNetworkInterfaces`, etc. (VPC access)
- `ecr:GetAuthorizationToken`, `ecr:BatchGetImage` (if using custom image)

---

## Network Configuration

### VPC Configuration

CodeBuild runs in the same VPC as RDS:

- **VPC**: Same as RDS instance
- **Subnets**: Private subnets
- **Security Group**: `turaf-codebuild-flyway-{env}`

### Security Groups

**CodeBuild Security Group**:
- Egress: TCP 5432 → RDS security group
- Egress: TCP 443 → Internet (for package downloads)

**RDS Security Group** (updated):
- Ingress: TCP 5432 ← CodeBuild security group

---

## Migration History

Flyway tracks migration history in the `flyway_schema_history` table in the `public` schema:

```sql
SELECT * FROM public.flyway_schema_history ORDER BY installed_rank;
```

Each migration is recorded with:
- Version number
- Description
- Script name
- Checksum
- Installed timestamp
- Execution time
- Success status

---

## Rollback Strategy

### Automated Rollback

If migration fails:
1. CodeBuild build fails
2. GitHub Actions workflow fails
3. Service deployments are blocked (migrations run first in CD pipeline)
4. Manual intervention required

### Manual Rollback

1. Create rollback migration script (e.g., `V012__identity_rollback_feature.sql`)
2. Commit and push to trigger migration pipeline
3. Verify rollback success
4. Deploy previous service versions if needed

### Emergency Rollback

1. Use Flyway's `repair` command to fix migration history
2. Manually execute rollback SQL via psql
3. Update Flyway history table
4. Document incident

---

## Local Development

### Prerequisites

- Docker installed
- PostgreSQL client installed
- Access to local PostgreSQL instance

### Run Migrations Locally

```bash
cd services/flyway-service

# Set environment variables
export DB_HOST=localhost
export DB_NAME=turaf
export DB_USER=postgres
export DB_PASSWORD=postgres

# Run migrations
./scripts/run-migrations.sh
```

### Validate Migrations

```bash
# Validate migration syntax and naming
./scripts/validate-migrations.sh
```

---

## Best Practices

### Migration Development

1. **Test Locally First**: Always test migrations on local database before committing
2. **Idempotent Migrations**: Use `IF NOT EXISTS` and `IF EXISTS` clauses
3. **Small Changes**: Keep migrations small and focused on single change
4. **No Data Migrations**: Separate schema changes from data migrations
5. **Backward Compatible**: Ensure migrations don't break running services
6. **Review Before Merge**: Always review migration scripts in PRs

### Naming Conventions

1. Use sequential numbering (no gaps)
2. Include service name for clarity
3. Use descriptive names
4. Follow snake_case convention

### Schema Isolation

1. Always use `SET search_path` to target specific schema
2. Reset search path to `public` at end of script
3. Never create cross-schema foreign keys
4. Use service identifier comments

---

## Monitoring

### CloudWatch Logs

All migration executions logged to:
- Log Group: `/aws/codebuild/turaf-flyway-migrations-{env}`
- Retention: 30 days

### Metrics

Monitor:
- Migration execution time
- Success/failure rate
- Number of migrations applied
- CodeBuild build duration

### Alerts

Set up CloudWatch alarms for:
- Migration failures
- Execution time exceeding threshold
- Repeated failures

---

## Security

### Credentials Management

- Database password stored in AWS Secrets Manager
- Never commit credentials to repository
- CodeBuild accesses credentials via IAM role

### Network Security

- CodeBuild runs in private VPC subnets
- No direct internet access (NAT Gateway for outbound)
- Security groups restrict access to RDS only

### Access Control

- OIDC authentication for GitHub Actions (no long-lived credentials)
- Least-privilege IAM roles
- Audit trail in CloudWatch and CloudTrail

---

## Troubleshooting

### Migration Fails

1. Check CodeBuild logs in CloudWatch
2. Verify SQL syntax in migration script
3. Check schema permissions
4. Verify network connectivity to RDS

### CodeBuild Cannot Connect to RDS

1. Verify security group rules
2. Check VPC configuration
3. Verify RDS endpoint is correct
4. Check Secrets Manager credentials

### Migration Already Applied

Flyway tracks applied migrations. If a migration was already applied, it will be skipped.

To reapply:
1. Delete entry from `flyway_schema_history` table
2. Re-run migration

---

## References

- [Flyway Documentation](https://flywaydb.org/documentation/)
- [AWS CodeBuild](https://docs.aws.amazon.com/codebuild/)
- [GitHub Actions OIDC](https://docs.github.com/en/actions/deployment/security-hardening-your-deployments/configuring-openid-connect-in-amazon-web-services)
- PROJECT.md - Section 27 (Data Architecture)
- specs/aws-infrastructure.md - RDS and CodeBuild configuration
- specs/ci-cd-pipelines.md - GitHub Actions workflows
