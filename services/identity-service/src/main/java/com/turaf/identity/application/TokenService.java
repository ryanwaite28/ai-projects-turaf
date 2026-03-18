package com.turaf.identity.application;

import com.turaf.identity.application.dto.RefreshTokenRequest;
import com.turaf.identity.application.dto.TokenResponse;
import com.turaf.identity.application.exception.InvalidTokenException;
import com.turaf.identity.application.exception.UserNotFoundException;
import com.turaf.identity.domain.*;
import com.turaf.identity.infrastructure.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class TokenService {

    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final long refreshTokenExpiration;

    public TokenService(
            JwtTokenProvider tokenProvider,
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration) {
        this.tokenProvider = tokenProvider;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public TokenResponse generateTokens(UserId userId, String organizationId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found"));

        String accessToken = tokenProvider.generateAccessToken(
            userId,
            user.getEmail().getValue(),
            organizationId
        );

        String refreshTokenValue = tokenProvider.generateRefreshToken();
        Instant expiresAt = Instant.now().plusMillis(refreshTokenExpiration);

        RefreshToken refreshToken = new RefreshToken(
            UUID.randomUUID().toString(),
            userId,
            refreshTokenValue,
            expiresAt
        );

        refreshTokenRepository.save(refreshToken);

        return new TokenResponse(
            accessToken,
            refreshTokenValue,
            tokenProvider.getAccessTokenExpiration() / 1000
        );
    }

    @Transactional(readOnly = true)
    public TokenResponse refreshAccessToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
            .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        if (refreshToken.isExpired()) {
            throw new InvalidTokenException("Refresh token expired");
        }

        User user = userRepository.findById(refreshToken.getUserId())
            .orElseThrow(() -> new UserNotFoundException("User not found"));

        String accessToken = tokenProvider.generateAccessToken(
            refreshToken.getUserId(),
            user.getEmail().getValue(),
            "default-org"
        );

        return new TokenResponse(
            accessToken,
            request.getRefreshToken(),
            tokenProvider.getAccessTokenExpiration() / 1000
        );
    }

    public void revokeRefreshToken(UserId userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    public void revokeAllRefreshTokens(UserId userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteExpiredTokens();
    }
}
