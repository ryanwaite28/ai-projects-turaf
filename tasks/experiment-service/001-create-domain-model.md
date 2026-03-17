# Task: Create Experiment Service Domain Model

**Service**: Experiment Service  
**Phase**: 4  
**Estimated Time**: 4 hours  

## Objective

Create the domain model for the Experiment Service including Problem, Hypothesis, and Experiment entities with state machine logic.

## Prerequisites

- [x] Task 001: Clean Architecture layers established
- [x] Task 002: DDD patterns implemented
- [x] Task 003: Multi-tenant context setup

## Scope

**Files to Create**:
- `services/experiment-service/src/main/java/com/turaf/experiment/domain/Problem.java`
- `services/experiment-service/src/main/java/com/turaf/experiment/domain/ProblemId.java`
- `services/experiment-service/src/main/java/com/turaf/experiment/domain/Hypothesis.java`
- `services/experiment-service/src/main/java/com/turaf/experiment/domain/HypothesisId.java`
- `services/experiment-service/src/main/java/com/turaf/experiment/domain/Experiment.java`
- `services/experiment-service/src/main/java/com/turaf/experiment/domain/ExperimentId.java`
- `services/experiment-service/src/main/java/com/turaf/experiment/domain/ExperimentStatus.java`
- `services/experiment-service/src/main/java/com/turaf/experiment/domain/ProblemRepository.java`
- `services/experiment-service/src/main/java/com/turaf/experiment/domain/HypothesisRepository.java`
- `services/experiment-service/src/main/java/com/turaf/experiment/domain/ExperimentRepository.java`
- `services/experiment-service/src/main/java/com/turaf/experiment/domain/event/ProblemCreated.java`
- `services/experiment-service/src/main/java/com/turaf/experiment/domain/event/ExperimentStarted.java`
- `services/experiment-service/src/main/java/com/turaf/experiment/domain/event/ExperimentCompleted.java`

## Implementation Details

### Problem Entity (Aggregate Root)

```java
public class Problem extends AggregateRoot<ProblemId> implements TenantAware {
    private String organizationId;
    private String title;
    private String description;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;
    
    public Problem(ProblemId id, String organizationId, String title, String description, String createdBy) {
        super(id);
        this.organizationId = Objects.requireNonNull(organizationId);
        this.title = validateTitle(title);
        this.description = description;
        this.createdBy = Objects.requireNonNull(createdBy);
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        
        registerEvent(new ProblemCreated(
            UUID.randomUUID().toString(),
            id.getValue(),
            organizationId,
            title,
            description,
            createdBy,
            createdAt
        ));
    }
    
    public void update(String title, String description) {
        this.title = validateTitle(title);
        this.description = description;
        this.updatedAt = Instant.now();
    }
    
    private String validateTitle(String title) {
        if (title == null || title.isBlank() || title.length() > 200) {
            throw new IllegalArgumentException("Title must be 1-200 characters");
        }
        return title;
    }
    
    // Getters, setters for TenantAware
}
```

### Hypothesis Entity (Aggregate Root)

```java
public class Hypothesis extends AggregateRoot<HypothesisId> implements TenantAware {
    private String organizationId;
    private ProblemId problemId;
    private String statement;
    private String expectedOutcome;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;
    
    public Hypothesis(HypothesisId id, String organizationId, ProblemId problemId, 
                     String statement, String expectedOutcome, String createdBy) {
        super(id);
        this.organizationId = Objects.requireNonNull(organizationId);
        this.problemId = Objects.requireNonNull(problemId);
        this.statement = validateStatement(statement);
        this.expectedOutcome = expectedOutcome;
        this.createdBy = Objects.requireNonNull(createdBy);
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
    
    private String validateStatement(String statement) {
        if (statement == null || statement.isBlank() || statement.length() > 500) {
            throw new IllegalArgumentException("Statement must be 1-500 characters");
        }
        return statement;
    }
    
    // Getters
}
```

### Experiment Entity (Aggregate Root)

```java
public class Experiment extends AggregateRoot<ExperimentId> implements TenantAware {
    private String organizationId;
    private HypothesisId hypothesisId;
    private String name;
    private String description;
    private ExperimentStatus status;
    private Instant startedAt;
    private Instant completedAt;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;
    
    public Experiment(ExperimentId id, String organizationId, HypothesisId hypothesisId,
                     String name, String description, String createdBy) {
        super(id);
        this.organizationId = Objects.requireNonNull(organizationId);
        this.hypothesisId = Objects.requireNonNull(hypothesisId);
        this.name = validateName(name);
        this.description = description;
        this.status = ExperimentStatus.DRAFT;
        this.createdBy = Objects.requireNonNull(createdBy);
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
    
    public void start() {
        if (status != ExperimentStatus.DRAFT) {
            throw new IllegalStateException("Can only start experiments in DRAFT status");
        }
        this.status = ExperimentStatus.RUNNING;
        this.startedAt = Instant.now();
        this.updatedAt = Instant.now();
        
        registerEvent(new ExperimentStarted(
            UUID.randomUUID().toString(),
            getId().getValue(),
            organizationId,
            hypothesisId.getValue(),
            startedAt
        ));
    }
    
    public void complete() {
        if (status != ExperimentStatus.RUNNING) {
            throw new IllegalStateException("Can only complete experiments in RUNNING status");
        }
        this.status = ExperimentStatus.COMPLETED;
        this.completedAt = Instant.now();
        this.updatedAt = Instant.now();
        
        registerEvent(new ExperimentCompleted(
            UUID.randomUUID().toString(),
            getId().getValue(),
            organizationId,
            hypothesisId.getValue(),
            completedAt
        ));
    }
    
    private String validateName(String name) {
        if (name == null || name.isBlank() || name.length() > 200) {
            throw new IllegalArgumentException("Name must be 1-200 characters");
        }
        return name;
    }
    
    // Getters
}
```

### ExperimentStatus Enum

```java
public enum ExperimentStatus {
    DRAFT,
    RUNNING,
    COMPLETED,
    CANCELLED
}
```

### Repository Interfaces

```java
public interface ProblemRepository extends Repository<Problem, ProblemId> {
    List<Problem> findByOrganizationId(String organizationId);
}

public interface HypothesisRepository extends Repository<Hypothesis, HypothesisId> {
    List<Hypothesis> findByProblemId(ProblemId problemId);
}

public interface ExperimentRepository extends Repository<Experiment, ExperimentId> {
    List<Experiment> findByHypothesisId(HypothesisId hypothesisId);
    List<Experiment> findByStatus(ExperimentStatus status);
}
```

## Acceptance Criteria

- [ ] Problem entity with validation
- [ ] Hypothesis entity with validation
- [ ] Experiment entity with state machine
- [ ] ExperimentStatus enum defined
- [ ] State transitions enforced (DRAFT → RUNNING → COMPLETED)
- [ ] Domain events registered
- [ ] Repository interfaces defined
- [ ] All entities implement TenantAware
- [ ] Unit tests pass

## Testing Requirements

**Unit Tests**:
- Test Problem creation and validation
- Test Hypothesis creation and validation
- Test Experiment state machine transitions
- Test invalid state transitions throw exceptions
- Test domain event registration

**Test Files to Create**:
- `ProblemTest.java`
- `HypothesisTest.java`
- `ExperimentTest.java`
- `ExperimentStateMachineTest.java`

## References

- Specification: `specs/experiment-service.md` (Domain Model section)
- Specification: `specs/domain-model.md`
- PROJECT.md: Section 40 (Experiment Service)
- Related Tasks: 002-implement-state-machine
