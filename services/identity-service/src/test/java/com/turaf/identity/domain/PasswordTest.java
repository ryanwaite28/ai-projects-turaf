package com.turaf.identity.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PasswordTest {

    private PasswordEncoder mockEncoder;

    @BeforeEach
    void setUp() {
        mockEncoder = mock(PasswordEncoder.class);
    }

    @Test
    void shouldCreatePasswordFromRawValue() {
        // Given
        String rawPassword = "SecureP@ss123";
        String hashedPassword = "hashed_value";
        when(mockEncoder.encode(rawPassword)).thenReturn(hashedPassword);

        // When
        Password password = Password.fromRaw(rawPassword, mockEncoder);

        // Then
        assertNotNull(password);
        assertEquals(hashedPassword, password.getHashedValue());
        verify(mockEncoder).encode(rawPassword);
    }

    @Test
    void shouldCreatePasswordFromHashedValue() {
        // Given
        String hashedPassword = "hashed_value";

        // When
        Password password = Password.fromHashed(hashedPassword, mockEncoder);

        // Then
        assertNotNull(password);
        assertEquals(hashedPassword, password.getHashedValue());
        verifyNoInteractions(mockEncoder);
    }

    @Test
    void shouldVerifyCorrectPassword() {
        // Given
        String rawPassword = "SecureP@ss123";
        String hashedPassword = "hashed_value";
        when(mockEncoder.encode(rawPassword)).thenReturn(hashedPassword);
        when(mockEncoder.matches(rawPassword, hashedPassword)).thenReturn(true);

        Password password = Password.fromRaw(rawPassword, mockEncoder);

        // When
        boolean matches = password.matches(rawPassword);

        // Then
        assertTrue(matches);
        verify(mockEncoder).matches(rawPassword, hashedPassword);
    }

    @Test
    void shouldRejectIncorrectPassword() {
        // Given
        String rawPassword = "SecureP@ss123";
        String wrongPassword = "WrongP@ss456";
        String hashedPassword = "hashed_value";
        when(mockEncoder.encode(rawPassword)).thenReturn(hashedPassword);
        when(mockEncoder.matches(wrongPassword, hashedPassword)).thenReturn(false);

        Password password = Password.fromRaw(rawPassword, mockEncoder);

        // When
        boolean matches = password.matches(wrongPassword);

        // Then
        assertFalse(matches);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "SecureP@ss123",
        "MyP@ssw0rd!",
        "C0mpl3x!Pass",
        "Str0ng#Passw0rd"
    })
    void shouldAcceptStrongPasswords(String strongPassword) {
        // Given
        when(mockEncoder.encode(anyString())).thenReturn("hashed");

        // When & Then
        assertDoesNotThrow(() -> Password.fromRaw(strongPassword, mockEncoder));
    }

    @Test
    void shouldRejectPasswordShorterThan8Characters() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Password.fromRaw("Short1!", mockEncoder);
        });
        assertTrue(exception.getMessage().contains("at least 8 characters"));
    }

    @Test
    void shouldRejectPasswordWithoutUppercase() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Password.fromRaw("nouppercase1!", mockEncoder);
        });
        assertTrue(exception.getMessage().contains("uppercase letter"));
    }

    @Test
    void shouldRejectPasswordWithoutLowercase() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Password.fromRaw("NOLOWERCASE1!", mockEncoder);
        });
        assertTrue(exception.getMessage().contains("lowercase letter"));
    }

    @Test
    void shouldRejectPasswordWithoutDigit() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Password.fromRaw("NoDigits!Pass", mockEncoder);
        });
        assertTrue(exception.getMessage().contains("digit"));
    }

    @Test
    void shouldRejectPasswordWithoutSpecialCharacter() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Password.fromRaw("NoSpecial123", mockEncoder);
        });
        assertTrue(exception.getMessage().contains("special character"));
    }

    @Test
    void shouldRejectNullRawPassword() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            Password.fromRaw(null, mockEncoder);
        });
    }

    @Test
    void shouldRejectNullHashedPassword() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            Password.fromHashed(null, mockEncoder);
        });
    }

    @Test
    void shouldRejectBlankHashedPassword() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            Password.fromHashed("   ", mockEncoder);
        });
    }

    @Test
    void shouldBeEqualWhenHashedValuesAreEqual() {
        // Given
        String hashedValue = "hashed_value";
        Password password1 = Password.fromHashed(hashedValue, mockEncoder);
        Password password2 = Password.fromHashed(hashedValue, mockEncoder);

        // Then
        assertEquals(password1, password2);
        assertEquals(password1.hashCode(), password2.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenHashedValuesAreDifferent() {
        // Given
        Password password1 = Password.fromHashed("hashed1", mockEncoder);
        Password password2 = Password.fromHashed("hashed2", mockEncoder);

        // Then
        assertNotEquals(password1, password2);
    }

    @Test
    void shouldNotExposePasswordInToString() {
        // Given
        Password password = Password.fromHashed("hashed_value", mockEncoder);

        // When
        String toString = password.toString();

        // Then
        assertNotNull(toString);
        assertFalse(toString.contains("hashed_value"));
        assertTrue(toString.contains("***"));
    }
}
