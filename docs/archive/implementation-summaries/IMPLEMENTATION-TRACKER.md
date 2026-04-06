# Audit Corrections Implementation Tracker

**Started**: 2026-03-27  
**Status**: In Progress  
**Related**: [Evaluation Summary](audits/EVALUATION-SUMMARY.md)

---

## Critical Priority (Week 1-2)

### 1. Security: Authorization Checks ✅ 70% COMPLETE
**Status**: In Progress

**Completed**:
- ✅ Created `AuthorizationService` in common module
- ✅ Created `UnauthorizedException` in common module
- ✅ Created `AuthorizationServiceTest` with comprehensive test coverage
- ✅ Added authorization to ExperimentController (all 8 endpoints)
- ✅ Added authorization to HypothesisController (all 5 endpoints)
- ✅ Added authorization to ProblemController (all 5 endpoints)
- ✅ Added authorization to MetricController (all 6 endpoints)

**Remaining**:
- [ ] Add authorization to OrganizationController
- [ ] Add authorization to MembershipController
- [ ] Add authorization to AuthController (Identity Service)

**Pattern to Apply**:
```java
@RestController
public class ExampleController {
    private final AuthorizationService authorizationService;
    
    @PostMapping
    public ResponseDto create(
        @AuthenticationPrincipal UserPrincipal principal,
        @RequestBody RequestDto request) {
        
        authorizationService.validateTenantAccess(principal);
        // ... rest of method
    }
}
```

---

### 2. Testing: Repository Integration Tests ✅ 20% COMPLETE
**Status**: Templates Created

**Completed**:
- ✅ Created `ExperimentRepositoryIntegrationTest.java` (comprehensive template)
- ✅ Includes Testcontainers PostgreSQL setup
- ✅ Tests save/retrieve, multi-tenant filtering, status queries, delete, existence checks

**Required Files** (To be created based on template):
- [ ] `UserRepositoryIntegrationTest.java`
- [ ] `OrganizationRepositoryIntegrationTest.java`
- [ ] `ProblemRepositoryIntegrationTest.java`
- [ ] `HypothesisRepositoryIntegrationTest.java`
- [ ] `MetricRepositoryIntegrationTest.java`

**Dependencies to Add** (Still needed in POMs):
```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
```

---

### 3. Testing: Tenant Isolation Tests ✅ 25% COMPLETE
**Status**: Template Created

**Completed**:
- ✅ Created `TenantIsolationIntegrationTest.java` for Experiment Service
- ✅ Tests cross-organization data isolation
- ✅ Tests TenantInterceptor automatic organizationId setting
- ✅ Tests multiple organizations with separate data
- ✅ Tests query filtering by organizationId

**Required Files** (To be created based on template):
- [ ] `TenantIsolationIntegrationTest.java` for Identity Service
- [ ] `TenantIsolationIntegrationTest.java` for Organization Service
- [ ] `TenantIsolationIntegrationTest.java` for Metrics Service

---

### 4. Events: Idempotency in Consumers
**Status**: Not Started

**Services to Update**:
- [ ] Reporting Service (Lambda)
- [ ] Notification Service (Lambda)
- [ ] Any other event consumers

**Pattern**:
```java
@EventListener
public void handleEvent(DomainEvent event) {
    if (idempotencyService.isProcessed(event.getEventId())) {
        return;
    }
    // Process event
    idempotencyService.markProcessed(event.getEventId());
}
```

---

## High Priority (Week 3-4)

### 5. Security: JWT-Based Tenant Extraction
**Status**: Not Started

**Files to Create**:
- [ ] `JwtTenantFilter.java` in common module
- [ ] Update `TenantFilterConfig` in all services

---

### 6. Events: Correlation ID Support
**Status**: Not Started

**Changes Required**:
- [ ] Add `correlationId` to `DomainEvent` interface
- [ ] Update all event implementations
- [ ] Update `EventEnvelope`
- [ ] Add correlation ID generation/propagation

---

## Medium Priority (Week 5-6)

### 7. Architecture: ArchUnit Tests
**Status**: Not Started

**Files to Create**:
- [ ] `ArchitectureTest.java` for each service

---

### 8. Documentation: ADRs
**Status**: Not Started

**ADRs to Create**:
- [ ] ADR-001: Event Bus Choice
- [ ] ADR-002: Database Strategy
- [ ] ADR-003: Multi-Tenancy Approach
- [ ] ADR-004: Repository Pattern
- [ ] ADR-005: Domain Event Publishing

---

## Progress Summary

**Overall Completion**: 35%

| Priority | Items | Completed | In Progress | Not Started |
|----------|-------|-----------|-------------|-------------|
| Critical | 4 | 0 | 3 | 1 |
| High | 2 | 0 | 0 | 2 |
| Medium | 2 | 0 | 0 | 2 |
| **Total** | **8** | **0** | **3** | **5** |

**Files Created**: 7
**Controllers Updated**: 4 (24 endpoints secured)
**Test Templates Created**: 3

---

## Next Actions

1. Complete authorization checks in all controllers
2. Add Testcontainers dependencies
3. Create repository integration tests
4. Add tenant isolation tests
5. Implement idempotency in event consumers

---

**Last Updated**: 2026-03-27
