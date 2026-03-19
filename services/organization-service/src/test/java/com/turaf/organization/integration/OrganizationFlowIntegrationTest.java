package com.turaf.organization.integration;

import com.turaf.organization.application.dto.*;
import com.turaf.organization.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for complete organization management flow.
 * Tests the entire lifecycle from creation through deletion.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
class OrganizationFlowIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private OrganizationRepository organizationRepository;
    
    @Autowired
    private OrganizationMemberRepository memberRepository;
    
    private HttpHeaders headers;
    
    @BeforeEach
    void setUp() {
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Id", "flow-test-user");
        headers.set("X-Organization-Id", "flow-test-org");
    }
    
    @Test
    void shouldCompleteFullOrganizationLifecycle() {
        // 1. Create organization
        CreateOrganizationRequest createRequest = new CreateOrganizationRequest(
            "Flow Test Organization",
            "flow-test-org"
        );
        
        ResponseEntity<OrganizationDto> createResponse = restTemplate.exchange(
            "/api/v1/organizations",
            HttpMethod.POST,
            new HttpEntity<>(createRequest, headers),
            OrganizationDto.class
        );
        
        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        assertNotNull(createResponse.getBody());
        String orgId = createResponse.getBody().getId();
        assertEquals("Flow Test Organization", createResponse.getBody().getName());
        assertEquals("flow-test-org", createResponse.getBody().getSlug());
        
        // Verify organization exists in database
        Optional<Organization> createdOrg = organizationRepository.findById(OrganizationId.of(orgId));
        assertTrue(createdOrg.isPresent());
        
        // 2. Get organization by ID
        ResponseEntity<OrganizationDto> getByIdResponse = restTemplate.exchange(
            "/api/v1/organizations/" + orgId,
            HttpMethod.GET,
            new HttpEntity<>(headers),
            OrganizationDto.class
        );
        
        assertEquals(HttpStatus.OK, getByIdResponse.getStatusCode());
        assertEquals("Flow Test Organization", getByIdResponse.getBody().getName());
        
        // 3. Get organization by slug
        ResponseEntity<OrganizationDto> getBySlugResponse = restTemplate.exchange(
            "/api/v1/organizations/slug/flow-test-org",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            OrganizationDto.class
        );
        
        assertEquals(HttpStatus.OK, getBySlugResponse.getStatusCode());
        assertEquals(orgId, getBySlugResponse.getBody().getId());
        
        // 4. Update organization name
        UpdateOrganizationRequest updateRequest = new UpdateOrganizationRequest("Updated Flow Org");
        
        ResponseEntity<OrganizationDto> updateResponse = restTemplate.exchange(
            "/api/v1/organizations/" + orgId,
            HttpMethod.PUT,
            new HttpEntity<>(updateRequest, headers),
            OrganizationDto.class
        );
        
        assertEquals(HttpStatus.OK, updateResponse.getStatusCode());
        assertEquals("Updated Flow Org", updateResponse.getBody().getName());
        
        // Verify update in database
        Optional<Organization> updatedOrg = organizationRepository.findById(OrganizationId.of(orgId));
        assertTrue(updatedOrg.isPresent());
        assertEquals("Updated Flow Org", updatedOrg.get().getName());
        
        // 5. Add first member
        AddMemberRequest addMember1 = new AddMemberRequest("member-1", "ADMIN");
        
        ResponseEntity<MemberDto> addMember1Response = restTemplate.exchange(
            "/api/v1/organizations/" + orgId + "/members",
            HttpMethod.POST,
            new HttpEntity<>(addMember1, headers),
            MemberDto.class
        );
        
        assertEquals(HttpStatus.CREATED, addMember1Response.getStatusCode());
        assertEquals("member-1", addMember1Response.getBody().getUserId());
        assertEquals("ADMIN", addMember1Response.getBody().getRole());
        
        // 6. Add second member
        AddMemberRequest addMember2 = new AddMemberRequest("member-2", "MEMBER");
        
        ResponseEntity<MemberDto> addMember2Response = restTemplate.exchange(
            "/api/v1/organizations/" + orgId + "/members",
            HttpMethod.POST,
            new HttpEntity<>(addMember2, headers),
            MemberDto.class
        );
        
        assertEquals(HttpStatus.CREATED, addMember2Response.getStatusCode());
        assertEquals("member-2", addMember2Response.getBody().getUserId());
        
        // 7. Get all members
        ResponseEntity<List<MemberDto>> getMembersResponse = restTemplate.exchange(
            "/api/v1/organizations/" + orgId + "/members",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            new ParameterizedTypeReference<List<MemberDto>>() {}
        );
        
        assertEquals(HttpStatus.OK, getMembersResponse.getStatusCode());
        assertEquals(2, getMembersResponse.getBody().size());
        
        // 8. Get specific member
        ResponseEntity<MemberDto> getMemberResponse = restTemplate.exchange(
            "/api/v1/organizations/" + orgId + "/members/member-1",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            MemberDto.class
        );
        
        assertEquals(HttpStatus.OK, getMemberResponse.getStatusCode());
        assertEquals("member-1", getMemberResponse.getBody().getUserId());
        
        // 9. Update member role
        com.turaf.organization.interfaces.rest.MembershipController.UpdateRoleRequest updateRoleRequest = 
            new com.turaf.organization.interfaces.rest.MembershipController.UpdateRoleRequest();
        updateRoleRequest.setRole("MEMBER");
        
        ResponseEntity<MemberDto> updateRoleResponse = restTemplate.exchange(
            "/api/v1/organizations/" + orgId + "/members/member-1/role",
            HttpMethod.PUT,
            new HttpEntity<>(updateRoleRequest, headers),
            MemberDto.class
        );
        
        assertEquals(HttpStatus.OK, updateRoleResponse.getStatusCode());
        assertEquals("MEMBER", updateRoleResponse.getBody().getRole());
        
        // 10. Remove first member
        ResponseEntity<Void> removeMember1Response = restTemplate.exchange(
            "/api/v1/organizations/" + orgId + "/members/member-1",
            HttpMethod.DELETE,
            new HttpEntity<>(headers),
            Void.class
        );
        
        assertEquals(HttpStatus.NO_CONTENT, removeMember1Response.getStatusCode());
        
        // Verify member removed from database
        Optional<OrganizationMember> removedMember = memberRepository.findByOrganizationIdAndUserId(
            OrganizationId.of(orgId),
            UserId.of("member-1")
        );
        assertFalse(removedMember.isPresent());
        
        // 11. Verify only one member remains
        ResponseEntity<List<MemberDto>> remainingMembersResponse = restTemplate.exchange(
            "/api/v1/organizations/" + orgId + "/members",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            new ParameterizedTypeReference<List<MemberDto>>() {}
        );
        
        assertEquals(HttpStatus.OK, remainingMembersResponse.getStatusCode());
        assertEquals(1, remainingMembersResponse.getBody().size());
        assertEquals("member-2", remainingMembersResponse.getBody().get(0).getUserId());
        
        // 12. Delete organization
        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
            "/api/v1/organizations/" + orgId,
            HttpMethod.DELETE,
            new HttpEntity<>(headers),
            Void.class
        );
        
        assertEquals(HttpStatus.NO_CONTENT, deleteResponse.getStatusCode());
        
        // Verify organization deleted from database
        Optional<Organization> deletedOrg = organizationRepository.findById(OrganizationId.of(orgId));
        assertFalse(deletedOrg.isPresent());
        
        // Verify members cascade deleted
        List<OrganizationMember> remainingMembers = memberRepository.findByOrganizationId(OrganizationId.of(orgId));
        assertTrue(remainingMembers.isEmpty());
    }
    
    @Test
    void shouldHandleUpdateOrganizationSettings() {
        // Given - create organization
        CreateOrganizationRequest createRequest = new CreateOrganizationRequest(
            "Settings Test Org",
            "settings-test-org"
        );
        
        ResponseEntity<OrganizationDto> createResponse = restTemplate.exchange(
            "/api/v1/organizations",
            HttpMethod.POST,
            new HttpEntity<>(createRequest, headers),
            OrganizationDto.class
        );
        
        String orgId = createResponse.getBody().getId();
        
        // When - update settings
        UpdateOrganizationRequest updateRequest = new UpdateOrganizationRequest();
        updateRequest.setAllowPublicExperiments(true);
        updateRequest.setMaxMembers(50);
        updateRequest.setMaxExperiments(200);
        
        ResponseEntity<OrganizationDto> updateResponse = restTemplate.exchange(
            "/api/v1/organizations/" + orgId,
            HttpMethod.PUT,
            new HttpEntity<>(updateRequest, headers),
            OrganizationDto.class
        );
        
        // Then
        assertEquals(HttpStatus.OK, updateResponse.getStatusCode());
        assertNotNull(updateResponse.getBody().getSettings());
        assertTrue(updateResponse.getBody().getSettings().isAllowPublicExperiments());
        assertEquals(50, updateResponse.getBody().getSettings().getMaxMembers());
        assertEquals(200, updateResponse.getBody().getSettings().getMaxExperiments());
        
        // Verify in database
        Optional<Organization> updated = organizationRepository.findById(OrganizationId.of(orgId));
        assertTrue(updated.isPresent());
        assertTrue(updated.get().getSettings().isAllowPublicExperiments());
        assertEquals(50, updated.get().getSettings().getMaxMembers());
    }
    
    @Test
    void shouldPreventDuplicateMemberAddition() {
        // Given - create organization and add member
        CreateOrganizationRequest createRequest = new CreateOrganizationRequest(
            "Duplicate Member Test",
            "duplicate-member-test"
        );
        
        ResponseEntity<OrganizationDto> createResponse = restTemplate.exchange(
            "/api/v1/organizations",
            HttpMethod.POST,
            new HttpEntity<>(createRequest, headers),
            OrganizationDto.class
        );
        
        String orgId = createResponse.getBody().getId();
        
        AddMemberRequest addMemberRequest = new AddMemberRequest("duplicate-user", "MEMBER");
        
        restTemplate.exchange(
            "/api/v1/organizations/" + orgId + "/members",
            HttpMethod.POST,
            new HttpEntity<>(addMemberRequest, headers),
            MemberDto.class
        );
        
        // When - try to add same member again
        ResponseEntity<com.turaf.organization.interfaces.rest.ErrorResponse> duplicateResponse = 
            restTemplate.exchange(
                "/api/v1/organizations/" + orgId + "/members",
                HttpMethod.POST,
                new HttpEntity<>(addMemberRequest, headers),
                com.turaf.organization.interfaces.rest.ErrorResponse.class
            );
        
        // Then
        assertEquals(HttpStatus.CONFLICT, duplicateResponse.getStatusCode());
        assertEquals("MEMBER_ALREADY_EXISTS", duplicateResponse.getBody().getCode());
    }
}
