# Centralized Flyway Migration Service

**Date**: March 23, 2026  
**Type**: Architecture Change  
**Impact**: Database Migration Management

---

## Summary

Implemented a centralized Flyway migration service to manage all database schema migrations across microservices. This architectural change moves migration management from individual services to a dedicated `flyway-service` executed via AWS CodeBuild.

---

## Motivation

### Previous Architecture

- Each microservice managed its own Flyway migrations
- Flyway dependency in every service's `pom.xml`
- Migrations ran during service startup
- Migration scripts scattered across multiple services
- Difficult to track database changes across the platform

### Problems

1. **Scattered Migration Scripts**: Migration scripts distributed across 5 services
2. **Dependency Overhead**: Each service carried Flyway dependency
3. **Deployment Coupling**: Migrations ran during service startup, coupling schema changes to deployments
4. **Audit Trail**: No single source of truth for database changes
5. **Rollback Complexity**: Difficult to rollback migrations across multiple services

---

## Solution

### Centralized Flyway Service

Created a dedicated `flyway-service` that:
- Consolidates all SQL migration scripts in one location
- Executes via AWS CodeBuild (not a running service)
- Triggered by GitHub Actions on migration script changes
- Runs **before** service deployments in CI/CD pipeline
- Manages all schemas using Flyway's multi-schema support

### Architecture

```
Developer commits migration
  ↓
GitHub Actions detects change
  ↓
Assumes GitHubActionsFlywayRole (OIDC)
  ↓
Triggers AWS CodeBuild
  ↓
CodeBuild (VPC, private subnet)
  ↓
Executes Flyway migrations
  ↓
Connects to RDS PostgreSQL
  ↓
Applies migrations to all schemas
  ↓
Logs results to CloudWatch
  ↓
Reports success/failure to GitHub Actions
  ↓
Service deployments proceed (if migrations succeed)
```

---

## Changes

### New Components

1. **flyway-service Directory Structure**
   - `services/flyway-service/README.md` - Service documentation
   - `services/flyway-service/flyway.conf` - Flyway configuration
   - `services/flyway-service/buildspec.yml` - CodeBuild specification
   - `services/flyway-service/migrations/*.sql` - All migration scripts (9 files)
   - `services/flyway-service/scripts/run-migrations.sh` - Execution script
   - `services/flyway-service/scripts/validate-migrations.sh` - Validation script

2. **GitHub Actions Workflow**
   - `.github/workflows/database-migrations.yml` - Migration execution workflow
   - Triggers: Push to `develop`, `release/*`, or manual dispatch
   - Environments: DEV, QA, PROD

3. **Documentation**
   - `specs/flyway-service.md` - Complete service specification
   - Updated `PROJECT.md` Section 27 - Migration Management

### Modified Components

1. **Microservices** (5 services updated)
   - Removed Flyway configuration from `application.yml`
   - Removed Flyway dependency from `pom.xml` (communications-service)
   - Deleted `src/main/resources/db/migration/` directories
   - Services: identity, organization, experiment, metrics, communications

2. **Migration Scripts** (9 scripts migrated)
   - Renamed with global sequential numbering
   - Added service identifier comments
   - Added schema isolation via `SET search_path`
   - Format: `V{NNN}__{service}_{description}.sql`

### Infrastructure Requirements

**IAM Roles** (to be created):
- `GitHubActionsFlywayRole` - Allows GitHub Actions to trigger CodeBuild
- `CodeBuildFlywayRole` - Allows CodeBuild to execute migrations

**AWS Resources** (to be created):
- CodeBuild project per environment (`turaf-flyway-migrations-{env}`)
- Security group for CodeBuild (`turaf-codebuild-flyway-{env}`)
- Updated RDS security group (allow CodeBuild access)

**GitHub Secrets** (to be added):
- `AWS_FLYWAY_ROLE_DEV` - ARN of GitHubActionsFlywayRole in dev account
- `AWS_FLYWAY_ROLE_QA` - ARN of GitHubActionsFlywayRole in qa account
- `AWS_FLYWAY_ROLE_PROD` - ARN of GitHubActionsFlywayRole in prod account

---

## Migration Naming Convention

### Old Format (per-service)
- `V001__create_users_table.sql` (identity-service)
- `V001__create_organizations_table.sql` (organization-service)
- `V001__create_problems_table.sql` (experiment-service)

### New Format (global sequence)
- `V001__identity_create_users_table.sql`
- `V002__identity_create_refresh_tokens_table.sql`
- `V003__organization_create_organizations_table.sql`
- `V004__organization_create_members_table.sql`
- `V005__experiment_create_problems_table.sql`
- `V006__experiment_create_hypotheses_table.sql`
- `V007__experiment_create_experiments_table.sql`
- `V008__metrics_create_metrics_table.sql`
- `V009__communications_create_tables.sql`

---

## Benefits

### 1. Single Source of Truth
- All database changes in one location
- Easy to track and audit all migrations
- Clear history of database evolution

### 2. Deployment Safety
- Migrations run **before** service deployments
- Service deployments blocked if migrations fail
- Reduces risk of schema/code mismatches

### 3. Simplified Services
- No Flyway dependencies in microservices
- Smaller service artifacts
- Faster service startup (no migration execution)

### 4. Centralized Control
- Single workflow for all database changes
- Consistent migration execution across environments
- Easier to implement migration policies and validations

### 5. Better Audit Trail
- All migrations logged to CloudWatch
- GitHub Actions provides execution history
- Clear visibility into what changed and when

### 6. Improved Rollback
- Centralized rollback procedures
- Can rollback migrations independently of service deployments
- Clear rollback migration naming convention

---

## Migration Guide

### For Developers

**Adding a New Migration**:

1. Create migration script in `services/flyway-service/migrations/`
2. Use next sequential version number
3. Follow naming convention: `V{NNN}__{service}_{description}.sql`
4. Add service identifier comments
5. Use `SET search_path` for schema isolation
6. Test locally first
7. Commit and push
8. GitHub Actions will automatically run migration

**Example**:

```sql
-- Service: identity-service
-- Schema: identity_schema
-- Description: Add email verification column

SET search_path TO identity_schema;

ALTER TABLE users ADD COLUMN email_verified BOOLEAN DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_users_email_verified ON users(email_verified);

SET search_path TO public;
```

### For Operations

**Manual Migration Execution**:

1. Go to GitHub Actions
2. Select "Database Migrations" workflow
3. Click "Run workflow"
4. Select environment (dev, qa, prod)
5. Click "Run workflow"
6. Monitor execution in GitHub Actions UI
7. Check CodeBuild logs in AWS Console if needed

---

## Testing

### Local Testing

```bash
cd services/flyway-service

# Set environment variables
export DB_HOST=localhost
export DB_NAME=turaf
export DB_USER=postgres
export DB_PASSWORD=postgres

# Run migrations
./scripts/run-migrations.sh

# Validate migrations
./scripts/validate-migrations.sh
```

### Validation

- ✅ All 9 migration scripts created and validated
- ✅ Flyway configuration tested locally
- ✅ Migration naming convention verified
- ✅ Schema isolation confirmed via `SET search_path`
- ✅ Service identifier comments added
- ⏳ GitHub Actions workflow (pending infrastructure)
- ⏳ CodeBuild execution (pending infrastructure)
- ⏳ End-to-end testing (pending infrastructure)

---

## Rollback Plan

If issues arise with centralized migrations:

1. **Immediate**: Manually execute rollback SQL via psql
2. **Short-term**: Create rollback migration script
3. **Long-term**: If architecture proves problematic, can revert to per-service migrations (not recommended)

---

## Next Steps

### Infrastructure Tasks

1. Create IAM roles (`GitHubActionsFlywayRole`, `CodeBuildFlywayRole`)
2. Create CodeBuild projects for each environment
3. Configure VPC and security groups for CodeBuild
4. Add GitHub secrets for Flyway role ARNs
5. Test migration execution in DEV environment
6. Validate end-to-end workflow
7. Deploy to QA and PROD environments

### Documentation Tasks

1. ✅ Update PROJECT.md with migration architecture
2. ✅ Create flyway-service specification
3. ⏳ Update infrastructure tasks for IAM roles and CodeBuild
4. ⏳ Update CI/CD documentation
5. ⏳ Create runbook for migration operations

---

## Related Changes

- **Architecture Decision**: ADR-006 (Single Database Multi-Schema) - Complements centralized migrations
- **Infrastructure**: Requires new IAM roles and CodeBuild projects
- **CI/CD**: GitHub Actions workflow for automated execution
- **Security**: OIDC authentication, least-privilege IAM roles

---

## Files Changed

### Created (15 files)
- `services/flyway-service/README.md`
- `services/flyway-service/flyway.conf`
- `services/flyway-service/buildspec.yml`
- `services/flyway-service/scripts/run-migrations.sh`
- `services/flyway-service/scripts/validate-migrations.sh`
- `services/flyway-service/migrations/V001__identity_create_users_table.sql`
- `services/flyway-service/migrations/V002__identity_create_refresh_tokens_table.sql`
- `services/flyway-service/migrations/V003__organization_create_organizations_table.sql`
- `services/flyway-service/migrations/V004__organization_create_members_table.sql`
- `services/flyway-service/migrations/V005__experiment_create_problems_table.sql`
- `services/flyway-service/migrations/V006__experiment_create_hypotheses_table.sql`
- `services/flyway-service/migrations/V007__experiment_create_experiments_table.sql`
- `services/flyway-service/migrations/V008__metrics_create_metrics_table.sql`
- `services/flyway-service/migrations/V009__communications_create_tables.sql`
- `.github/workflows/database-migrations.yml`

### Modified (6 files)
- `PROJECT.md` (Section 27 - Migration Management)
- `services/identity-service/src/main/resources/application.yml`
- `services/organization-service/src/main/resources/application.yml`
- `services/experiment-service/src/main/resources/application.yml`
- `services/metrics-service/src/main/resources/application.yml`
- `services/communications-service/src/main/resources/application.yml`
- `services/communications-service/pom.xml`

### Deleted (5 directories)
- `services/identity-service/src/main/resources/db/migration/`
- `services/organization-service/src/main/resources/db/migration/`
- `services/experiment-service/src/main/resources/db/migration/`
- `services/metrics-service/src/main/resources/db/migration/`
- `services/communications-service/src/main/resources/db/migration/`

---

## Impact Assessment

### Positive Impacts

- ✅ Simplified microservice dependencies
- ✅ Centralized migration management
- ✅ Improved deployment safety
- ✅ Better audit trail
- ✅ Easier rollback procedures

### Risks

- ⚠️ Requires infrastructure setup (IAM roles, CodeBuild)
- ⚠️ New workflow for developers to learn
- ⚠️ Single point of failure (mitigated by GitHub Actions retry and manual execution)

### Mitigation

- Comprehensive documentation provided
- Local testing capability maintained
- Manual execution option available
- Rollback procedures documented

---

## Conclusion

The centralized Flyway migration service represents a significant architectural improvement for database change management. By consolidating migrations into a single service executed via AWS CodeBuild, we achieve better control, safety, and auditability of database changes while simplifying microservice dependencies.

This change aligns with infrastructure-as-code principles and establishes a foundation for scalable database management as the platform grows.
