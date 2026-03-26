# Single Database Multi-Schema Architecture Implementation Plan

Refactor the database architecture from separate databases per service to a single PostgreSQL database with isolated schemas per service, maintaining microservice principles while simplifying infrastructure for early-stage development.

---

## Current State Analysis

**Current Architecture:**
- Each microservice is designed to connect to its own separate RDS PostgreSQL database
- Infrastructure specs define individual database instances per service
- Services have Flyway migrations in `src/main/resources/db/migration/`
- Connection configurations assume separate database endpoints

**Services with Database Dependencies:**
1. **identity-service**: Users, refresh tokens
2. **organization-service**: Organizations, organization members
3. **experiment-service**: Problems, hypotheses, experiments
4. **metrics-service**: Metrics, metric aggregations

---

## Target Architecture

**Single Database, Multi-Schema Design:**
- **Database**: Single RDS PostgreSQL instance (`turaf-db-{env}`)
- **Schemas**: One schema per service
  - `identity_schema`
  - `organization_schema`
  - `experiment_schema`
  - `metrics_schema`
- **Users**: One database user per service with schema-scoped permissions
  - `identity_user` → `identity_schema` (full access)
  - `organization_user` → `organization_schema` (full access)
  - `experiment_user` → `experiment_schema` (full access)
  - `metrics_user` → `metrics_schema` (full access)
- **Isolation**: No cross-schema foreign keys or references
- **Migrations**: Each service manages its own schema migrations independently

**Microservice Principles Maintained:**
- ✅ Services remain independently deployable
- ✅ Each service owns its data (schema isolation)
- ✅ No shared tables or cross-schema dependencies
- ✅ BFF aggregates data via service APIs (not database joins)
- ✅ Services maintain single responsibility

---

## Changes Required

### 1. PROJECT.md Updates

**Section 27: Data Architecture** (Lines 1080-1096)
- **Current**: Describes "Primary Database" with generic table list
- **Update**: 
  - Clarify single database with multiple schemas
  - List schemas instead of tables
  - Explain schema-per-service isolation strategy
  - Add rationale for early-stage simplification

**Section 54: Encryption Strategy** (Lines 2165-2192)
- **Current**: Mentions "RDS encryption" generically
- **Update**: Clarify single RDS instance with encryption at rest

**New Section**: Add "Database Schema Architecture" section
- Schema naming convention
- User permission model
- Migration strategy per service
- Cross-schema isolation policy
- Connection pooling considerations

### 2. specs/aws-infrastructure.md Updates

**Data Storage Section** (Lines 323-385)
- **Current**: Describes RDS configuration without schema details
- **Update**:
  - Add schema creation strategy
  - Document database user creation per service
  - Add permission grants per schema
  - Update connection string patterns
  - Add schema initialization scripts

**Example Addition**:
```hcl
# Schema and user setup (executed once per environment)
resource "null_resource" "database_schemas" {
  provisioner "local-exec" {
    command = <<-EOT
      psql -h ${aws_db_instance.postgres.endpoint} -U turaf_admin -d turaf <<SQL
        -- Create schemas
        CREATE SCHEMA IF NOT EXISTS identity_schema;
        CREATE SCHEMA IF NOT EXISTS organization_schema;
        CREATE SCHEMA IF NOT EXISTS experiment_schema;
        CREATE SCHEMA IF NOT EXISTS metrics_schema;
        
        -- Create users
        CREATE USER identity_user WITH PASSWORD '${random_password.identity_user.result}';
        CREATE USER organization_user WITH PASSWORD '${random_password.organization_user.result}';
        CREATE USER experiment_user WITH PASSWORD '${random_password.experiment_user.result}';
        CREATE USER metrics_user WITH PASSWORD '${random_password.metrics_user.result}';
        
        -- Grant permissions
        GRANT ALL PRIVILEGES ON SCHEMA identity_schema TO identity_user;
        GRANT ALL PRIVILEGES ON SCHEMA organization_schema TO organization_user;
        GRANT ALL PRIVILEGES ON SCHEMA experiment_schema TO experiment_user;
        GRANT ALL PRIVILEGES ON SCHEMA metrics_schema TO metrics_user;
      SQL
    EOT
  }
}
```

### 3. specs/architecture.md Updates

**Data Storage Section** (Lines 53-57)
- **Current**: "Amazon RDS PostgreSQL (transactional data)"
- **Update**: Add detail about schema-based isolation

**New Subsection**: "Database Schema Isolation Strategy"
- Explain schema-per-service pattern
- Document why this approach for early-stage
- Clarify migration path to separate databases later
- Emphasize maintained microservice boundaries

### 4. Service Specification Updates

**Files to Update:**
- `specs/identity-service.md`
- `specs/organization-service.md`
- `specs/experiment-service.md`
- `specs/metrics-service.md`

**Changes per file:**
- Update "Database" section from "PostgreSQL (shared RDS instance)" to "PostgreSQL schema: {service}_schema on shared RDS instance"
- Add schema name to database schema documentation
- Update connection configuration examples
- Add Flyway schema configuration

**Example for identity-service.md:**
```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:5432/turaf?currentSchema=identity_schema
    username: identity_user
    password: ${DB_PASSWORD}
  flyway:
    schemas: identity_schema
    default-schema: identity_schema
```

### 5. Task Updates

**infrastructure/004-create-database-module.md**
- **Current**: Creates separate RDS instances per service
- **Update**:
  - Change to single RDS instance
  - Add schema creation logic
  - Add user creation per service
  - Add permission grants
  - Update secrets management (one password per service user)

**New Task**: `infrastructure/013-setup-database-schemas.md`
- Create initialization script for schemas and users
- Document schema migration strategy
- Add validation tests for schema isolation

**Service Repository Tasks** (experiment-service/003, etc.)
- Update migration scripts to include schema specification
- Add schema configuration to application.yml examples
- Update JPA configuration for schema awareness

### 6. Code Changes Required

**Each Service's application.yml:**
```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:5432/${DB_NAME:turaf}?currentSchema={service}_schema
    username: ${DB_USER:{service}_user}
    password: ${DB_PASSWORD}
  jpa:
    properties:
      hibernate:
        default_schema: {service}_schema
  flyway:
    schemas: {service}_schema
    default-schema: {service}_schema
```

**Migration Scripts:**
- Prepend schema name to all table references (if not using default schema)
- Or rely on `default_schema` configuration (preferred)

**Example Migration Update:**
```sql
-- V001__create_users_table.sql
-- Tables will be created in identity_schema due to Flyway configuration
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    -- ... rest of schema
);
```

### 7. Terraform Module Updates

**modules/database/main.tf:**
- Remove per-service database instances
- Create single RDS instance
- Add schema initialization
- Create database users per service
- Store passwords in Secrets Manager per user

**modules/database/variables.tf:**
- Add `services` list variable
- Add schema naming configuration
- Add user naming configuration

**modules/database/outputs.tf:**
- Output single database endpoint
- Output schema names per service
- Output secret ARNs per service user

### 8. Documentation Updates

**New ADR**: `docs/adr/ADR-006-single-database-multi-schema.md`
- **Context**: Early-stage infrastructure simplification
- **Decision**: Single database with schema isolation
- **Consequences**: 
  - Pros: Simplified infrastructure, lower cost, easier local development
  - Cons: Shared resource, potential scaling limitations
  - Migration path: Can split to separate databases when needed

**README Updates:**
- Update local development setup instructions
- Document schema creation for local PostgreSQL
- Add connection string examples per service

---

## Implementation Phases

### Phase 1: Documentation Updates
1. Update PROJECT.md with new database architecture section
2. Update specs/aws-infrastructure.md with schema details
3. Update specs/architecture.md with isolation strategy
4. Update all service specs with schema configuration
5. Create ADR-006 for architectural decision

### Phase 2: Task Updates
1. Update infrastructure/004-create-database-module.md
2. Create infrastructure/013-setup-database-schemas.md
3. Update service repository tasks with schema config
4. Update integration test tasks with schema setup

### Phase 3: Code Configuration Updates
1. Update application.yml for each service with schema config
2. Verify Flyway configurations include schema settings
3. Update migration scripts if needed (table prefixes)
4. Update local development docker-compose with schema init

### Phase 4: Terraform Updates
1. Refactor database module for single instance
2. Add schema initialization scripts
3. Add user creation per service
4. Update secrets management for per-service users
5. Update environment configs (dev, qa, prod)

### Phase 5: Validation
1. Test local development setup with schemas
2. Verify migration scripts run in correct schemas
3. Test service isolation (no cross-schema access)
4. Validate Terraform plans for all environments
5. Update CI/CD pipelines if needed

---

## Migration Strategy

**For Existing Deployments:**
1. Create new single-database infrastructure
2. Export data from existing separate databases
3. Import data into respective schemas
4. Update service configurations
5. Deploy updated services
6. Decommission old database instances

**For New Deployments:**
- Follow new architecture from start
- No migration needed

---

## Testing Strategy

**Schema Isolation Tests:**
- Verify service A cannot access service B's schema
- Test permission boundaries
- Validate migration script execution

**Integration Tests:**
- Update test configurations to use schemas
- Verify Flyway migrations work correctly
- Test multi-service scenarios

**Performance Tests:**
- Validate connection pooling per service
- Test concurrent schema access
- Monitor resource utilization

---

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Shared resource contention | Medium | Connection pooling, monitoring, auto-scaling |
| Schema migration conflicts | Low | Independent Flyway per service, version control |
| Backup/restore complexity | Low | Schema-aware backup scripts, documented procedures |
| Future scaling limitations | Medium | Clear migration path to separate databases documented |
| Developer confusion | Low | Clear documentation, examples, ADR |

---

## Success Criteria

- [ ] All documentation updated with schema architecture
- [ ] Service specs include schema configuration
- [ ] Terraform creates single database with schemas
- [ ] Each service connects to its own schema
- [ ] Flyway migrations run in correct schemas
- [ ] No cross-schema dependencies exist
- [ ] Local development setup documented
- [ ] ADR created and reviewed
- [ ] All services compile and run with new config
- [ ] Integration tests pass with schema isolation

---

## Files to Create/Update

**Documentation (8 files):**
- PROJECT.md
- specs/aws-infrastructure.md
- specs/architecture.md
- specs/identity-service.md
- specs/organization-service.md
- specs/experiment-service.md
- specs/metrics-service.md
- docs/adr/ADR-006-single-database-multi-schema.md (new)

**Tasks (6 files):**
- tasks/infrastructure/004-create-database-module.md
- tasks/infrastructure/013-setup-database-schemas.md (new)
- tasks/experiment-service/003-create-repositories.md
- tasks/identity-service/003-create-repositories.md (if exists)
- tasks/organization-service/003-create-repositories.md (if exists)
- tasks/metrics-service/003-create-repositories.md (if exists)

**Code (4+ files):**
- services/identity-service/src/main/resources/application.yml
- services/organization-service/src/main/resources/application.yml
- services/experiment-service/src/main/resources/application.yml
- services/metrics-service/src/main/resources/application.yml

**Infrastructure (3+ files):**
- infrastructure/terraform/modules/database/main.tf
- infrastructure/terraform/modules/database/variables.tf
- infrastructure/terraform/modules/database/outputs.tf

---

## Timeline Estimate

- **Phase 1 (Documentation)**: 2-3 hours
- **Phase 2 (Tasks)**: 1-2 hours
- **Phase 3 (Code Config)**: 1-2 hours
- **Phase 4 (Terraform)**: 3-4 hours
- **Phase 5 (Validation)**: 2-3 hours

**Total**: 9-14 hours of focused work

---

## References

- PostgreSQL Schema Documentation: https://www.postgresql.org/docs/current/ddl-schemas.html
- Flyway Schema Configuration: https://flywaydb.org/documentation/configuration/parameters/schemas
- Spring Boot Multi-Schema: https://docs.spring.io/spring-boot/docs/current/reference/html/data.html#data.sql.datasource
- Microservices Database Patterns: https://microservices.io/patterns/data/database-per-service.html
