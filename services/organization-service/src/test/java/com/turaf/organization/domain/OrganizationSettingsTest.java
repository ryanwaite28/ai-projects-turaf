package com.turaf.organization.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for OrganizationSettings value object.
 */
class OrganizationSettingsTest {
    
    @Test
    void shouldCreateDefaultSettings() {
        // When
        OrganizationSettings settings = OrganizationSettings.createDefault();
        
        // Then
        assertNotNull(settings);
        assertFalse(settings.isAllowPublicExperiments());
        assertEquals(10, settings.getMaxMembers());
        assertEquals(100, settings.getMaxExperiments());
    }
    
    @Test
    void shouldCreateCustomSettings() {
        // When
        OrganizationSettings settings = OrganizationSettings.create(true, 50, 200);
        
        // Then
        assertNotNull(settings);
        assertTrue(settings.isAllowPublicExperiments());
        assertEquals(50, settings.getMaxMembers());
        assertEquals(200, settings.getMaxExperiments());
    }
    
    @Test
    void shouldUpdateAllowPublicExperiments() {
        // Given
        OrganizationSettings settings = OrganizationSettings.createDefault();
        
        // When
        OrganizationSettings updated = settings.withAllowPublicExperiments(true);
        
        // Then
        assertTrue(updated.isAllowPublicExperiments());
        assertEquals(settings.getMaxMembers(), updated.getMaxMembers());
        assertEquals(settings.getMaxExperiments(), updated.getMaxExperiments());
    }
    
    @Test
    void shouldUpdateMaxMembers() {
        // Given
        OrganizationSettings settings = OrganizationSettings.createDefault();
        
        // When
        OrganizationSettings updated = settings.withMaxMembers(25);
        
        // Then
        assertEquals(25, updated.getMaxMembers());
        assertEquals(settings.isAllowPublicExperiments(), updated.isAllowPublicExperiments());
        assertEquals(settings.getMaxExperiments(), updated.getMaxExperiments());
    }
    
    @Test
    void shouldUpdateMaxExperiments() {
        // Given
        OrganizationSettings settings = OrganizationSettings.createDefault();
        
        // When
        OrganizationSettings updated = settings.withMaxExperiments(150);
        
        // Then
        assertEquals(150, updated.getMaxExperiments());
        assertEquals(settings.isAllowPublicExperiments(), updated.isAllowPublicExperiments());
        assertEquals(settings.getMaxMembers(), updated.getMaxMembers());
    }
    
    @Test
    void shouldThrowExceptionForInvalidMaxMembers() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            OrganizationSettings.create(false, 0, 100)
        );
        assertThrows(IllegalArgumentException.class, () ->
            OrganizationSettings.create(false, -1, 100)
        );
    }
    
    @Test
    void shouldThrowExceptionForInvalidMaxExperiments() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            OrganizationSettings.create(false, 10, 0)
        );
        assertThrows(IllegalArgumentException.class, () ->
            OrganizationSettings.create(false, 10, -1)
        );
    }
    
    @Test
    void shouldBeImmutable() {
        // Given
        OrganizationSettings original = OrganizationSettings.create(false, 10, 100);
        
        // When
        OrganizationSettings modified = original.withMaxMembers(20);
        
        // Then - original should be unchanged
        assertEquals(10, original.getMaxMembers());
        assertEquals(20, modified.getMaxMembers());
    }
    
    @Test
    void shouldBeEqualForSameValues() {
        // Given
        OrganizationSettings settings1 = OrganizationSettings.create(true, 10, 100);
        OrganizationSettings settings2 = OrganizationSettings.create(true, 10, 100);
        
        // When/Then
        assertEquals(settings1, settings2);
        assertEquals(settings1.hashCode(), settings2.hashCode());
    }
    
    @Test
    void shouldNotBeEqualForDifferentValues() {
        // Given
        OrganizationSettings settings1 = OrganizationSettings.create(true, 10, 100);
        OrganizationSettings settings2 = OrganizationSettings.create(false, 10, 100);
        
        // When/Then
        assertNotEquals(settings1, settings2);
    }
}
