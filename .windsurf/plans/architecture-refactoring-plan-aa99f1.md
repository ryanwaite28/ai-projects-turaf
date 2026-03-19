# Architecture Refactoring Plan: DDD & Multi-Tenant Consolidation

Comprehensive refactoring to consolidate DDD base classes, implement multi-tenant infrastructure across all services, and enforce Clean Architecture principles following SOLID, DDD, and Spring Boot best practices.

## Executive Summary

The architecture tasks (001-003) were implemented **after** the services were built, creating inconsistencies:

1. **Duplicate DDD Base Classes**: Organization-service has its own `domain.common` package duplicating the common module
2. **Incomplete Multi-Tenant Integration**: Only experiment-service and metrics-service implement `TenantAware`
3. **Duplicate TenantFilter**: Organization-service has its own TenantFilter instead of using the common module
4. **Missing TenantInterceptor Configuration**: Hibernate interceptor not configured in any service
5. **Inconsistent Architecture**: Services don't uniformly leverage the common module infrastructure

## Refactoring Objectives

### 1. Consolidate DDD Base Classes
- Remove duplicate `domain.common` package from organization-service
- Migrate all services to use `com.turaf.common.domain.*` base classes
- Update imports across all affected files
- Ensure consistent domain modeling patterns

### 2. Implement Multi-Tenant Infrastructure
- Add `TenantAware` interface to all tenant-scoped entities:
  - `User` (identity-service) - needs organizationId field
  - `Organization` (organization-service) - already has ID, needs interface
  - `OrganizationMember` (organization-service)
- Configure `TenantFilter` from common module in all services
- Configure `TenantInterceptor` for automatic organizationId management
- Remove duplicate TenantFilter implementations

### 3. Standardize Configuration
- Create consistent tenant configuration across services
- Add JPA/Hibernate interceptor configuration
- Ensure proper filter registration with correct precedence
- Add common module dependency where missing

## Detailed Refactoring Tasks

### Phase 1: Organization Service - DDD Consolidation

**Files to Modify**:
- `Organization.java` - Change import from `domain.common.AggregateRoot` to `com.turaf.common.domain.AggregateRoot`
- `OrganizationMember.java` - Update AggregateRoot import
- `OrganizationRepository.java` - Update Repository import
- `EventPublisher.java` - Update DomainEvent import
- All domain events - Update DomainEvent import
- `EventMapper.java` - Update DomainEvent import
- Test files - Update imports

**Files to Delete**:
- `domain/common/AggregateRoot.java`
- `domain/common/Entity.java`
- `domain/common/DomainEvent.java`
- `domain/common/Repository.java`

**Expected Impact**: ~12 files modified, 4 files deleted

### Phase 2: Organization Service - Multi-Tenant Integration

**Files to Modify**:
- `Organization.java`:
  - Implement `TenantAware` interface
  - Note: Organization IS the tenant, so organizationId = getId().getValue()
  - Add getter/setter for TenantAware compliance
- `OrganizationMember.java`:
  - Implement `TenantAware` interface
  - Add `organizationId` field
  - Add getter/setter methods

**Files to Delete**:
- `infrastructure/tenant/TenantFilter.java` (use common module version)
- `infrastructure/tenant/TenantContext.java` (use common module version)
- `infrastructure/tenant/TenantContextHolder.java` (use common module version)
- `infrastructure/tenant/TenantFilterTest.java` (redundant)

**Files to Create/Modify**:
- `infrastructure/config/TenantFilterConfig.java` - Update to use `com.turaf.common.tenant.TenantFilter`
- `infrastructure/config/JpaConfig.java` - Add TenantInterceptor configuration

**Expected Impact**: ~8 files modified, 4 files deleted, 1 file created

### Phase 3: Identity Service - Multi-Tenant Integration

**Files to Modify**:
- `User.java`:
  - Implement `TenantAware` interface
  - Add `organizationId` field (String)
  - Add getter/setter methods
  - Update constructor to accept organizationId
  - Update domain events to include organizationId

**Files to Create**:
- `infrastructure/config/TenantFilterConfig.java` - Register TenantFilter
- `infrastructure/config/JpaConfig.java` - Configure TenantInterceptor

**Dependencies to Add**:
- Ensure `turaf-common` dependency in `pom.xml`

**Expected Impact**: ~5 files modified, 2 files created

### Phase 4: Experiment Service - Verify Multi-Tenant Setup

**Status**: Already implements TenantAware ✓

**Files to Verify**:
- `Experiment.java` - Already implements TenantAware ✓
- `Hypothesis.java` - Already implements TenantAware ✓
- `Problem.java` - Already implements TenantAware ✓

**Files to Create**:
- `infrastructure/config/TenantFilterConfig.java` - Register TenantFilter
- `infrastructure/config/JpaConfig.java` - Configure TenantInterceptor

**Expected Impact**: 2 files created

### Phase 5: Metrics Service - Verify Multi-Tenant Setup

**Status**: Already implements TenantAware ✓

**Files to Verify**:
- `Metric.java` - Already implements TenantAware ✓

**Files to Create**:
- `infrastructure/config/TenantFilterConfig.java` - Register TenantFilter
- `infrastructure/config/JpaConfig.java` - Configure TenantInterceptor

**Expected Impact**: 2 files created

### Phase 6: Update Common Module POM Dependencies

**Files to Modify**:
- `services/common/pom.xml` - Ensure all dependencies are properly scoped

**Verify Services Have Common Dependency**:
- identity-service/pom.xml
- organization-service/pom.xml
- experiment-service/pom.xml
- metrics-service/pom.xml

### Phase 7: Integration Testing

**Test Areas**:
1. Verify all services compile successfully
2. Run unit tests for modified domain entities
3. Verify TenantFilter sets context correctly
4. Verify TenantInterceptor sets organizationId on save
5. Test cross-tenant isolation
6. Verify domain events include organizationId

**Test Files to Update**:
- Update tests that create entities to provide organizationId
- Add tests for TenantAware implementation
- Verify integration tests still pass

## Architecture Principles Validation

### SOLID Principles
- **Single Responsibility**: Each service manages its own domain, common module provides shared infrastructure
- **Open/Closed**: Base classes extensible without modification
- **Liskov Substitution**: All entities can use common base classes
- **Interface Segregation**: TenantAware is focused interface
- **Dependency Inversion**: Services depend on abstractions in common module

### DDD Principles
- **Ubiquitous Language**: Consistent terminology (AggregateRoot, Entity, ValueObject)
- **Bounded Contexts**: Each service maintains its domain boundary
- **Aggregates**: Proper aggregate root identification
- **Domain Events**: Consistent event modeling
- **Repositories**: Uniform repository pattern

### Clean Architecture
- **Dependency Rule**: Domain layer has no infrastructure dependencies
- **Layer Separation**: Clear boundaries between domain, application, infrastructure
- **Framework Independence**: Domain logic independent of Spring/Hibernate

### Multi-Tenancy
- **Data Isolation**: organizationId enforced at entity level
- **Automatic Context**: TenantFilter sets context per request
- **Automatic Assignment**: TenantInterceptor sets organizationId on save
- **Validation**: Prevents cross-tenant data modification

## Risk Assessment

### Low Risk
- Adding TenantAware to entities (additive change)
- Creating configuration classes (new files)
- Adding common module dependency

### Medium Risk
- Changing imports in organization-service (breaking change, but compile-time safe)
- Deleting duplicate classes (must ensure no references remain)
- Modifying User entity to add organizationId (requires data migration consideration)

### High Risk
- None identified (all changes are compile-time verified)

## Migration Strategy

### Compilation Safety
All changes are compile-time safe. If imports are incorrect or interfaces not implemented, compilation will fail, preventing runtime issues.

### Data Migration Considerations
- **User.organizationId**: New field requires database migration
- **OrganizationMember.organizationId**: New field requires database migration
- Existing data will need organizationId populated (separate migration task)

### Rollback Plan
- Changes are isolated per service
- Git commits per phase allow selective rollback
- No database schema changes in this refactoring (field additions only)

## Success Criteria

1. ✅ All services use `com.turaf.common.domain.*` base classes
2. ✅ No duplicate DDD infrastructure code
3. ✅ All tenant-scoped entities implement `TenantAware`
4. ✅ TenantFilter configured in all services
5. ✅ TenantInterceptor configured in all services
6. ✅ All services compile successfully
7. ✅ All existing tests pass
8. ✅ New tenant isolation tests pass
9. ✅ Code follows SOLID, DDD, Clean Architecture principles
10. ✅ Consistent multi-tenant infrastructure across all services

## Estimated Effort

- **Phase 1**: 1-2 hours (Organization service DDD consolidation)
- **Phase 2**: 1-2 hours (Organization service multi-tenant)
- **Phase 3**: 1-2 hours (Identity service multi-tenant)
- **Phase 4**: 30 minutes (Experiment service verification)
- **Phase 5**: 30 minutes (Metrics service verification)
- **Phase 6**: 30 minutes (Dependency verification)
- **Phase 7**: 1-2 hours (Integration testing)

**Total**: 6-9 hours

## Implementation Order

Execute phases sequentially to minimize risk and ensure each service is fully refactored before moving to the next:

1. Phase 1 → Phase 2 (Organization service complete)
2. Phase 3 (Identity service complete)
3. Phase 4 (Experiment service complete)
4. Phase 5 (Metrics service complete)
5. Phase 6 (Dependency verification)
6. Phase 7 (Integration testing)

## Post-Refactoring Benefits

1. **Consistency**: All services use same DDD patterns
2. **Maintainability**: Single source of truth for base classes
3. **Security**: Automatic tenant isolation prevents data leaks
4. **Developer Experience**: Clear patterns reduce cognitive load
5. **Testability**: Consistent infrastructure simplifies testing
6. **Scalability**: Multi-tenant architecture ready for growth
