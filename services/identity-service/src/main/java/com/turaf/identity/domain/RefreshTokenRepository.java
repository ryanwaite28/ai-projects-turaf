package com.turaf.identity.domain;

import java.util.Optional;

public interface RefreshTokenRepository {
    
    Optional<RefreshToken> findByToken(String token);
    
    void save(RefreshToken refreshToken);
    
    void deleteByUserId(UserId userId);
    
    void deleteExpiredTokens();
}
