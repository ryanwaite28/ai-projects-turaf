package com.turaf.identity.infrastructure.persistence;

import com.turaf.identity.domain.PasswordResetToken;
import com.turaf.identity.domain.UserId;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetTokenJpaEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "token", nullable = false, unique = true, length = 500)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used", nullable = false)
    private boolean used;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PasswordResetTokenJpaEntity() {
    }

    public PasswordResetTokenJpaEntity(String id, String userId, String token, Instant expiresAt, boolean used, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.token = token;
        this.expiresAt = expiresAt;
        this.used = used;
        this.createdAt = createdAt;
    }

    public PasswordResetToken toDomain() {
        return new PasswordResetToken(
            id,
            UserId.of(userId),
            token,
            expiresAt,
            used,
            createdAt
        );
    }

    public static PasswordResetTokenJpaEntity fromDomain(PasswordResetToken resetToken) {
        return new PasswordResetTokenJpaEntity(
            resetToken.getId(),
            resetToken.getUserId().getValue(),
            resetToken.getToken(),
            resetToken.getExpiresAt(),
            resetToken.isUsed(),
            resetToken.getCreatedAt()
        );
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PasswordResetTokenJpaEntity that = (PasswordResetTokenJpaEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
