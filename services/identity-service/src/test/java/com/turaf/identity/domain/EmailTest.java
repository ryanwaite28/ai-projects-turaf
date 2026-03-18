package com.turaf.identity.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class EmailTest {

    @Test
    void shouldCreateEmailWithValidFormat() {
        // Given
        String validEmail = "user@example.com";

        // When
        Email email = new Email(validEmail);

        // Then
        assertNotNull(email);
        assertEquals("user@example.com", email.getValue());
    }

    @Test
    void shouldConvertEmailToLowerCase() {
        // Given
        String mixedCaseEmail = "User@Example.COM";

        // When
        Email email = new Email(mixedCaseEmail);

        // Then
        assertEquals("user@example.com", email.getValue());
    }

    @Test
    void shouldTrimWhitespace() {
        // Given
        String emailWithWhitespace = "  user@example.com  ";

        // When
        Email email = new Email(emailWithWhitespace);

        // Then
        assertEquals("user@example.com", email.getValue());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "user@example.com",
        "john.doe@company.co.uk",
        "test+tag@domain.org",
        "user_name@sub.domain.com",
        "123@numbers.net"
    })
    void shouldAcceptValidEmailFormats(String validEmail) {
        // When & Then
        assertDoesNotThrow(() -> new Email(validEmail));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "invalid",
        "@example.com",
        "user@",
        "user @example.com",
        "user@example",
        "user@@example.com",
        ""
    })
    void shouldRejectInvalidEmailFormats(String invalidEmail) {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            new Email(invalidEmail);
        });
    }

    @Test
    void shouldThrowExceptionForNullEmail() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            new Email(null);
        });
    }

    @Test
    void shouldThrowExceptionForBlankEmail() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            new Email("   ");
        });
    }

    @Test
    void shouldBeEqualWhenValuesAreEqual() {
        // Given
        Email email1 = new Email("user@example.com");
        Email email2 = new Email("USER@EXAMPLE.COM");

        // Then
        assertEquals(email1, email2);
        assertEquals(email1.hashCode(), email2.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenValuesAreDifferent() {
        // Given
        Email email1 = new Email("user1@example.com");
        Email email2 = new Email("user2@example.com");

        // Then
        assertNotEquals(email1, email2);
    }

    @Test
    void shouldHaveStringRepresentation() {
        // Given
        Email email = new Email("user@example.com");

        // When
        String toString = email.toString();

        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("user@example.com"));
    }
}
