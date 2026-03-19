package com.turaf.organization.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for UserId value object.
 */
class UserIdTest {
    
    @Test
    void shouldCreateWithValidId() {
        // Given
        String id = UUID.randomUUID().toString();
        
        // When
        UserId userId = UserId.of(id);
        
        // Then
        assertNotNull(userId);
        assertEquals(id, userId.getValue());
    }
    
    @Test
    void shouldGenerateNewId() {
        // When
        UserId userId = UserId.generate();
        
        // Then
        assertNotNull(userId);
        assertNotNull(userId.getValue());
        assertDoesNotThrow(() -> UUID.fromString(userId.getValue()));
    }
    
    @Test
    void shouldThrowExceptionForNullId() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> UserId.of(null));
    }
    
    @Test
    void shouldThrowExceptionForBlankId() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> UserId.of(""));
        assertThrows(IllegalArgumentException.class, () -> UserId.of("   "));
    }
    
    @Test
    void shouldBeEqualForSameId() {
        // Given
        String id = UUID.randomUUID().toString();
        UserId id1 = UserId.of(id);
        UserId id2 = UserId.of(id);
        
        // When/Then
        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
    }
    
    @Test
    void shouldNotBeEqualForDifferentIds() {
        // Given
        UserId id1 = UserId.generate();
        UserId id2 = UserId.generate();
        
        // When/Then
        assertNotEquals(id1, id2);
    }
    
    @Test
    void shouldHaveToStringRepresentation() {
        // Given
        String id = UUID.randomUUID().toString();
        UserId userId = UserId.of(id);
        
        // When
        String toString = userId.toString();
        
        // Then
        assertNotNull(toString);
        assertTrue(toString.contains(id));
    }
}
