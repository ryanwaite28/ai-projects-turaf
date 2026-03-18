package com.turaf.identity.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BCryptPasswordEncoderTest {

    private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
    }

    @Test
    void shouldEncodePassword() {
        // Given
        String rawPassword = "SecureP@ss123";

        // When
        String encoded = passwordEncoder.encode(rawPassword);

        // Then
        assertNotNull(encoded);
        assertNotEquals(rawPassword, encoded);
        assertTrue(encoded.startsWith("$2a$") || encoded.startsWith("$2b$"));
    }

    @Test
    void shouldGenerateDifferentHashesForSamePassword() {
        // Given
        String rawPassword = "SecureP@ss123";

        // When
        String encoded1 = passwordEncoder.encode(rawPassword);
        String encoded2 = passwordEncoder.encode(rawPassword);

        // Then
        assertNotEquals(encoded1, encoded2);
    }

    @Test
    void shouldMatchCorrectPassword() {
        // Given
        String rawPassword = "SecureP@ss123";
        String encoded = passwordEncoder.encode(rawPassword);

        // When
        boolean matches = passwordEncoder.matches(rawPassword, encoded);

        // Then
        assertTrue(matches);
    }

    @Test
    void shouldNotMatchIncorrectPassword() {
        // Given
        String rawPassword = "SecureP@ss123";
        String wrongPassword = "WrongP@ss456";
        String encoded = passwordEncoder.encode(rawPassword);

        // When
        boolean matches = passwordEncoder.matches(wrongPassword, encoded);

        // Then
        assertFalse(matches);
    }

    @Test
    void shouldHandleEmptyPassword() {
        // Given
        String emptyPassword = "";

        // When
        String encoded = passwordEncoder.encode(emptyPassword);

        // Then
        assertNotNull(encoded);
        assertTrue(passwordEncoder.matches(emptyPassword, encoded));
    }

    @Test
    void shouldHandleSpecialCharacters() {
        // Given
        String passwordWithSpecialChars = "P@$$w0rd!#%&*()";

        // When
        String encoded = passwordEncoder.encode(passwordWithSpecialChars);

        // Then
        assertNotNull(encoded);
        assertTrue(passwordEncoder.matches(passwordWithSpecialChars, encoded));
    }

    @Test
    void shouldHandleLongPassword() {
        // Given
        String longPassword = "ThisIsAVeryLongPasswordWithMoreThan50CharactersToTestTheEncoder123!@#";

        // When
        String encoded = passwordEncoder.encode(longPassword);

        // Then
        assertNotNull(encoded);
        assertTrue(passwordEncoder.matches(longPassword, encoded));
    }

    @Test
    void shouldNotMatchWithDifferentCase() {
        // Given
        String rawPassword = "SecureP@ss123";
        String encoded = passwordEncoder.encode(rawPassword);

        // When
        boolean matches = passwordEncoder.matches("securep@ss123", encoded);

        // Then
        assertFalse(matches);
    }

    @Test
    void shouldProduceConsistentLength() {
        // Given
        String password1 = "short";
        String password2 = "ThisIsAMuchLongerPasswordForTesting";

        // When
        String encoded1 = passwordEncoder.encode(password1);
        String encoded2 = passwordEncoder.encode(password2);

        // Then
        assertEquals(60, encoded1.length());
        assertEquals(60, encoded2.length());
    }
}
