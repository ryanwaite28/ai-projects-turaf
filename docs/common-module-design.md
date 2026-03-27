# Common Module Design Documentation

**Created**: 2026-03-26  
**Status**: Current  
**Related Documents**: 
- [Architecture Specification](../specs/architecture.md)
- [Domain Model Specification](../specs/domain-model.md)
- [Core Services Evaluation Plan](../.windsurf/plans/active/core-services-evaluation-plan.md)

---

## Overview

The `turaf-common` module provides shared domain-driven design (DDD) base classes, event infrastructure, and multi-tenancy support for all Turaf microservices. It follows Clean Architecture principles by providing abstractions that services can depend on without coupling to specific implementations.

**Module Location**: `/services/common`  
**Package Root**: `com.turaf.common`  
**Artifact ID**: `turaf-common`

---

## Design Principles

### 1. Dependency Inversion
- Services depend on abstractions (interfaces) defined in common
- Implementations can be swapped without changing service code
- Example: `EventPublisher` interface with `EventBridgeEventPublisher` implementation

### 2. Single Responsibility
- Each class has one clear purpose
- Domain concerns separated from infrastructure concerns
- Example: `AggregateRoot` manages domain events, not persistence

### 3. Open/Closed Principle
- Base classes are open for extension via inheritance
- Closed for modification - services extend, don't modify
- Example: Services extend `AggregateRoot<ID>` for their aggregate roots

### 4. Interface Segregation
- Small, focused interfaces
- Clients only depend on methods they use
- Example: `TenantAware` has only 2 methods

### 5. Minimal Dependencies
- Only essential external dependencies
- No service-specific logic
- Provided scope for optional dependencies (Hibernate, Servlet API)

---

## Package Structure

```
com.turaf.common/
├── domain/              # Core DDD building blocks
│   ├── AggregateRoot    # Base class for aggregate roots
│   ├── Entity           # Base class for entities
│   ├── ValueObject      # Base class for value objects
│   ├── Repository       # Repository interface pattern
│   ├── DomainEvent      # Domain event interface
│   └── DomainException  # Base domain exception
├── event/               # Event infrastructure
│   ├── EventPublisher   # Event publishing abstraction
│   ├── EventBridgeEventPublisher  # AWS EventBridge implementation
│   ├── EventEnvelope    # Event wrapper with metadata
│   ├── EventSerializer  # JSON serialization
│   ├── EventValidator   # Event validation
│   ├── IdempotencyService  # Duplicate event prevention
│   └── EventMetadata    # Event metadata model
├── tenant/              # Multi-tenancy support
│   ├── TenantAware      # Interface for tenant-scoped entities
│   ├── TenantContext    # Tenant context value object
│   ├── TenantContextHolder  # ThreadLocal context storage
│   ├── TenantFilter     # Servlet filter for context setup
│   ├── TenantInterceptor  # Hibernate interceptor for auto-setting orgId
│   └── TenantException  # Tenant-related exceptions
└── security/            # Security abstractions
    └── UserPrincipal    # Authenticated user context
```

---

## Domain Layer (`com.turaf.common.domain`)

### AggregateRoot<ID>

**Purpose**: Base class for all aggregate roots in the system

**Key Features**:
- Extends `Entity<ID>` for identity-based equality
- Manages domain events via `registerEvent()`, `getDomainEvents()`, `clearDomainEvents()`
- Enforces aggregate boundary pattern
- Thread-safe event collection

**Usage**:
```java
public class Experiment extends AggregateRoot<ExperimentId> {
    public Experiment(ExperimentId id, ...) {
        super(id);
    }
    
    public void start() {
        // Business logic
        this.status = ExperimentStatus.RUNNING;
        
        // Register domain event
        registerEvent(new ExperimentStarted(
            UUID.randomUUID().toString(),
            getId().getValue(),
            organizationId,
            Instant.now()
        ));
    }
}
```

**Design Rationale**:
- Centralizes domain event management
- Ensures events are published after persistence (application service responsibility)
- Provides consistent pattern across all aggregates

---

### Entity<ID>

**Purpose**: Base class for all domain entities

**Key Features**:
- Identity-based equality (two entities equal if same ID)
- Immutable ID after construction
- Proper `equals()`, `hashCode()`, `toString()` implementations

**Usage**:
```java
public class User extends Entity<UserId> {
    public User(UserId id, ...) {
        super(id);
    }
}
```

**Design Rationale**:
- Enforces DDD entity semantics
- Prevents common equality bugs
- Type-safe ID parameter

---

### ValueObject

**Purpose**: Base class for all value objects

**Key Features**:
- Structural equality (equal if all components equal)
- Immutability enforced by design
- Subclasses define equality components

**Usage**:
```java
public class Email extends ValueObject {
    private final String value;
    
    public Email(String value) {
        // Validation
        this.value = value;
    }
    
    @Override
    protected List<Object> getEqualityComponents() {
        return List.of(value.toLowerCase());
    }
}
```

**Design Rationale**:
- Enforces value object semantics
- Consistent equality behavior
- Prevents mutable value objects

---

### Repository<T, ID>

**Purpose**: Base interface for all repositories

**Key Features**:
- Generic interface for aggregate root persistence
- Only works with aggregate roots, not entities
- Provides `findById()`, `save()`, `delete()`, `existsById()`

**Usage**:
```java
public interface ExperimentRepository extends Repository<Experiment, ExperimentId> {
    List<Experiment> findByOrganizationId(OrganizationId orgId);
    List<Experiment> findByStatus(ExperimentStatus status);
}
```

**Design Rationale**:
- Enforces repository pattern
- Type-safe with generics
- Minimal interface (ISP)
- Domain layer defines contract, infrastructure implements

---

### DomainEvent

**Purpose**: Interface for all domain events

**Key Features**:
- Immutable by design
- Required fields: `eventId`, `eventType`, `occurredAt`, `organizationId`
- Past-tense naming convention

**Usage**:
```java
public class ExperimentStarted implements DomainEvent {
    private final String eventId;
    private final String experimentId;
    private final String organizationId;
    private final Instant occurredAt;
    
    // Constructor, getters...
    
    @Override
    public String getEventType() {
        return "ExperimentStarted";
    }
}
```

**Design Rationale**:
- Consistent event structure across services
- Multi-tenancy built-in (organizationId)
- Idempotency support (eventId)
- Timestamp for event ordering

---

### DomainException

**Purpose**: Base exception for domain-level errors

**Key Features**:
- Includes error code for categorization
- RuntimeException (unchecked)
- Supports cause chaining

**Usage**:
```java
public class InvalidExperimentStateException extends DomainException {
    public InvalidExperimentStateException(String message) {
        super(message, "INVALID_EXPERIMENT_STATE");
    }
}
```

**Design Rationale**:
- Distinguishes domain errors from infrastructure errors
- Error codes enable consistent error handling
- Unchecked to avoid cluttering domain code

---

## Event Layer (`com.turaf.common.event`)

### EventPublisher

**Purpose**: Abstraction for publishing domain events

**Key Features**:
- Single event publishing: `publish(DomainEvent)`
- Batch publishing: `publishBatch(List<DomainEvent>)`
- Implementation-agnostic

**Design Rationale**:
- Decouples services from EventBridge
- Enables testing with mock implementations
- Supports future event bus migrations

---

### EventBridgeEventPublisher

**Purpose**: AWS EventBridge implementation of EventPublisher

**Key Features**:
- Wraps events in `EventEnvelope` with metadata
- Validates events before publishing
- Automatic batching (10 events per request limit)
- Comprehensive error handling and logging
- Source identification: `turaf.{service-name}`

**Configuration**:
```yaml
aws:
  eventbridge:
    bus-name: ${EVENTBRIDGE_BUS_NAME}
spring:
  application:
    name: experiment-service
```

**Design Rationale**:
- Isolates AWS-specific code
- Provides observability via logging
- Handles EventBridge constraints
- Configurable via Spring properties

---

### EventEnvelope

**Purpose**: Wraps domain events with metadata

**Key Features**:
- Adds correlation ID, source service, version
- Includes original event as `payload`
- Serializable to JSON

**Structure**:
```json
{
  "eventId": "uuid",
  "eventType": "ExperimentStarted",
  "occurredAt": "2026-03-26T10:00:00Z",
  "organizationId": "org-123",
  "source": "experiment-service",
  "version": "1.0",
  "correlationId": "uuid",
  "payload": { /* original event */ }
}
```

**Design Rationale**:
- Consistent event structure across all services
- Supports distributed tracing (correlationId)
- Event versioning for schema evolution
- Preserves original event data

---

### EventValidator

**Purpose**: Validates event structure and content

**Key Features**:
- Validates required fields
- Checks field formats (UUIDs, timestamps)
- Validates organization ID format
- Throws `EventValidationException` on failure

**Design Rationale**:
- Fail fast on invalid events
- Prevents malformed events in event bus
- Consistent validation across services

---

### IdempotencyService

**Purpose**: Prevents duplicate event processing

**Key Features**:
- Stores processed event IDs in DynamoDB
- TTL-based cleanup (7 days default)
- Thread-safe operations

**Design Rationale**:
- At-least-once delivery requires idempotency
- Centralized deduplication logic
- Automatic cleanup via TTL

---

## Tenant Layer (`com.turaf.common.tenant`)

### TenantAware

**Purpose**: Marker interface for tenant-scoped entities

**Key Features**:
- `getOrganizationId()` / `setOrganizationId(String)`
- Enables automatic organization ID setting

**Usage**:
```java
public class Experiment extends AggregateRoot<ExperimentId> implements TenantAware {
    private String organizationId;
    
    @Override
    public String getOrganizationId() {
        return organizationId;
    }
    
    @Override
    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }
}
```

**Design Rationale**:
- Enables automatic tenant scoping
- Works with `TenantInterceptor`
- Explicit interface for tenant-aware entities

---

### TenantContext

**Purpose**: Immutable value object holding tenant information

**Key Features**:
- Contains `organizationId` and `userId`
- Immutable after construction
- Proper value object equality

**Design Rationale**:
- Type-safe tenant context
- Immutability prevents accidental modification
- Clear contract for tenant information

---

### TenantContextHolder

**Purpose**: ThreadLocal storage for tenant context

**Key Features**:
- Static methods: `setContext()`, `getContext()`, `clear()`
- Convenience methods: `getOrganizationId()`, `getUserId()`
- Optional access: `getContextOptional()`
- Context validation: `hasContext()`

**Usage**:
```java
// In application service
String orgId = TenantContextHolder.getOrganizationId();
List<Experiment> experiments = experimentRepository.findByOrganizationId(orgId);
```

**Design Rationale**:
- Similar pattern to Spring Security's SecurityContextHolder
- Thread-safe via ThreadLocal
- Automatic cleanup prevents memory leaks
- Convenient static access

---

### TenantFilter

**Purpose**: Servlet filter that sets up tenant context

**Key Features**:
- Extracts `X-Organization-Id` and `X-User-Id` headers
- Creates and sets `TenantContext`
- Automatically clears context after request
- Extensible via protected methods

**Configuration**:
```java
@Bean
public FilterRegistrationBean<TenantFilter> tenantFilter() {
    FilterRegistrationBean<TenantFilter> registration = new FilterRegistrationBean<>();
    registration.setFilter(new TenantFilter());
    registration.addUrlPatterns("/*");
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return registration;
}
```

**Design Rationale**:
- Centralized tenant context setup
- Runs before all other filters
- Automatic cleanup prevents leaks
- Extensible for custom extraction logic

---

### TenantInterceptor

**Purpose**: Hibernate interceptor for automatic organization ID setting

**Key Features**:
- Automatically sets `organizationId` on save for `TenantAware` entities
- Validates organization ID on update (prevents cross-tenant modification)
- Logs warnings if no context available

**Configuration**:
```java
@Bean
public HibernatePropertiesCustomizer hibernatePropertiesCustomizer() {
    return hibernateProperties -> 
        hibernateProperties.put(AvailableSettings.INTERCEPTOR, new TenantInterceptor());
}
```

**Design Rationale**:
- Automatic tenant scoping without manual code
- Prevents cross-tenant data corruption
- Fail-safe: warns but doesn't fail if no context

---

## Security Layer (`com.turaf.common.security`)

### UserPrincipal

**Purpose**: Represents authenticated user with organization context

**Key Features**:
- Contains `userId`, `email`, `name`, `organizationId`
- Immutable value object
- Extracted from JWT token

**Usage**:
```java
@PostMapping("/experiments")
public ExperimentDto createExperiment(
    @AuthenticationPrincipal UserPrincipal principal,
    @RequestBody CreateExperimentRequest request) {
    
    String orgId = principal.getOrganizationId();
    // Use in service call
}
```

**Design Rationale**:
- Type-safe authentication context
- Carries organization for authorization
- Works with Spring Security's `@AuthenticationPrincipal`

---

## Dependencies

### Required Dependencies
- **Spring Context** (5.x+): For `@Component`, dependency injection
- **Jackson** (2.x): For JSON serialization
- **AWS SDK v2 EventBridge**: For event publishing
- **AWS SDK v2 DynamoDB**: For idempotency tracking
- **SLF4J** (2.x): For logging

### Provided Dependencies (Optional)
- **Jakarta Servlet API** (6.x): For `TenantFilter` (only if using servlet containers)
- **Hibernate Core** (6.x): For `TenantInterceptor` (only if using JPA/Hibernate)

### Test Dependencies
- **JUnit Jupiter** (5.x)
- **AssertJ** (3.x)
- **Mockito** (5.x)

---

## Usage Guidelines

### For Service Developers

1. **Extend Base Classes**:
   - Aggregate roots extend `AggregateRoot<ID>`
   - Entities extend `Entity<ID>`
   - Value objects extend `ValueObject`

2. **Implement Interfaces**:
   - Repositories extend `Repository<T, ID>`
   - Domain events implement `DomainEvent`
   - Tenant-scoped entities implement `TenantAware`

3. **Use Event Publishing**:
   - Inject `EventPublisher` in application services
   - Call `publish()` after successful persistence
   - Clear domain events after publishing

4. **Configure Multi-Tenancy**:
   - Register `TenantFilter` as highest precedence filter
   - Configure `TenantInterceptor` with Hibernate
   - Use `TenantContextHolder` to access tenant context

5. **Handle Exceptions**:
   - Extend `DomainException` for domain errors
   - Include meaningful error codes
   - Let infrastructure layer translate to HTTP responses

---

## Design Decisions

### Why Not Use Spring Data Commons?
- **Reason**: Spring Data Commons is tied to Spring Data infrastructure
- **Benefit**: Our `Repository` interface is pure domain, no Spring coupling
- **Trade-off**: Must implement repository pattern manually

### Why ThreadLocal for Tenant Context?
- **Reason**: Request-scoped context without passing parameters everywhere
- **Benefit**: Clean service method signatures
- **Trade-off**: Must ensure cleanup to prevent memory leaks (handled by filter)

### Why EventBridge-Specific Implementation in Common?
- **Reason**: All services use EventBridge, no other event bus planned
- **Benefit**: Shared implementation reduces duplication
- **Trade-off**: If switching event bus, must update common module
- **Mitigation**: Interface abstraction makes switching easier

### Why Separate Event and Domain Packages?
- **Reason**: Domain layer should have zero infrastructure dependencies
- **Benefit**: `DomainEvent` interface is pure domain, `EventPublisher` is infrastructure
- **Trade-off**: Slight package complexity
- **Alignment**: Follows Clean Architecture dependency rules

---

## Testing Strategy

### Unit Tests
- Test base class behavior (Entity equality, ValueObject equality)
- Test event validation logic
- Test tenant context management
- Mock external dependencies (EventBridge, DynamoDB)

### Integration Tests
- Test EventBridge publishing with LocalStack
- Test DynamoDB idempotency with Testcontainers
- Test Hibernate interceptor with test database
- Test servlet filter with MockMvc

---

## Migration Guide

### Adding Common Module to Existing Service

1. **Add Dependency**:
```xml
<dependency>
    <groupId>com.turaf</groupId>
    <artifactId>turaf-common</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

2. **Update Domain Classes**:
   - Change aggregate roots to extend `AggregateRoot<ID>`
   - Change entities to extend `Entity<ID>`
   - Change value objects to extend `ValueObject`

3. **Update Repositories**:
   - Change repository interfaces to extend `Repository<T, ID>`
   - Keep custom query methods

4. **Configure Multi-Tenancy**:
   - Register `TenantFilter` bean
   - Configure `TenantInterceptor` with Hibernate
   - Implement `TenantAware` on aggregate roots

5. **Configure Event Publishing**:
   - Add EventBridge configuration properties
   - Inject `EventPublisher` in application services
   - Replace manual EventBridge calls with `eventPublisher.publish()`

---

## Future Enhancements

### Potential Additions
- **Specification Pattern**: For complex queries
- **Domain Service Base**: For domain services that don't fit in aggregates
- **Saga Support**: For distributed transactions
- **Outbox Pattern**: For transactional event publishing

### Potential Improvements
- **Event Schema Registry**: Validate events against schemas
- **Event Replay**: Replay events for debugging/recovery
- **Tenant Context Propagation**: Async/reactive support
- **Performance Monitoring**: Built-in metrics for event publishing

---

## References

- [Clean Architecture (Robert C. Martin)](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Domain-Driven Design (Eric Evans)](https://www.domainlanguage.com/ddd/)
- [Implementing Domain-Driven Design (Vaughn Vernon)](https://vaughnvernon.com/)
- [AWS EventBridge Documentation](https://docs.aws.amazon.com/eventbridge/)
- [Spring Framework Reference](https://docs.spring.io/spring-framework/reference/)
