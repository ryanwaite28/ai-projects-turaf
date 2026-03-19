# Task: Implement Organization Service

**Service**: Organization Service  
**Phase**: 3  
**Estimated Time**: 3 hours  

## Objective

Implement the application layer service for organization lifecycle management including create, update, and delete operations.

## Prerequisites

- [x] Task 001: Domain model created
- [x] Task 002: Repositories implemented

## Scope

**Files to Create**:
- `services/organization-service/src/main/java/com/turaf/organization/application/OrganizationService.java`
- `services/organization-service/src/main/java/com/turaf/organization/application/dto/CreateOrganizationRequest.java`
- `services/organization-service/src/main/java/com/turaf/organization/application/dto/UpdateOrganizationRequest.java`
- `services/organization-service/src/main/java/com/turaf/organization/application/dto/OrganizationDto.java`
- `services/organization-service/src/main/java/com/turaf/organization/application/exception/OrganizationAlreadyExistsException.java`
- `services/organization-service/src/main/java/com/turaf/organization/application/exception/OrganizationNotFoundException.java`

## Implementation Details

### Organization Service

```java
@Service
@Transactional
public class OrganizationService {
    private final OrganizationRepository organizationRepository;
    private final EventPublisher eventPublisher;
    
    public OrganizationDto createOrganization(CreateOrganizationRequest request, UserId createdBy) {
        if (organizationRepository.existsBySlug(request.getSlug())) {
            throw new OrganizationAlreadyExistsException("Organization with slug already exists");
        }
        
        OrganizationId id = OrganizationId.generate();
        Organization organization = new Organization(
            id,
            request.getName(),
            request.getSlug(),
            createdBy
        );
        
        Organization saved = organizationRepository.save(organization);
        
        // Publish domain events
        saved.getDomainEvents().forEach(eventPublisher::publish);
        saved.clearDomainEvents();
        
        return OrganizationDto.fromDomain(saved);
    }
    
    public OrganizationDto getOrganization(OrganizationId id) {
        Organization organization = organizationRepository.findById(id)
            .orElseThrow(() -> new OrganizationNotFoundException("Organization not found"));
        return OrganizationDto.fromDomain(organization);
    }
    
    public OrganizationDto getOrganizationBySlug(String slug) {
        Organization organization = organizationRepository.findBySlug(slug)
            .orElseThrow(() -> new OrganizationNotFoundException("Organization not found"));
        return OrganizationDto.fromDomain(organization);
    }
    
    public OrganizationDto updateOrganization(OrganizationId id, UpdateOrganizationRequest request) {
        Organization organization = organizationRepository.findById(id)
            .orElseThrow(() -> new OrganizationNotFoundException("Organization not found"));
        
        organization.updateName(request.getName());
        Organization updated = organizationRepository.save(organization);
        
        return OrganizationDto.fromDomain(updated);
    }
    
    public void deleteOrganization(OrganizationId id) {
        Organization organization = organizationRepository.findById(id)
            .orElseThrow(() -> new OrganizationNotFoundException("Organization not found"));
        
        organizationRepository.delete(organization);
    }
}
```

### DTOs

```java
public class CreateOrganizationRequest {
    @NotBlank
    @Size(min = 1, max = 100)
    private String name;
    
    @NotBlank
    @Pattern(regexp = "^[a-z0-9-]{3,50}$")
    private String slug;
    
    // Getters, setters
}

public class UpdateOrganizationRequest {
    @NotBlank
    @Size(min = 1, max = 100)
    private String name;
    
    // Getters, setters
}

public class OrganizationDto {
    private String id;
    private String name;
    private String slug;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;
    
    public static OrganizationDto fromDomain(Organization org) {
        OrganizationDto dto = new OrganizationDto();
        dto.setId(org.getId().getValue());
        dto.setName(org.getName());
        dto.setSlug(org.getSlug());
        dto.setCreatedBy(org.getCreatedBy().getValue());
        dto.setCreatedAt(org.getCreatedAt());
        dto.setUpdatedAt(org.getUpdatedAt());
        return dto;
    }
}
```

## Acceptance Criteria

- [x] Create organization validates slug uniqueness
- [x] Get organization by ID works
- [x] Get organization by slug works
- [x] Update organization works
- [x] Delete organization works
- [x] Domain events published on creation
- [x] All validation rules enforced
- [x] Unit tests pass

## Testing Requirements

**Unit Tests**:
- Test successful organization creation
- Test duplicate slug rejection
- Test organization retrieval
- Test organization update
- Test organization deletion
- Test event publishing

**Test Files to Create**:
- `OrganizationServiceTest.java`

## References

- Specification: `specs/organization-service.md` (Application Services section)
- Related Tasks: 004-implement-membership-service
