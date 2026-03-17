# Task: Add Unit Tests

**Service**: Organization Service  
**Phase**: 3  
**Estimated Time**: 3 hours  

## Objective

Create comprehensive unit tests for domain model, application services, and infrastructure components.

## Prerequisites

- [x] All organization-service implementation tasks completed

## Scope

**Test Files to Create**:
- `OrganizationTest.java`
- `OrganizationMemberTest.java`
- `OrganizationServiceTest.java`
- `MembershipServiceTest.java`
- `EventBridgePublisherTest.java`
- `EventMapperTest.java`

## Implementation Details

### Organization Service Test

```java
@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {
    @Mock
    private OrganizationRepository organizationRepository;
    
    @Mock
    private EventPublisher eventPublisher;
    
    @InjectMocks
    private OrganizationService organizationService;
    
    @Test
    void createOrganization_WithValidData_ShouldCreateOrganization() {
        CreateOrganizationRequest request = new CreateOrganizationRequest("Test Org", "test-org");
        UserId createdBy = new UserId("user-123");
        
        when(organizationRepository.existsBySlug("test-org")).thenReturn(false);
        when(organizationRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        
        OrganizationDto result = organizationService.createOrganization(request, createdBy);
        
        assertNotNull(result);
        assertEquals("Test Org", result.getName());
        assertEquals("test-org", result.getSlug());
        verify(organizationRepository).save(any(Organization.class));
        verify(eventPublisher).publish(any(OrganizationCreated.class));
    }
    
    @Test
    void createOrganization_WithDuplicateSlug_ShouldThrowException() {
        CreateOrganizationRequest request = new CreateOrganizationRequest("Test Org", "test-org");
        UserId createdBy = new UserId("user-123");
        
        when(organizationRepository.existsBySlug("test-org")).thenReturn(true);
        
        assertThrows(OrganizationAlreadyExistsException.class, () ->
            organizationService.createOrganization(request, createdBy));
    }
}
```

### Membership Service Test

```java
@ExtendWith(MockitoExtension.class)
class MembershipServiceTest {
    @Mock
    private OrganizationRepository organizationRepository;
    
    @Mock
    private OrganizationMemberRepository memberRepository;
    
    @Mock
    private EventPublisher eventPublisher;
    
    @InjectMocks
    private MembershipService membershipService;
    
    @Test
    void addMember_WithValidData_ShouldAddMember() {
        OrganizationId orgId = new OrganizationId("org-123");
        AddMemberRequest request = new AddMemberRequest("user-456", "MEMBER");
        UserId addedBy = new UserId("admin-789");
        
        Organization org = createTestOrganization(orgId);
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
        when(memberRepository.findByOrganizationIdAndUserId(any(), any())).thenReturn(Optional.empty());
        
        MemberDto result = membershipService.addMember(orgId, request, addedBy);
        
        assertNotNull(result);
        assertEquals("user-456", result.getUserId());
        assertEquals("MEMBER", result.getRole());
        verify(memberRepository).save(any(OrganizationMember.class));
        verify(eventPublisher).publish(any(MemberAdded.class));
    }
    
    @Test
    void addMember_WhenAlreadyMember_ShouldThrowException() {
        OrganizationId orgId = new OrganizationId("org-123");
        AddMemberRequest request = new AddMemberRequest("user-456", "MEMBER");
        UserId addedBy = new UserId("admin-789");
        
        Organization org = createTestOrganization(orgId);
        OrganizationMember existingMember = createTestMember();
        
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
        when(memberRepository.findByOrganizationIdAndUserId(any(), any()))
            .thenReturn(Optional.of(existingMember));
        
        assertThrows(MemberAlreadyExistsException.class, () ->
            membershipService.addMember(orgId, request, addedBy));
    }
}
```

## Acceptance Criteria

- [ ] All domain model tests pass
- [ ] All application service tests pass
- [ ] All infrastructure tests pass
- [ ] Code coverage > 80%
- [ ] All edge cases covered
- [ ] Mock dependencies properly
- [ ] Tests are isolated and independent

## Testing Requirements

**Unit Test Coverage**:
- Domain entities and value objects
- Application services
- Event publisher
- Event mapper
- Repository implementations
- Exception scenarios

## References

- Specification: `specs/organization-service.md`
- Related Tasks: 009-add-integration-tests
