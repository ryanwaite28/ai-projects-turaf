package com.turaf.identity.domain;

import com.turaf.common.domain.Entity;

import java.time.Instant;
import java.util.Objects;

public class RefreshToken extends Entity<String> {
    
    private final UserId userId;
    private final String token;
    private final Instant expiresAt;
    private final Instant createdAt;

    public RefreshToken(String id, UserId userId, String token, Instant expiresAt) {
        super(id);
        this.userId = Objects.requireNonNull(userId, "UserId cannot be null");
        this.token = validateToken(token);
        this.expiresAt = Objects.requireNonNull(expiresAt, "ExpiresAt cannot be null");
        this.createdAt = Instant.now();
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !isExpired();
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
}
