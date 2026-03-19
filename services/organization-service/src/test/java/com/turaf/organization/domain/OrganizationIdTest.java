package com.turaf.organization.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for OrganizationId value object.
 */
class OrganizationIdTest {
    
    @Test
    void shouldCreateWithValidId() {
        // Given
        String id = UUID.randomUUID().toString();
        
        // When
        OrganizationId organizationId = OrganizationId.of(id);
        
        // Then
        assertNotNull(organizationId);
        assertEquals(id, organizationId.getValue());
    }
    
    @Test
    void shouldGenerateNewId() {
        // When
        OrganizationId organizationId = OrganizationId.generate();
        
        // Then
        assertNotNull(organizationId);
        assertNotNull(organizationId.getValue());
        assertDoesNotThrow(() -> UUID.fromString(organizationId.getValue()));
    }
    
    @Test
    void shouldThrowExceptionForNullId() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> OrganizationId.of(null));
    }
    
    @Test
    void shouldThrowExceptionForBlankId() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> OrganizationId.of(""));
        assertThrows(IllegalArgumentException.class, () -> OrganizationId.of("   "));
    }
    
    @Test
    void shouldBeEqualForSameId() {
        // Given
        String id = UUID.randomUUID().toString();
        OrganizationId id1 = OrganizationId.of(id);
        OrganizationId id2 = OrganizationId.of(id);
        
        // When/Then
        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
    }
    
    @Test
    void shouldNotBeEqualForDifferentIds() {
        // Given
        OrganizationId id1 = OrganizationId.generate();
        OrganizationId id2 = OrganizationId.generate();
        
        // When/Then
        assertNotEquals(id1, id2);
    }
    
    @Test
    void shouldHaveToStringRepresentation() {
        // Given
        String id = UUID.randomUUID().toString();
        OrganizationId organizationId = OrganizationId.of(id);
        
        // When
        String toString = organizationId.toString();
        
        // Then
        assertNotNull(toString);
        assertTrue(toString.contains(id));
    }
}
