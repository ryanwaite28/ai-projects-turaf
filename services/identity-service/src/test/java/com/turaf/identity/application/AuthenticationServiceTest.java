package com.turaf.identity.application;

import com.turaf.common.event.EventPublisher;
import com.turaf.identity.application.dto.*;
import com.turaf.identity.application.exception.*;
import com.turaf.identity.domain.*;
import com.turaf.identity.domain.event.UserCreated;
import com.turaf.identity.domain.event.UserPasswordChanged;
import com.turaf.identity.domain.event.UserProfileUpdated;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    private AuthenticationService authenticationService;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "SecureP@ss123";
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_FIRST_NAME = "Test";
    private static final String TEST_LAST_NAME = "User";
    private static final String TEST_ORG_ID = "org-123";

    private static final long PASSWORD_RESET_EXPIRATION = 3600000L;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationService(
            userRepository, passwordEncoder, eventPublisher,
            passwordResetTokenRepository, PASSWORD_RESET_EXPIRATION
        );
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
    }

    @Test
    void shouldRegisterNewUser() {
        // Given
        RegisterRequest request = new RegisterRequest(TEST_EMAIL, TEST_PASSWORD, TEST_USERNAME, TEST_FIRST_NAME, TEST_LAST_NAME, TEST_ORG_ID);
        when(userRepository.existsByEmail(any(Email.class))).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        UserDto result = authenticationService.register(request);

        // Then
        assertNotNull(result);
        assertEquals(TEST_EMAIL, result.getEmail());
        assertEquals(TEST_USERNAME, result.getUsername());
        assertEquals(TEST_FIRST_NAME, result.getFirstName());
        assertEquals(TEST_LAST_NAME, result.getLastName());
        assertNotNull(result.getId());
        assertNotNull(result.getCreatedAt());

        verify(userRepository).existsByEmail(any(Email.class));
        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode(TEST_PASSWORD);
        verify(eventPublisher).publish(any(UserCreated.class));
    }

    @Test
    void shouldThrowExceptionWhenRegisteringDuplicateEmail() {
        // Given
        RegisterRequest request = new RegisterRequest(TEST_EMAIL, TEST_PASSWORD, TEST_USERNAME, TEST_FIRST_NAME, TEST_LAST_NAME, TEST_ORG_ID);
        when(userRepository.existsByEmail(any(Email.class))).thenReturn(true);

        // When & Then
        UserAlreadyExistsException exception = assertThrows(UserAlreadyExistsException.class, () -> {
            authenticationService.register(request);
        });

        assertTrue(exception.getMessage().contains(TEST_EMAIL));
        verify(userRepository).existsByEmail(any(Email.class));
        verify(userRepository, never()).save(any(User.class));
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void shouldPublishUserCreatedEventOnRegistration() {
        // Given
        RegisterRequest request = new RegisterRequest(TEST_EMAIL, TEST_PASSWORD, TEST_USERNAME, TEST_FIRST_NAME, TEST_LAST_NAME, TEST_ORG_ID);
        when(userRepository.existsByEmail(any(Email.class))).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        authenticationService.register(request);

        // Then
        ArgumentCaptor<UserCreated> eventCaptor = ArgumentCaptor.forClass(UserCreated.class);
        verify(eventPublisher).publish(eventCaptor.capture());

        UserCreated event = eventCaptor.getValue();
        assertNotNull(event);
        assertEquals(TEST_EMAIL, event.getEmail());
        assertEquals(TEST_USERNAME, event.getUsername());
    }

    @Test
    void shouldLoginWithValidCredentials() {
        // Given
        LoginRequest request = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);
        UserId userId = UserId.generate();
        Email email = new Email(TEST_EMAIL);
        Password password = Password.fromRaw(TEST_PASSWORD, passwordEncoder);
        User user = new User(userId, TEST_ORG_ID, email, password, TEST_USERNAME, TEST_FIRST_NAME, TEST_LAST_NAME);

        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(TEST_PASSWORD, "hashed_password")).thenReturn(true);

        // When
        UserDto result = authenticationService.login(request);

        // Then
        assertNotNull(result);
        assertEquals(userId.getValue(), result.getId());
        assertEquals(TEST_EMAIL, result.getEmail());
        assertEquals(TEST_USERNAME, result.getUsername());

        verify(userRepository).findByEmail(any(Email.class));
    }

    @Test
    void shouldThrowExceptionWhenLoginWithNonExistentEmail() {
        // Given
        LoginRequest request = new LoginRequest("nonexistent@example.com", TEST_PASSWORD);
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.empty());

        // When & Then
        InvalidCredentialsException exception = assertThrows(InvalidCredentialsException.class, () -> {
            authenticationService.login(request);
        });

        assertEquals("Invalid email or password", exception.getMessage());
        verify(userRepository).findByEmail(any(Email.class));
    }

    @Test
    void shouldThrowExceptionWhenLoginWithIncorrectPassword() {
        // Given
        LoginRequest request = new LoginRequest(TEST_EMAIL, "WrongP@ss456");
        UserId userId = UserId.generate();
        Email email = new Email(TEST_EMAIL);
        Password password = Password.fromRaw(TEST_PASSWORD, passwordEncoder);
        User user = new User(userId, TEST_ORG_ID, email, password, TEST_USERNAME, TEST_FIRST_NAME, TEST_LAST_NAME);

        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongP@ss456", "hashed_password")).thenReturn(false);

        // When & Then
        InvalidCredentialsException exception = assertThrows(InvalidCredentialsException.class, () -> {
            authenticationService.login(request);
        });

        assertEquals("Invalid email or password", exception.getMessage());
        verify(userRepository).findByEmail(any(Email.class));
    }

    @Test
    void shouldChangePasswordWithCorrectCurrentPassword() {
        // Given
        UserId userId = UserId.generate();
        Email email = new Email(TEST_EMAIL);
        Password password = Password.fromRaw(TEST_PASSWORD, passwordEncoder);
        User user = new User(userId, TEST_ORG_ID, email, password, TEST_USERNAME, TEST_FIRST_NAME, TEST_LAST_NAME);

        ChangePasswordRequest request = new ChangePasswordRequest(TEST_PASSWORD, "NewP@ssw0rd!");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(TEST_PASSWORD, "hashed_password")).thenReturn(true);
        when(passwordEncoder.encode("NewP@ssw0rd!")).thenReturn("new_hashed_password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        authenticationService.changePassword(userId, request);

        // Then
        verify(userRepository).findById(userId);
        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode("NewP@ssw0rd!");
        verify(eventPublisher).publish(any(UserPasswordChanged.class));
    }

    @Test
    void shouldThrowExceptionWhenChangePasswordWithIncorrectCurrentPassword() {
        // Given
        UserId userId = UserId.generate();
        Email email = new Email(TEST_EMAIL);
        Password password = Password.fromRaw(TEST_PASSWORD, passwordEncoder);
        User user = new User(userId, TEST_ORG_ID, email, password, TEST_USERNAME, TEST_FIRST_NAME, TEST_LAST_NAME);

        ChangePasswordRequest request = new ChangePasswordRequest("WrongP@ss456", "NewP@ssw0rd!");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongP@ss456", "hashed_password")).thenReturn(false);

        // When & Then
        InvalidCredentialsException exception = assertThrows(InvalidCredentialsException.class, () -> {
            authenticationService.changePassword(userId, request);
        });

        assertEquals("Current password is incorrect", exception.getMessage());
        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any(User.class));
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void shouldThrowExceptionWhenChangePasswordForNonExistentUser() {
        // Given
        UserId userId = UserId.generate();
        ChangePasswordRequest request = new ChangePasswordRequest(TEST_PASSWORD, "NewP@ssw0rd!");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> {
            authenticationService.changePassword(userId, request);
        });

        assertEquals("User not found", exception.getMessage());
        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldPublishPasswordChangedEvent() {
        // Given
        UserId userId = UserId.generate();
        Email email = new Email(TEST_EMAIL);
        Password password = Password.fromRaw(TEST_PASSWORD, passwordEncoder);
        User user = new User(userId, TEST_ORG_ID, email, password, TEST_USERNAME, TEST_FIRST_NAME, TEST_LAST_NAME);

        ChangePasswordRequest request = new ChangePasswordRequest(TEST_PASSWORD, "NewP@ssw0rd!");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(TEST_PASSWORD, "hashed_password")).thenReturn(true);
        when(passwordEncoder.encode("NewP@ssw0rd!")).thenReturn("new_hashed_password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        authenticationService.changePassword(userId, request);

        // Then
        ArgumentCaptor<UserPasswordChanged> eventCaptor = ArgumentCaptor.forClass(UserPasswordChanged.class);
        verify(eventPublisher).publish(eventCaptor.capture());

        UserPasswordChanged event = eventCaptor.getValue();
        assertNotNull(event);
        assertEquals(userId.getValue(), event.getUserId());
    }

    @Test
    void shouldGetUserById() {
        // Given
        UserId userId = UserId.generate();
        Email email = new Email(TEST_EMAIL);
        Password password = Password.fromRaw(TEST_PASSWORD, passwordEncoder);
        User user = new User(userId, TEST_ORG_ID, email, password, TEST_USERNAME, TEST_FIRST_NAME, TEST_LAST_NAME);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        UserDto result = authenticationService.getUserById(userId);

        // Then
        assertNotNull(result);
        assertEquals(userId.getValue(), result.getId());
        assertEquals(TEST_EMAIL, result.getEmail());
        assertEquals(TEST_USERNAME, result.getUsername());

        verify(userRepository).findById(userId);
    }

    @Test
    void shouldThrowExceptionWhenGetUserByIdNotFound() {
        // Given
        UserId userId = UserId.generate();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> {
            authenticationService.getUserById(userId);
        });

        assertEquals("User not found", exception.getMessage());
        verify(userRepository).findById(userId);
    }

    @Test
    void shouldGetUserByEmail() {
        // Given
        UserId userId = UserId.generate();
        Email email = new Email(TEST_EMAIL);
        Password password = Password.fromRaw(TEST_PASSWORD, passwordEncoder);
        User user = new User(userId, TEST_ORG_ID, email, password, TEST_USERNAME, TEST_FIRST_NAME, TEST_LAST_NAME);

        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(user));

        // When
        UserDto result = authenticationService.getUserByEmail(TEST_EMAIL);

        // Then
        assertNotNull(result);
        assertEquals(userId.getValue(), result.getId());
        assertEquals(TEST_EMAIL, result.getEmail());
        assertEquals(TEST_USERNAME, result.getUsername());

        verify(userRepository).findByEmail(any(Email.class));
    }

    @Test
    void shouldThrowExceptionWhenGetUserByEmailNotFound() {
        // Given
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.empty());

        // When & Then
        UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> {
            authenticationService.getUserByEmail("nonexistent@example.com");
        });

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void shouldUpdateProfile() {
        // Given
        UserId userId = UserId.generate();
        Email email = new Email(TEST_EMAIL);
        Password password = Password.fromRaw(TEST_PASSWORD, passwordEncoder);
        User user = new User(userId, TEST_ORG_ID, email, password, TEST_USERNAME, TEST_FIRST_NAME, TEST_LAST_NAME);

        String newFirstName = "Updated";
        String newLastName = "Name";

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        UserDto result = authenticationService.updateProfile(userId, newFirstName, newLastName);

        // Then
        assertNotNull(result);
        assertEquals(newFirstName, result.getFirstName());
        assertEquals(newLastName, result.getLastName());

        verify(userRepository).findById(userId);
        verify(userRepository).save(any(User.class));
        verify(eventPublisher).publish(any(UserProfileUpdated.class));
    }

    @Test
    void shouldPublishProfileUpdatedEvent() {
        // Given
        UserId userId = UserId.generate();
        Email email = new Email(TEST_EMAIL);
        Password password = Password.fromRaw(TEST_PASSWORD, passwordEncoder);
        User user = new User(userId, TEST_ORG_ID, email, password, TEST_USERNAME, TEST_FIRST_NAME, TEST_LAST_NAME);

        String newFirstName = "Updated";
        String newLastName = "Name";

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        authenticationService.updateProfile(userId, newFirstName, newLastName);

        // Then
        ArgumentCaptor<UserProfileUpdated> eventCaptor = ArgumentCaptor.forClass(UserProfileUpdated.class);
        verify(eventPublisher).publish(eventCaptor.capture());

        UserProfileUpdated event = eventCaptor.getValue();
        assertNotNull(event);
        assertEquals(userId.getValue(), event.getUserId());
        assertEquals("Updated Name", event.getName());
    }

    @Test
    void shouldThrowExceptionWhenUpdateProfileForNonExistentUser() {
        // Given
        UserId userId = UserId.generate();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> {
            authenticationService.updateProfile(userId, "New", "Name");
        });

        assertEquals("User not found", exception.getMessage());
        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldValidateEmailFormatDuringRegistration() {
        // Given
        RegisterRequest request = new RegisterRequest("invalid-email", TEST_PASSWORD, TEST_USERNAME, TEST_FIRST_NAME, TEST_LAST_NAME, TEST_ORG_ID);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            authenticationService.register(request);
        });

        verify(userRepository, never()).existsByEmail(any(Email.class));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldValidatePasswordStrengthDuringRegistration() {
        // Given
        RegisterRequest request = new RegisterRequest(TEST_EMAIL, "weak", TEST_USERNAME, TEST_FIRST_NAME, TEST_LAST_NAME, TEST_ORG_ID);
        when(userRepository.existsByEmail(any(Email.class))).thenReturn(false);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            authenticationService.register(request);
        });

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldRequestPasswordResetForExistingUser() {
        // Given
        UserId userId = UserId.generate();
        Email email = new Email(TEST_EMAIL);
        Password password = Password.fromRaw(TEST_PASSWORD, passwordEncoder);
        User user = new User(userId, TEST_ORG_ID, email, password, TEST_USERNAME, TEST_FIRST_NAME, TEST_LAST_NAME);

        PasswordResetRequest request = new PasswordResetRequest(TEST_EMAIL);
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(user));

        // When
        authenticationService.requestPasswordReset(request);

        // Then
        verify(passwordResetTokenRepository).deleteByUserId(userId);
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
    }

    @Test
    void shouldNotThrowWhenRequestingPasswordResetForNonExistentEmail() {
        // Given
        PasswordResetRequest request = new PasswordResetRequest("nonexistent@example.com");
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.empty());

        // When - should not throw (prevents email enumeration)
        authenticationService.requestPasswordReset(request);

        // Then
        verify(passwordResetTokenRepository, never()).save(any());
    }

    @Test
    void shouldConfirmPasswordReset() {
        // Given
        UserId userId = UserId.generate();
        Email email = new Email(TEST_EMAIL);
        Password password = Password.fromRaw(TEST_PASSWORD, passwordEncoder);
        User user = new User(userId, TEST_ORG_ID, email, password, TEST_USERNAME, TEST_FIRST_NAME, TEST_LAST_NAME);

        PasswordResetToken resetToken = PasswordResetToken.create(userId, PASSWORD_RESET_EXPIRATION);
        PasswordResetConfirmRequest request = new PasswordResetConfirmRequest(resetToken.getToken(), "NewP@ssw0rd!");

        when(passwordResetTokenRepository.findByToken(resetToken.getToken())).thenReturn(Optional.of(resetToken));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NewP@ssw0rd!")).thenReturn("new_hashed_password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        authenticationService.confirmPasswordReset(request);

        // Then
        verify(passwordResetTokenRepository).findByToken(resetToken.getToken());
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        verify(userRepository).save(any(User.class));
        verify(eventPublisher).publish(any(UserPasswordChanged.class));
    }

    @Test
    void shouldThrowExceptionWhenConfirmingWithInvalidToken() {
        // Given
        PasswordResetConfirmRequest request = new PasswordResetConfirmRequest("invalid-token", "NewP@ssw0rd!");
        when(passwordResetTokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        // When & Then
        InvalidTokenException exception = assertThrows(InvalidTokenException.class, () -> {
            authenticationService.confirmPasswordReset(request);
        });

        assertEquals("Invalid password reset token", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldClearDomainEventsAfterPublishing() {
        // Given
        RegisterRequest request = new RegisterRequest(TEST_EMAIL, TEST_PASSWORD, TEST_USERNAME, TEST_FIRST_NAME, TEST_LAST_NAME, TEST_ORG_ID);
        when(userRepository.existsByEmail(any(Email.class))).thenReturn(false);
        
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(userCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        authenticationService.register(request);

        // Then
        verify(eventPublisher).publish(any(UserCreated.class));
        
        // Verify events were cleared after publishing
        User savedUser = userCaptor.getValue();
        assertEquals(0, savedUser.getDomainEvents().size());
    }
}
