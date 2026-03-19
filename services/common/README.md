# Turaf Common Module

This module contains shared Domain-Driven Design (DDD) base classes and patterns used across all Turaf microservices.

## Purpose

The common module provides foundational DDD building blocks that ensure consistency and enforce domain modeling best practices across all services.

## Components

### Domain Model Base Classes

- **Entity**: Base class for entities with identity-based equality
- **ValueObject**: Base class for value objects with structural equality
- **AggregateRoot**: Base class for aggregate roots with domain event management
- **DomainEvent**: Interface for all domain events
- **Repository**: Interface for aggregate root repositories
- **DomainException**: Base exception for domain-level errors

## Usage

### Creating an Entity

```java
public class User extends Entity<String> {
    private String email;
    private String name;
    
    public User(String id, String email, String name) {
        super(id);
        this.email = email;
        this.name = name;
    }
}
```

### Creating a Value Object

```java
public class Email extends ValueObject {
    private final String value;
    
    public Email(String value) {
        this.value = value;
    }
    
    @Override
    protected List<Object> getEqualityComponents() {
        return List.of(value);
    }
}
```

### Creating an Aggregate Root

```java
public class Experiment extends AggregateRoot<String> {
    private String name;
    private ExperimentStatus status;
    
    public Experiment(String id, String name) {
        super(id);
        this.name = name;
        this.status = ExperimentStatus.DRAFT;
    }
    
    public void start() {
        this.status = ExperimentStatus.RUNNING;
        registerEvent(new ExperimentStartedEvent(getId(), Instant.now()));
    }
}
```

### Creating a Domain Event

```java
public class ExperimentStartedEvent implements DomainEvent {
    private final String eventId;
    private final String experimentId;
    private final Instant timestamp;
    private final String organizationId;
    
    @Override
    public String getEventId() {
        return eventId;
    }
    
    @Override
    public String getEventType() {
        return "ExperimentStarted";
    }
    
    @Override
    public Instant getTimestamp() {
        return timestamp;
    }
    
    @Override
    public String getOrganizationId() {
        return organizationId;
    }
}
```

### Creating a Repository

```java
public interface ExperimentRepository extends Repository<Experiment, String> {
    List<Experiment> findByOrganizationId(String organizationId);
    List<Experiment> findByStatus(ExperimentStatus status);
}
```

### Throwing Domain Exceptions

```java
public void validateExperiment() {
    if (name == null || name.isEmpty()) {
        throw new DomainException(
            "Experiment name cannot be empty",
            "INVALID_EXPERIMENT_NAME"
        );
    }
}
```

## Design Principles

### Entity vs Value Object

- **Entities** have identity and lifecycle (e.g., User, Experiment)
- **Value Objects** are immutable and defined by their attributes (e.g., Email, Money)

### Aggregate Roots

- Enforce consistency boundaries
- Manage domain events
- Serve as entry points to aggregates
- All modifications go through the aggregate root

### Domain Events

- Represent significant business occurrences
- Named in past tense
- Immutable
- Include organization ID for multi-tenancy

### Repositories

- Work only with aggregate roots
- Provide collection-like interface
- Hide persistence details from domain

## Multi-Tenant Context

The common module provides infrastructure for multi-tenant data isolation using organization ID as the tenant identifier.

### TenantContext

Immutable context object holding tenant information:
- `organizationId` - The tenant identifier
- `userId` - The current user making the request

### TenantContextHolder

Thread-local holder for accessing tenant context:

```java
// Get current organization ID
String orgId = TenantContextHolder.getOrganizationId();

// Get current user ID
String userId = TenantContextHolder.getUserId();

// Check if context is available
if (TenantContextHolder.hasContext()) {
    // Context is set
}
```

### TenantFilter

Servlet filter that extracts tenant information from request headers and sets up the context. Configure in your Spring Boot application:

```java
@Bean
public FilterRegistrationBean<TenantFilter> tenantFilter() {
    FilterRegistrationBean<TenantFilter> registration = new FilterRegistrationBean<>();
    registration.setFilter(new TenantFilter());
    registration.addUrlPatterns("/api/*");
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return registration;
}
```

### TenantAware Interface

Entities implementing this interface will have their `organizationId` automatically set:

```java
@Entity
public class Experiment extends AggregateRoot<String> implements TenantAware {
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

### TenantInterceptor

Hibernate interceptor that automatically sets `organizationId` on save and validates it on update. Configure in your JPA setup:

```java
@Bean
public LocalSessionFactoryBean sessionFactory() {
    LocalSessionFactoryBean sessionFactory = new LocalSessionFactoryBean();
    sessionFactory.setHibernateProperties(hibernateProperties());
    sessionFactory.setInterceptor(new TenantInterceptor());
    return sessionFactory;
}
```

## Dependencies

This module has minimal dependencies to keep the domain layer clean:
- JUnit 5 (test only)
- AssertJ (test only)
- Mockito (test only)
- Jakarta Servlet API (for TenantFilter)
- Hibernate Core (for TenantInterceptor)
- SLF4J (for logging)

## Testing

Run tests with:
```bash
mvn test
```

## Integration

Add this dependency to your service's `pom.xml`:

```xml
<dependency>
    <groupId>com.turaf</groupId>
    <artifactId>turaf-common</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```
