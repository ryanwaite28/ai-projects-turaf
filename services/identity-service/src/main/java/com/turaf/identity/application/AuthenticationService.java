package com.turaf.identity.application;

import com.turaf.common.event.EventPublisher;
import com.turaf.identity.application.dto.*;
import com.turaf.identity.application.exception.*;
import com.turaf.identity.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EventPublisher eventPublisher;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final long passwordResetTokenExpiration;

    public AuthenticationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EventPublisher eventPublisher,
            PasswordResetTokenRepository passwordResetTokenRepository,
            @Value("${identity.password-reset.expiration-ms:3600000}") long passwordResetTokenExpiration) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordResetTokenExpiration = passwordResetTokenExpiration;
    }

    public UserDto register(RegisterRequest request) {
        Email email = new Email(request.getEmail());

        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("User with email " + email.getValue() + " already exists");
        }

        UserId userId = UserId.generate();
        Password password = Password.fromRaw(request.getPassword(), passwordEncoder);

        User user = new User(userId, request.getOrganizationId(), email, password, request.getUsername(), request.getFirstName(), request.getLastName());
        User savedUser = userRepository.save(user);

        savedUser.getDomainEvents().forEach(eventPublisher::publish);
        savedUser.clearDomainEvents();

        return UserDto.fromDomain(savedUser);
    }

    @Transactional(readOnly = true)
    public UserDto login(LoginRequest request) {
        Email email = new Email(request.getEmail());

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!user.verifyPassword(request.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        return UserDto.fromDomain(user);
    }

    public void changePassword(UserId userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!user.verifyPassword(request.getCurrentPassword())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        Password newPassword = Password.fromRaw(request.getNewPassword(), passwordEncoder);
        user.updatePassword(newPassword);
        
        User savedUser = userRepository.save(user);
        savedUser.getDomainEvents().forEach(eventPublisher::publish);
        savedUser.clearDomainEvents();
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(UserId userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found"));
        return UserDto.fromDomain(user);
    }

    @Transactional(readOnly = true)
    public UserDto getUserByEmail(String emailAddress) {
        Email email = new Email(emailAddress);
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UserNotFoundException("User not found"));
        return UserDto.fromDomain(user);
    }

    @Transactional(readOnly = true)
    public String getUserOrganizationId(UserId userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found"));
        return user.getOrganizationId();
    }

    public UserDto updateProfile(UserId userId, String newFirstName, String newLastName) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found"));

        user.updateProfile(newFirstName, newLastName);
        
        User savedUser = userRepository.save(user);
        savedUser.getDomainEvents().forEach(eventPublisher::publish);
        savedUser.clearDomainEvents();

        return UserDto.fromDomain(savedUser);
    }

    public void requestPasswordReset(PasswordResetRequest request) {
        Email email = new Email(request.getEmail());

        userRepository.findByEmail(email).ifPresent(user -> {
            // Invalidate any existing reset tokens for this user
            passwordResetTokenRepository.deleteByUserId(user.getId());

            PasswordResetToken resetToken = PasswordResetToken.create(
                user.getId(), passwordResetTokenExpiration
            );
            passwordResetTokenRepository.save(resetToken);

            // In production, send email with reset token
            // For now, log the token (would be replaced by email service integration)
            log.info("Password reset token generated for user: {}", user.getEmail().getValue());
        });

        // Always return success to prevent email enumeration
    }

    public void confirmPasswordReset(PasswordResetConfirmRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
            .orElseThrow(() -> new InvalidTokenException("Invalid password reset token"));

        if (!resetToken.isValid()) {
            throw new InvalidTokenException("Password reset token is expired or already used");
        }

        User user = userRepository.findById(resetToken.getUserId())
            .orElseThrow(() -> new UserNotFoundException("User not found"));

        Password newPassword = Password.fromRaw(request.getNewPassword(), passwordEncoder);
        user.updatePassword(newPassword);

        resetToken.markAsUsed();
        passwordResetTokenRepository.save(resetToken);

        User savedUser = userRepository.save(user);
        savedUser.getDomainEvents().forEach(eventPublisher::publish);
        savedUser.clearDomainEvents();
    }
}
