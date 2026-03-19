# Task: Create Organization Service Domain Model

**Service**: Organization Service  
**Phase**: 3  
**Estimated Time**: 3-4 hours  

## Objective

Create the domain model for the Organization Service including Organization and OrganizationMember entities, value objects, and domain logic for multi-tenant organization management.

## Prerequisites

- [x] Task 001: Clean Architecture layers established
- [x] Task 002: DDD patterns implemented
- [x] Task 003: Multi-tenant context setup

## Scope

**Files to Create**:
- `services/organization-service/src/main/java/com/turaf/organization/domain/Organization.java`
- `services/organization-service/src/main/java/com/turaf/organization/domain/OrganizationId.java`
- `services/organization-service/src/main/java/com/turaf/organization/domain/OrganizationMember.java`
- `services/organization-service/src/main/java/com/turaf/organization/domain/MemberRole.java`
- `services/organization-service/src/main/java/com/turaf/organization/domain/OrganizationSettings.java`
- `services/organization-service/src/main/java/com/turaf/organization/domain/OrganizationRepository.java`
- `services/organization-service/src/main/java/com/turaf/organization/domain/OrganizationMemberRepository.java`
- `services/organization-service/src/main/java/com/turaf/organization/domain/event/OrganizationCreated.java`
- `services/organization-service/src/main/java/com/turaf/organization/domain/event/MemberAdded.java`
- `services/organization-service/src/main/java/com/turaf/organization/domain/event/MemberRemoved.java`

## Implementation Details

### Organization Entity (Aggregate Root)

```java
public class Organization extends AggregateRoot<OrganizationId> {
    private String name;
    private String slug;
    private Instant createdAt;
    private Instant updatedAt;
    private UserId createdBy;
    
    public Organization(OrganizationId id, String name, String slug, UserId createdBy) {
        super(id);
        this.name = validateName(name);
        this.slug = validateSlug(slug);
        this.createdBy = Objects.requireNonNull(createdBy);
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        
        registerEvent(new OrganizationCreated(
            UUID.randomUUID().toString(),
            id.getValue(),
            name,
            slug,
            createdBy.getValue(),
            createdAt
        ));
    }
    
    public void updateName(String newName) {
        this.name = validateName(newName);
        this.updatedAt = Instant.now();
    }
    
    private String validateName(String name) {
        if (name == null || name.isBlank() || name.length() > 100) {
            throw new IllegalArgumentException("Organization name must be 1-100 characters");
        }
        return name;
    }
    
    private String validateSlug(String slug) {
        if (slug == null || !slug.matches("^[a-z0-9-]{3,50}$")) {
            throw new IllegalArgumentException("Slug must be 3-50 lowercase alphanumeric characters with hyphens");
        }
        return slug;
    }
    
    // Getters
}
```

### OrganizationMember Entity

```java
public class OrganizationMember extends Entity<String> {
    private OrganizationId organizationId;
    private UserId userId;
    private MemberRole role;
    private Instant addedAt;
    private UserId addedBy;
    
    public OrganizationMember(String id, OrganizationId organizationId, UserId userId, 
                             MemberRole role, UserId addedBy) {
        super(id);
        this.organizationId = Objects.requireNonNull(organizationId);
        this.userId = Objects.requireNonNull(userId);
        this.role = Objects.requireNonNull(role);
        this.addedBy = Objects.requireNonNull(addedBy);
        this.addedAt = Instant.now();
    }
    
    public void changeRole(MemberRole newRole) {
        this.role = Objects.requireNonNull(newRole);
    }
    
    public boolean isAdmin() {
        return role == MemberRole.ADMIN;
    }
    
    // Getters
}
```

### MemberRole Enum

```java
public enum MemberRole {
    ADMIN,
    MEMBER
}
```

### Domain Events

```java
public class OrganizationCreated implements DomainEvent {
    private final String eventId;
    private final String organizationId;
    private final String name;
    private final String slug;
    private final String createdBy;
    private final Instant timestamp;
    
    @Override
    public String getEventType() {
        return "OrganizationCreated";
    }
    
    @Override
    public String getOrganizationId() {
        return organizationId;
    }
    
    // Constructor, getters
}

public class MemberAdded implements DomainEvent {
    private final String eventId;
    private final String organizationId;
    private final String userId;
    private final String role;
    private final String addedBy;
    private final Instant timestamp;
    
    @Override
    public String getEventType() {
        return "MemberAdded";
    }
    
    // Constructor, getters
}
```

### Repository Interfaces

```java
public interface OrganizationRepository extends Repository<Organization, OrganizationId> {
    Optional<Organization> findBySlug(String slug);
    boolean existsBySlug(String slug);
}

public interface OrganizationMemberRepository {
    List<OrganizationMember> findByOrganizationId(OrganizationId organizationId);
    Optional<OrganizationMember> findByOrganizationIdAndUserId(OrganizationId organizationId, UserId userId);
    void save(OrganizationMember member);
    void delete(OrganizationMember member);
}
```

## Acceptance Criteria

- [x] Organization entity extends AggregateRoot
- [x] Organization validates name and slug
- [x] OrganizationMember entity created
- [x] MemberRole enum defined
- [x] Domain events created for organization lifecycle
- [x] Repository interfaces defined
- [x] All domain invariants enforced
- [x] Unit tests pass

## Testing Requirements

**Unit Tests**:
- Test Organization creation and validation
- Test name and slug validation rules
- Test OrganizationMember creation
- Test role assignment and checking
- Test domain event registration

**Test Files to Create**:
- `OrganizationTest.java`
- `OrganizationMemberTest.java`
- `OrganizationCreatedEventTest.java`

## References

- Specification: `specs/organization-service.md` (Domain Model section)
- Specification: `specs/domain-model.md` (Organization entity)
- PROJECT.md: Section 40 (Organization Service)
- Related Tasks: 002-create-repositories
