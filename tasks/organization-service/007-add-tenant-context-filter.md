# Task: Add Tenant Context Filter

**Service**: Organization Service  
**Phase**: 3  
**Estimated Time**: 2 hours  

## Objective

Implement tenant context filtering to ensure all database queries are automatically scoped to the current organization.

## Prerequisites

- [x] Task 003: Multi-tenant context setup (architecture)
- [x] Task 002: Repositories implemented

## Scope

**Files to Create**:
- `services/organization-service/src/main/java/com/turaf/organization/infrastructure/config/TenantFilterConfig.java`
- `services/organization-service/src/main/java/com/turaf/organization/infrastructure/persistence/TenantAwareJpaRepository.java`

## Implementation Details

### Tenant Filter Configuration

```java
@Configuration
public class TenantFilterConfig {
    
    @Bean
    public FilterRegistrationBean<TenantFilter> tenantFilter() {
        FilterRegistrationBean<TenantFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new TenantFilter());
        registration.addUrlPatterns("/api/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
```

### Tenant-Aware Repository Base

```java
@NoRepositoryBean
public interface TenantAwareJpaRepository<T, ID> extends JpaRepository<T, ID> {
    
    @Override
    @Query("SELECT e FROM #{#entityName} e WHERE e.organizationId = :#{T(com.turaf.common.tenant.TenantContextHolder).getOrganizationId()}")
    List<T> findAll();
    
    @Query("SELECT e FROM #{#entityName} e WHERE e.id = :id AND e.organizationId = :#{T(com.turaf.common.tenant.TenantContextHolder).getOrganizationId()}")
    Optional<T> findByIdAndOrganization(@Param("id") ID id);
}
```

### Update Organization JPA Repository

```java
public interface OrganizationJpaRepository extends JpaRepository<OrganizationJpaEntity, String> {
    
    @Query("SELECT o FROM OrganizationJpaEntity o WHERE o.id = :id AND o.id = :#{T(com.turaf.common.tenant.TenantContextHolder).getOrganizationId()}")
    Optional<OrganizationJpaEntity> findById(@Param("id") String id);
    
    Optional<OrganizationJpaEntity> findBySlug(String slug);
    
    boolean existsBySlug(String slug);
}
```

### Update Organization Member JPA Repository

```java
public interface OrganizationMemberJpaRepository extends JpaRepository<OrganizationMemberJpaEntity, String> {
    
    @Query("SELECT m FROM OrganizationMemberJpaEntity m WHERE m.organizationId = :orgId AND m.organizationId = :#{T(com.turaf.common.tenant.TenantContextHolder).getOrganizationId()}")
    List<OrganizationMemberJpaEntity> findByOrganizationId(@Param("orgId") String organizationId);
    
    @Query("SELECT m FROM OrganizationMemberJpaEntity m WHERE m.organizationId = :orgId AND m.userId = :userId AND m.organizationId = :#{T(com.turaf.common.tenant.TenantContextHolder).getOrganizationId()}")
    Optional<OrganizationMemberJpaEntity> findByOrganizationIdAndUserId(
        @Param("orgId") String organizationId,
        @Param("userId") String userId
    );
}
```

## Acceptance Criteria

- [ ] Tenant filter configured and active
- [ ] All queries automatically scoped to current organization
- [ ] Cross-tenant data access prevented
- [ ] Tenant context extracted from JWT
- [ ] Integration tests verify tenant isolation
- [ ] Unauthorized access attempts blocked

## Testing Requirements

**Integration Tests**:
- Test queries scoped to current organization
- Test cross-tenant access blocked
- Test tenant context from JWT
- Test missing tenant context handled

**Test Files to Create**:
- `TenantFilterIntegrationTest.java`

## References

- Specification: `specs/architecture.md` (Multi-Tenant Architecture section)
- Related Tasks: 008-add-unit-tests
