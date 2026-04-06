package com.turaf.identity.infrastructure.persistence;

import com.turaf.identity.domain.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DataJpaTest
@ActiveProfiles("test")
@Import(UserRepositoryImpl.class)
class UserRepositoryImplTest {

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private UserRepositoryImpl userRepository;

    private PasswordEncoder mockEncoder;

    @BeforeEach
    void setUp() {
        mockEncoder = mock(PasswordEncoder.class);
        when(mockEncoder.encode(anyString())).thenReturn("hashed_password");
        when(mockEncoder.matches(anyString(), anyString())).thenReturn(true);
        
        userJpaRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        userJpaRepository.deleteAll();
    }

    @Test
    void shouldSaveUser() {
        // Given
        UserId userId = UserId.generate();
        Email email = new Email("test@example.com");
        Password password = Password.fromRaw("SecureP@ss123", mockEncoder);
        User user = new User(userId, "org-1", email, password, "testuser", "Test", "User");

        // When
        User saved = userRepository.save(user);

        // Then
        assertNotNull(saved);
        assertEquals(userId, saved.getId());
        assertEquals(email, saved.getEmail());
        assertEquals("testuser", saved.getUsername());
    }

    @Test
    void shouldFindUserById() {
        // Given
        UserId userId = UserId.generate();
        Email email = new Email("test@example.com");
        Password password = Password.fromRaw("SecureP@ss123", mockEncoder);
        User user = new User(userId, "org-1", email, password, "testuser", "Test", "User");
        userRepository.save(user);

        // When
        Optional<User> found = userRepository.findById(userId);

        // Then
        assertTrue(found.isPresent());
        assertEquals(userId, found.get().getId());
        assertEquals(email, found.get().getEmail());
        assertEquals("testuser", found.get().getUsername());
    }

    @Test
    void shouldReturnEmptyWhenUserNotFoundById() {
        // Given
        UserId nonExistentId = UserId.generate();

        // When
        Optional<User> found = userRepository.findById(nonExistentId);

        // Then
        assertFalse(found.isPresent());
    }

    @Test
    void shouldFindUserByEmail() {
        // Given
        UserId userId = UserId.generate();
        Email email = new Email("test@example.com");
        Password password = Password.fromRaw("SecureP@ss123", mockEncoder);
        User user = new User(userId, "org-1", email, password, "testuser", "Test", "User");
        userRepository.save(user);

        // When
        Optional<User> found = userRepository.findByEmail(email);

        // Then
        assertTrue(found.isPresent());
        assertEquals(userId, found.get().getId());
        assertEquals(email, found.get().getEmail());
    }

    @Test
    void shouldReturnEmptyWhenUserNotFoundByEmail() {
        // Given
        Email nonExistentEmail = new Email("nonexistent@example.com");

        // When
        Optional<User> found = userRepository.findByEmail(nonExistentEmail);

        // Then
        assertFalse(found.isPresent());
    }

    @Test
    void shouldCheckIfEmailExists() {
        // Given
        UserId userId = UserId.generate();
        Email email = new Email("test@example.com");
        Password password = Password.fromRaw("SecureP@ss123", mockEncoder);
        User user = new User(userId, "org-1", email, password, "testuser", "Test", "User");
        userRepository.save(user);

        // When
        boolean exists = userRepository.existsByEmail(email);
        boolean notExists = userRepository.existsByEmail(new Email("other@example.com"));

        // Then
        assertTrue(exists);
        assertFalse(notExists);
    }

    @Test
    void shouldUpdateUser() {
        // Given
        UserId userId = UserId.generate();
        Email email = new Email("test@example.com");
        Password password = Password.fromRaw("SecureP@ss123", mockEncoder);
        User user = new User(userId, "org-1", email, password, "testuser", "Test", "User");
        userRepository.save(user);

        // When
        user.updateProfile("Updated", "Name");
        User updated = userRepository.save(user);

        // Then
        assertEquals("Updated", updated.getFirstName());
        assertEquals("Name", updated.getLastName());
        
        Optional<User> found = userRepository.findById(userId);
        assertTrue(found.isPresent());
        assertEquals("Updated", found.get().getFirstName());
        assertEquals("Name", found.get().getLastName());
    }

    @Test
    void shouldDeleteUser() {
        // Given
        UserId userId = UserId.generate();
        Email email = new Email("test@example.com");
        Password password = Password.fromRaw("SecureP@ss123", mockEncoder);
        User user = new User(userId, "org-1", email, password, "testuser", "Test", "User");
        userRepository.save(user);

        // When
        userRepository.delete(user);

        // Then
        Optional<User> found = userRepository.findById(userId);
        assertFalse(found.isPresent());
    }

    @Test
    void shouldFindAllUsers() {
        // Given
        User user1 = new User(
            UserId.generate(),
            "org-1",
            new Email("user1@example.com"),
            Password.fromRaw("SecureP@ss123", mockEncoder),
            "user1",
            "User",
            "One"
        );
        User user2 = new User(
            UserId.generate(),
            "org-1",
            new Email("user2@example.com"),
            Password.fromRaw("SecureP@ss123", mockEncoder),
            "user2",
            "User",
            "Two"
        );
        userRepository.save(user1);
        userRepository.save(user2);

        // When
        List<User> users = userRepository.findAll();

        // Then
        assertEquals(2, users.size());
    }

    @Test
    void shouldEnforceUniqueEmailConstraint() {
        // Given
        Email email = new Email("test@example.com");
        User user1 = new User(
            UserId.generate(),
            "org-1",
            email,
            Password.fromRaw("SecureP@ss123", mockEncoder),
            "user1",
            "User",
            "One"
        );
        User user2 = new User(
            UserId.generate(),
            "org-1",
            email,
            Password.fromRaw("SecureP@ss123", mockEncoder),
            "user2",
            "User",
            "Two"
        );

        // When
        userRepository.save(user1);

        // Then
        assertThrows(Exception.class, () -> {
            userRepository.save(user2);
        });
    }

    @Test
    void shouldPreservePasswordHash() {
        // Given
        UserId userId = UserId.generate();
        Email email = new Email("test@example.com");
        Password password = Password.fromRaw("SecureP@ss123", mockEncoder);
        User user = new User(userId, "org-1", email, password, "testuser", "Test", "User");

        // When
        User saved = userRepository.save(user);
        Optional<User> found = userRepository.findById(userId);

        // Then
        assertTrue(found.isPresent());
        assertEquals(password.getHashedValue(), found.get().getPassword().getHashedValue());
    }

    @Test
    void shouldPreserveTimestamps() {
        // Given
        UserId userId = UserId.generate();
        Email email = new Email("test@example.com");
        Password password = Password.fromRaw("SecureP@ss123", mockEncoder);
        User user = new User(userId, "org-1", email, password, "testuser", "Test", "User");

        // When
        User saved = userRepository.save(user);
        Optional<User> found = userRepository.findById(userId);

        // Then
        assertTrue(found.isPresent());
        assertNotNull(found.get().getCreatedAt());
        assertNotNull(found.get().getUpdatedAt());
        assertEquals(saved.getCreatedAt(), found.get().getCreatedAt());
        assertEquals(saved.getUpdatedAt(), found.get().getUpdatedAt());
    }

    @Test
    void shouldClearDomainEventsAfterConversion() {
        // Given
        UserId userId = UserId.generate();
        Email email = new Email("test@example.com");
        Password password = Password.fromRaw("SecureP@ss123", mockEncoder);
        User user = new User(userId, "org-1", email, password, "testuser", "Test", "User");
        
        // User has domain events from creation
        assertEquals(1, user.getDomainEvents().size());

        // When
        User saved = userRepository.save(user);
        Optional<User> found = userRepository.findById(userId);

        // Then - Domain events should be cleared after retrieval
        assertTrue(found.isPresent());
        assertEquals(0, found.get().getDomainEvents().size());
    }
}
