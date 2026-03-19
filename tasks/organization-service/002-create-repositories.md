# Task: Create Organization Service Repositories

**Service**: Organization Service  
**Phase**: 3  
**Estimated Time**: 2-3 hours  

## Objective

Implement repository interfaces for Organization and OrganizationMember entities using Spring Data JPA, including database schema and migrations.

## Prerequisites

- [x] Task 001: Domain model created

## Scope

**Files to Create**:
- `services/organization-service/src/main/java/com/turaf/organization/infrastructure/persistence/OrganizationJpaEntity.java`
- `services/organization-service/src/main/java/com/turaf/organization/infrastructure/persistence/OrganizationMemberJpaEntity.java`
- `services/organization-service/src/main/java/com/turaf/organization/infrastructure/persistence/OrganizationJpaRepository.java`
- `services/organization-service/src/main/java/com/turaf/organization/infrastructure/persistence/OrganizationMemberJpaRepository.java`
- `services/organization-service/src/main/java/com/turaf/organization/infrastructure/persistence/OrganizationRepositoryImpl.java`
- `services/organization-service/src/main/java/com/turaf/organization/infrastructure/persistence/OrganizationMemberRepositoryImpl.java`
- `services/organization-service/src/main/resources/db/migration/V001__create_organizations_table.sql`
- `services/organization-service/src/main/resources/db/migration/V002__create_organization_members_table.sql`

## Implementation Details

### Database Migrations

```sql
-- V001__create_organizations_table.sql
CREATE TABLE organizations (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(50) NOT NULL UNIQUE,
    created_by VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_organizations_slug ON organizations(slug);

-- V002__create_organization_members_table.sql
CREATE TABLE organization_members (
    id VARCHAR(36) PRIMARY KEY,
    organization_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    role VARCHAR(20) NOT NULL,
    added_by VARCHAR(36) NOT NULL,
    added_at TIMESTAMP NOT NULL,
    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    UNIQUE (organization_id, user_id)
);

CREATE INDEX idx_org_members_org_id ON organization_members(organization_id);
CREATE INDEX idx_org_members_user_id ON organization_members(user_id);
```

### JPA Entities

```java
@Entity
@Table(name = "organizations")
public class OrganizationJpaEntity {
    @Id
    private String id;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(nullable = false, length = 50, unique = true)
    private String slug;
    
    @Column(name = "created_by", nullable = false)
    private String createdBy;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    public Organization toDomain() {
        return new Organization(
            new OrganizationId(id),
            name,
            slug,
            new UserId(createdBy)
        );
    }
    
    public static OrganizationJpaEntity fromDomain(Organization org) {
        OrganizationJpaEntity entity = new OrganizationJpaEntity();
        entity.setId(org.getId().getValue());
        entity.setName(org.getName());
        entity.setSlug(org.getSlug());
        entity.setCreatedBy(org.getCreatedBy().getValue());
        entity.setCreatedAt(org.getCreatedAt());
        entity.setUpdatedAt(org.getUpdatedAt());
        return entity;
    }
}
```

### Repository Implementation

```java
@Repository
public class OrganizationRepositoryImpl implements OrganizationRepository {
    private final OrganizationJpaRepository jpaRepository;
    
    @Override
    public Optional<Organization> findById(OrganizationId id) {
        return jpaRepository.findById(id.getValue())
            .map(OrganizationJpaEntity::toDomain);
    }
    
    @Override
    public Optional<Organization> findBySlug(String slug) {
        return jpaRepository.findBySlug(slug)
            .map(OrganizationJpaEntity::toDomain);
    }
    
    @Override
    public Organization save(Organization organization) {
        OrganizationJpaEntity entity = OrganizationJpaEntity.fromDomain(organization);
        OrganizationJpaEntity saved = jpaRepository.save(entity);
        return saved.toDomain();
    }
    
    @Override
    public boolean existsBySlug(String slug) {
        return jpaRepository.existsBySlug(slug);
    }
}
```

## Acceptance Criteria

- [x] JPA entities created with proper mappings
- [x] Database migrations create correct schema
- [x] Indexes created for performance
- [x] Unique constraint on organization slug
- [x] Foreign key constraints enforced
- [x] Repository implementations work correctly
- [x] Integration tests pass

## Testing Requirements

**Integration Tests**:
- Test save and retrieve organization
- Test find by slug
- Test slug uniqueness constraint
- Test organization member CRUD operations
- Test cascade delete of members

**Test Files to Create**:
- `OrganizationRepositoryImplTest.java`
- `OrganizationMemberRepositoryImplTest.java`

## References

- Specification: `specs/organization-service.md` (Database Schema section)
- Related Tasks: 001-create-domain-model, 003-implement-organization-service
