package com.turaf.identity.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserIdTest {

    @Test
    void shouldCreateUserIdWithValidValue() {
        // Given
        String value = "user-123";

        // When
        UserId userId = UserId.of(value);

        // Then
        assertNotNull(userId);
        assertEquals(value, userId.getValue());
    }

    @Test
    void shouldGenerateRandomUserId() {
        // When
        UserId userId1 = UserId.generate();
        UserId userId2 = UserId.generate();

        // Then
        assertNotNull(userId1);
        assertNotNull(userId2);
        assertNotEquals(userId1, userId2);
        assertNotNull(userId1.getValue());
        assertNotNull(userId2.getValue());
    }

    @Test
    void shouldThrowExceptionForNullValue() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            UserId.of(null);
        });
    }

    @Test
    void shouldThrowExceptionForBlankValue() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            UserId.of("");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            UserId.of("   ");
        });
    }

    @Test
    void shouldBeEqualWhenValuesAreEqual() {
        // Given
        String value = "user-123";
        UserId userId1 = UserId.of(value);
        UserId userId2 = UserId.of(value);

        // Then
        assertEquals(userId1, userId2);
        assertEquals(userId1.hashCode(), userId2.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenValuesAreDifferent() {
        // Given
        UserId userId1 = UserId.of("user-123");
        UserId userId2 = UserId.of("user-456");

        // Then
        assertNotEquals(userId1, userId2);
    }

    @Test
    void shouldHaveStringRepresentation() {
        // Given
        UserId userId = UserId.of("user-123");

        // When
        String toString = userId.toString();

        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("user-123"));
    }
}
