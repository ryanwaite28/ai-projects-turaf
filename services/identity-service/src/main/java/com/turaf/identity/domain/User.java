package com.turaf.identity.domain;

import com.turaf.common.domain.AggregateRoot;
import com.turaf.identity.domain.event.UserCreated;
import com.turaf.identity.domain.event.UserPasswordChanged;
import com.turaf.identity.domain.event.UserProfileUpdated;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class User extends AggregateRoot<UserId> {
    
    private Email email;
    private Password password;
    private String name;
    private Instant createdAt;
    private Instant updatedAt;

    public User(UserId id, Email email, Password password, String name) {
        super(id);
        this.email = Objects.requireNonNull(email, "Email cannot be null");
        this.password = Objects.requireNonNull(password, "Password cannot be null");
        this.name = validateName(name);
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        
        registerEvent(new UserCreated(
            UUID.randomUUID().toString(),
            id.getValue(),
            email.getValue(),
            name
        ));
    }

    public void updatePassword(Password newPassword) {
        Objects.requireNonNull(newPassword, "New password cannot be null");
        this.password = newPassword;
        this.updatedAt = Instant.now();
        
        registerEvent(new UserPasswordChanged(
            UUID.randomUUID().toString(),
            getId().getValue(),
            updatedAt
        ));
    }

    public void updateProfile(String newName) {
        this.name = validateName(newName);
        this.updatedAt = Instant.now();
        
        registerEvent(new UserProfileUpdated(
            UUID.randomUUID().toString(),
            getId().getValue(),
            name,
            updatedAt
        ));
    }

    public boolean verifyPassword(String rawPassword) {
        Objects.requireNonNull(rawPassword, "Password to verify cannot be null");
        return password.matches(rawPassword);
    }

    private String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
        if (name.length() > 100) {
            throw new IllegalArgumentException("Name cannot exceed 100 characters");
        }
        return name.trim();
    }

    public Email getEmail() {
        return email;
    }

    public Password getPassword() {
        return password;
    }

    public String getName() {
        return name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
