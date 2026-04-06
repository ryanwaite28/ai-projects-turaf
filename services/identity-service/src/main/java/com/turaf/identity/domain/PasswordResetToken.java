package com.turaf.identity.domain;

import com.turaf.common.domain.Entity;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class PasswordResetToken extends Entity<String> {

    private final UserId userId;
    private final String token;
    private final Instant expiresAt;
    private final Instant createdAt;
    private boolean used;

    public PasswordResetToken(String id, UserId userId, String token, Instant expiresAt) {
        super(id);
        this.userId = Objects.requireNonNull(userId, "UserId cannot be null");
        this.token = validateToken(token);
        this.expiresAt = Objects.requireNonNull(expiresAt, "ExpiresAt cannot be null");
        this.createdAt = Instant.now();
        this.used = false;
    }

    public PasswordResetToken(String id, UserId userId, String token, Instant expiresAt, boolean used, Instant createdAt) {
        super(id);
        this.userId = Objects.requireNonNull(userId, "UserId cannot be null");
        this.token = validateToken(token);
        this.expiresAt = Objects.requireNonNull(expiresAt, "ExpiresAt cannot be null");
        this.createdAt = Objects.requireNonNull(createdAt, "CreatedAt cannot be null");
        this.used = used;
    }

    public static PasswordResetToken create(UserId userId, long expirationMillis) {
        String id = UUID.randomUUID().toString();
        String token = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plusMillis(expirationMillis);
        return new PasswordResetToken(id, userId, token, expiresAt);
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !isExpired() && !used;
    }

    public void markAsUsed() {
        if (!isValid()) {
            throw new IllegalStateException("Token is already used or expired");
        }
        this.used = true;
    }

    private String validateToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token cannot be null or blank");
        }
        return token;
    }

    public UserId getUserId() {
        return userId;
    }

    public String getToken() {
        return token;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isUsed() {
        return used;
    }
}
