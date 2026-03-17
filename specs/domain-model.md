# Domain Model Specification

**Source**: PROJECT.md (Section 6)

This specification defines the core domain model for the Turaf platform using Domain-Driven Design principles.

---

## Core Entities

### Organization (Aggregate Root)

**Purpose**: Represents a tenant in the multi-tenant system

**Attributes**:
- `id` (OrganizationId): Unique identifier
- `name` (String): Organization display name
- `slug` (String): URL-friendly identifier
- `createdAt` (Timestamp): Creation timestamp
- `updatedAt` (Timestamp): Last update timestamp

**Invariants**:
- Name must be unique within the system
- Slug must be unique and URL-safe
- Cannot be deleted if active experiments exist

**Aggregate Members**:
- Users (via membership relationship)
- Problems (owned by organization)

---

### User

**Purpose**: Represents a person who can authenticate and access the system

**Attributes**:
- `id` (UserId): Unique identifier
- `email` (Email): Email address (unique)
- `passwordHash` (String): Hashed password
- `name` (String): Display name
- `createdAt` (Timestamp): Creation timestamp
- `updatedAt` (Timestamp): Last update timestamp

**Invariants**:
- Email must be unique across the system
- Email must be valid format
- Password must meet security requirements
- User must belong to at least one organization

**Relationships**:
- Belongs to one or more Organizations (many-to-many)
- Has roles within each organization

---

### Problem (Aggregate Root)

**Purpose**: Represents a problem that needs to be solved or validated

**Attributes**:
- `id` (ProblemId): Unique identifier
- `organizationId` (OrganizationId): Owning organization
- `title` (String): Problem title
- `description` (String): Detailed problem description
- `affectedUsers` (String): Description of affected users
- `context` (String): Additional context
- `createdBy` (UserId): User who created the problem
- `createdAt` (Timestamp): Creation timestamp
- `updatedAt` (Timestamp): Last update timestamp

**Invariants**:
- Must belong to an organization
- Title cannot be empty
- Creator must be member of organization

**Aggregate Members**:
- Hypotheses (owned by problem)

---

### Hypothesis

**Purpose**: Represents a proposed solution to a problem

**Attributes**:
- `id` (HypothesisId): Unique identifier
- `problemId` (ProblemId): Parent problem
- `organizationId` (OrganizationId): Owning organization
- `statement` (String): Hypothesis statement
- `expectedOutcome` (String): What success looks like
- `createdBy` (UserId): User who created the hypothesis
- `createdAt` (Timestamp): Creation timestamp
- `updatedAt` (Timestamp): Last update timestamp

**Invariants**:
- Must belong to a problem
- Statement must follow "If X, then Y" format
- Creator must be member of organization

**Relationships**:
- Belongs to one Problem
- Can have multiple Experiments

---

### Experiment (Aggregate Root)

**Purpose**: Represents a structured test of a hypothesis

**Attributes**:
- `id` (ExperimentId): Unique identifier
- `hypothesisId` (HypothesisId): Hypothesis being tested
- `organizationId` (OrganizationId): Owning organization
- `name` (String): Experiment name
- `description` (String): Experiment description
- `status` (ExperimentStatus): Current state
- `startTime` (Timestamp): When experiment started
- `endTime` (Timestamp): When experiment ended
- `createdBy` (UserId): User who created the experiment
- `createdAt` (Timestamp): Creation timestamp
- `updatedAt` (Timestamp): Last update timestamp

**Status Values** (ExperimentStatus enum):
- `DRAFT`: Created but not started
- `RUNNING`: Currently active
- `COMPLETED`: Finished
- `CANCELLED`: Terminated early

**Invariants**:
- Must belong to a hypothesis
- Can only transition: DRAFT → RUNNING → COMPLETED
- Cannot transition back to previous states
- Must have startTime when status is RUNNING
- Must have endTime when status is COMPLETED
- Creator must be member of organization

**Aggregate Members**:
- Metrics (recorded during experiment)
- ExperimentResult (outcome summary)

**Business Rules**:
- Cannot start if already RUNNING or COMPLETED
- Cannot complete if not RUNNING
- Cannot add metrics if not RUNNING
- Must have at least one metric to complete

---

### Metric

**Purpose**: Represents a measured data point during an experiment

**Attributes**:
- `id` (MetricId): Unique identifier
- `experimentId` (ExperimentId): Parent experiment
- `organizationId` (OrganizationId): Owning organization
- `name` (String): Metric name (e.g., "conversion_rate")
- `value` (MetricValue): Measured value
- `unit` (String): Unit of measurement
- `recordedAt` (Timestamp): When metric was recorded
- `metadata` (JSON): Additional context

**Invariants**:
- Must belong to an experiment
- Experiment must be in RUNNING status
- Value must be numeric
- RecordedAt must be between experiment start and end times

**Value Object**: MetricValue
- Encapsulates numeric value with validation
- Supports different numeric types (integer, decimal)
- Immutable

---

### ExperimentResult

**Purpose**: Represents the outcome summary of a completed experiment

**Attributes**:
- `id` (ResultId): Unique identifier
- `experimentId` (ExperimentId): Parent experiment
- `summary` (String): Outcome summary
- `outcome` (OutcomeType): Classification of result
- `metricsAnalysis` (String): Analysis of collected metrics
- `createdAt` (Timestamp): When result was recorded

**Outcome Values** (OutcomeType enum):
- `VALIDATED`: Hypothesis was validated
- `INVALIDATED`: Hypothesis was not validated
- `INCONCLUSIVE`: Results were inconclusive

**Invariants**:
- Must belong to a COMPLETED experiment
- One result per experiment
- Cannot be modified after creation

---

### Report

**Purpose**: Represents a generated report for an experiment

**Attributes**:
- `id` (ReportId): Unique identifier
- `experimentId` (ExperimentId): Experiment being reported on
- `organizationId` (OrganizationId): Owning organization
- `reportLocation` (S3Uri): S3 location of report file
- `reportFormat` (ReportFormat): Format of report
- `generatedAt` (Timestamp): When report was generated

**Report Format Values** (ReportFormat enum):
- `PDF`: PDF document
- `HTML`: HTML document

**Invariants**:
- Must belong to a COMPLETED experiment
- Report location must be valid S3 URI
- Cannot be deleted (immutable)

---

## Value Objects

### OrganizationId
- Wraps UUID
- Immutable
- Type-safe identifier

### UserId
- Wraps UUID
- Immutable
- Type-safe identifier

### ProblemId
- Wraps UUID
- Immutable
- Type-safe identifier

### HypothesisId
- Wraps UUID
- Immutable
- Type-safe identifier

### ExperimentId
- Wraps UUID
- Immutable
- Type-safe identifier

### MetricValue
- Wraps numeric value
- Validates range and precision
- Immutable
- Supports comparison operations

### DateRange
- Start and end timestamps
- Validates start < end
- Immutable
- Supports overlap checking

### Email
- Validates email format
- Immutable
- Case-insensitive equality

### S3Uri
- Validates S3 URI format
- Immutable
- Provides bucket and key extraction

---

## Entity Relationships

```
Organization
  ├── Users (many-to-many via OrganizationMember)
  └── Problems (one-to-many)
      └── Hypotheses (one-to-many)
          └── Experiments (one-to-many)
              ├── Metrics (one-to-many)
              ├── ExperimentResult (one-to-one)
              └── Report (one-to-one)
```

---

## Repository Interfaces

### OrganizationRepository

```java
public interface OrganizationRepository {
    Organization save(Organization organization);
    Optional<Organization> findById(OrganizationId id);
    Optional<Organization> findBySlug(String slug);
    List<Organization> findByUserId(UserId userId);
    void delete(OrganizationId id);
}
```

### UserRepository

```java
public interface UserRepository {
    User save(User user);
    Optional<User> findById(UserId id);
    Optional<User> findByEmail(Email email);
    List<User> findByOrganizationId(OrganizationId orgId);
    void delete(UserId id);
}
```

### ProblemRepository

```java
public interface ProblemRepository {
    Problem save(Problem problem);
    Optional<Problem> findById(ProblemId id);
    List<Problem> findByOrganizationId(OrganizationId orgId);
    void delete(ProblemId id);
}
```

### HypothesisRepository

```java
public interface HypothesisRepository {
    Hypothesis save(Hypothesis hypothesis);
    Optional<Hypothesis> findById(HypothesisId id);
    List<Hypothesis> findByProblemId(ProblemId problemId);
    List<Hypothesis> findByOrganizationId(OrganizationId orgId);
    void delete(HypothesisId id);
}
```

### ExperimentRepository

```java
public interface ExperimentRepository {
    Experiment save(Experiment experiment);
    Optional<Experiment> findById(ExperimentId id);
    List<Experiment> findByHypothesisId(HypothesisId hypothesisId);
    List<Experiment> findByOrganizationId(OrganizationId orgId);
    List<Experiment> findByStatus(ExperimentStatus status);
    void delete(ExperimentId id);
}
```

### MetricRepository

```java
public interface MetricRepository {
    Metric save(Metric metric);
    List<Metric> saveAll(List<Metric> metrics);
    Optional<Metric> findById(MetricId id);
    List<Metric> findByExperimentId(ExperimentId experimentId);
    List<Metric> findByExperimentIdAndName(ExperimentId experimentId, String name);
}
```

### ReportRepository

```java
public interface ReportRepository {
    Report save(Report report);
    Optional<Report> findById(ReportId id);
    Optional<Report> findByExperimentId(ExperimentId experimentId);
    List<Report> findByOrganizationId(OrganizationId orgId);
}
```

---

## Domain Events

Domain events are published when significant state changes occur:

- **ProblemCreated**: When a new problem is created
- **HypothesisCreated**: When a new hypothesis is created
- **ExperimentStarted**: When experiment transitions to RUNNING
- **MetricRecorded**: When a metric is recorded
- **ExperimentCompleted**: When experiment transitions to COMPLETED
- **ReportGenerated**: When a report is generated

See `event-flow.md` for detailed event specifications.

---

## Domain Logic

### Experiment State Machine

**States**: DRAFT → RUNNING → COMPLETED

**Transitions**:

```java
public class Experiment {
    public void start() {
        if (status != DRAFT) {
            throw new IllegalStateException("Can only start DRAFT experiments");
        }
        this.status = RUNNING;
        this.startTime = Instant.now();
        // Publish ExperimentStarted event
    }
    
    public void complete(String summary, OutcomeType outcome) {
        if (status != RUNNING) {
            throw new IllegalStateException("Can only complete RUNNING experiments");
        }
        if (!hasMetrics()) {
            throw new IllegalStateException("Cannot complete without metrics");
        }
        this.status = COMPLETED;
        this.endTime = Instant.now();
        this.result = new ExperimentResult(summary, outcome);
        // Publish ExperimentCompleted event
    }
}
```

### Validation Rules

**Problem Validation**:
- Title: 1-200 characters
- Description: 1-5000 characters
- Must belong to valid organization

**Hypothesis Validation**:
- Statement: 1-500 characters
- Must belong to valid problem
- Must follow "If X, then Y" pattern (recommended)

**Experiment Validation**:
- Name: 1-200 characters
- Must belong to valid hypothesis
- Start time must be before end time
- Cannot have metrics before starting

**Metric Validation**:
- Name: 1-100 characters
- Value must be numeric
- Recorded time must be within experiment duration

---

## Aggregate Design

### Why Organization is an Aggregate Root
- Enforces tenant isolation
- Controls membership lifecycle
- Ensures consistency of organization data

### Why Problem is an Aggregate Root
- Owns hypotheses lifecycle
- Enforces problem-hypothesis consistency
- Independent lifecycle from organization

### Why Experiment is an Aggregate Root
- Owns metrics and results
- Enforces experiment state machine
- Controls metric recording rules
- Independent lifecycle from hypothesis

---

## Multi-Tenancy in Domain Model

**Organization ID Propagation**:
- All entities include `organizationId`
- Repositories filter by organization
- Domain services validate organization context
- Events include organization context

**Tenant Isolation Rules**:
- Users can only access their organization's data
- Cross-organization references forbidden
- Repository queries always filter by organization
- Authorization checks in application layer

---

## Persistence Considerations

**JPA Mapping**:
- Entities map to database tables
- Value objects can be embedded or separate tables
- Use optimistic locking (@Version)
- Lazy loading for collections

**Database Schema**:
- Tables: organizations, users, organization_members, problems, hypotheses, experiments, metrics, experiment_results, reports
- Foreign keys enforce referential integrity
- Indexes on organizationId for multi-tenant queries
- Indexes on frequently queried fields

---

## Testing Domain Model

**Unit Tests**:
- Test entity invariants
- Test state machine transitions
- Test validation rules
- Test value object behavior

**Integration Tests**:
- Test repository implementations
- Test database constraints
- Test transaction boundaries
- Test multi-tenant isolation

---

## References

- PROJECT.md: Domain model definition
- Domain-Driven Design (Eric Evans)
- Implementing Domain-Driven Design (Vaughn Vernon)
- Clean Architecture (Robert C. Martin)
