# Task: Add Integration Tests

**Service**: Organization Service  
**Phase**: 3  
**Estimated Time**: 3 hours  

## Objective

Create integration tests that verify the complete organization and membership management flow from API endpoints through to database.

## Prerequisites

- [x] All organization-service implementation tasks completed
- [x] Task 008: Unit tests added

## Scope

**Test Files to Create**:
- `OrganizationControllerIntegrationTest.java`
- `MembershipControllerIntegrationTest.java`
- `OrganizationFlowIntegrationTest.java`

## Implementation Details

### Organization Controller Integration Test

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase
@Transactional
class OrganizationControllerIntegrationTest {
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private OrganizationRepository organizationRepository;
    
    @Test
    void createOrganization_WithValidData_ShouldCreateOrganization() {
        CreateOrganizationRequest request = new CreateOrganizationRequest(
            "Test Organization",
            "test-org"
        );
        
        HttpHeaders headers = createAuthHeaders();
        HttpEntity<CreateOrganizationRequest> entity = new HttpEntity<>(request, headers);
        
        ResponseEntity<OrganizationDto> response = restTemplate.exchange(
            "/api/v1/organizations",
            HttpMethod.POST,
            entity,
            OrganizationDto.class
        );
        
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Test Organization", response.getBody().getName());
        assertEquals("test-org", response.getBody().getSlug());
        
        // Verify saved in database
        Optional<Organization> saved = organizationRepository.findBySlug("test-org");
        assertTrue(saved.isPresent());
    }
    
    @Test
    void createOrganization_WithDuplicateSlug_ShouldReturn409() {
        createTestOrganization("existing-org");
        
        CreateOrganizationRequest request = new CreateOrganizationRequest(
            "Another Organization",
            "existing-org"
        );
        
        HttpHeaders headers = createAuthHeaders();
        HttpEntity<CreateOrganizationRequest> entity = new HttpEntity<>(request, headers);
        
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
            "/api/v1/organizations",
            HttpMethod.POST,
            entity,
            ErrorResponse.class
        );
        
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("ORGANIZATION_ALREADY_EXISTS", response.getBody().getCode());
    }
}
```

### Organization Flow Integration Test

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase
@Transactional
class OrganizationFlowIntegrationTest {
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void completeOrganizationFlow_ShouldWork() {
        HttpHeaders headers = createAuthHeaders();
        
        // 1. Create organization
        CreateOrganizationRequest createRequest = new CreateOrganizationRequest(
            "Flow Test Org",
            "flow-test-org"
        );
        
        ResponseEntity<OrganizationDto> createResponse = restTemplate.exchange(
            "/api/v1/organizations",
            HttpMethod.POST,
            new HttpEntity<>(createRequest, headers),
            OrganizationDto.class
        );
        
        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        String orgId = createResponse.getBody().getId();
        
        // 2. Get organization
        ResponseEntity<OrganizationDto> getResponse = restTemplate.exchange(
            "/api/v1/organizations/" + orgId,
            HttpMethod.GET,
            new HttpEntity<>(headers),
            OrganizationDto.class
        );
        
        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertEquals("Flow Test Org", getResponse.getBody().getName());
        
        // 3. Add member
        AddMemberRequest addMemberRequest = new AddMemberRequest("user-123", "MEMBER");
        
        ResponseEntity<MemberDto> addMemberResponse = restTemplate.exchange(
            "/api/v1/organizations/" + orgId + "/members",
            HttpMethod.POST,
            new HttpEntity<>(addMemberRequest, headers),
            MemberDto.class
        );
        
        assertEquals(HttpStatus.CREATED, addMemberResponse.getStatusCode());
        
        // 4. Get members
        ResponseEntity<List> getMembersResponse = restTemplate.exchange(
            "/api/v1/organizations/" + orgId + "/members",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            List.class
        );
        
        assertEquals(HttpStatus.OK, getMembersResponse.getStatusCode());
        assertTrue(getMembersResponse.getBody().size() > 0);
        
        // 5. Update organization
        UpdateOrganizationRequest updateRequest = new UpdateOrganizationRequest("Updated Name");
        
        ResponseEntity<OrganizationDto> updateResponse = restTemplate.exchange(
            "/api/v1/organizations/" + orgId,
            HttpMethod.PUT,
            new HttpEntity<>(updateRequest, headers),
            OrganizationDto.class
        );
        
        assertEquals(HttpStatus.OK, updateResponse.getStatusCode());
        assertEquals("Updated Name", updateResponse.getBody().getName());
        
        // 6. Remove member
        ResponseEntity<Void> removeMemberResponse = restTemplate.exchange(
            "/api/v1/organizations/" + orgId + "/members/user-123",
            HttpMethod.DELETE,
            new HttpEntity<>(headers),
            Void.class
        );
        
        assertEquals(HttpStatus.NO_CONTENT, removeMemberResponse.getStatusCode());
        
        // 7. Delete organization
        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
            "/api/v1/organizations/" + orgId,
            HttpMethod.DELETE,
            new HttpEntity<>(headers),
            Void.class
        );
        
        assertEquals(HttpStatus.NO_CONTENT, deleteResponse.getStatusCode());
    }
}
```

## Acceptance Criteria

- [x] All API endpoints tested end-to-end
- [x] Database interactions verified
- [x] Complete organization flow tested
- [x] Membership flow tested
- [x] Authorization checks verified
- [x] Error scenarios tested
- [x] All integration tests pass
- [x] Tests use test database
- [x] Tests are transactional and isolated

## Testing Requirements

**Integration Test Coverage**:
- Organization CRUD operations
- Membership management
- Authorization enforcement
- Tenant isolation
- Event publishing
- Error scenarios

## References

- Specification: `specs/organization-service.md`
- Related Tasks: All organization-service tasks
