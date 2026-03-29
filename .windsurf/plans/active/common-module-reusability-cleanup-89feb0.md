# Common Module Reusability Cleanup

Remove 5 infrastructure implementation classes from `services/common`, add an `IdempotencyChecker` abstraction interface, delete associated tests, and clean up `pom.xml` dependencies to ensure the common module contains only reusable DDD base classes and abstractions.

---

## Context

The `services/common` module is described as "Shared domain-driven design base classes and utilities" but contains 5 concrete infrastructure classes that violate Clean Architecture and the Dependency Inversion Principle. Per PROJECT.md §12, §23 and ADR-006, the common module should provide abstractions — not JPA entities, DynamoDB clients, or EventBridge implementations.

---

## Classes to Remove (5 source + 2 test files)

| File | Reason |
|------|--------|
| `common/event/ProcessedEvent.java` | JPA `@Entity` — persistence concern, not a reusable abstraction |
| `common/event/ProcessedEventRepository.java` | Spring Data JPA repository — infrastructure layer |
| `common/event/DatabaseIdempotencyService.java` | PostgreSQL-based `@Service` — infrastructure implementation |
| `common/event/IdempotencyService.java` | DynamoDB-based implementation — infrastructure implementation |
| `common/event/EventBridgeEventPublisher.java` | AWS EventBridge impl — services already have their own (e.g. org-service `EventBridgePublisher`) |
| `common/test/.../EventBridgeEventPublisherTest.java` | Test for removed class |
| `common/test/.../IdempotencyServiceTest.java` | Test for removed class |

**No other service imports these classes** — grep confirms all references are within common itself.

---

## Abstraction to Add

**New file**: `common/event/IdempotencyChecker.java`

```java
package com.turaf.common.event;

/**
 * Abstraction for event idempotency checking.
 * Services implement this in their infrastructure layer using
 * their preferred persistence (PostgreSQL, DynamoDB, etc.).
 */
public interface IdempotencyChecker {
    boolean isProcessed(String eventId);
    void markProcessed(String eventId, String eventType, String organizationId);
}
```

This gives consuming services a shared contract. `IdempotencyRecord.java` (pure value object, already in common) remains as-is.

---

## pom.xml Dependency Cleanup

Remove from `common/pom.xml`:

| Dependency | Reason |
|------------|--------|
| `software.amazon.awssdk:eventbridge` | Only used by removed `EventBridgeEventPublisher` |
| `software.amazon.awssdk:dynamodb` | Only used by removed `IdempotencyService` |

**Keep**: `spring-boot-starter-data-jpa` (still used by tenant package), `jackson-*`, `spring-context`, `spring-boot-starter-web`, `jjwt-*`, `jakarta.servlet-api`, `hibernate-core`, `slf4j-api`, test deps.

---

## Classes Verified as Reusable (no changes needed)

- **`domain/`** (6 classes): `AggregateRoot`, `DomainEvent`, `DomainException`, `Entity`, `Repository`, `ValueObject` — pure DDD building blocks
- **`event/`** (remaining 5): `EventPublisher` (interface), `EventEnvelope`, `EventMetadata`, `EventSerializer`, `EventValidator`, `EventPublishException`, `EventValidationException`, `IdempotencyRecord` — abstractions and models
- **`security/`** (3 classes): `AuthorizationService`, `UnauthorizedException`, `UserPrincipal` — cross-cutting auth
- **`tenant/`** (7 classes): `TenantAware`, `TenantContext`, `TenantContextHolder`, `TenantException`, `TenantFilter`, `JwtTenantFilter`, `TenantInterceptor` — cross-cutting multi-tenancy

---

## Implementation Steps

1. Delete the 5 source files listed above
2. Delete the 2 test files listed above
3. Create `IdempotencyChecker.java` interface in `common/event/`
4. Remove `eventbridge` and `dynamodb` AWS SDK deps from `common/pom.xml`
5. Verify build: `mvn compile -pl services/common -am` from project root

---

## Risk Assessment

- **Low risk**: No service outside common imports any of the removed classes (verified via grep)
- **No migration needed**: The removed classes were never consumed by other services
- **Build verification**: Step 5 confirms no compile breakage
