# Flyway Service - Centralized Database Migrations

This service manages all database migrations for the Turaf platform using Flyway. It centralizes migration management across all microservices and schemas.

## Overview

**Purpose**: Centralized database migration management for all Turaf services

**Execution**: AWS CodeBuild triggered by GitHub Actions

**Database**: Single PostgreSQL instance with multi-schema architecture

**Schemas Managed**:
- `identity_schema` - User authentication and authorization
- `organization_schema` - Organization and membership management
- `experiment_schema` - Problems, hypotheses, and experiments
- `metrics_schema` - Metrics and aggregations
- `communications_schema` - Conversations and messages

## Directory Structure

```
flyway-service/
├── README.md                    # This file
├── flyway.conf                  # Flyway configuration
├── buildspec.yml                # AWS CodeBuild specification
├── migrations/                  # SQL migration scripts
│   ├── V001__identity_create_users_table.sql
│   ├── V002__identity_create_refresh_tokens_table.sql
│   ├── V003__organization_create_organizations_table.sql
│   └── ...
└── scripts/
    ├── run-migrations.sh        # Migration execution script
    └── validate-migrations.sh   # Migration validation script
```

## Migration Naming Convention

**Format**: `V{NNN}__{service}_{description}.sql`

**Examples**:
- `V001__identity_create_users_table.sql`
- `V002__identity_create_refresh_tokens_table.sql`
- `V003__organization_create_organizations_table.sql`
- `V010__identity_add_email_verification.sql`

**Rules**:
1. Use 3-digit zero-padded version numbers (V001, V002, etc.)
2. Include service name after version number
3. Use descriptive snake_case names
4. Add service identifier comment at top of file

## Migration Script Template

```sql
-- Service: {service-name}
-- Schema: {schema_name}
-- Description: {what this migration does}

-- Set search path to target schema
SET search_path TO {schema_name};

-- Migration SQL
CREATE TABLE IF NOT EXISTS table_name (
    id UUID PRIMARY KEY,
    -- columns...
);

-- Reset search path
SET search_path TO public;
```

## Execution Flow

1. Developer creates new migration script in `migrations/` directory
2. Developer commits and pushes to GitHub
3. GitHub Actions workflow triggers on push to `develop`, `release/*`, or manual dispatch
4. GitHub Actions assumes `GitHubActionsFlywayRole` via OIDC
5. GitHub Actions triggers AWS CodeBuild project
6. CodeBuild runs in VPC with access to RDS
7. CodeBuild executes `run-migrations.sh` script
8. Flyway applies migrations to all schemas
9. Migration results logged to CloudWatch
10. GitHub Actions reports success/failure

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

## AWS CodeBuild Execution

### Environment Variables

CodeBuild project is configured with:

- `DB_HOST` - RDS endpoint (from Terraform output)
- `DB_NAME` - Database name (`turaf`)
- `DB_USER` - Database user (`postgres`)
- `DB_PASSWORD` - Database password (from Secrets Manager)

### VPC Configuration

CodeBuild runs in:
- **VPC**: Same VPC as RDS
- **Subnets**: Private subnets
- **Security Group**: `turaf-codebuild-flyway-{env}`

### IAM Role

CodeBuild uses `CodeBuildFlywayRole` with permissions for:
- Read Secrets Manager (database credentials)
- Write CloudWatch Logs
- Create VPC network interfaces
- Pull from ECR (if needed)

## GitHub Actions Integration

### Workflow File

`.github/workflows/database-migrations.yml`

### Triggers

- **Automatic**: Push to `develop` (DEV), `release/*` (QA)
- **Manual**: Workflow dispatch with environment selection
- **Path Filter**: Only when `services/flyway-service/**` changes

### Secrets Required

- `AWS_FLYWAY_ROLE_DEV` - ARN of GitHubActionsFlywayRole in dev account
- `AWS_FLYWAY_ROLE_QA` - ARN of GitHubActionsFlywayRole in qa account
- `AWS_FLYWAY_ROLE_PROD` - ARN of GitHubActionsFlywayRole in prod account

## Rollback Strategy

### Automated Rollback

If migration fails:
1. CodeBuild build fails
2. GitHub Actions workflow fails
3. Service deployments are blocked (migrations run first)
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

## Migration History

Flyway tracks migration history in `flyway_schema_history` table in each schema:

```sql
-- View migration history for identity schema
SET search_path TO identity_schema;
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
```

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

## Best Practices

1. **Test Locally First**: Always test migrations on local database before committing
2. **Idempotent Migrations**: Use `IF NOT EXISTS` and `IF EXISTS` clauses
3. **Small Changes**: Keep migrations small and focused
4. **No Data Migrations**: Separate schema changes from data migrations
5. **Backward Compatible**: Ensure migrations don't break running services
6. **Review Before Merge**: Always review migration scripts in PRs

## Security

- Database credentials stored in AWS Secrets Manager
- CodeBuild runs in private VPC subnets
- No direct internet access (NAT Gateway for outbound)
- OIDC authentication for GitHub Actions (no long-lived credentials)
- Least-privilege IAM roles

## Monitoring

- CodeBuild build status in AWS Console
- CloudWatch Logs for migration output
- GitHub Actions workflow status
- Flyway migration history in database

## References

- [Flyway Documentation](https://flywaydb.org/documentation/)
- [AWS CodeBuild](https://docs.aws.amazon.com/codebuild/)
- [GitHub Actions OIDC](https://docs.github.com/en/actions/deployment/security-hardening-your-deployments/configuring-openid-connect-in-amazon-web-services)
- PROJECT.md - Section 27 (Data Architecture)
- specs/flyway-service.md - Complete specification
