# Task: Implement DDD Patterns

**Service**: Architecture Foundation  
**Phase**: 1  
**Estimated Time**: 3-4 hours  

## Objective

Implement foundational Domain-Driven Design patterns and base classes that will be used across all microservices to ensure consistency and enforce DDD principles.

## Prerequisites

- [x] Task 001: Clean Architecture layers established
- [ ] Understanding of DDD tactical patterns

## Scope

**Files to Create**:
- `services/common/src/main/java/com/turaf/common/domain/Entity.java`
- `services/common/src/main/java/com/turaf/common/domain/AggregateRoot.java`
- `services/common/src/main/java/com/turaf/common/domain/ValueObject.java`
- `services/common/src/main/java/com/turaf/common/domain/DomainEvent.java`
- `services/common/src/main/java/com/turaf/common/domain/Repository.java`
- `services/common/src/main/java/com/turaf/common/domain/DomainException.java`
- `services/common/pom.xml`

## Implementation Details

### Base Entity Class

```java
public abstract class Entity<ID> {
    protected ID id;
    
    protected Entity(ID id) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
    }
    
    public ID getId() {
        return id;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Entity)) return false;
        Entity<?> entity = (Entity<?>) o;
        return id.equals(entity.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
```

### Aggregate Root Class

```java
public abstract class AggregateRoot<ID> extends Entity<ID> {
    private final List<DomainEvent> domainEvents = new ArrayList<>();
    
    protected AggregateRoot(ID id) {
        super(id);
    }
    
    protected void registerEvent(DomainEvent event) {
        domainEvents.add(event);
    }
    
    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }
    
    public void clearDomainEvents() {
        domainEvents.clear();
    }
}
```

### Value Object Base Class

```java
public abstract class ValueObject {
    protected abstract List<Object> getEqualityComponents();
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValueObject that = (ValueObject) o;
        return Objects.equals(getEqualityComponents(), that.getEqualityComponents());
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(getEqualityComponents().toArray());
    }
}
```

### Domain Event Interface

```java
public interface DomainEvent {
    String getEventId();
    String getEventType();
    Instant getTimestamp();
    String getOrganizationId();
}
```

### Repository Interface

```java
public interface Repository<T extends AggregateRoot<ID>, ID> {
    Optional<T> findById(ID id);
    T save(T aggregate);
    void delete(T aggregate);
}
```

### Domain Exception

```java
public class DomainException extends RuntimeException {
    private final String errorCode;
    
    public DomainException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
```

### Common Module POM

Create a shared `common` module that all services depend on:
```xml
<project>
    <artifactId>turaf-common</artifactId>
    <packaging>jar</packaging>
    
    <dependencies>
        <!-- Minimal dependencies for domain patterns -->
    </dependencies>
</project>
```

## Acceptance Criteria

- [ ] Common module created with base DDD classes
- [ ] Entity base class implements identity equality
- [ ] AggregateRoot manages domain events
- [ ] ValueObject implements value equality
- [ ] DomainEvent interface defined
- [ ] Repository interface follows DDD pattern
- [ ] DomainException provides error context
- [ ] All services can depend on common module
- [ ] Maven build succeeds
- [ ] Unit tests for base classes pass

## Testing Requirements

**Unit Tests**:
- Test Entity equality based on ID
- Test ValueObject equality based on components
- Test AggregateRoot event registration and clearing
- Test DomainException creation

**Test Files to Create**:
- `EntityTest.java`
- `ValueObjectTest.java`
- `AggregateRootTest.java`

## References

- Specification: `specs/architecture.md` (DDD Principles section)
- Specification: `specs/domain-model.md`
- PROJECT.md: Section 9 (Domain-Driven Design)
- Domain-Driven Design by Eric Evans
- Related Tasks: All domain model tasks depend on this
