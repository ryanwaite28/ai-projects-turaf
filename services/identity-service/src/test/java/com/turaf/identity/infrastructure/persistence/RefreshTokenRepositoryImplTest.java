package com.turaf.identity.infrastructure.persistence;

import com.turaf.identity.domain.RefreshToken;
import com.turaf.identity.domain.UserId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Import(RefreshTokenRepositoryImpl.class)
class RefreshTokenRepositoryImplTest {

    @Autowired
    private RefreshTokenJpaRepository refreshTokenJpaRepository;

    @Autowired
    private RefreshTokenRepositoryImpl refreshTokenRepository;

    @BeforeEach
    void setUp() {
        refreshTokenJpaRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        refreshTokenJpaRepository.deleteAll();
    }

    @Test
    void shouldSaveRefreshToken() {
        // Given
        UserId userId = UserId.generate();
        String tokenId = UUID.randomUUID().toString();
        String tokenValue = "refresh_token_abc123";
        Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);
        RefreshToken token = new RefreshToken(tokenId, userId, tokenValue, expiresAt);

        // When
        refreshTokenRepository.save(token);

        // Then
        Optional<RefreshToken> found = refreshTokenRepository.findByToken(tokenValue);
        assertTrue(found.isPresent());
        assertEquals(tokenId, found.get().getId());
        assertEquals(userId, found.get().getUserId());
        assertEquals(tokenValue, found.get().getToken());
    }

    @Test
    void shouldFindRefreshTokenByToken() {
        // Given
        UserId userId = UserId.generate();
        String tokenId = UUID.randomUUID().toString();
        String tokenValue = "refresh_token_abc123";
        Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);
        RefreshToken token = new RefreshToken(tokenId, userId, tokenValue, expiresAt);
        refreshTokenRepository.save(token);

        // When
        Optional<RefreshToken> found = refreshTokenRepository.findByToken(tokenValue);

        // Then
        assertTrue(found.isPresent());
        assertEquals(tokenId, found.get().getId());
        assertEquals(userId, found.get().getUserId());
        assertEquals(tokenValue, found.get().getToken());
        assertEquals(expiresAt.truncatedTo(ChronoUnit.MILLIS), 
                     found.get().getExpiresAt().truncatedTo(ChronoUnit.MILLIS));
    }

    @Test
    void shouldReturnEmptyWhenTokenNotFound() {
        // Given
        String nonExistentToken = "non_existent_token";

        // When
        Optional<RefreshToken> found = refreshTokenRepository.findByToken(nonExistentToken);

        // Then
        assertFalse(found.isPresent());
    }

    @Test
    void shouldDeleteTokensByUserId() {
        // Given
        UserId userId = UserId.generate();
        RefreshToken token1 = new RefreshToken(
            UUID.randomUUID().toString(),
            userId,
            "token_1",
            Instant.now().plus(7, ChronoUnit.DAYS)
        );
        RefreshToken token2 = new RefreshToken(
            UUID.randomUUID().toString(),
            userId,
            "token_2",
            Instant.now().plus(7, ChronoUnit.DAYS)
        );
        refreshTokenRepository.save(token1);
        refreshTokenRepository.save(token2);

        // When
        refreshTokenRepository.deleteByUserId(userId);

        // Then
        Optional<RefreshToken> found1 = refreshTokenRepository.findByToken("token_1");
        Optional<RefreshToken> found2 = refreshTokenRepository.findByToken("token_2");
        assertFalse(found1.isPresent());
        assertFalse(found2.isPresent());
    }

    @Test
    void shouldDeleteExpiredTokens() {
        // Given
        UserId userId = UserId.generate();
        
        // Expired token
        RefreshToken expiredToken = new RefreshToken(
            UUID.randomUUID().toString(),
            userId,
            "expired_token",
            Instant.now().minus(1, ChronoUnit.DAYS)
        );
        
        // Valid token
        RefreshToken validToken = new RefreshToken(
            UUID.randomUUID().toString(),
            userId,
            "valid_token",
            Instant.now().plus(7, ChronoUnit.DAYS)
        );
        
        refreshTokenRepository.save(expiredToken);
        refreshTokenRepository.save(validToken);

        // When
        refreshTokenRepository.deleteExpiredTokens();

        // Then
        Optional<RefreshToken> foundExpired = refreshTokenRepository.findByToken("expired_token");
        Optional<RefreshToken> foundValid = refreshTokenRepository.findByToken("valid_token");
        
        assertFalse(foundExpired.isPresent());
        assertTrue(foundValid.isPresent());
    }

    @Test
    void shouldEnforceUniqueTokenConstraint() {
        // Given
        UserId userId1 = UserId.generate();
        UserId userId2 = UserId.generate();
        String tokenValue = "duplicate_token";
        Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);
        
        RefreshToken token1 = new RefreshToken(
            UUID.randomUUID().toString(),
            userId1,
            tokenValue,
            expiresAt
        );
        RefreshToken token2 = new RefreshToken(
            UUID.randomUUID().toString(),
            userId2,
            tokenValue,
            expiresAt
        );

        // When
        refreshTokenRepository.save(token1);

        // Then
        assertThrows(Exception.class, () -> {
            refreshTokenRepository.save(token2);
        });
    }

    @Test
    void shouldPreserveExpirationTimestamp() {
        // Given
        UserId userId = UserId.generate();
        String tokenId = UUID.randomUUID().toString();
        String tokenValue = "refresh_token_abc123";
        Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);
        RefreshToken token = new RefreshToken(tokenId, userId, tokenValue, expiresAt);

        // When
        refreshTokenRepository.save(token);
        Optional<RefreshToken> found = refreshTokenRepository.findByToken(tokenValue);

        // Then
        assertTrue(found.isPresent());
        assertEquals(expiresAt.truncatedTo(ChronoUnit.MILLIS), 
                     found.get().getExpiresAt().truncatedTo(ChronoUnit.MILLIS));
    }

    @Test
    void shouldPreserveCreatedAtTimestamp() {
        // Given
        UserId userId = UserId.generate();
        String tokenId = UUID.randomUUID().toString();
        String tokenValue = "refresh_token_abc123";
        Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);
        RefreshToken token = new RefreshToken(tokenId, userId, tokenValue, expiresAt);

        // When
        refreshTokenRepository.save(token);
        Optional<RefreshToken> found = refreshTokenRepository.findByToken(tokenValue);

        // Then
        assertTrue(found.isPresent());
        assertNotNull(found.get().getCreatedAt());
        assertEquals(token.getCreatedAt().truncatedTo(ChronoUnit.MILLIS), 
                     found.get().getCreatedAt().truncatedTo(ChronoUnit.MILLIS));
    }

    @Test
    void shouldAllowMultipleTokensPerUser() {
        // Given
        UserId userId = UserId.generate();
        RefreshToken token1 = new RefreshToken(
            UUID.randomUUID().toString(),
            userId,
            "token_1",
            Instant.now().plus(7, ChronoUnit.DAYS)
        );
        RefreshToken token2 = new RefreshToken(
            UUID.randomUUID().toString(),
            userId,
            "token_2",
            Instant.now().plus(7, ChronoUnit.DAYS)
        );

        // When
        refreshTokenRepository.save(token1);
        refreshTokenRepository.save(token2);

        // Then
        Optional<RefreshToken> found1 = refreshTokenRepository.findByToken("token_1");
        Optional<RefreshToken> found2 = refreshTokenRepository.findByToken("token_2");
        
        assertTrue(found1.isPresent());
        assertTrue(found2.isPresent());
        assertEquals(userId, found1.get().getUserId());
        assertEquals(userId, found2.get().getUserId());
    }

    @Test
    void shouldUpdateExistingToken() {
        // Given
        UserId userId = UserId.generate();
        String tokenId = UUID.randomUUID().toString();
        String tokenValue = "refresh_token_abc123";
        Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);
        RefreshToken token = new RefreshToken(tokenId, userId, tokenValue, expiresAt);
        refreshTokenRepository.save(token);

        // When - Save again with same ID but different expiration
        Instant newExpiresAt = Instant.now().plus(14, ChronoUnit.DAYS);
        RefreshToken updatedToken = new RefreshToken(tokenId, userId, tokenValue, newExpiresAt);
        refreshTokenRepository.save(updatedToken);

        // Then
        Optional<RefreshToken> found = refreshTokenRepository.findByToken(tokenValue);
        assertTrue(found.isPresent());
        assertEquals(newExpiresAt.truncatedTo(ChronoUnit.MILLIS), 
                     found.get().getExpiresAt().truncatedTo(ChronoUnit.MILLIS));
    }
}
