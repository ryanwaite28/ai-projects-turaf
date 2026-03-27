package com.turaf.identity.application;

import com.turaf.common.event.EventPublisher;
import com.turaf.identity.application.dto.*;
import com.turaf.identity.application.exception.*;
import com.turaf.identity.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EventPublisher eventPublisher;

    public AuthenticationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
    }

    public UserDto register(RegisterRequest request) {
        Email email = new Email(request.getEmail());

        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("User with email " + email.getValue() + " already exists");
        }

        UserId userId = UserId.generate();
        Password password = Password.fromRaw(request.getPassword(), passwordEncoder);

        User user = new User(userId, request.getOrganizationId(), email, password, request.getName());
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

    public UserDto updateProfile(UserId userId, String newName) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found"));

        user.updateProfile(newName);
        
        User savedUser = userRepository.save(user);
        savedUser.getDomainEvents().forEach(eventPublisher::publish);
        savedUser.clearDomainEvents();

        return UserDto.fromDomain(savedUser);
    }
}
