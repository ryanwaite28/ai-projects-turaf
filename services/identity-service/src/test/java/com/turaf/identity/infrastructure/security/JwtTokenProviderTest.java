package com.turaf.identity.infrastructure.security;

import com.turaf.identity.domain.UserId;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private static final String TEST_SECRET = "test-secret-key-for-testing-must-be-at-least-32-characters-long-for-hs512";
    private static final long ACCESS_TOKEN_EXPIRATION = 900000L; // 15 minutes

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(TEST_SECRET, ACCESS_TOKEN_EXPIRATION);
    }

    @Test
    void shouldGenerateAccessToken() {
        // Given
        UserId userId = UserId.generate();
        String email = "test@example.com";
        String organizationId = "org-123";

        // When
        String token = jwtTokenProvider.generateAccessToken(userId, email, organizationId);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.split("\\.").length == 3); // JWT has 3 parts
    }

    @Test
    void shouldGenerateRefreshToken() {
        // When
        String refreshToken1 = jwtTokenProvider.generateRefreshToken();
        String refreshToken2 = jwtTokenProvider.generateRefreshToken();

        // Then
        assertNotNull(refreshToken1);
        assertNotNull(refreshToken2);
        assertNotEquals(refreshToken1, refreshToken2);
        assertTrue(refreshToken1.length() > 0);
    }

    @Test
    void shouldExtractUserIdFromToken() {
        // Given
        UserId userId = UserId.generate();
        String email = "test@example.com";
        String organizationId = "org-123";
        String token = jwtTokenProvider.generateAccessToken(userId, email, organizationId);

        // When
        UserId extractedUserId = jwtTokenProvider.getUserIdFromToken(token);

        // Then
        assertEquals(userId, extractedUserId);
    }

    @Test
    void shouldExtractEmailFromToken() {
        // Given
        UserId userId = UserId.generate();
        String email = "test@example.com";
        String organizationId = "org-123";
        String token = jwtTokenProvider.generateAccessToken(userId, email, organizationId);

        // When
        String extractedEmail = jwtTokenProvider.getEmailFromToken(token);

        // Then
        assertEquals(email, extractedEmail);
    }

    @Test
    void shouldExtractOrganizationIdFromToken() {
        // Given
        UserId userId = UserId.generate();
        String email = "test@example.com";
        String organizationId = "org-123";
        String token = jwtTokenProvider.generateAccessToken(userId, email, organizationId);

        // When
        String extractedOrgId = jwtTokenProvider.getOrganizationIdFromToken(token);

        // Then
        assertEquals(organizationId, extractedOrgId);
    }

    @Test
    void shouldValidateValidToken() {
        // Given
        UserId userId = UserId.generate();
        String email = "test@example.com";
        String organizationId = "org-123";
        String token = jwtTokenProvider.generateAccessToken(userId, email, organizationId);

        // When
        boolean isValid = jwtTokenProvider.validateToken(token);

        // Then
        assertTrue(isValid);
    }

    @Test
    void shouldRejectInvalidToken() {
        // Given
        String invalidToken = "invalid.token.here";

        // When
        boolean isValid = jwtTokenProvider.validateToken(invalidToken);

        // Then
        assertFalse(isValid);
    }

    @Test
    void shouldRejectTokenWithWrongSignature() {
        // Given
        UserId userId = UserId.generate();
        String email = "test@example.com";
        String organizationId = "org-123";
        String token = jwtTokenProvider.generateAccessToken(userId, email, organizationId);
        
        // Tamper with the token
        String tamperedToken = token.substring(0, token.length() - 5) + "xxxxx";

        // When
        boolean isValid = jwtTokenProvider.validateToken(tamperedToken);

        // Then
        assertFalse(isValid);
    }

    @Test
    void shouldRejectNullToken() {
        // When
        boolean isValid = jwtTokenProvider.validateToken(null);

        // Then
        assertFalse(isValid);
    }

    @Test
    void shouldRejectEmptyToken() {
        // When
        boolean isValid = jwtTokenProvider.validateToken("");

        // Then
        assertFalse(isValid);
    }

    @Test
    void shouldThrowExceptionWhenExtractingUserIdFromInvalidToken() {
        // Given
        String invalidToken = "invalid.token.here";

        // When & Then
        assertThrows(JwtException.class, () -> {
            jwtTokenProvider.getUserIdFromToken(invalidToken);
        });
    }

    @Test
    void shouldIncludeAllClaimsInToken() {
        // Given
        UserId userId = UserId.generate();
        String email = "test@example.com";
        String organizationId = "org-123";

        // When
        String token = jwtTokenProvider.generateAccessToken(userId, email, organizationId);

        // Then
        UserId extractedUserId = jwtTokenProvider.getUserIdFromToken(token);
        String extractedEmail = jwtTokenProvider.getEmailFromToken(token);
        String extractedOrgId = jwtTokenProvider.getOrganizationIdFromToken(token);

        assertEquals(userId, extractedUserId);
        assertEquals(email, extractedEmail);
        assertEquals(organizationId, extractedOrgId);
    }

    @Test
    void shouldGenerateDifferentTokensForDifferentUsers() {
        // Given
        UserId userId1 = UserId.generate();
        UserId userId2 = UserId.generate();
        String email = "test@example.com";
        String organizationId = "org-123";

        // When
        String token1 = jwtTokenProvider.generateAccessToken(userId1, email, organizationId);
        String token2 = jwtTokenProvider.generateAccessToken(userId2, email, organizationId);

        // Then
        assertNotEquals(token1, token2);
    }

    @Test
    void shouldReturnAccessTokenExpiration() {
        // When
        long expiration = jwtTokenProvider.getAccessTokenExpiration();

        // Then
        assertEquals(ACCESS_TOKEN_EXPIRATION, expiration);
    }

    @Test
    void shouldGenerateTokenWithCorrectExpiration() throws InterruptedException {
        // Given
        JwtTokenProvider shortExpirationProvider = new JwtTokenProvider(TEST_SECRET, 100L); // 100ms
        UserId userId = UserId.generate();
        String email = "test@example.com";
        String organizationId = "org-123";

        // When
        String token = shortExpirationProvider.generateAccessToken(userId, email, organizationId);
        
        // Token should be valid immediately
        assertTrue(shortExpirationProvider.validateToken(token));
        
        // Wait for expiration
        Thread.sleep(150);
        
        // Then - Token should be expired
        assertFalse(shortExpirationProvider.validateToken(token));
    }
}
