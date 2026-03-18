package com.turaf.identity.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class RefreshTokenTest {

    private UserId userId;
    private String tokenId;
    private String tokenValue;

    @BeforeEach
    void setUp() {
        userId = UserId.generate();
        tokenId = "token-123";
        tokenValue = "refresh_token_value_abc123";
    }

    @Test
    void shouldCreateRefreshTokenWithValidData() {
        // Given
        Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);

        // When
        RefreshToken token = new RefreshToken(tokenId, userId, tokenValue, expiresAt);

        // Then
        assertNotNull(token);
        assertEquals(tokenId, token.getId());
        assertEquals(userId, token.getUserId());
        assertEquals(tokenValue, token.getToken());
        assertEquals(expiresAt, token.getExpiresAt());
        assertNotNull(token.getCreatedAt());
    }

    @Test
    void shouldNotBeExpiredWhenExpiresInFuture() {
        // Given
        Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);
        RefreshToken token = new RefreshToken(tokenId, userId, tokenValue, expiresAt);

        // When
        boolean expired = token.isExpired();
        boolean valid = token.isValid();

        // Then
        assertFalse(expired);
        assertTrue(valid);
    }

    @Test
    void shouldBeExpiredWhenExpiresInPast() {
        // Given
        Instant expiresAt = Instant.now().minus(1, ChronoUnit.DAYS);
        RefreshToken token = new RefreshToken(tokenId, userId, tokenValue, expiresAt);

        // When
        boolean expired = token.isExpired();
        boolean valid = token.isValid();

        // Then
        assertTrue(expired);
        assertFalse(valid);
    }

    @Test
    void shouldBeExpiredWhenExpiresNow() throws InterruptedException {
        // Given
        Instant expiresAt = Instant.now().plusMillis(100);
        RefreshToken token = new RefreshToken(tokenId, userId, tokenValue, expiresAt);

        // Wait for expiration
        Thread.sleep(150);

        // When
        boolean expired = token.isExpired();

        // Then
        assertTrue(expired);
    }

    @Test
    void shouldThrowExceptionForNullUserId() {
        // Given
        Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);

        // When & Then
        assertThrows(NullPointerException.class, () -> {
            new RefreshToken(tokenId, null, tokenValue, expiresAt);
        });
    }

    @Test
    void shouldThrowExceptionForNullToken() {
        // Given
        Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            new RefreshToken(tokenId, userId, null, expiresAt);
        });
    }

    @Test
    void shouldThrowExceptionForBlankToken() {
        // Given
        Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            new RefreshToken(tokenId, userId, "   ", expiresAt);
        });
    }

    @Test
    void shouldThrowExceptionForNullExpiresAt() {
        // When & Then
        assertThrows(NullPointerException.class, () -> {
            new RefreshToken(tokenId, userId, tokenValue, null);
        });
    }

    @Test
    void shouldSetCreatedAtToCurrentTime() {
        // Given
        Instant before = Instant.now();
        Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);

        // When
        RefreshToken token = new RefreshToken(tokenId, userId, tokenValue, expiresAt);
        Instant after = Instant.now();

        // Then
        assertTrue(!token.getCreatedAt().isBefore(before));
        assertTrue(!token.getCreatedAt().isAfter(after));
    }

    @Test
    void shouldBeEqualWhenIdsAreEqual() {
        // Given
        Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);
        RefreshToken token1 = new RefreshToken(tokenId, userId, tokenValue, expiresAt);
        RefreshToken token2 = new RefreshToken(tokenId, userId, "different_token", expiresAt);

        // Then
        assertEquals(token1, token2);
        assertEquals(token1.hashCode(), token2.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenIdsAreDifferent() {
        // Given
        Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);
        RefreshToken token1 = new RefreshToken("token-1", userId, tokenValue, expiresAt);
        RefreshToken token2 = new RefreshToken("token-2", userId, tokenValue, expiresAt);

        // Then
        assertNotEquals(token1, token2);
    }
}
