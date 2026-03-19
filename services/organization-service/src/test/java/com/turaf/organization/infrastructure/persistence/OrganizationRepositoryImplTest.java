package com.turaf.organization.infrastructure.persistence;

import com.turaf.organization.domain.Organization;
import com.turaf.organization.domain.OrganizationId;
import com.turaf.organization.domain.OrganizationSettings;
import com.turaf.organization.domain.UserId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for OrganizationRepositoryImpl.
 * Tests repository operations against an in-memory database.
 */
@DataJpaTest
@Import(OrganizationRepositoryImpl.class)
@ActiveProfiles("test")
class OrganizationRepositoryImplTest {
    
    @Autowired
    private OrganizationRepositoryImpl repository;
    
    @Test
    void shouldSaveAndRetrieveOrganization() {
        // Given
        OrganizationId id = OrganizationId.generate();
        UserId createdBy = UserId.generate();
        Organization org = new Organization(id, "Test Org", "test-org", createdBy);
        
        // When
        Organization saved = repository.save(org);
        Optional<Organization> retrieved = repository.findById(id);
        
        // Then
        assertTrue(retrieved.isPresent());
        assertEquals(id, retrieved.get().getId());
        assertEquals("Test Org", retrieved.get().getName());
        assertEquals("test-org", retrieved.get().getSlug());
    }
    
    @Test
    void shouldFindOrganizationBySlug() {
        // Given
        OrganizationId id = OrganizationId.generate();
        UserId createdBy = UserId.generate();
        Organization org = new Organization(id, "Test Org", "test-org", createdBy);
        repository.save(org);
        
        // When
        Optional<Organization> found = repository.findBySlug("test-org");
        
        // Then
        assertTrue(found.isPresent());
        assertEquals(id, found.get().getId());
        assertEquals("test-org", found.get().getSlug());
    }
    
    @Test
    void shouldReturnEmptyWhenSlugNotFound() {
        // When
        Optional<Organization> found = repository.findBySlug("nonexistent");
        
        // Then
        assertFalse(found.isPresent());
    }
    
    @Test
    void shouldCheckSlugExistence() {
        // Given
        OrganizationId id = OrganizationId.generate();
        UserId createdBy = UserId.generate();
        Organization org = new Organization(id, "Test Org", "test-org", createdBy);
        repository.save(org);
        
        // When/Then
        assertTrue(repository.existsBySlug("test-org"));
        assertFalse(repository.existsBySlug("nonexistent"));
    }
    
    @Test
    void shouldEnforceUniqueSlugConstraint() {
        // Given
        UserId createdBy = UserId.generate();
        Organization org1 = new Organization(
            OrganizationId.generate(),
            "Test Org 1",
            "test-org",
            createdBy
        );
        repository.save(org1);
        
        // When/Then - Attempting to save with duplicate slug should fail
        Organization org2 = new Organization(
            OrganizationId.generate(),
            "Test Org 2",
            "test-org",
            createdBy
        );
        
        assertThrows(Exception.class, () -> repository.save(org2));
    }
    
    @Test
    void shouldUpdateOrganization() {
        // Given
        OrganizationId id = OrganizationId.generate();
        UserId createdBy = UserId.generate();
        Organization org = new Organization(id, "Old Name", "test-org", createdBy);
        repository.save(org);
        
        // When
        org.updateName("New Name");
        repository.save(org);
        Optional<Organization> updated = repository.findById(id);
        
        // Then
        assertTrue(updated.isPresent());
        assertEquals("New Name", updated.get().getName());
    }
    
    @Test
    void shouldDeleteOrganization() {
        // Given
        OrganizationId id = OrganizationId.generate();
        UserId createdBy = UserId.generate();
        Organization org = new Organization(id, "Test Org", "test-org", createdBy);
        repository.save(org);
        
        // When
        repository.delete(org);
        
        // Then
        assertFalse(repository.existsById(id));
    }
    
    @Test
    void shouldPersistOrganizationSettings() {
        // Given
        OrganizationId id = OrganizationId.generate();
        UserId createdBy = UserId.generate();
        Organization org = new Organization(id, "Test Org", "test-org", createdBy);
        OrganizationSettings customSettings = OrganizationSettings.create(true, 50, 200);
        org.updateSettings(customSettings);
        
        // When
        repository.save(org);
        Optional<Organization> retrieved = repository.findById(id);
        
        // Then
        assertTrue(retrieved.isPresent());
        OrganizationSettings settings = retrieved.get().getSettings();
        assertTrue(settings.isAllowPublicExperiments());
        assertEquals(50, settings.getMaxMembers());
        assertEquals(200, settings.getMaxExperiments());
    }
    
    @Test
    void shouldCheckOrganizationExistence() {
        // Given
        OrganizationId id = OrganizationId.generate();
        UserId createdBy = UserId.generate();
        Organization org = new Organization(id, "Test Org", "test-org", createdBy);
        repository.save(org);
        
        // When/Then
        assertTrue(repository.existsById(id));
        assertFalse(repository.existsById(OrganizationId.generate()));
    }
}
