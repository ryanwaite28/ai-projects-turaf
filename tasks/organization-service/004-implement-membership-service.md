# Task: Implement Membership Service

**Service**: Organization Service  
**Phase**: 3  
**Estimated Time**: 3 hours  

## Objective

Implement the application layer service for organization membership management including add, remove, and list members.

## Prerequisites

- [x] Task 001: Domain model created
- [x] Task 002: Repositories implemented
- [x] Task 003: Organization service implemented

## Scope

**Files to Create**:
- `services/organization-service/src/main/java/com/turaf/organization/application/MembershipService.java`
- `services/organization-service/src/main/java/com/turaf/organization/application/dto/AddMemberRequest.java`
- `services/organization-service/src/main/java/com/turaf/organization/application/dto/MemberDto.java`
- `services/organization-service/src/main/java/com/turaf/organization/application/exception/MemberAlreadyExistsException.java`
- `services/organization-service/src/main/java/com/turaf/organization/application/exception/MemberNotFoundException.java`

## Implementation Details

### Membership Service

```java
@Service
@Transactional
public class MembershipService {
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository memberRepository;
    private final EventPublisher eventPublisher;
    
    public MemberDto addMember(OrganizationId organizationId, AddMemberRequest request, UserId addedBy) {
        // Verify organization exists
        Organization organization = organizationRepository.findById(organizationId)
            .orElseThrow(() -> new OrganizationNotFoundException("Organization not found"));
        
        // Check if member already exists
        if (memberRepository.findByOrganizationIdAndUserId(organizationId, new UserId(request.getUserId())).isPresent()) {
            throw new MemberAlreadyExistsException("User is already a member");
        }
        
        OrganizationMember member = new OrganizationMember(
            UUID.randomUUID().toString(),
            organizationId,
            new UserId(request.getUserId()),
            MemberRole.valueOf(request.getRole()),
            addedBy
        );
        
        memberRepository.save(member);
        
        // Publish event
        MemberAdded event = new MemberAdded(
            UUID.randomUUID().toString(),
            organizationId.getValue(),
            request.getUserId(),
            request.getRole(),
            addedBy.getValue(),
            Instant.now()
        );
        eventPublisher.publish(event);
        
        return MemberDto.fromDomain(member);
    }
    
    public List<MemberDto> getMembers(OrganizationId organizationId) {
        return memberRepository.findByOrganizationId(organizationId)
            .stream()
            .map(MemberDto::fromDomain)
            .collect(Collectors.toList());
    }
    
    public void removeMember(OrganizationId organizationId, UserId userId) {
        OrganizationMember member = memberRepository.findByOrganizationIdAndUserId(organizationId, userId)
            .orElseThrow(() -> new MemberNotFoundException("Member not found"));
        
        memberRepository.delete(member);
        
        // Publish event
        MemberRemoved event = new MemberRemoved(
            UUID.randomUUID().toString(),
            organizationId.getValue(),
            userId.getValue(),
            TenantContextHolder.getUserId(),
            Instant.now()
        );
        eventPublisher.publish(event);
    }
    
    public boolean isMember(OrganizationId organizationId, UserId userId) {
        return memberRepository.findByOrganizationIdAndUserId(organizationId, userId).isPresent();
    }
    
    public boolean isAdmin(OrganizationId organizationId, UserId userId) {
        return memberRepository.findByOrganizationIdAndUserId(organizationId, userId)
            .map(OrganizationMember::isAdmin)
            .orElse(false);
    }
}
```

### DTOs

```java
public class AddMemberRequest {
    @NotBlank
    private String userId;
    
    @NotBlank
    @Pattern(regexp = "ADMIN|MEMBER")
    private String role;
    
    // Getters, setters
}

public class MemberDto {
    private String id;
    private String organizationId;
    private String userId;
    private String role;
    private String addedBy;
    private Instant addedAt;
    
    public static MemberDto fromDomain(OrganizationMember member) {
        MemberDto dto = new MemberDto();
        dto.setId(member.getId());
        dto.setOrganizationId(member.getOrganizationId().getValue());
        dto.setUserId(member.getUserId().getValue());
        dto.setRole(member.getRole().name());
        dto.setAddedBy(member.getAddedBy().getValue());
        dto.setAddedAt(member.getAddedAt());
        return dto;
    }
}
```

## Acceptance Criteria

- [ ] Add member validates organization exists
- [ ] Add member prevents duplicates
- [ ] Get members returns all organization members
- [ ] Remove member works correctly
- [ ] isMember check works
- [ ] isAdmin check works
- [ ] Domain events published
- [ ] Unit tests pass

## Testing Requirements

**Unit Tests**:
- Test add member successfully
- Test add duplicate member fails
- Test get members
- Test remove member
- Test isMember check
- Test isAdmin check
- Test event publishing

**Test Files to Create**:
- `MembershipServiceTest.java`

## References

- Specification: `specs/organization-service.md` (Membership Management section)
- Related Tasks: 005-implement-rest-controllers
