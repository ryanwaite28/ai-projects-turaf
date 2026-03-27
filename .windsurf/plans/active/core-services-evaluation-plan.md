# Core Services Implementation Evaluation Plan

**Created**: 2026-03-26  
**Status**: Active  
**Related Documents**: 
- [Architecture Specification](../../../specs/architecture.md)
- [Domain Model Specification](../../../specs/domain-model.md)
- [PROJECT.md](../../../PROJECT.md)

**Related Plans**: 
- Previous Docker build fixes (completed during this session)

---

## Objective

Re-evaluate the implementation of core Spring Boot microservices to ensure:
1. **Cohesive logic** across all services
2. **Proper usage** of the common module
3. **Alignment** with Clean Architecture and DDD principles from PROJECT.md
4. **Consistency** in multi-tenancy implementation
5. **Correct** domain event handling and publishing

---

## Current State Assessment

### Common Module Structure

The `common` module currently provides:
- **Domain Layer** (`com.turaf.common.domain`):
  - `AggregateRoot<ID>` - Base class for aggregate roots
  - `Entity<ID>` - Base class for entities
  - `ValueObject` - Marker interface for value objects
  - `Repository<T, ID>` - Base repository interface
  - `DomainEvent` - Interface for domain events
  - `DomainException` - Base exception class

- **Event Layer** (`com.turaf.common.event`):
  - `EventPublisher` - Interface for publishing domain events
  - `EventBridgeEventPublisher` - AWS EventBridge implementation
  - `EventEnvelope` - Wrapper for domain events with metadata
  - `EventValidator` - Validates event structure
  - `EventSerializer` - Serializes events to JSON
  - `IdempotencyService` - Prevents duplicate event processing
  - `EventPublishException` - Event publishing errors
  - `EventValidationException` - Event validation errors

- **Tenant Layer** (`com.turaf.common.tenant`):
  - `TenantAware` - Interface for tenant-aware entities
  - `TenantContext` - Holds current tenant context
  - `TenantContextHolder` - ThreadLocal tenant context storage
  - `TenantFilter` - Servlet filter for extracting tenant from requests
  - `TenantInterceptor` - Hibernate interceptor for multi-tenancy
  - `TenantException` - Tenant-related errors

- **Security Layer** (`com.turaf.common.security`):
  - `UserPrincipal` - Authenticated user context with organization

### Issues Identified During Build Fixes

1. **Package Naming Inconsistencies**:
   - Services importing from `com.turaf.common.event` (singular) vs `com.turaf.common.events` (plural)
   - Services importing from `com.turaf.common.multitenancy` vs `com.turaf.common.tenant`
   - Services importing from `com.turaf.common.domain` for `TenantAware` when it's in `tenant` package

2. **DomainEvent Interface Mismatch**:
   - Interface defines `getOccurredAt()` but some implementations use `getTimestamp()`
   - Duplicate `DomainEvent` interfaces existed in multiple packages

3. **Missing Common Components**:
   - `UserPrincipal` was service-specific (identity-service) but needed across services
   - No shared authentication/authorization utilities

4. **Constructor Signature Mismatches**:
   - Domain entities expecting different parameters than what services provide
   - Missing `organizationId` in User constructor calls

5. **Lombok Compilation Issues**:
   - Java 17 module access restrictions causing Lombok failures
   - Required compiler argument configuration

---

## Evaluation Areas

### 1. Common Module Design Review

**Goal**: Ensure the common module provides the right abstractions without coupling services

**Tasks**:
- [ ] Review `AggregateRoot` and `Entity` base classes for proper DDD implementation
- [ ] Verify `Repository` interface follows repository pattern correctly
- [ ] Ensure `DomainEvent` interface is complete and consistent
- [ ] Validate `EventPublisher` abstraction doesn't leak AWS-specific details
- [ ] Review multi-tenancy abstractions (`TenantAware`, `TenantContext`, etc.)
- [ ] Check if common module has appropriate dependencies (should be minimal)
- [ ] Verify no business logic in common module (only infrastructure concerns)

**Success Criteria**:
- Common module contains only cross-cutting concerns
- No service-specific logic in common module
- Clear separation between domain and infrastructure concerns
- All abstractions follow SOLID principles

---

### 2. Domain Layer Consistency

**Goal**: Ensure all services implement domain layer correctly per Clean Architecture

**Tasks**:
- [ ] **Identity Service**:
  - [ ] Verify `User` aggregate follows DDD patterns
  - [ ] Check `Email`, `Password`, `UserId` value objects are immutable
  - [ ] Ensure domain events (`UserCreated`, `UserPasswordChanged`, `UserProfileUpdated`) are properly structured
  - [ ] Validate repository interface is in domain layer, implementation in infrastructure

- [ ] **Organization Service**:
  - [ ] Verify `Organization` aggregate root design
  - [ ] Check `OrganizationMember` entity relationship
  - [ ] Ensure domain events (`OrganizationCreated`, `MemberAdded`, `MemberRemoved`) are consistent
  - [ ] Validate organization-level authorization logic

- [ ] **Experiment Service**:
  - [ ] Verify `Problem`, `Hypothesis`, `Experiment` aggregate boundaries
  - [ ] Check experiment state machine implementation (DRAFT → RUNNING → COMPLETED)
  - [ ] Ensure proper aggregate root selection
  - [ ] Validate domain events for each state transition

- [ ] **Metrics Service**:
  - [ ] Verify `Metric` entity design
  - [ ] Check metric aggregation logic placement (domain vs application layer)
  - [ ] Ensure batch processing follows domain rules

**Success Criteria**:
- All entities extend appropriate base classes from common module
- Value objects are immutable and properly encapsulated
- Domain logic is in domain layer, not application or infrastructure
- Repository interfaces are in domain, implementations in infrastructure
- Domain events follow consistent naming and structure

---

### 3. Multi-Tenancy Implementation

**Goal**: Ensure consistent multi-tenancy across all services

**Tasks**:
- [ ] Verify all aggregate roots implement `TenantAware` interface
- [ ] Check all entities include `organizationId` field
- [ ] Ensure `TenantFilter` is configured in all services
- [ ] Validate `TenantInterceptor` is registered with Hibernate in all services
- [ ] Review repository queries to ensure they filter by `organizationId`
- [ ] Check authorization logic validates organization context
- [ ] Verify JWT tokens include organization information
- [ ] Ensure `UserPrincipal` carries organization context

**Success Criteria**:
- All data access filtered by organization
- No cross-organization data leakage possible
- Consistent tenant context propagation
- Authorization checks at service boundaries

---

### 4. Event Publishing Consistency

**Goal**: Ensure all services publish domain events correctly

**Tasks**:
- [ ] Verify all aggregate roots use `registerEvent()` pattern
- [ ] Check events are published after persistence (transactional consistency)
- [ ] Ensure `EventPublisher` is injected via constructor (DI)
- [ ] Validate event payloads include all required fields
- [ ] Review event naming conventions (past tense, domain-specific)
- [ ] Check `EventEnvelope` wrapping is consistent
- [ ] Verify idempotency handling for event consumers
- [ ] Ensure correlation IDs propagate through event chains

**Success Criteria**:
- Events published only after successful persistence
- Consistent event structure across services
- No duplicate event processing
- Event traceability via correlation IDs

---

### 5. Dependency Injection & Configuration

**Goal**: Ensure proper Spring Boot configuration and DI

**Tasks**:
- [ ] Review component scanning configuration in each service
- [ ] Verify repository implementations are properly annotated
- [ ] Check service classes use constructor injection (not field injection)
- [ ] Ensure configuration classes are in infrastructure layer
- [ ] Validate database configuration (schema selection, connection pooling)
- [ ] Review security configuration consistency
- [ ] Check EventBridge client configuration

**Success Criteria**:
- Constructor injection used throughout
- No circular dependencies
- Configuration externalized (application.yml)
- Proper bean scopes

---

### 6. Application Layer Review

**Goal**: Ensure application services orchestrate correctly without business logic

**Tasks**:
- [ ] Verify application services delegate to domain objects
- [ ] Check transaction boundaries are at application service level
- [ ] Ensure DTOs are used for API contracts, not domain entities
- [ ] Validate use case implementation follows single responsibility
- [ ] Review error handling and exception translation
- [ ] Check application services don't contain domain logic

**Success Criteria**:
- Application layer is thin orchestration
- Domain logic stays in domain layer
- Clear separation of concerns
- Proper transaction management

---

### 7. Infrastructure Layer Review

**Goal**: Ensure infrastructure implementations don't leak into domain

**Tasks**:
- [ ] Review JPA entity mappings vs domain entities
- [ ] Check repository implementations properly map between JPA and domain
- [ ] Verify EventBridge publisher doesn't expose AWS details to domain
- [ ] Ensure database migrations are service-specific
- [ ] Validate no domain logic in repository implementations
- [ ] Review persistence exception handling

**Success Criteria**:
- Clean separation between domain and persistence models
- Infrastructure concerns isolated
- Domain layer has zero infrastructure dependencies
- Proper exception translation

---

### 8. API Layer (Interfaces) Review

**Goal**: Ensure REST controllers follow best practices

**Tasks**:
- [ ] Verify controllers are thin and delegate to application services
- [ ] Check request/response DTOs are properly validated
- [ ] Ensure proper HTTP status codes are used
- [ ] Validate OpenAPI/Swagger documentation
- [ ] Review error response structure consistency
- [ ] Check authentication/authorization at controller level
- [ ] Verify `UserPrincipal` injection from JWT

**Success Criteria**:
- Controllers contain no business logic
- Consistent API response structure
- Proper validation and error handling
- Complete API documentation

---

### 9. Testing Strategy Alignment

**Goal**: Ensure tests follow the testing strategy from PROJECT.md

**Tasks**:
- [ ] Verify unit tests for domain logic (80%+ coverage target)
- [ ] Check integration tests for repositories
- [ ] Ensure application service tests mock repositories
- [ ] Validate API tests use MockMvc
- [ ] Review test organization (unit vs integration)
- [ ] Check for Testcontainers usage in integration tests
- [ ] Verify LocalStack usage for AWS service mocking

**Success Criteria**:
- Comprehensive unit test coverage
- Integration tests for infrastructure
- No tests with external dependencies in unit tests
- Fast test execution

---

### 10. Cross-Service Consistency

**Goal**: Ensure patterns are consistent across all services

**Tasks**:
- [ ] Compare package structures across services
- [ ] Verify naming conventions are consistent
- [ ] Check exception handling patterns
- [ ] Ensure logging patterns are uniform
- [ ] Validate configuration patterns
- [ ] Review dependency versions in POMs

**Success Criteria**:
- Consistent project structure
- Uniform coding patterns
- Same architectural approach across services
- Aligned dependency versions

---

## Implementation Approach

### Phase 1: Common Module Refinement (Priority: High)
1. Document current common module design
2. Identify missing abstractions or over-coupling
3. Refactor common module if needed
4. Update service dependencies on common module
5. Verify all services compile after changes

### Phase 2: Domain Layer Audit (Priority: High)
1. Review each service's domain model
2. Verify aggregate boundaries
3. Check value object implementations
4. Validate domain event structures
5. Document findings and create fix tasks

### Phase 3: Multi-Tenancy Verification (Priority: High)
1. Audit tenant context propagation
2. Review authorization checks
3. Test cross-organization data isolation
4. Fix any tenant leakage issues

### Phase 4: Event Publishing Audit (Priority: Medium)
1. Review event publishing patterns
2. Check transactional consistency
3. Verify idempotency handling
4. Test event flow end-to-end

### Phase 5: Layer Separation Audit (Priority: Medium)
1. Check dependency directions
2. Verify no domain logic in outer layers
3. Ensure infrastructure isolation
4. Fix any architectural violations

### Phase 6: Testing & Documentation (Priority: Low)
1. Add missing tests
2. Update service documentation
3. Create architecture decision records (ADRs)
4. Document patterns and conventions

---

## Deliverables

1. **Common Module Documentation**:
   - Design rationale
   - Usage guidelines
   - Extension points

2. **Service Audit Reports**:
   - Per-service findings
   - Architectural violations
   - Recommended fixes

3. **Refactoring Tasks**:
   - Prioritized list of fixes
   - Implementation guidance
   - Testing requirements

4. **Architecture Decision Records**:
   - Document key design decisions
   - Rationale for common module design
   - Multi-tenancy strategy

5. **Updated Specifications**:
   - Common module spec
   - Service implementation guidelines
   - Testing guidelines

---

## Success Metrics

- [ ] All services follow Clean Architecture strictly
- [ ] Zero architectural violations (dependency rule)
- [ ] Consistent multi-tenancy implementation
- [ ] All domain events properly structured
- [ ] 80%+ unit test coverage for domain logic
- [ ] Zero cross-organization data leakage
- [ ] Consistent patterns across all services
- [ ] Complete and accurate documentation

---

## Risks & Mitigation

**Risk**: Large-scale refactoring breaks existing functionality  
**Mitigation**: Incremental changes with comprehensive testing after each change

**Risk**: Common module becomes too complex or coupled  
**Mitigation**: Keep common module minimal, only cross-cutting concerns

**Risk**: Services diverge in implementation patterns  
**Mitigation**: Document and enforce coding standards, use code reviews

**Risk**: Multi-tenancy bugs cause data leakage  
**Mitigation**: Comprehensive integration tests for tenant isolation

---

## Next Steps

1. Begin Phase 1: Common Module Refinement
2. Create detailed audit checklist for each service
3. Set up automated architecture tests (ArchUnit)
4. Schedule service-by-service review sessions
5. Document findings and create fix tasks

---

## Notes

- This plan emerged from Docker build issues that revealed inconsistencies
- Focus on alignment with PROJECT.md and specs as authoritative sources
- Prioritize correctness over speed
- Document all architectural decisions
- Keep changes incremental and testable
