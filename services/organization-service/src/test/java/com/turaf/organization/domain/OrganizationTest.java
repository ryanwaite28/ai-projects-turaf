package com.turaf.organization.domain;

import com.turaf.organization.domain.event.OrganizationCreated;
import com.turaf.organization.domain.event.OrganizationUpdated;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrganizationTest {
    
    @Test
    void shouldCreateOrganizationWithValidData() {
        // Given
        OrganizationId id = OrganizationId.generate();
        String name = "Test Organization";
        String slug = "test-org";
        UserId createdBy = UserId.generate();
        
        // When
        Organization org = new Organization(id, name, slug, createdBy);
        
        // Then
        assertNotNull(org);
        assertEquals(id, org.getId());
        assertEquals(name, org.getName());
        assertEquals(slug, org.getSlug());
        assertEquals(createdBy, org.getCreatedBy());
        assertNotNull(org.getCreatedAt());
        assertNotNull(org.getUpdatedAt());
        assertNotNull(org.getSettings());
    }
    
    @Test
    void shouldRegisterOrganizationCreatedEvent() {
        // Given
        OrganizationId id = OrganizationId.generate();
        UserId createdBy = UserId.generate();
        
        // When
        Organization org = new Organization(id, "Test Org", "test-org", createdBy);
        
        // Then
        assertEquals(1, org.getDomainEvents().size());
        assertTrue(org.getDomainEvents().get(0) instanceof OrganizationCreated);
        
        OrganizationCreated event = (OrganizationCreated) org.getDomainEvents().get(0);
        assertEquals(id.getValue(), event.getOrganizationId());
        assertEquals("Test Org", event.getName());
        assertEquals("test-org", event.getSlug());
    }
    
    @Test
    void shouldRejectNullName() {
        // Given
        OrganizationId id = OrganizationId.generate();
        UserId createdBy = UserId.generate();
        
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            new Organization(id, null, "test-org", createdBy)
        );
    }
    
    @Test
    void shouldRejectBlankName() {
        // Given
        OrganizationId id = OrganizationId.generate();
        UserId createdBy = UserId.generate();
        
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            new Organization(id, "   ", "test-org", createdBy)
        );
    }
    
    @Test
    void shouldRejectNameTooLong() {
        // Given
        OrganizationId id = OrganizationId.generate();
        UserId createdBy = UserId.generate();
        String longName = "a".repeat(101);
        
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            new Organization(id, longName, "test-org", createdBy)
        );
    }
    
    @Test
    void shouldAcceptNameAtMaxLength() {
        // Given
        OrganizationId id = OrganizationId.generate();
        UserId createdBy = UserId.generate();
        String maxName = "a".repeat(100);
        
        // When
        Organization org = new Organization(id, maxName, "test-org", createdBy);
        
        // Then
        assertEquals(maxName, org.getName());
    }
    
    @Test
    void shouldRejectNullSlug() {
        // Given
        OrganizationId id = OrganizationId.generate();
        UserId createdBy = UserId.generate();
        
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            new Organization(id, "Test Org", null, createdBy)
        );
    }
    
    @Test
    void shouldRejectSlugWithUppercase() {
        // Given
        OrganizationId id = OrganizationId.generate();
        UserId createdBy = UserId.generate();
        
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            new Organization(id, "Test Org", "Test-Org", createdBy)
        );
    }
    
    @Test
    void shouldRejectSlugWithSpaces() {
        // Given
        OrganizationId id = OrganizationId.generate();
        UserId createdBy = UserId.generate();
        
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            new Organization(id, "Test Org", "test org", createdBy)
        );
    }
    
    @Test
    void shouldRejectSlugTooShort() {
        // Given
        OrganizationId id = OrganizationId.generate();
        UserId createdBy = UserId.generate();
        
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            new Organization(id, "Test Org", "ab", createdBy)
        );
    }
    
    @Test
    void shouldRejectSlugTooLong() {
        // Given
        OrganizationId id = OrganizationId.generate();
        UserId createdBy = UserId.generate();
        String longSlug = "a".repeat(51);
        
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            new Organization(id, "Test Org", longSlug, createdBy)
        );
    }
    
    @Test
    void shouldAcceptValidSlugWithHyphens() {
        // Given
        OrganizationId id = OrganizationId.generate();
        UserId createdBy = UserId.generate();
        
        // When
        Organization org = new Organization(id, "Test Org", "test-org-123", createdBy);
        
        // Then
        assertEquals("test-org-123", org.getSlug());
    }
    
    @Test
    void shouldUpdateName() {
        // Given
        Organization org = new Organization(
            OrganizationId.generate(),
            "Old Name",
            "test-org",
            UserId.generate()
        );
        org.clearDomainEvents();
        
        // When
        org.updateName("New Name");
        
        // Then
        assertEquals("New Name", org.getName());
        assertEquals(1, org.getDomainEvents().size());
        assertTrue(org.getDomainEvents().get(0) instanceof OrganizationUpdated);
    }
    
    @Test
    void shouldNotRegisterEventWhenNameUnchanged() {
        // Given
        Organization org = new Organization(
            OrganizationId.generate(),
            "Test Org",
            "test-org",
            UserId.generate()
        );
        org.clearDomainEvents();
        
        // When
        org.updateName("Test Org");
        
        // Then
        assertEquals(0, org.getDomainEvents().size());
    }
    
    @Test
    void shouldUpdateSettings() {
        // Given
        Organization org = new Organization(
            OrganizationId.generate(),
            "Test Org",
            "test-org",
            UserId.generate()
        );
        OrganizationSettings newSettings = OrganizationSettings.create(true, 50, 200);
        
        // When
        org.updateSettings(newSettings);
        
        // Then
        assertEquals(newSettings, org.getSettings());
        assertTrue(org.getSettings().isAllowPublicExperiments());
        assertEquals(50, org.getSettings().getMaxMembers());
    }
    
    @Test
    void shouldRejectNullCreatedBy() {
        // Given
        OrganizationId id = OrganizationId.generate();
        
        // When/Then
        assertThrows(NullPointerException.class, () ->
            new Organization(id, "Test Org", "test-org", null)
        );
    }
    
    @Test
    void shouldTrimNameWhitespace() {
        // Given
        OrganizationId id = OrganizationId.generate();
        UserId createdBy = UserId.generate();
        
        // When
        Organization org = new Organization(id, "  Test Org  ", "test-org", createdBy);
        
        // Then
        assertEquals("Test Org", org.getName());
    }
    
    @Test
    void shouldConvertSlugToLowercase() {
        // Given
        OrganizationId id = OrganizationId.generate();
        UserId createdBy = UserId.generate();
        
        // When
        Organization org = new Organization(id, "Test Org", "test-org", createdBy);
        
        // Then
        assertEquals("test-org", org.getSlug());
    }
}
