package com.turaf.identity.application;

import com.turaf.identity.application.dto.RefreshTokenRequest;
import com.turaf.identity.application.dto.TokenResponse;
import com.turaf.identity.application.exception.InvalidTokenException;
import com.turaf.identity.application.exception.UserNotFoundException;
import com.turaf.identity.domain.*;
import com.turaf.identity.infrastructure.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    private TokenService tokenService;

    private static final long REFRESH_TOKEN_EXPIRATION = 604800000L; // 7 days
    private static final long ACCESS_TOKEN_EXPIRATION = 900000L; // 15 minutes

    @BeforeEach
    void setUp() {
        tokenService = new TokenService(
            tokenProvider,
            refreshTokenRepository,
            userRepository,
            REFRESH_TOKEN_EXPIRATION
        );
    }

    @Test
    void shouldGenerateTokens() {
        // Given
        UserId userId = UserId.generate();
        String organizationId = "org-123";
        Email email = new Email("test@example.com");
        PasswordEncoder mockEncoder = mock(PasswordEncoder.class);
        when(mockEncoder.encode(anyString())).thenReturn("hashed");
        Password password = Password.fromRaw("SecureP@ss123", mockEncoder);
        User user = new User(userId, organizationId, email, password, "testuser", "Test", "User");

        String accessToken = "access.token.here";
        String refreshToken = "refresh-token-uuid";

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tokenProvider.generateAccessToken(userId, email.getValue(), organizationId))
            .thenReturn(accessToken);
        when(tokenProvider.generateRefreshToken()).thenReturn(refreshToken);
        when(tokenProvider.getAccessTokenExpiration()).thenReturn(ACCESS_TOKEN_EXPIRATION);

        // When
        TokenResponse response = tokenService.generateTokens(userId, organizationId);

        // Then
        assertNotNull(response);
        assertEquals(accessToken, response.getAccessToken());
        assertEquals(refreshToken, response.getRefreshToken());
        assertEquals(ACCESS_TOKEN_EXPIRATION / 1000, response.getExpiresIn());
        assertEquals("Bearer", response.getTokenType());

        verify(userRepository).findById(userId);
        verify(tokenProvider).generateAccessToken(userId, email.getValue(), organizationId);
        verify(tokenProvider).generateRefreshToken();
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void shouldSaveRefreshTokenWhenGeneratingTokens() {
        // Given
        UserId userId = UserId.generate();
        String organizationId = "org-123";
        Email email = new Email("test@example.com");
        PasswordEncoder mockEncoder = mock(PasswordEncoder.class);
        when(mockEncoder.encode(anyString())).thenReturn("hashed");
        Password password = Password.fromRaw("SecureP@ss123", mockEncoder);
        User user = new User(userId, organizationId, email, password, "testuser", "Test", "User");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tokenProvider.generateAccessToken(any(), any(), any())).thenReturn("access.token");
        when(tokenProvider.generateRefreshToken()).thenReturn("refresh-token");
        when(tokenProvider.getAccessTokenExpiration()).thenReturn(ACCESS_TOKEN_EXPIRATION);

        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);

        // When
        tokenService.generateTokens(userId, organizationId);

        // Then
        verify(refreshTokenRepository).save(tokenCaptor.capture());
        RefreshToken savedToken = tokenCaptor.getValue();
        
        assertNotNull(savedToken);
        assertEquals(userId, savedToken.getUserId());
        assertEquals("refresh-token", savedToken.getToken());
        assertNotNull(savedToken.getExpiresAt());
        assertTrue(savedToken.getExpiresAt().isAfter(Instant.now()));
    }

    @Test
    void shouldThrowExceptionWhenGeneratingTokensForNonExistentUser() {
        // Given
        UserId userId = UserId.generate();
        String organizationId = "org-123";
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> {
            tokenService.generateTokens(userId, organizationId);
        });

        assertEquals("User not found", exception.getMessage());
        verify(userRepository).findById(userId);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void shouldRefreshAccessToken() {
        // Given
        UserId userId = UserId.generate();
        String refreshTokenValue = "refresh-token-uuid";
        Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);
        RefreshToken refreshToken = new RefreshToken(
            UUID.randomUUID().toString(),
            userId,
            refreshTokenValue,
            expiresAt
        );

        Email email = new Email("test@example.com");
        PasswordEncoder mockEncoder = mock(PasswordEncoder.class);
        when(mockEncoder.encode(anyString())).thenReturn("hashed");
        Password password = Password.fromRaw("SecureP@ss123", mockEncoder);
        User user = new User(userId, "org-123", email, password, "testuser", "Test", "User");

        String newAccessToken = "new.access.token";
        RefreshTokenRequest request = new RefreshTokenRequest(refreshTokenValue);

        when(refreshTokenRepository.findByToken(refreshTokenValue)).thenReturn(Optional.of(refreshToken));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tokenProvider.generateAccessToken(userId, email.getValue(), "org-123"))
            .thenReturn(newAccessToken);
        when(tokenProvider.getAccessTokenExpiration()).thenReturn(ACCESS_TOKEN_EXPIRATION);

        // When
        TokenResponse response = tokenService.refreshAccessToken(request);

        // Then
        assertNotNull(response);
        assertEquals(newAccessToken, response.getAccessToken());
        assertEquals(refreshTokenValue, response.getRefreshToken());
        assertEquals(ACCESS_TOKEN_EXPIRATION / 1000, response.getExpiresIn());

        verify(refreshTokenRepository).findByToken(refreshTokenValue);
        verify(userRepository).findById(userId);
        verify(tokenProvider).generateAccessToken(userId, email.getValue(), "org-123");
    }

    @Test
    void shouldThrowExceptionWhenRefreshingWithInvalidToken() {
        // Given
        String invalidToken = "invalid-token";
        RefreshTokenRequest request = new RefreshTokenRequest(invalidToken);
        when(refreshTokenRepository.findByToken(invalidToken)).thenReturn(Optional.empty());

        // When & Then
        InvalidTokenException exception = assertThrows(InvalidTokenException.class, () -> {
            tokenService.refreshAccessToken(request);
        });

        assertEquals("Invalid refresh token", exception.getMessage());
        verify(refreshTokenRepository).findByToken(invalidToken);
        verify(userRepository, never()).findById(any());
    }

    @Test
    void shouldThrowExceptionWhenRefreshingWithExpiredToken() {
        // Given
        UserId userId = UserId.generate();
        String refreshTokenValue = "expired-token";
        Instant expiresAt = Instant.now().minus(1, ChronoUnit.DAYS);
        RefreshToken refreshToken = new RefreshToken(
            UUID.randomUUID().toString(),
            userId,
            refreshTokenValue,
            expiresAt
        );

        RefreshTokenRequest request = new RefreshTokenRequest(refreshTokenValue);
        when(refreshTokenRepository.findByToken(refreshTokenValue)).thenReturn(Optional.of(refreshToken));

        // When & Then
        InvalidTokenException exception = assertThrows(InvalidTokenException.class, () -> {
            tokenService.refreshAccessToken(request);
        });

        assertEquals("Refresh token expired", exception.getMessage());
        verify(refreshTokenRepository).findByToken(refreshTokenValue);
        verify(userRepository, never()).findById(any());
    }

    @Test
    void shouldThrowExceptionWhenRefreshingForNonExistentUser() {
        // Given
        UserId userId = UserId.generate();
        String refreshTokenValue = "refresh-token-uuid";
        Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);
        RefreshToken refreshToken = new RefreshToken(
            UUID.randomUUID().toString(),
            userId,
            refreshTokenValue,
            expiresAt
        );

        RefreshTokenRequest request = new RefreshTokenRequest(refreshTokenValue);
        when(refreshTokenRepository.findByToken(refreshTokenValue)).thenReturn(Optional.of(refreshToken));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> {
            tokenService.refreshAccessToken(request);
        });

        assertEquals("User not found", exception.getMessage());
        verify(refreshTokenRepository).findByToken(refreshTokenValue);
        verify(userRepository).findById(userId);
    }

    @Test
    void shouldRevokeRefreshToken() {
        // Given
        UserId userId = UserId.generate();

        // When
        tokenService.revokeRefreshToken(userId);

        // Then
        verify(refreshTokenRepository).deleteByUserId(userId);
    }

    @Test
    void shouldRevokeAllRefreshTokens() {
        // Given
        UserId userId = UserId.generate();

        // When
        tokenService.revokeAllRefreshTokens(userId);

        // Then
        verify(refreshTokenRepository).deleteByUserId(userId);
    }

    @Test
    void shouldCleanupExpiredTokens() {
        // When
        tokenService.cleanupExpiredTokens();

        // Then
        verify(refreshTokenRepository).deleteExpiredTokens();
    }

    @Test
    void shouldKeepSameRefreshTokenWhenRefreshing() {
        // Given
        UserId userId = UserId.generate();
        String refreshTokenValue = "refresh-token-uuid";
        Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);
        RefreshToken refreshToken = new RefreshToken(
            UUID.randomUUID().toString(),
            userId,
            refreshTokenValue,
            expiresAt
        );

        Email email = new Email("test@example.com");
        PasswordEncoder mockEncoder = mock(PasswordEncoder.class);
        when(mockEncoder.encode(anyString())).thenReturn("hashed");
        Password password = Password.fromRaw("SecureP@ss123", mockEncoder);
        User user = new User(userId, "org-123", email, password, "testuser", "Test", "User");

        RefreshTokenRequest request = new RefreshTokenRequest(refreshTokenValue);

        when(refreshTokenRepository.findByToken(refreshTokenValue)).thenReturn(Optional.of(refreshToken));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tokenProvider.generateAccessToken(any(), any(), any())).thenReturn("new.access.token");
        when(tokenProvider.getAccessTokenExpiration()).thenReturn(ACCESS_TOKEN_EXPIRATION);

        // When
        TokenResponse response = tokenService.refreshAccessToken(request);

        // Then
        assertEquals(refreshTokenValue, response.getRefreshToken());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void shouldSetCorrectExpirationOnRefreshToken() {
        // Given
        UserId userId = UserId.generate();
        String organizationId = "org-123";
        Email email = new Email("test@example.com");
        PasswordEncoder mockEncoder = mock(PasswordEncoder.class);
        when(mockEncoder.encode(anyString())).thenReturn("hashed");
        Password password = Password.fromRaw("SecureP@ss123", mockEncoder);
        User user = new User(userId, organizationId, email, password, "testuser", "Test", "User");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tokenProvider.generateAccessToken(any(), any(), any())).thenReturn("access.token");
        when(tokenProvider.generateRefreshToken()).thenReturn("refresh-token");
        when(tokenProvider.getAccessTokenExpiration()).thenReturn(ACCESS_TOKEN_EXPIRATION);

        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);

        Instant before = Instant.now();

        // When
        tokenService.generateTokens(userId, organizationId);

        Instant after = Instant.now();

        // Then
        verify(refreshTokenRepository).save(tokenCaptor.capture());
        RefreshToken savedToken = tokenCaptor.getValue();
        
        Instant expectedExpiration = before.plusMillis(REFRESH_TOKEN_EXPIRATION);
        assertTrue(savedToken.getExpiresAt().isAfter(expectedExpiration.minusSeconds(1)));
        assertTrue(savedToken.getExpiresAt().isBefore(after.plusMillis(REFRESH_TOKEN_EXPIRATION).plusSeconds(1)));
    }
}
