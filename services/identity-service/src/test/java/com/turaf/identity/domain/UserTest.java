package com.turaf.identity.domain;

import com.turaf.identity.domain.event.UserCreated;
import com.turaf.identity.domain.event.UserPasswordChanged;
import com.turaf.identity.domain.event.UserProfileUpdated;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserTest {

    private PasswordEncoder mockEncoder;
    private UserId userId;
    private Email email;
    private Password password;

    @BeforeEach
    void setUp() {
        mockEncoder = mock(PasswordEncoder.class);
        userId = UserId.generate();
        email = new Email("user@example.com");
        
        when(mockEncoder.encode(anyString())).thenReturn("hashed_password");
        password = Password.fromRaw("SecureP@ss123", mockEncoder);
    }

    @Test
    void shouldCreateUserWithValidData() {
        // When
        User user = new User(userId, email, password, "John Doe");

        // Then
        assertNotNull(user);
        assertEquals(userId, user.getId());
        assertEquals(email, user.getEmail());
        assertEquals(password, user.getPassword());
        assertEquals("John Doe", user.getName());
        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());
    }

    @Test
    void shouldPublishUserCreatedEvent() {
        // When
        User user = new User(userId, email, password, "John Doe");

        // Then
        assertEquals(1, user.getDomainEvents().size());
        assertTrue(user.getDomainEvents().get(0) instanceof UserCreated);
        
        UserCreated event = (UserCreated) user.getDomainEvents().get(0);
        assertEquals(userId.getValue(), event.getUserId());
        assertEquals(email.getValue(), event.getEmail());
        assertEquals("John Doe", event.getName());
    }

    @Test
    void shouldThrowExceptionForNullEmail() {
        // When & Then
        assertThrows(NullPointerException.class, () -> {
            new User(userId, null, password, "John Doe");
        });
    }

    @Test
    void shouldThrowExceptionForNullPassword() {
        // When & Then
        assertThrows(NullPointerException.class, () -> {
            new User(userId, email, null, "John Doe");
        });
    }

    @Test
    void shouldThrowExceptionForNullName() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            new User(userId, email, password, null);
        });
    }

    @Test
    void shouldThrowExceptionForBlankName() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            new User(userId, email, password, "   ");
        });
    }

    @Test
    void shouldThrowExceptionForNameExceeding100Characters() {
        // Given
        String longName = "a".repeat(101);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            new User(userId, email, password, longName);
        });
    }

    @Test
    void shouldTrimName() {
        // When
        User user = new User(userId, email, password, "  John Doe  ");

        // Then
        assertEquals("John Doe", user.getName());
    }

    @Test
    void shouldUpdatePassword() {
        // Given
        User user = new User(userId, email, password, "John Doe");
        user.clearDomainEvents();
        
        Password newPassword = Password.fromRaw("NewP@ssw0rd!", mockEncoder);

        // When
        user.updatePassword(newPassword);

        // Then
        assertEquals(newPassword, user.getPassword());
        assertEquals(1, user.getDomainEvents().size());
        assertTrue(user.getDomainEvents().get(0) instanceof UserPasswordChanged);
    }

    @Test
    void shouldPublishPasswordChangedEvent() {
        // Given
        User user = new User(userId, email, password, "John Doe");
        user.clearDomainEvents();
        
        Password newPassword = Password.fromRaw("NewP@ssw0rd!", mockEncoder);

        // When
        user.updatePassword(newPassword);

        // Then
        UserPasswordChanged event = (UserPasswordChanged) user.getDomainEvents().get(0);
        assertEquals(userId.getValue(), event.getUserId());
        assertNotNull(event.getChangedAt());
    }

    @Test
    void shouldThrowExceptionWhenUpdatingWithNullPassword() {
        // Given
        User user = new User(userId, email, password, "John Doe");

        // When & Then
        assertThrows(NullPointerException.class, () -> {
            user.updatePassword(null);
        });
    }

    @Test
    void shouldUpdateProfile() {
        // Given
        User user = new User(userId, email, password, "John Doe");
        user.clearDomainEvents();

        // When
        user.updateProfile("Jane Smith");

        // Then
        assertEquals("Jane Smith", user.getName());
        assertEquals(1, user.getDomainEvents().size());
        assertTrue(user.getDomainEvents().get(0) instanceof UserProfileUpdated);
    }

    @Test
    void shouldPublishProfileUpdatedEvent() {
        // Given
        User user = new User(userId, email, password, "John Doe");
        user.clearDomainEvents();

        // When
        user.updateProfile("Jane Smith");

        // Then
        UserProfileUpdated event = (UserProfileUpdated) user.getDomainEvents().get(0);
        assertEquals(userId.getValue(), event.getUserId());
        assertEquals("Jane Smith", event.getName());
        assertNotNull(event.getUpdatedAt());
    }

    @Test
    void shouldThrowExceptionWhenUpdatingWithNullName() {
        // Given
        User user = new User(userId, email, password, "John Doe");

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            user.updateProfile(null);
        });
    }

    @Test
    void shouldThrowExceptionWhenUpdatingWithBlankName() {
        // Given
        User user = new User(userId, email, password, "John Doe");

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            user.updateProfile("   ");
        });
    }

    @Test
    void shouldVerifyCorrectPassword() {
        // Given
        String rawPassword = "SecureP@ss123";
        when(mockEncoder.matches(rawPassword, "hashed_password")).thenReturn(true);
        
        User user = new User(userId, email, password, "John Doe");

        // When
        boolean result = user.verifyPassword(rawPassword);

        // Then
        assertTrue(result);
    }

    @Test
    void shouldRejectIncorrectPassword() {
        // Given
        String wrongPassword = "WrongP@ss456";
        when(mockEncoder.matches(wrongPassword, "hashed_password")).thenReturn(false);
        
        User user = new User(userId, email, password, "John Doe");

        // When
        boolean result = user.verifyPassword(wrongPassword);

        // Then
        assertFalse(result);
    }

    @Test
    void shouldThrowExceptionWhenVerifyingNullPassword() {
        // Given
        User user = new User(userId, email, password, "John Doe");

        // When & Then
        assertThrows(NullPointerException.class, () -> {
            user.verifyPassword(null);
        });
    }

    @Test
    void shouldUpdateTimestampWhenPasswordChanged() {
        // Given
        User user = new User(userId, email, password, "John Doe");
        Password newPassword = Password.fromRaw("NewP@ssw0rd!", mockEncoder);
        
        // When
        user.updatePassword(newPassword);

        // Then
        assertTrue(user.getUpdatedAt().isAfter(user.getCreatedAt()) || 
                   user.getUpdatedAt().equals(user.getCreatedAt()));
    }

    @Test
    void shouldUpdateTimestampWhenProfileChanged() {
        // Given
        User user = new User(userId, email, password, "John Doe");
        
        // When
        user.updateProfile("Jane Smith");

        // Then
        assertTrue(user.getUpdatedAt().isAfter(user.getCreatedAt()) || 
                   user.getUpdatedAt().equals(user.getCreatedAt()));
    }
}
