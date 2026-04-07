package com.turaf.identity.domain;

import java.util.Optional;

public interface PasswordResetTokenRepository {

    Optional<PasswordResetToken> findByToken(String token);

    void save(PasswordResetToken passwordResetToken);

    void deleteByUserId(UserId userId);

    void deleteExpiredTokens();
}
