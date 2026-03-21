# ADR-006: Single Database Multi-Schema Architecture

**Status**: Accepted  
**Date**: 2026-03-19  
**Decision Makers**: Architecture Team  
**Related ADRs**: ADR-002 (Database Strategy)

---

## Context

The Turaf platform is designed as a microservices architecture with four core services (Identity, Organization, Experiment, Metrics). The initial architectural vision called for separate database instances per service to maintain strict service boundaries and enable independent scaling.

However, as an early-stage platform, we face several practical considerations:

1. **Infrastructure Complexity**: Managing multiple RDS instances increases operational overhead
2. **Cost Efficiency**: Multiple database instances significantly increase AWS costs in early stages
3. **Development Experience**: Local development requires managing multiple database containers
4. **Deployment Complexity**: Database migrations, backups, and monitoring multiply across instances

At the same time, we must maintain microservice principles:
- Service autonomy and independent deployability
- Data ownership and bounded contexts
- No tight coupling through shared databases
- Clear service boundaries

---

## Decision

We will implement a **single PostgreSQL database with schema-based isolation per service**.

### Architecture

**Database Structure**:
- Single RDS PostgreSQL instance per environment (`turaf-db-{env}`)
- Four isolated schemas:
  - `identity_schema` - User authentication and authorization
  - `organization_schema` - Organization and membership management
  - `experiment_schema` - Problems, hypotheses, and experiments
  - `metrics_schema` - Metrics and aggregations

**Access Control**:
- One database user per service with schema-scoped permissions
  - `identity_user` → `identity_schema` (full access)
  - `organization_user` → `organization_schema` (full access)
  - `experiment_user` → `experiment_schema` (full access)
  - `metrics_user` → `metrics_schema` (full access)
- No cross-schema foreign keys or references
- Database-level permission enforcement prevents cross-schema access

**Migration Management**:
- Each service manages its own schema migrations using Flyway
- Independent migration version control per service
- Schema-scoped migration history tables
- No migration conflicts between services

---

## Consequences

### Positive

1. **Simplified Infrastructure**
   - Single RDS instance to manage, monitor, and backup
   - Reduced operational complexity
   - Simplified disaster recovery procedures

2. **Cost Efficiency**
   - ~75% reduction in database costs compared to four separate instances
   - Single instance for DEV environment (db.t3.micro vs 4x db.t3.micro)
   - Consolidated backup and storage costs

3. **Development Experience**
   - Single PostgreSQL container for local development
   - Simplified database initialization scripts
   - Easier integration testing across services

4. **Maintained Microservice Principles**
   - Schema isolation enforces service boundaries
   - No shared tables or cross-schema dependencies
   - Services remain independently deployable
   - BFF aggregates data via service APIs, not database joins

5. **Clear Migration Path**
   - Schema-based design makes future split to separate databases straightforward
   - Can migrate one service at a time to its own database
   - No application code changes required for migration

### Negative

1. **Shared Resource Contention**
   - All services share database CPU, memory, and I/O
   - One service's heavy load could impact others
   - **Mitigation**: Connection pooling limits, monitoring, auto-scaling

2. **Single Point of Failure**
   - Database outage affects all services
   - **Mitigation**: Multi-AZ deployment, automated failover, read replicas

3. **Scaling Limitations**
   - Cannot scale database resources per service independently
   - **Mitigation**: Monitor per-schema resource usage, plan migration when needed

4. **Backup/Restore Complexity**
   - Cannot backup/restore individual services independently
   - **Mitigation**: Schema-aware backup scripts, point-in-time recovery

5. **Perception vs Reality**
   - May appear to violate "database per service" microservice pattern
   - **Mitigation**: Clear documentation of schema isolation strategy

### Neutral

1. **Connection Management**
   - Each service maintains its own connection pool
   - Total connections = sum of all service pools
   - Requires careful connection limit configuration

2. **Schema Coordination**
   - Schema names must be coordinated across services
   - Database user management requires coordination
   - Handled through infrastructure-as-code (Terraform)

---

## Implementation

### Database Configuration

```hcl
# Terraform: Single RDS instance
resource "aws_db_instance" "postgres" {
  identifier     = "turaf-db-${var.environment}"
  engine         = "postgres"
  engine_version = "15.3"
  db_name        = "turaf"
  
  # Schema and user creation via provisioner
  # See aws-infrastructure.md for full implementation
}
```

### Service Configuration

Each service configures its schema in `application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:5432/turaf?currentSchema={service}_schema
    username: {service}_user
    password: ${DB_PASSWORD}
  jpa:
    properties:
      hibernate:
        default_schema: {service}_schema
  flyway:
    schemas: {service}_schema
    default-schema: {service}_schema
```

### Migration Strategy

Each service's Flyway migrations automatically target its schema:

```sql
-- V001__create_users_table.sql (identity-service)
-- Runs in identity_schema due to Flyway configuration
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    -- ...
);
```

---

## Alternatives Considered

### 1. Separate Database Per Service (Original Plan)

**Pros**:
- True service isolation
- Independent scaling
- No shared resource contention

**Cons**:
- 4x infrastructure costs
- 4x operational complexity
- Complex local development setup
- Overkill for early-stage platform

**Decision**: Rejected for early-stage; remains future migration path

### 2. Shared Database, Shared Schema

**Pros**:
- Simplest implementation
- Lowest cost

**Cons**:
- Violates service boundaries
- No isolation enforcement
- Tight coupling through shared tables
- Cannot migrate to separate databases easily

**Decision**: Rejected - violates microservice principles

### 3. Separate Databases on Single RDS Instance

**Pros**:
- Logical separation
- Some isolation

**Cons**:
- Limited isolation (still shares resources)
- More complex than schemas
- No significant benefit over schema approach

**Decision**: Rejected - schemas provide equivalent isolation with less complexity

---

## Migration Path to Separate Databases

When scaling requires it, migration is straightforward:

1. **Create new RDS instance** for target service
2. **Export schema data** using `pg_dump --schema={service}_schema`
3. **Import to new database** using `psql`
4. **Update service configuration** to new database endpoint
5. **Deploy updated service**
6. **Decommission old schema** after verification

No application code changes required - only configuration updates.

---

## Monitoring and Metrics

Track per-schema metrics to inform migration decisions:

- Query performance per schema
- Connection count per service user
- Table sizes per schema
- I/O operations per schema

**Migration Trigger**: When any service consistently uses >50% of shared database resources.

---

## References

- PostgreSQL Schema Documentation: https://www.postgresql.org/docs/current/ddl-schemas.html
- Microservices Database Patterns: https://microservices.io/patterns/data/database-per-service.html
- Spring Boot Multi-Schema: https://docs.spring.io/spring-boot/docs/current/reference/html/data.html
- PROJECT.md: Section 27 (Data Architecture)
- specs/aws-infrastructure.md: RDS PostgreSQL section
- specs/architecture.md: Database Schema Isolation Strategy

---

## Review and Revision

This decision should be reviewed when:
- Any service consistently uses >50% of database resources
- Platform reaches 1000+ organizations
- Service-specific scaling requirements emerge
- Database becomes a performance bottleneck

**Next Review Date**: Q3 2026 or upon reaching 500 organizations
