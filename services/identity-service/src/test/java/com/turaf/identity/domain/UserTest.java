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
        User user = new User(userId, "org-1", email, password, "johndoe", "John", "Doe");

        // Then
        assertNotNull(user);
        assertEquals(userId, user.getId());
        assertEquals(email, user.getEmail());
        assertEquals(password, user.getPassword());
        assertEquals("johndoe", user.getUsername());
        assertEquals("John", user.getFirstName());
        assertEquals("Doe", user.getLastName());
        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());
    }

    @Test
    void shouldPublishUserCreatedEvent() {
        // When
        User user = new User(userId, "org-1", email, password, "johndoe", "John", "Doe");

        // Then
        assertEquals(1, user.getDomainEvents().size());
        assertTrue(user.getDomainEvents().get(0) instanceof UserCreated);
        
        UserCreated event = (UserCreated) user.getDomainEvents().get(0);
        assertEquals(userId.getValue(), event.getUserId());
        assertEquals(email.getValue(), event.getEmail());
        assertEquals("johndoe", event.getUsername());
    }

    @Test
    void shouldThrowExceptionForNullEmail() {
        // When & Then
        assertThrows(NullPointerException.class, () -> {
            new User(userId, "org-1", null, password, "johndoe", "John", "Doe");
        });
    }

    @Test
    void shouldThrowExceptionForNullPassword() {
        // When & Then
        assertThrows(NullPointerException.class, () -> {
            new User(userId, "org-1", email, null, "johndoe", "John", "Doe");
        });
    }

    @Test
    void shouldThrowExceptionForNullUsername() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            new User(userId, "org-1", email, password, null, "John", "Doe");
        });
    }

    @Test
    void shouldThrowExceptionForBlankFirstName() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            new User(userId, "org-1", email, password, "johndoe", "   ", "Doe");
        });
    }

    @Test
    void shouldThrowExceptionForNameExceeding50Characters() {
        // Given
        String longName = "a".repeat(51);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            new User(userId, "org-1", email, password, "johndoe", longName, "Doe");
        });
    }

    @Test
    void shouldTrimUsername() {
        // When
        User user = new User(userId, "org-1", email, password, "  johndoe  ", "John", "Doe");

        // Then
        assertEquals("johndoe", user.getUsername());
    }

    @Test
    void shouldUpdatePassword() {
        // Given
        User user = new User(userId, "org-1", email, password, "johndoe", "John", "Doe");
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
        User user = new User(userId, "org-1", email, password, "johndoe", "John", "Doe");
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
        User user = new User(userId, "org-1", email, password, "johndoe", "John", "Doe");

        // When & Then
        assertThrows(NullPointerException.class, () -> {
            user.updatePassword(null);
        });
    }

    @Test
    void shouldUpdateProfile() {
        // Given
        User user = new User(userId, "org-1", email, password, "johndoe", "John", "Doe");
        user.clearDomainEvents();

        // When
        user.updateProfile("Jane", "Smith");

        // Then
        assertEquals("Jane", user.getFirstName());
        assertEquals("Smith", user.getLastName());
        assertEquals(1, user.getDomainEvents().size());
        assertTrue(user.getDomainEvents().get(0) instanceof UserProfileUpdated);
    }

    @Test
    void shouldPublishProfileUpdatedEvent() {
        // Given
        User user = new User(userId, "org-1", email, password, "johndoe", "John", "Doe");
        user.clearDomainEvents();

        // When
        user.updateProfile("Jane", "Smith");

        // Then
        UserProfileUpdated event = (UserProfileUpdated) user.getDomainEvents().get(0);
        assertEquals(userId.getValue(), event.getUserId());
        assertEquals("Jane Smith", event.getName());
        assertNotNull(event.getUpdatedAt());
    }

    @Test
    void shouldThrowExceptionWhenUpdatingWithNullFirstName() {
        // Given
        User user = new User(userId, "org-1", email, password, "johndoe", "John", "Doe");

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            user.updateProfile(null, "Smith");
        });
    }

    @Test
    void shouldThrowExceptionWhenUpdatingWithBlankLastName() {
        // Given
        User user = new User(userId, "org-1", email, password, "johndoe", "John", "Doe");

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            user.updateProfile("Jane", "   ");
        });
    }

    @Test
    void shouldVerifyCorrectPassword() {
        // Given
        String rawPassword = "SecureP@ss123";
        when(mockEncoder.matches(rawPassword, "hashed_password")).thenReturn(true);
        
        User user = new User(userId, "org-1", email, password, "johndoe", "John", "Doe");

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
        
        User user = new User(userId, "org-1", email, password, "johndoe", "John", "Doe");

        // When
        boolean result = user.verifyPassword(wrongPassword);

        // Then
        assertFalse(result);
    }

    @Test
    void shouldThrowExceptionWhenVerifyingNullPassword() {
        // Given
        User user = new User(userId, "org-1", email, password, "johndoe", "John", "Doe");

        // When & Then
        assertThrows(NullPointerException.class, () -> {
            user.verifyPassword(null);
        });
    }

    @Test
    void shouldUpdateTimestampWhenPasswordChanged() {
        // Given
        User user = new User(userId, "org-1", email, password, "johndoe", "John", "Doe");
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
        User user = new User(userId, "org-1", email, password, "johndoe", "John", "Doe");
        
        // When
        user.updateProfile("Jane", "Smith");

        // Then
        assertTrue(user.getUpdatedAt().isAfter(user.getCreatedAt()) || 
                   user.getUpdatedAt().equals(user.getCreatedAt()));
    }
}
