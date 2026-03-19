package com.turaf.organization.integration;

import com.turaf.organization.application.dto.AddMemberRequest;
import com.turaf.organization.application.dto.MemberDto;
import com.turaf.organization.domain.*;
import com.turaf.organization.interfaces.rest.ErrorResponse;
import com.turaf.organization.interfaces.rest.MembershipController;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for MembershipController.
 * Tests complete flow from REST API through to database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
class MembershipControllerIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private OrganizationRepository organizationRepository;
    
    @Autowired
    private OrganizationMemberRepository memberRepository;
    
    private HttpHeaders headers;
    private Organization testOrganization;
    private UserId testUserId;
    
    @BeforeEach
    void setUp() {
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Id", "admin-user-123");
        headers.set("X-Organization-Id", "test-org-123");
        
        testUserId = UserId.of("admin-user-123");
        
        // Create test organization
        testOrganization = new Organization(
            OrganizationId.generate(),
            "Test Organization",
            "test-org",
            testUserId
        );
        testOrganization = organizationRepository.save(testOrganization);
    }
    
    @Test
    void shouldAddMember() {
        // Given
        AddMemberRequest request = new AddMemberRequest("new-user-456", "MEMBER");
        HttpEntity<AddMemberRequest> entity = new HttpEntity<>(request, headers);
        
        // When
        ResponseEntity<MemberDto> response = restTemplate.exchange(
            "/api/v1/organizations/" + testOrganization.getId().getValue() + "/members",
            HttpMethod.POST,
            entity,
            MemberDto.class
        );
        
        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("new-user-456", response.getBody().getUserId());
        assertEquals("MEMBER", response.getBody().getRole());
        
        // Verify saved in database
        Optional<OrganizationMember> saved = memberRepository.findByOrganizationIdAndUserId(
            testOrganization.getId(),
            UserId.of("new-user-456")
        );
        assertTrue(saved.isPresent());
        assertEquals(MemberRole.MEMBER, saved.get().getRole());
    }
    
    @Test
    void shouldReturnConflictForDuplicateMember() {
        // Given - add member first
        UserId userId = UserId.of("duplicate-user");
        OrganizationMember existingMember = new OrganizationMember(
            UUID.randomUUID().toString(),
            testOrganization.getId(),
            userId,
            MemberRole.MEMBER,
            testUserId
        );
        memberRepository.save(existingMember);
        
        AddMemberRequest request = new AddMemberRequest("duplicate-user", "ADMIN");
        HttpEntity<AddMemberRequest> entity = new HttpEntity<>(request, headers);
        
        // When
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
            "/api/v1/organizations/" + testOrganization.getId().getValue() + "/members",
            HttpMethod.POST,
            entity,
            ErrorResponse.class
        );
        
        // Then
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("MEMBER_ALREADY_EXISTS", response.getBody().getCode());
    }
    
    @Test
    void shouldGetAllMembers() {
        // Given - add multiple members
        OrganizationMember member1 = new OrganizationMember(
            UUID.randomUUID().toString(),
            testOrganization.getId(),
            UserId.of("user-1"),
            MemberRole.ADMIN,
            testUserId
        );
        OrganizationMember member2 = new OrganizationMember(
            UUID.randomUUID().toString(),
            testOrganization.getId(),
            UserId.of("user-2"),
            MemberRole.MEMBER,
            testUserId
        );
        memberRepository.save(member1);
        memberRepository.save(member2);
        
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        
        // When
        ResponseEntity<List<MemberDto>> response = restTemplate.exchange(
            "/api/v1/organizations/" + testOrganization.getId().getValue() + "/members",
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<List<MemberDto>>() {}
        );
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
    }
    
    @Test
    void shouldGetSpecificMember() {
        // Given
        UserId userId = UserId.of("specific-user");
        OrganizationMember member = new OrganizationMember(
            UUID.randomUUID().toString(),
            testOrganization.getId(),
            userId,
            MemberRole.MEMBER,
            testUserId
        );
        memberRepository.save(member);
        
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        
        // When
        ResponseEntity<MemberDto> response = restTemplate.exchange(
            "/api/v1/organizations/" + testOrganization.getId().getValue() + "/members/specific-user",
            HttpMethod.GET,
            entity,
            MemberDto.class
        );
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("specific-user", response.getBody().getUserId());
        assertEquals("MEMBER", response.getBody().getRole());
    }
    
    @Test
    void shouldReturnNotFoundForNonexistentMember() {
        // Given
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        
        // When
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
            "/api/v1/organizations/" + testOrganization.getId().getValue() + "/members/nonexistent-user",
            HttpMethod.GET,
            entity,
            ErrorResponse.class
        );
        
        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("MEMBER_NOT_FOUND", response.getBody().getCode());
    }
    
    @Test
    void shouldUpdateMemberRole() {
        // Given
        UserId userId = UserId.of("role-update-user");
        OrganizationMember member = new OrganizationMember(
            UUID.randomUUID().toString(),
            testOrganization.getId(),
            userId,
            MemberRole.MEMBER,
            testUserId
        );
        memberRepository.save(member);
        
        MembershipController.UpdateRoleRequest request = new MembershipController.UpdateRoleRequest();
        request.setRole("ADMIN");
        HttpEntity<MembershipController.UpdateRoleRequest> entity = new HttpEntity<>(request, headers);
        
        // When
        ResponseEntity<MemberDto> response = restTemplate.exchange(
            "/api/v1/organizations/" + testOrganization.getId().getValue() + "/members/role-update-user/role",
            HttpMethod.PUT,
            entity,
            MemberDto.class
        );
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("ADMIN", response.getBody().getRole());
        
        // Verify in database
        Optional<OrganizationMember> updated = memberRepository.findByOrganizationIdAndUserId(
            testOrganization.getId(),
            userId
        );
        assertTrue(updated.isPresent());
        assertEquals(MemberRole.ADMIN, updated.get().getRole());
    }
    
    @Test
    void shouldRemoveMember() {
        // Given
        UserId userId = UserId.of("remove-user");
        OrganizationMember member = new OrganizationMember(
            UUID.randomUUID().toString(),
            testOrganization.getId(),
            userId,
            MemberRole.MEMBER,
            testUserId
        );
        memberRepository.save(member);
        
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        
        // When
        ResponseEntity<Void> response = restTemplate.exchange(
            "/api/v1/organizations/" + testOrganization.getId().getValue() + "/members/remove-user",
            HttpMethod.DELETE,
            entity,
            Void.class
        );
        
        // Then
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        
        // Verify deleted from database
        Optional<OrganizationMember> deleted = memberRepository.findByOrganizationIdAndUserId(
            testOrganization.getId(),
            userId
        );
        assertFalse(deleted.isPresent());
    }
    
    @Test
    void shouldReturnBadRequestForInvalidRole() {
        // Given
        AddMemberRequest request = new AddMemberRequest("user-123", "INVALID_ROLE");
        HttpEntity<AddMemberRequest> entity = new HttpEntity<>(request, headers);
        
        // When
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
            "/api/v1/organizations/" + testOrganization.getId().getValue() + "/members",
            HttpMethod.POST,
            entity,
            ErrorResponse.class
        );
        
        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
    
    @Test
    void shouldReturnNotFoundForNonexistentOrganization() {
        // Given
        AddMemberRequest request = new AddMemberRequest("user-123", "MEMBER");
        HttpEntity<AddMemberRequest> entity = new HttpEntity<>(request, headers);
        
        // When
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
            "/api/v1/organizations/nonexistent-org/members",
            HttpMethod.POST,
            entity,
            ErrorResponse.class
        );
        
        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
