# System Architecture Specification

**Source**: PROJECT.md (Sections 9, 23, 26)

This specification defines the overall system architecture for the Turaf event-driven SaaS platform.

---

## Architecture Pattern

**Event-Driven Microservices Architecture** with Clean Architecture principles

- Microservices communicate asynchronously via domain events
- Each service maintains its own bounded context
- Services are loosely coupled through event bus
- Clean Architecture enforces strict layer separation
- Domain-Driven Design guides service boundaries

---

## System Layers

### Client Applications
- Angular SPA frontend
- Future: Mobile applications

### API Layer
- AWS API Gateway for request routing
- Authentication and authorization
- Rate limiting and throttling
- Request validation

### Application Services (Microservices)
- Identity Service
- Organization Service
- Experiment Service
- Metrics Service
- Reporting Service (Lambda)
- Notification Service (Lambda)

### Event Infrastructure
- AWS EventBridge (custom event bus)
- Event routing and filtering
- Dead letter queues for failed events
- Event archiving

### Async Processing Services
- Lambda-based event consumers
- Report generation
- Notification delivery
- Metric aggregation

### Data Storage
- Amazon RDS PostgreSQL (transactional data)
- Amazon S3 (reports, artifacts)
- Multi-AZ deployment for high availability

---

## Clean Architecture Layers

### Domain Layer (Innermost)
**Location**: `src/domain/`

**Contents**:
- Entities (Organization, User, Problem, Hypothesis, Experiment, Metric, Report)
- Value Objects (OrganizationId, UserId, MetricValue, DateRange)
- Domain Events (ProblemCreated, ExperimentCompleted, etc.)
- Repository Interfaces (contracts only, no implementations)

**Dependencies**: None (pure domain logic)

**Rules**:
- No dependencies on infrastructure or frameworks
- Contains only business logic
- Immutable where possible
- Rich domain models with behavior

### Application Layer
**Location**: `src/application/`

**Contents**:
- Use Cases (CreateExperiment, RecordMetric, GenerateReport)
- Application Services (orchestration logic)
- DTOs (Data Transfer Objects)
- Command and Query handlers

**Dependencies**: Domain layer only

**Rules**:
- Orchestrates domain objects
- Implements use case workflows
- No infrastructure concerns
- Transaction boundaries

### Infrastructure Layer
**Location**: `src/infrastructure/`

**Contents**:
- Repository Implementations (JPA, PostgreSQL)
- Event Publishers (EventBridge adapters)
- External Service Clients
- Configuration classes
- Database migrations

**Dependencies**: Domain and Application layers

**Rules**:
- Implements domain repository interfaces
- Handles all external concerns
- Framework-specific code lives here
- Dependency injection configuration

### Interface Layer (Outermost)
**Location**: `src/interfaces/`

**Contents**:
- REST Controllers
- Event Handlers (EventBridge consumers)
- GraphQL Resolvers (optional)
- Request/Response models

**Dependencies**: Application layer

**Rules**:
- Handles HTTP/event protocol concerns
- Maps external models to DTOs
- Validation annotations
- API documentation (OpenAPI)

---

## Dependency Rules

**The Dependency Rule**: Source code dependencies must point inward

```
Interfaces → Application → Domain
     ↓            ↓
Infrastructure → Domain
```

- Domain has zero dependencies
- Application depends only on Domain
- Infrastructure depends on Domain and Application
- Interfaces depend on Application
- **Never**: Inner layers depend on outer layers

---

## SOLID Principles Application

### Single Responsibility Principle
- Each service has one reason to change
- Each class/module has one responsibility
- Separation of concerns enforced by layers

### Open/Closed Principle
- Services open for extension via events
- Closed for modification through interfaces
- New features added without changing existing code

### Liskov Substitution Principle
- Repository implementations are interchangeable
- Event handlers can be swapped
- Interface contracts must be honored

### Interface Segregation Principle
- Small, focused repository interfaces
- Service-specific event contracts
- No fat interfaces

### Dependency Inversion Principle
- High-level modules depend on abstractions
- Repository interfaces in domain layer
- Implementations in infrastructure layer
- Dependency injection throughout

---

## Multi-Tenant Architecture

### Tenant Isolation Strategy

**Organization-Based Tenancy**:
- Each organization is a tenant
- All data includes `organizationId` field
- Row-level security via query filters
- No shared data between organizations

**Data Isolation**:
- All queries filtered by `organizationId`
- Authorization checks at service layer
- Separate encryption keys per tenant (KMS)
- Audit logging per organization

**API Security**:
- JWT tokens include organization context
- Middleware validates organization access
- Users can only access their organization's data
- Cross-organization requests blocked

---

## Event-Driven Architecture

### Event Flow Pattern

```
User Action → API Service → Domain Logic → Domain Event → Event Bus → Event Consumers → Side Effects
```

### Event Characteristics
- **Immutable**: Events cannot be changed once published
- **Versioned**: Events include version number for evolution
- **Timestamped**: All events have ISO-8601 timestamp
- **Traceable**: Events include correlation IDs
- **Reliable**: At-least-once delivery guarantee

### Event Processing
- Asynchronous processing via EventBridge
- Idempotent event handlers
- Retry policies with exponential backoff
- Dead letter queues for failed events
- Event replay capability via archives

---

## Service Boundaries (Bounded Contexts)

### Identity Context
- User authentication and authorization
- JWT token management
- Password security

### Organization Context
- Organization lifecycle
- Membership management
- Tenant isolation

### Experiment Context
- Problem definition
- Hypothesis tracking
- Experiment execution
- Results management

### Metrics Context
- Metric ingestion
- Aggregation and analysis
- Time-series data

### Reporting Context
- Report generation
- Template management
- Report storage

### Notification Context
- Event-driven notifications
- Multi-channel delivery
- Webhook integrations

---

## Scalability Strategy

### Horizontal Scaling
- ECS services scale via task count
- Auto-scaling based on CPU/memory metrics
- Lambda functions scale automatically
- Database read replicas for read-heavy workloads

### Caching Strategy
- API response caching (future: ElastiCache)
- CloudFront CDN for frontend assets
- Database query result caching
- Event deduplication

### Performance Optimization
- Database connection pooling
- Batch processing for metrics
- Asynchronous event processing
- Optimized database indexes

---

## Reliability Strategy

### Fault Tolerance
- Multi-AZ deployment for RDS
- ECS tasks distributed across AZs
- Retry policies for event processing
- Circuit breakers for external calls

### Data Durability
- RDS automated backups (daily)
- S3 versioning for reports
- Event archiving in EventBridge
- Point-in-time recovery capability

### Error Handling
- Dead letter queues for failed events
- Structured error logging
- Alerting on error rate thresholds
- Graceful degradation

---

## Security Architecture

### Authentication & Authorization
- JWT-based authentication
- Role-based access control (RBAC)
- Organization-level authorization
- API key authentication for webhooks

### Data Security
- Encryption at rest (KMS)
- Encryption in transit (TLS)
- Secrets management (AWS Secrets Manager)
- Least privilege IAM roles

### Network Security
- VPC isolation
- Private subnets for services
- Security groups for network segmentation
- WAF for API Gateway

---

## Observability Architecture

### Three Pillars

**Logging**:
- Structured JSON logs
- Centralized in CloudWatch Logs
- Correlation IDs for tracing
- Log levels per environment

**Metrics**:
- Custom CloudWatch metrics
- Business metrics (experiments_started, etc.)
- Infrastructure metrics (CPU, memory, latency)
- Dashboards for visualization

**Tracing**:
- AWS X-Ray distributed tracing
- Trace across service boundaries
- Event flow visualization
- Performance bottleneck identification

---

## Technology Stack Summary

**Backend**: Java 17, Spring Boot 3.x, Maven  
**Frontend**: Angular 17.x  
**Database**: PostgreSQL (RDS)  
**Event Bus**: AWS EventBridge  
**Compute**: ECS Fargate, AWS Lambda (Python 3.11)  
**Storage**: Amazon S3  
**IaC**: Terraform  
**CI/CD**: GitHub Actions  
**Monitoring**: CloudWatch, X-Ray  

---

## Architecture Decision Records

Key architectural decisions documented in `/docs/adr/`:
- ADR-001: Event bus choice (EventBridge)
- ADR-002: Database strategy (PostgreSQL)
- ADR-003: Compute platform (ECS Fargate + Lambda)
- ADR-004: IaC tool (Terraform)
- ADR-005: Frontend framework (Angular)

---

## References

- PROJECT.md: Authoritative system design
- Clean Architecture (Robert C. Martin)
- Domain-Driven Design (Eric Evans)
- AWS Well-Architected Framework
