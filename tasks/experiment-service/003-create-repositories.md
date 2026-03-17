# Task: Create Experiment Service Repositories

**Service**: Experiment Service  
**Phase**: 4  
**Estimated Time**: 3 hours  

## Objective

Implement repository interfaces for Problem, Hypothesis, and Experiment entities using Spring Data JPA with tenant-aware queries.

## Prerequisites

- [x] Task 001: Domain model created
- [x] Task 002: State machine implemented

## Scope

**Files to Create**:
- `services/experiment-service/src/main/java/com/turaf/experiment/infrastructure/persistence/ProblemJpaEntity.java`
- `services/experiment-service/src/main/java/com/turaf/experiment/infrastructure/persistence/HypothesisJpaEntity.java`
- `services/experiment-service/src/main/java/com/turaf/experiment/infrastructure/persistence/ExperimentJpaEntity.java`
- `services/experiment-service/src/main/java/com/turaf/experiment/infrastructure/persistence/ProblemJpaRepository.java`
- `services/experiment-service/src/main/java/com/turaf/experiment/infrastructure/persistence/HypothesisJpaRepository.java`
- `services/experiment-service/src/main/java/com/turaf/experiment/infrastructure/persistence/ExperimentJpaRepository.java`
- `services/experiment-service/src/main/java/com/turaf/experiment/infrastructure/persistence/ProblemRepositoryImpl.java`
- `services/experiment-service/src/main/java/com/turaf/experiment/infrastructure/persistence/HypothesisRepositoryImpl.java`
- `services/experiment-service/src/main/java/com/turaf/experiment/infrastructure/persistence/ExperimentRepositoryImpl.java`
- `services/experiment-service/src/main/resources/db/migration/V001__create_problems_table.sql`
- `services/experiment-service/src/main/resources/db/migration/V002__create_hypotheses_table.sql`
- `services/experiment-service/src/main/resources/db/migration/V003__create_experiments_table.sql`

## Implementation Details

### Database Migrations

```sql
-- V001__create_problems_table.sql
CREATE TABLE problems (
    id VARCHAR(36) PRIMARY KEY,
    organization_id VARCHAR(36) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    created_by VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_problems_org_id ON problems(organization_id);

-- V002__create_hypotheses_table.sql
CREATE TABLE hypotheses (
    id VARCHAR(36) PRIMARY KEY,
    organization_id VARCHAR(36) NOT NULL,
    problem_id VARCHAR(36) NOT NULL,
    statement VARCHAR(500) NOT NULL,
    expected_outcome TEXT,
    created_by VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    FOREIGN KEY (problem_id) REFERENCES problems(id) ON DELETE CASCADE
);

CREATE INDEX idx_hypotheses_org_id ON hypotheses(organization_id);
CREATE INDEX idx_hypotheses_problem_id ON hypotheses(problem_id);

-- V003__create_experiments_table.sql
CREATE TABLE experiments (
    id VARCHAR(36) PRIMARY KEY,
    organization_id VARCHAR(36) NOT NULL,
    hypothesis_id VARCHAR(36) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_by VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    FOREIGN KEY (hypothesis_id) REFERENCES hypotheses(id) ON DELETE CASCADE
);

CREATE INDEX idx_experiments_org_id ON experiments(organization_id);
CREATE INDEX idx_experiments_hypothesis_id ON experiments(hypothesis_id);
CREATE INDEX idx_experiments_status ON experiments(status);
```

### JPA Entities

```java
@Entity
@Table(name = "experiments")
public class ExperimentJpaEntity {
    @Id
    private String id;
    
    @Column(name = "organization_id", nullable = false)
    private String organizationId;
    
    @Column(name = "hypothesis_id", nullable = false)
    private String hypothesisId;
    
    @Column(nullable = false, length = 200)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExperimentStatus status;
    
    @Column(name = "started_at")
    private Instant startedAt;
    
    @Column(name = "completed_at")
    private Instant completedAt;
    
    @Column(name = "created_by", nullable = false)
    private String createdBy;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    public Experiment toDomain() {
        Experiment experiment = new Experiment(
            new ExperimentId(id),
            organizationId,
            new HypothesisId(hypothesisId),
            name,
            description,
            createdBy
        );
        // Set internal state via reflection or package-private setters
        return experiment;
    }
    
    public static ExperimentJpaEntity fromDomain(Experiment experiment) {
        ExperimentJpaEntity entity = new ExperimentJpaEntity();
        entity.setId(experiment.getId().getValue());
        entity.setOrganizationId(experiment.getOrganizationId());
        entity.setHypothesisId(experiment.getHypothesisId().getValue());
        entity.setName(experiment.getName());
        entity.setDescription(experiment.getDescription());
        entity.setStatus(experiment.getStatus());
        entity.setStartedAt(experiment.getStartedAt());
        entity.setCompletedAt(experiment.getCompletedAt());
        entity.setCreatedBy(experiment.getCreatedBy());
        entity.setCreatedAt(experiment.getCreatedAt());
        entity.setUpdatedAt(experiment.getUpdatedAt());
        return entity;
    }
}
```

### Repository Implementation

```java
@Repository
public class ExperimentRepositoryImpl implements ExperimentRepository {
    private final ExperimentJpaRepository jpaRepository;
    
    @Override
    public Optional<Experiment> findById(ExperimentId id) {
        return jpaRepository.findById(id.getValue())
            .map(ExperimentJpaEntity::toDomain);
    }
    
    @Override
    public Experiment save(Experiment experiment) {
        ExperimentJpaEntity entity = ExperimentJpaEntity.fromDomain(experiment);
        ExperimentJpaEntity saved = jpaRepository.save(entity);
        return saved.toDomain();
    }
    
    @Override
    public List<Experiment> findByHypothesisId(HypothesisId hypothesisId) {
        return jpaRepository.findByHypothesisId(hypothesisId.getValue())
            .stream()
            .map(ExperimentJpaEntity::toDomain)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Experiment> findByStatus(ExperimentStatus status) {
        return jpaRepository.findByStatus(status)
            .stream()
            .map(ExperimentJpaEntity::toDomain)
            .collect(Collectors.toList());
    }
}
```

## Acceptance Criteria

- [ ] JPA entities created for all domain entities
- [ ] Database migrations create correct schema
- [ ] Foreign key constraints enforced
- [ ] Indexes created for performance
- [ ] Repository implementations work correctly
- [ ] Tenant-aware queries implemented
- [ ] Integration tests pass

## Testing Requirements

**Integration Tests**:
- Test save and retrieve entities
- Test foreign key constraints
- Test cascade deletes
- Test queries by organization
- Test queries by status

**Test Files to Create**:
- `ProblemRepositoryImplTest.java`
- `HypothesisRepositoryImplTest.java`
- `ExperimentRepositoryImplTest.java`

## References

- Specification: `specs/experiment-service.md` (Database Schema section)
- Related Tasks: 001-create-domain-model, 004-implement-problem-service
