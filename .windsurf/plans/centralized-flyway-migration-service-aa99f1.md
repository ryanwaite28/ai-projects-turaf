# Centralized Flyway Migration Service

This plan implements a dedicated flyway-service to centralize all database migrations across microservices, removing Flyway dependencies from individual services and establishing a CI/CD pipeline using AWS CodeBuild for migration execution.

---

## Overview

**Current State**:
- Each service (identity, organization, experiment, metrics, communications) manages its own Flyway migrations
- Flyway dependencies exist in each service's `pom.xml`
- Migrations run during service startup via Spring Boot auto-configuration
- Migration scripts located in each service's `src/main/resources/db/migration/`

**Target State**:
- New `flyway-service` repository structure containing all SQL migration scripts
- Centralized migration management for all schemas (identity_schema, organization_schema, experiment_schema, metrics_schema, communications_schema)
- GitHub Actions workflow triggers AWS CodeBuild to execute migrations
- Migrations run **before** service deployments in the CD pipeline
- Services remove Flyway Maven dependencies and migration scripts

---

## Architecture Decisions

Based on your requirements and industry best practices:

1. **Service Structure**: Simple directory structure with SQL scripts and Flyway CLI configuration (not a Spring Boot app)
2. **AWS Execution**: Use **AWS CodeBuild** directly (simpler, more cost-effective than CodePipeline for this use case)
3. **Migration Versioning**: Global sequential numbering with service names in filenames (e.g., `V001__identity_create_users_table.sql`)
4. **Deployment Timing**: Migrations run before service deployments as a separate pipeline step
5. **Schema Management**: Use Flyway's native multi-schema support for maintainability

---

## Implementation Plan

### Phase 1: Create flyway-service Structure

**Directory Structure**:
```
services/flyway-service/
├── README.md
├── flyway.conf                           # Flyway configuration
├── buildspec.yml                         # AWS CodeBuild specification
├── migrations/
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
    ├── run-migrations.sh                 # Migration execution script
    └── validate-migrations.sh            # Validation script
```

**Key Files**:

1. **flyway.conf**: Multi-schema Flyway configuration
   - Configure all schemas: identity_schema, organization_schema, experiment_schema, metrics_schema, communications_schema
   - Use environment variables for database credentials
   - Set migration locations and validation rules

2. **buildspec.yml**: CodeBuild specification
   - Install Flyway CLI
   - Configure AWS credentials
   - Execute migrations with proper error handling
   - Output migration results

3. **run-migrations.sh**: Wrapper script
   - Validate environment variables
   - Run Flyway migrate command
   - Handle errors and rollbacks
   - Log migration status

### Phase 2: Migrate Existing SQL Scripts

**Migration Consolidation**:

Move and rename all existing migration scripts from services to flyway-service:

**From identity-service**:
- `V001__create_users_table.sql` → `V001__identity_create_users_table.sql`
- `V002__create_refresh_tokens_table.sql` → `V002__identity_create_refresh_tokens_table.sql`

**From organization-service**:
- `V001__create_organizations_table.sql` → `V003__organization_create_organizations_table.sql`
- `V002__create_organization_members_table.sql` → `V004__organization_create_members_table.sql`

**From experiment-service**:
- `V001__create_problems_table.sql` → `V005__experiment_create_problems_table.sql`
- `V002__create_hypotheses_table.sql` → `V006__experiment_create_hypotheses_table.sql`
- `V003__create_experiments_table.sql` → `V007__experiment_create_experiments_table.sql`

**From metrics-service**:
- `V001__create_metrics_table.sql` → `V008__metrics_create_metrics_table.sql`

**From communications-service**:
- `V001__create_communications_tables.sql` → `V009__communications_create_tables.sql`

**Script Updates**:
- Add service identifier comment at top of each file
- Ensure schema-qualified table names if needed
- Validate SQL syntax

### Phase 3: Update Microservices

**Remove Flyway from Services**:

For each service (identity, organization, experiment, metrics, communications):

1. **pom.xml**: Remove Flyway dependency
   ```xml
   <!-- REMOVE THIS -->
   <dependency>
       <groupId>org.flywaydb</groupId>
       <artifactId>flyway-core</artifactId>
   </dependency>
   ```

2. **application.yml**: Remove Flyway configuration
   ```yaml
   # REMOVE THIS SECTION
   flyway:
     schemas: {service}_schema
     default-schema: {service}_schema
   ```

3. **Delete migration directories**:
   - Remove `src/main/resources/db/migration/` directory
   - Remove `target/classes/db/migration/` if present

4. **Update README.md**: Document that migrations are now managed centrally

### Phase 4: Create AWS Infrastructure

**CodeBuild Project** (via Terraform or AWS Console):

```hcl
resource "aws_codebuild_project" "flyway_migrations" {
  name          = "turaf-flyway-migrations-${var.environment}"
  service_role  = aws_iam_role.codebuild_flyway.arn

  artifacts {
    type = "NO_ARTIFACTS"
  }

  environment {
    compute_type                = "BUILD_GENERAL1_SMALL"
    image                       = "aws/codebuild/standard:7.0"
    type                        = "LINUX_CONTAINER"
    
    environment_variable {
      name  = "DB_HOST"
      value = aws_db_instance.postgres.endpoint
    }
    
    environment_variable {
      name  = "DB_NAME"
      value = "turaf"
    }
    
    environment_variable {
      name  = "DB_PASSWORD"
      type  = "SECRETS_MANAGER"
      value = "${aws_secretsmanager_secret.db_master.arn}:password::"
    }
  }

  source {
    type            = "GITHUB"
    location        = "https://github.com/ryanwaite28/ai-projects-turaf.git"
    buildspec       = "services/flyway-service/buildspec.yml"
  }
}
```

**IAM Role for CodeBuild**:
- Permissions to read RDS endpoint
- Permissions to read Secrets Manager (database credentials)
- Permissions to write CloudWatch Logs
- Network access to RDS instance (VPC configuration)

### Phase 5: Create GitHub Actions Workflow

**New Workflow**: `.github/workflows/database-migrations.yml`

```yaml
name: Database Migrations

on:
  workflow_dispatch:
    inputs:
      environment:
        description: 'Target environment'
        required: true
        type: choice
        options:
          - dev
          - qa
          - prod
  push:
    branches:
      - develop    # Auto-run for DEV
      - release/*  # Auto-run for QA
    paths:
      - 'services/flyway-service/**'

jobs:
  migrate:
    name: Run Database Migrations
    runs-on: ubuntu-latest
    
    permissions:
      id-token: write
      contents: read
    
    steps:
      - name: Checkout code
        uses: actions/checkout@v4
      
      - name: Determine environment
        id: env
        run: |
          if [[ "${{ github.event_name }}" == "workflow_dispatch" ]]; then
            echo "environment=${{ inputs.environment }}" >> $GITHUB_OUTPUT
          elif [[ "${{ github.ref }}" == "refs/heads/develop" ]]; then
            echo "environment=dev" >> $GITHUB_OUTPUT
          elif [[ "${{ github.ref }}" =~ ^refs/heads/release/ ]]; then
            echo "environment=qa" >> $GITHUB_OUTPUT
          fi
      
      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: ${{ secrets[format('AWS_ROLE_{0}', steps.env.outputs.environment)] }}
          aws-region: us-east-1
      
      - name: Trigger CodeBuild
        run: |
          BUILD_ID=$(aws codebuild start-build \
            --project-name turaf-flyway-migrations-${{ steps.env.outputs.environment }} \
            --source-version ${{ github.sha }} \
            --query 'build.id' \
            --output text)
          
          echo "Build ID: $BUILD_ID"
          echo "build_id=$BUILD_ID" >> $GITHUB_OUTPUT
        id: codebuild
      
      - name: Wait for CodeBuild completion
        run: |
          aws codebuild wait build-complete --ids ${{ steps.codebuild.outputs.build_id }}
      
      - name: Get build status
        run: |
          STATUS=$(aws codebuild batch-get-builds \
            --ids ${{ steps.codebuild.outputs.build_id }} \
            --query 'builds[0].buildStatus' \
            --output text)
          
          if [ "$STATUS" != "SUCCEEDED" ]; then
            echo "Migration failed with status: $STATUS"
            exit 1
          fi
          
          echo "Migrations completed successfully"
```

### Phase 6: Update CD Pipelines

**Modify Existing CD Workflows** (`cd-dev.yml`, `cd-qa.yml`, `cd-prod.yml`):

Add migration step **before** service deployments:

```yaml
jobs:
  # NEW JOB: Run migrations first
  migrate-database:
    name: Run Database Migrations
    uses: ./.github/workflows/database-migrations.yml
    with:
      environment: ${{ matrix.environment }}
    secrets: inherit
  
  # EXISTING JOB: Deploy services (add dependency)
  deploy-services:
    needs: migrate-database  # <-- Add this dependency
    name: Deploy Services
    runs-on: ubuntu-latest
    # ... rest of deployment job
```

### Phase 7: Update Documentation

**Files to Update**:

1. **PROJECT.md**:
   - Section 27 (Data Architecture): Add subsection on centralized migration management
   - Section 49-50 (CI/CD): Document migration pipeline
   - Add reference to flyway-service

2. **specs/architecture.md**:
   - Update Database Schema Isolation Strategy section
   - Document migration execution flow

3. **specs/aws-infrastructure.md**:
   - Add CodeBuild project for migrations
   - Document IAM roles and permissions

4. **specs/ci-cd-pipelines.md**:
   - Add database-migrations.yml workflow documentation
   - Document migration execution before deployments

5. **specs/{service}-service.md** (for each service):
   - Remove Flyway configuration sections
   - Add note that migrations are managed centrally

6. **docs/DATABASE_ARCHITECTURE_IMPLEMENTATION.md**:
   - Update migration management section
   - Document centralized approach

7. **New spec**: `specs/flyway-service.md`
   - Complete specification for flyway-service
   - Migration naming conventions
   - Execution flow
   - Rollback procedures

### Phase 8: Create Tasks

**New Tasks Directory**: `tasks/flyway-service/`

1. **001-create-service-structure.md**
   - Create directory structure
   - Create flyway.conf
   - Create buildspec.yml
   - Create helper scripts

2. **002-migrate-sql-scripts.md**
   - Move scripts from all services
   - Rename with global versioning
   - Add service identifier comments
   - Validate SQL syntax

3. **003-update-microservices.md**
   - Remove Flyway dependencies from pom.xml
   - Remove Flyway configuration from application.yml
   - Delete migration directories
   - Update service READMEs

4. **004-create-codebuild-project.md**
   - Create CodeBuild project via Terraform
   - Configure IAM roles
   - Set up VPC access to RDS
   - Configure Secrets Manager access

5. **005-create-github-workflow.md**
   - Create database-migrations.yml
   - Configure OIDC authentication
   - Test workflow execution
   - Validate error handling

6. **006-update-cd-pipelines.md**
   - Add migration dependency to cd-dev.yml
   - Add migration dependency to cd-qa.yml
   - Add migration dependency to cd-prod.yml
   - Test end-to-end deployment flow

7. **007-test-migrations.md**
   - Test migrations in DEV environment
   - Validate schema isolation
   - Test rollback procedures
   - Document troubleshooting steps

**Update Existing Tasks**:
- Update service-specific tasks to remove migration-related steps
- Update infrastructure tasks to include CodeBuild project

---

## Migration Naming Convention

**Format**: `V{NNN}__{service}_{description}.sql`

**Examples**:
- `V001__identity_create_users_table.sql`
- `V002__identity_create_refresh_tokens_table.sql`
- `V003__organization_create_organizations_table.sql`
- `V010__identity_add_email_verification.sql`
- `V011__experiment_add_status_column.sql`

**Rules**:
1. Use 3-digit zero-padded version numbers (V001, V002, etc.)
2. Include service name after version number
3. Use descriptive snake_case names
4. Add service identifier comment at top of file:
   ```sql
   -- Service: identity-service
   -- Schema: identity_schema
   -- Description: Create users table for authentication
   ```

---

## Flyway Configuration

**flyway.conf** (multi-schema support):

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

**Migration Script Template**:

```sql
-- Service: {service-name}
-- Schema: {schema_name}
-- Description: {what this migration does}

-- Set search path to target schema
SET search_path TO ${schema_name};

-- Migration SQL
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    -- ...
);

-- Reset search path
SET search_path TO public;
```

---

## Benefits

1. **Centralized Control**: All database changes in one place
2. **Deployment Safety**: Migrations run before service deployments
3. **Reduced Complexity**: Services don't manage their own migrations
4. **Audit Trail**: Clear history of all database changes
5. **Rollback Support**: Centralized rollback procedures
6. **Schema Isolation**: Maintains multi-schema architecture
7. **CI/CD Integration**: Automated migration execution
8. **Cost Efficiency**: CodeBuild only runs when needed

---

## Rollback Strategy

**Automated Rollback** (if migration fails):
- CodeBuild build fails
- GitHub Actions workflow fails
- Service deployments are blocked
- Manual intervention required

**Manual Rollback** (if needed after deployment):
1. Create rollback migration script (e.g., `V012__identity_rollback_feature.sql`)
2. Commit and push to trigger migration pipeline
3. Verify rollback success
4. Deploy previous service versions if needed

**Emergency Rollback**:
1. Use Flyway's `repair` command to fix migration history
2. Manually execute rollback SQL via psql
3. Update Flyway history table
4. Document incident

---

## Testing Strategy

**Local Testing**:
```bash
cd services/flyway-service
export DB_HOST=localhost
export DB_NAME=turaf
export DB_USER=postgres
export DB_PASSWORD=postgres
./scripts/run-migrations.sh
```

**DEV Environment Testing**:
- Trigger workflow manually via GitHub Actions UI
- Monitor CodeBuild logs
- Validate schema changes via psql
- Test service connectivity

**QA/PROD Deployment**:
- Require manual approval for PROD migrations
- Run migrations during maintenance window
- Monitor for errors
- Have rollback plan ready

---

## Security Considerations

1. **Credentials**: Stored in AWS Secrets Manager, accessed via CodeBuild
2. **IAM Roles**: Least-privilege access for CodeBuild
3. **Network**: CodeBuild runs in VPC with access to RDS
4. **Audit**: All migrations logged to CloudWatch
5. **Approval**: PROD migrations require manual approval

---

## Success Criteria

- [ ] flyway-service structure created with all migration scripts
- [ ] All services have Flyway dependencies removed
- [ ] CodeBuild project created for each environment
- [ ] GitHub Actions workflow executes migrations successfully
- [ ] CD pipelines updated to run migrations before deployments
- [ ] All documentation updated
- [ ] Tasks created for implementation
- [ ] Migrations tested in DEV environment
- [ ] Rollback procedures documented and tested

---

## Implementation Order

1. Create flyway-service structure and configuration
2. Migrate and rename all SQL scripts
3. Create CodeBuild project in DEV environment
4. Create GitHub Actions workflow
5. Test migrations in DEV
6. Update CD pipeline for DEV
7. Remove Flyway from services (one at a time)
8. Test end-to-end deployment in DEV
9. Replicate for QA environment
10. Replicate for PROD environment
11. Update all documentation
12. Create changelog entry

---

## Files to Create/Modify

### New Files (9):
1. `services/flyway-service/README.md`
2. `services/flyway-service/flyway.conf`
3. `services/flyway-service/buildspec.yml`
4. `services/flyway-service/scripts/run-migrations.sh`
5. `services/flyway-service/scripts/validate-migrations.sh`
6. `services/flyway-service/migrations/*.sql` (9 migration files)
7. `.github/workflows/database-migrations.yml`
8. `specs/flyway-service.md`
9. `tasks/flyway-service/*.md` (7 task files)

### Modified Files (20+):
1. `PROJECT.md` (Section 27, 49-50)
2. `specs/architecture.md`
3. `specs/aws-infrastructure.md`
4. `specs/ci-cd-pipelines.md`
5. `specs/identity-service.md`
6. `specs/organization-service.md`
7. `specs/experiment-service.md`
8. `specs/metrics-service.md`
9. `specs/communications-service.md`
10. `docs/DATABASE_ARCHITECTURE_IMPLEMENTATION.md`
11. `.github/workflows/cd-dev.yml`
12. `.github/workflows/cd-qa.yml`
13. `.github/workflows/cd-prod.yml`
14. `services/identity-service/pom.xml`
15. `services/organization-service/pom.xml`
16. `services/experiment-service/pom.xml`
17. `services/metrics-service/pom.xml`
18. `services/communications-service/pom.xml`
19. `services/identity-service/src/main/resources/application.yml`
20. (+ 4 more application.yml files for other services)

### Deleted Directories (5):
1. `services/identity-service/src/main/resources/db/migration/`
2. `services/organization-service/src/main/resources/db/migration/`
3. `services/experiment-service/src/main/resources/db/migration/`
4. `services/metrics-service/src/main/resources/db/migration/`
5. `services/communications-service/src/main/resources/db/migration/`

### Infrastructure (Terraform):
- New CodeBuild project resource
- New IAM role for CodeBuild
- New IAM policies for RDS and Secrets Manager access
- VPC configuration for CodeBuild

---

## Changelog Entry

Create: `changelog/2026-03-23-centralized-flyway-migration-service.md`

Document:
- Rationale for centralization
- Architecture changes
- Migration from service-managed to centralized approach
- Impact on deployment pipelines
- Benefits and trade-offs
