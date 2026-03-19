package com.turaf.organization.integration;

import com.turaf.organization.application.dto.CreateOrganizationRequest;
import com.turaf.organization.application.dto.OrganizationDto;
import com.turaf.organization.application.dto.UpdateOrganizationRequest;
import com.turaf.organization.domain.Organization;
import com.turaf.organization.domain.OrganizationId;
import com.turaf.organization.domain.OrganizationRepository;
import com.turaf.organization.domain.UserId;
import com.turaf.organization.interfaces.rest.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for OrganizationController.
 * Tests complete flow from REST API through to database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
class OrganizationControllerIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private OrganizationRepository organizationRepository;
    
    private HttpHeaders headers;
    private UserId testUserId;
    
    @BeforeEach
    void setUp() {
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Id", "test-user-123");
        headers.set("X-Organization-Id", "test-org-123");
        
        testUserId = UserId.of("test-user-123");
    }
    
    @Test
    void shouldCreateOrganization() {
        // Given
        CreateOrganizationRequest request = new CreateOrganizationRequest(
            "Test Organization",
            "test-org"
        );
        
        HttpEntity<CreateOrganizationRequest> entity = new HttpEntity<>(request, headers);
        
        // When
        ResponseEntity<OrganizationDto> response = restTemplate.exchange(
            "/api/v1/organizations",
            HttpMethod.POST,
            entity,
            OrganizationDto.class
        );
        
        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Test Organization", response.getBody().getName());
        assertEquals("test-org", response.getBody().getSlug());
        assertNotNull(response.getBody().getId());
        
        // Verify saved in database
        Optional<Organization> saved = organizationRepository.findBySlug("test-org");
        assertTrue(saved.isPresent());
        assertEquals("Test Organization", saved.get().getName());
    }
    
    @Test
    void shouldReturnConflictForDuplicateSlug() {
        // Given - create first organization
        Organization existing = new Organization(
            OrganizationId.generate(),
            "Existing Org",
            "duplicate-slug",
            testUserId
        );
        organizationRepository.save(existing);
        
        CreateOrganizationRequest request = new CreateOrganizationRequest(
            "Another Organization",
            "duplicate-slug"
        );
        
        HttpEntity<CreateOrganizationRequest> entity = new HttpEntity<>(request, headers);
        
        // When
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
            "/api/v1/organizations",
            HttpMethod.POST,
            entity,
            ErrorResponse.class
        );
        
        // Then
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("ORGANIZATION_ALREADY_EXISTS", response.getBody().getCode());
    }
    
    @Test
    void shouldGetOrganizationById() {
        // Given
        Organization org = new Organization(
            OrganizationId.generate(),
            "Get Test Org",
            "get-test-org",
            testUserId
        );
        Organization saved = organizationRepository.save(org);
        
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        
        // When
        ResponseEntity<OrganizationDto> response = restTemplate.exchange(
            "/api/v1/organizations/" + saved.getId().getValue(),
            HttpMethod.GET,
            entity,
            OrganizationDto.class
        );
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Get Test Org", response.getBody().getName());
        assertEquals("get-test-org", response.getBody().getSlug());
    }
    
    @Test
    void shouldReturnNotFoundForNonexistentOrganization() {
        // Given
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        
        // When
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
            "/api/v1/organizations/nonexistent-id",
            HttpMethod.GET,
            entity,
            ErrorResponse.class
        );
        
        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("ORGANIZATION_NOT_FOUND", response.getBody().getCode());
    }
    
    @Test
    void shouldGetOrganizationBySlug() {
        // Given
        Organization org = new Organization(
            OrganizationId.generate(),
            "Slug Test Org",
            "slug-test-org",
            testUserId
        );
        organizationRepository.save(org);
        
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        
        // When
        ResponseEntity<OrganizationDto> response = restTemplate.exchange(
            "/api/v1/organizations/slug/slug-test-org",
            HttpMethod.GET,
            entity,
            OrganizationDto.class
        );
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Slug Test Org", response.getBody().getName());
        assertEquals("slug-test-org", response.getBody().getSlug());
    }
    
    @Test
    void shouldUpdateOrganization() {
        // Given
        Organization org = new Organization(
            OrganizationId.generate(),
            "Original Name",
            "update-test-org",
            testUserId
        );
        Organization saved = organizationRepository.save(org);
        
        UpdateOrganizationRequest request = new UpdateOrganizationRequest("Updated Name");
        HttpEntity<UpdateOrganizationRequest> entity = new HttpEntity<>(request, headers);
        
        // When
        ResponseEntity<OrganizationDto> response = restTemplate.exchange(
            "/api/v1/organizations/" + saved.getId().getValue(),
            HttpMethod.PUT,
            entity,
            OrganizationDto.class
        );
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Updated Name", response.getBody().getName());
        
        // Verify in database
        Optional<Organization> updated = organizationRepository.findById(saved.getId());
        assertTrue(updated.isPresent());
        assertEquals("Updated Name", updated.get().getName());
    }
    
    @Test
    void shouldDeleteOrganization() {
        // Given
        Organization org = new Organization(
            OrganizationId.generate(),
            "Delete Test Org",
            "delete-test-org",
            testUserId
        );
        Organization saved = organizationRepository.save(org);
        
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        
        // When
        ResponseEntity<Void> response = restTemplate.exchange(
            "/api/v1/organizations/" + saved.getId().getValue(),
            HttpMethod.DELETE,
            entity,
            Void.class
        );
        
        // Then
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        
        // Verify deleted from database
        Optional<Organization> deleted = organizationRepository.findById(saved.getId());
        assertFalse(deleted.isPresent());
    }
    
    @Test
    void shouldReturnBadRequestForInvalidData() {
        // Given - empty name
        CreateOrganizationRequest request = new CreateOrganizationRequest("", "test-org");
        HttpEntity<CreateOrganizationRequest> entity = new HttpEntity<>(request, headers);
        
        // When
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
            "/api/v1/organizations",
            HttpMethod.POST,
            entity,
            ErrorResponse.class
        );
        
        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
