# Experiment Service Specification

**Source**: PROJECT.md (Section 40)

This specification defines the Experiment Service, responsible for problem management, hypothesis tracking, and experiment lifecycle.

---

## Service Overview

**Purpose**: Manage problems, hypotheses, and experiments through their complete lifecycle

**Bounded Context**: Experiment Management

**Service Type**: Core microservice (ECS Fargate)

---

## Responsibilities

- Problem definition and management
- Hypothesis creation and tracking
- Experiment lifecycle management (DRAFT → RUNNING → COMPLETED)
- Experiment state transitions
- Experiment results recording
- Domain event publishing for experiment workflows

---

## Technology Stack

**Framework**: Spring Boot 3.x  
**Persistence**: Spring Data JPA  
**Database**: PostgreSQL schema `experiment_schema` on shared RDS instance  
**Database User**: `experiment_user` (schema-scoped permissions)  
**Events**: Spring Cloud AWS (EventBridge)  
**Build Tool**: Maven  
**Java Version**: Java 17  

**Key Dependencies**:
- `spring-boot-starter-web`
- `spring-boot-starter-data-jpa`
- `spring-boot-starter-validation`
- `spring-boot-starter-actuator`
- `spring-cloud-aws-messaging`
- `postgresql` driver

---

## API Endpoints

### Problem Management

#### POST /api/v1/problems

**Purpose**: Create a new problem

**Headers**:
```
Authorization: Bearer {access-token}
X-Organization-Id: {organization-id}
```

**Request Body**:
```json
{
  "title": "Low user engagement on dashboard",
  "description": "Users spend less than 2 minutes on the dashboard",
  "affectedUsers": "All users",
  "context": "Dashboard was redesigned 3 months ago"
}
```

**Response** (201 Created):
```json
{
  "problemId": "uuid",
  "organizationId": "uuid",
  "title": "Low user engagement on dashboard",
  "description": "Users spend less than 2 minutes on the dashboard",
  "affectedUsers": "All users",
  "context": "Dashboard was redesigned 3 months ago",
  "createdBy": "uuid",
  "createdAt": "ISO-8601"
}
```

**Business Rules**:
- User must be member of organization
- Publishes ProblemCreated event

---

#### GET /api/v1/problems

**Purpose**: List problems for organization

**Headers**:
```
Authorization: Bearer {access-token}
X-Organization-Id: {organization-id}
```

**Query Parameters**:
- `page` (default: 0)
- `size` (default: 20)
- `sort` (default: createdAt,desc)

**Response** (200 OK):
```json
{
  "problems": [
    {
      "problemId": "uuid",
      "title": "Low user engagement on dashboard",
      "hypothesesCount": 3,
      "experimentsCount": 5,
      "createdBy": "uuid",
      "createdAt": "ISO-8601"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 15,
  "totalPages": 1
}
```

---

#### GET /api/v1/problems/{id}

**Purpose**: Get problem details

**Response** (200 OK):
```json
{
  "problemId": "uuid",
  "organizationId": "uuid",
  "title": "Low user engagement on dashboard",
  "description": "Users spend less than 2 minutes on the dashboard",
  "affectedUsers": "All users",
  "context": "Dashboard was redesigned 3 months ago",
  "hypotheses": [
    {
      "hypothesisId": "uuid",
      "statement": "If we add quick actions, engagement will increase by 20%"
    }
  ],
  "createdBy": "uuid",
  "createdAt": "ISO-8601",
  "updatedAt": "ISO-8601"
}
```

---

#### PUT /api/v1/problems/{id}

**Purpose**: Update problem details

**Request Body**:
```json
{
  "title": "Updated title",
  "description": "Updated description"
}
```

**Response** (200 OK): Updated problem object

---

### Hypothesis Management

#### POST /api/v1/hypotheses

**Purpose**: Create a new hypothesis for a problem

**Request Body**:
```json
{
  "problemId": "uuid",
  "statement": "If we add quick actions to the dashboard, user engagement will increase by 20%",
  "expectedOutcome": "Average session time increases from 2 to 2.4 minutes"
}
```

**Response** (201 Created):
```json
{
  "hypothesisId": "uuid",
  "problemId": "uuid",
  "organizationId": "uuid",
  "statement": "If we add quick actions to the dashboard, user engagement will increase by 20%",
  "expectedOutcome": "Average session time increases from 2 to 2.4 minutes",
  "createdBy": "uuid",
  "createdAt": "ISO-8601"
}
```

**Business Rules**:
- Problem must exist and belong to organization
- Publishes HypothesisCreated event

---

#### GET /api/v1/hypotheses

**Purpose**: List hypotheses for organization

**Query Parameters**:
- `problemId` (optional filter)
- `page`, `size`, `sort`

**Response** (200 OK): Paginated list of hypotheses

---

#### GET /api/v1/hypotheses/{id}

**Purpose**: Get hypothesis details

**Response** (200 OK):
```json
{
  "hypothesisId": "uuid",
  "problemId": "uuid",
  "organizationId": "uuid",
  "statement": "If we add quick actions, engagement will increase by 20%",
  "expectedOutcome": "Average session time increases from 2 to 2.4 minutes",
  "experiments": [
    {
      "experimentId": "uuid",
      "name": "Quick Actions A/B Test",
      "status": "COMPLETED"
    }
  ],
  "createdBy": "uuid",
  "createdAt": "ISO-8601"
}
```

---

#### PUT /api/v1/hypotheses/{id}

**Purpose**: Update hypothesis

**Request Body**:
```json
{
  "statement": "Updated statement",
  "expectedOutcome": "Updated outcome"
}
```

**Response** (200 OK): Updated hypothesis object

---

### Experiment Management

#### POST /api/v1/experiments

**Purpose**: Create a new experiment

**Request Body**:
```json
{
  "hypothesisId": "uuid",
  "name": "Quick Actions A/B Test",
  "description": "Test quick actions feature with 50% of users"
}
```

**Response** (201 Created):
```json
{
  "experimentId": "uuid",
  "hypothesisId": "uuid",
  "organizationId": "uuid",
  "name": "Quick Actions A/B Test",
  "description": "Test quick actions feature with 50% of users",
  "status": "DRAFT",
  "createdBy": "uuid",
  "createdAt": "ISO-8601"
}
```

**Business Rules**:
- Hypothesis must exist and belong to organization
- Initial status is DRAFT

---

#### GET /api/v1/experiments

**Purpose**: List experiments for organization

**Query Parameters**:
- `hypothesisId` (optional filter)
- `status` (optional filter: DRAFT, RUNNING, COMPLETED)
- `page`, `size`, `sort`

**Response** (200 OK):
```json
{
  "experiments": [
    {
      "experimentId": "uuid",
      "name": "Quick Actions A/B Test",
      "status": "RUNNING",
      "startTime": "ISO-8601",
      "metricsCount": 15,
      "createdAt": "ISO-8601"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 25,
  "totalPages": 2
}
```

---

#### GET /api/v1/experiments/{id}

**Purpose**: Get experiment details

**Response** (200 OK):
```json
{
  "experimentId": "uuid",
  "hypothesisId": "uuid",
  "organizationId": "uuid",
  "name": "Quick Actions A/B Test",
  "description": "Test quick actions feature with 50% of users",
  "status": "RUNNING",
  "startTime": "ISO-8601",
  "endTime": null,
  "metricsCount": 15,
  "result": null,
  "createdBy": "uuid",
  "createdAt": "ISO-8601",
  "updatedAt": "ISO-8601"
}
```

---

#### PUT /api/v1/experiments/{id}

**Purpose**: Update experiment details (only in DRAFT status)

**Request Body**:
```json
{
  "name": "Updated name",
  "description": "Updated description"
}
```

**Response** (200 OK): Updated experiment object

**Business Rules**:
- Can only update if status is DRAFT

---

#### POST /api/v1/experiments/{id}/start

**Purpose**: Start an experiment (DRAFT → RUNNING)

**Response** (200 OK):
```json
{
  "experimentId": "uuid",
  "status": "RUNNING",
  "startTime": "ISO-8601"
}
```

**Business Rules**:
- Experiment must be in DRAFT status
- Sets startTime to current timestamp
- Changes status to RUNNING
- Publishes ExperimentStarted event

---

#### POST /api/v1/experiments/{id}/complete

**Purpose**: Complete an experiment (RUNNING → COMPLETED)

**Request Body**:
```json
{
  "summary": "Quick actions increased engagement by 25%",
  "outcome": "VALIDATED"
}
```

**Response** (200 OK):
```json
{
  "experimentId": "uuid",
  "status": "COMPLETED",
  "endTime": "ISO-8601",
  "result": {
    "summary": "Quick actions increased engagement by 25%",
    "outcome": "VALIDATED"
  }
}
```

**Business Rules**:
- Experiment must be in RUNNING status
- Must have at least one metric recorded
- Sets endTime to current timestamp
- Changes status to COMPLETED
- Creates ExperimentResult
- Publishes ExperimentCompleted event

---

## Database Schema

**Schema**: `experiment_schema`  
**Connection Configuration**:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:5432/turaf?currentSchema=experiment_schema
    username: experiment_user
    password: ${DB_PASSWORD}
  jpa:
    properties:
      hibernate:
        default_schema: experiment_schema
  flyway:
    schemas: experiment_schema
    default-schema: experiment_schema
```

### problems

```sql
CREATE TABLE problems (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    affected_users TEXT,
    context TEXT,
    created_by UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_problems_org_id ON problems(organization_id);
CREATE INDEX idx_problems_created_by ON problems(created_by);
CREATE INDEX idx_problems_created_at ON problems(created_at DESC);
```

### hypotheses

```sql
CREATE TABLE hypotheses (
    id UUID PRIMARY KEY,
    problem_id UUID NOT NULL REFERENCES problems(id) ON DELETE CASCADE,
    organization_id UUID NOT NULL,
    statement TEXT NOT NULL,
    expected_outcome TEXT,
    created_by UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_hypotheses_problem_id ON hypotheses(problem_id);
CREATE INDEX idx_hypotheses_org_id ON hypotheses(organization_id);
CREATE INDEX idx_hypotheses_created_at ON hypotheses(created_at DESC);
```

### experiments

```sql
CREATE TABLE experiments (
    id UUID PRIMARY KEY,
    hypothesis_id UUID NOT NULL REFERENCES hypotheses(id) ON DELETE CASCADE,
    organization_id UUID NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    created_by UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_status CHECK (status IN ('DRAFT', 'RUNNING', 'COMPLETED', 'CANCELLED'))
);

CREATE INDEX idx_experiments_hypothesis_id ON experiments(hypothesis_id);
CREATE INDEX idx_experiments_org_id ON experiments(organization_id);
CREATE INDEX idx_experiments_status ON experiments(status);
CREATE INDEX idx_experiments_created_at ON experiments(created_at DESC);
```

### experiment_results

```sql
CREATE TABLE experiment_results (
    id UUID PRIMARY KEY,
    experiment_id UUID NOT NULL REFERENCES experiments(id) ON DELETE CASCADE,
    summary TEXT NOT NULL,
    outcome VARCHAR(20) NOT NULL,
    metrics_analysis TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_outcome CHECK (outcome IN ('VALIDATED', 'INVALIDATED', 'INCONCLUSIVE')),
    UNIQUE(experiment_id)
);

CREATE INDEX idx_results_experiment_id ON experiment_results(experiment_id);
```

---

## Domain Logic

### Experiment State Machine

**States**: DRAFT → RUNNING → COMPLETED

**Transitions**:

```java
@Entity
@Table(name = "experiments")
public class Experiment {
    
    @Id
    private UUID id;
    
    @Enumerated(EnumType.STRING)
    private ExperimentStatus status;
    
    private Instant startTime;
    private Instant endTime;
    
    public void start() {
        if (status != ExperimentStatus.DRAFT) {
            throw new IllegalStateTransitionException(
                "Can only start experiments in DRAFT status"
            );
        }
        this.status = ExperimentStatus.RUNNING;
        this.startTime = Instant.now();
        // Publish ExperimentStarted event
    }
    
    public void complete(String summary, OutcomeType outcome) {
        if (status != ExperimentStatus.RUNNING) {
            throw new IllegalStateTransitionException(
                "Can only complete experiments in RUNNING status"
            );
        }
        this.status = ExperimentStatus.COMPLETED;
        this.endTime = Instant.now();
        this.result = new ExperimentResult(summary, outcome);
        // Publish ExperimentCompleted event
    }
    
    public boolean canRecordMetrics() {
        return status == ExperimentStatus.RUNNING;
    }
}
```

---

## Application Services

### ProblemService

```java
public interface ProblemService {
    ProblemDto createProblem(CreateProblemRequest request, UserId createdBy, OrganizationId orgId);
    ProblemDto getProblem(ProblemId id, OrganizationId orgId);
    Page<ProblemDto> getProblems(OrganizationId orgId, Pageable pageable);
    ProblemDto updateProblem(ProblemId id, UpdateProblemRequest request, OrganizationId orgId);
    void deleteProblem(ProblemId id, OrganizationId orgId);
}
```

### HypothesisService

```java
public interface HypothesisService {
    HypothesisDto createHypothesis(CreateHypothesisRequest request, UserId createdBy, OrganizationId orgId);
    HypothesisDto getHypothesis(HypothesisId id, OrganizationId orgId);
    Page<HypothesisDto> getHypotheses(OrganizationId orgId, Optional<ProblemId> problemId, Pageable pageable);
    HypothesisDto updateHypothesis(HypothesisId id, UpdateHypothesisRequest request, OrganizationId orgId);
    void deleteHypothesis(HypothesisId id, OrganizationId orgId);
}
```

### ExperimentService

```java
public interface ExperimentService {
    ExperimentDto createExperiment(CreateExperimentRequest request, UserId createdBy, OrganizationId orgId);
    ExperimentDto getExperiment(ExperimentId id, OrganizationId orgId);
    Page<ExperimentDto> getExperiments(OrganizationId orgId, Optional<ExperimentStatus> status, Pageable pageable);
    ExperimentDto updateExperiment(ExperimentId id, UpdateExperimentRequest request, OrganizationId orgId);
    ExperimentDto startExperiment(ExperimentId id, OrganizationId orgId);
    ExperimentDto completeExperiment(ExperimentId id, CompleteExperimentRequest request, OrganizationId orgId);
}
```

---

## Events Published

### ProblemCreated

**Payload**:
```json
{
  "problemId": "uuid",
  "title": "string",
  "description": "string",
  "createdBy": "userId"
}
```

### HypothesisCreated

**Payload**:
```json
{
  "hypothesisId": "uuid",
  "problemId": "uuid",
  "statement": "string",
  "createdBy": "userId"
}
```

### ExperimentStarted

**Payload**:
```json
{
  "experimentId": "uuid",
  "hypothesisId": "uuid",
  "name": "string",
  "startTime": "ISO-8601"
}
```

### ExperimentCompleted

**Payload**:
```json
{
  "experimentId": "uuid",
  "endTime": "ISO-8601",
  "resultSummary": "string",
  "outcome": "string"
}
```

**Critical Event**: Triggers report generation and notifications

---

## Validation Rules

**Problem**:
- Title: 1-200 characters, required
- Description: 1-5000 characters, required

**Hypothesis**:
- Statement: 1-500 characters, required
- Must belong to existing problem

**Experiment**:
- Name: 1-200 characters, required
- Must belong to existing hypothesis
- Cannot start without being in DRAFT
- Cannot complete without metrics

---

## Error Handling

**Error Codes**:
- `EXP_001`: Experiment not found
- `EXP_002`: Invalid state transition
- `EXP_003`: Cannot complete without metrics
- `EXP_004`: Hypothesis not found
- `EXP_005`: Problem not found

---

## Testing Strategy

### Unit Tests
- Test state machine transitions
- Test validation rules
- Test business logic
- Test event publishing

### Integration Tests
- Test repository methods
- Test transaction boundaries
- Test event publishing to EventBridge

### API Tests
- Test all endpoints
- Test authorization
- Test state transitions
- Test error scenarios

---

## Monitoring

### Metrics
- Experiments created
- Experiments started
- Experiments completed
- Average experiment duration
- Problems created
- Hypotheses created

### Logging
- Log all state transitions
- Log all events published
- Include experiment context

---

## References

- PROJECT.md: Experiment Service specification
- domain-model.md: Entity definitions
- event-flow.md: Event specifications
