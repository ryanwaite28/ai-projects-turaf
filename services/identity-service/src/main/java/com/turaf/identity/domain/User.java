package com.turaf.identity.domain;

import com.turaf.common.domain.AggregateRoot;
import com.turaf.common.tenant.TenantAware;
import com.turaf.identity.domain.event.UserCreated;
import com.turaf.identity.domain.event.UserPasswordChanged;
import com.turaf.identity.domain.event.UserProfileUpdated;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class User extends AggregateRoot<UserId> implements TenantAware {
    
    private String organizationId;
    private Email email;
    private Password password;
    private String name;
    private Instant createdAt;
    private Instant updatedAt;

    public User(UserId id, String organizationId, Email email, Password password, String name) {
        super(id);
        this.organizationId = Objects.requireNonNull(organizationId, "Organization ID cannot be null");
        this.email = Objects.requireNonNull(email, "Email cannot be null");
        this.password = Objects.requireNonNull(password, "Password cannot be null");
        this.name = validateName(name);
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        
        registerEvent(new UserCreated(
            UUID.randomUUID().toString(),
            id.getValue(),
            organizationId,
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
            organizationId,
            updatedAt
        ));
    }

    public void updateProfile(String newName) {
        this.name = validateName(newName);
        this.updatedAt = Instant.now();
        
        registerEvent(new UserProfileUpdated(
            UUID.randomUUID().toString(),
            getId().getValue(),
            organizationId,
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
    
    /**
     * Gets the organization ID for this user.
     * Implements TenantAware interface.
     *
     * @return The organization ID
     */
    @Override
    public String getOrganizationId() {
        return organizationId;
    }
    
    /**
     * Sets the organization ID for this user.
     * Implements TenantAware interface for automatic tenant assignment.
     *
     * @param organizationId The organization ID to set
     */
    @Override
    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }
}
