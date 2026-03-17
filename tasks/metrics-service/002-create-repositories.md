# Task: Create Metrics Service Repositories

**Service**: Metrics Service  
**Phase**: 5  
**Estimated Time**: 3 hours  

## Objective

Implement repository for Metric entity using Spring Data JPA with optimized time-series queries.

## Prerequisites

- [x] Task 001: Domain model created

## Scope

**Files to Create**:
- `services/metrics-service/src/main/java/com/turaf/metrics/infrastructure/persistence/MetricJpaEntity.java`
- `services/metrics-service/src/main/java/com/turaf/metrics/infrastructure/persistence/MetricJpaRepository.java`
- `services/metrics-service/src/main/java/com/turaf/metrics/infrastructure/persistence/MetricRepositoryImpl.java`
- `services/metrics-service/src/main/resources/db/migration/V001__create_metrics_table.sql`

## Implementation Details

### Database Migration

```sql
CREATE TABLE metrics (
    id VARCHAR(36) PRIMARY KEY,
    organization_id VARCHAR(36) NOT NULL,
    experiment_id VARCHAR(36) NOT NULL,
    name VARCHAR(100) NOT NULL,
    value DOUBLE PRECISION NOT NULL,
    type VARCHAR(20) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    tags JSONB
);

CREATE INDEX idx_metrics_org_id ON metrics(organization_id);
CREATE INDEX idx_metrics_experiment_id ON metrics(experiment_id);
CREATE INDEX idx_metrics_timestamp ON metrics(timestamp);
CREATE INDEX idx_metrics_experiment_name ON metrics(experiment_id, name);
CREATE INDEX idx_metrics_experiment_time ON metrics(experiment_id, timestamp);
```

## Acceptance Criteria

- [ ] JPA entity created
- [ ] Database migration creates schema
- [ ] Indexes optimized for time-series queries
- [ ] Repository implementation works
- [ ] Integration tests pass

## Testing Requirements

**Integration Tests**:
- Test save and retrieve metrics
- Test time-range queries
- Test aggregation queries

**Test Files to Create**:
- `MetricRepositoryImplTest.java`

## References

- Specification: `specs/metrics-service.md` (Database Schema section)
- Related Tasks: 003-implement-metric-service
