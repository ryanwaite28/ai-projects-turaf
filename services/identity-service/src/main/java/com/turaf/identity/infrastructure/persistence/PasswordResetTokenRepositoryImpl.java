package com.turaf.identity.infrastructure.persistence;

import com.turaf.identity.domain.PasswordResetToken;
import com.turaf.identity.domain.PasswordResetTokenRepository;
import com.turaf.identity.domain.UserId;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Repository
public class PasswordResetTokenRepositoryImpl implements PasswordResetTokenRepository {

    private final PasswordResetTokenJpaRepository jpaRepository;

    public PasswordResetTokenRepositoryImpl(PasswordResetTokenJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<PasswordResetToken> findByToken(String token) {
        return jpaRepository.findByToken(token)
            .map(PasswordResetTokenJpaEntity::toDomain);
    }

    @Override
    public void save(PasswordResetToken passwordResetToken) {
        PasswordResetTokenJpaEntity entity = PasswordResetTokenJpaEntity.fromDomain(passwordResetToken);
        jpaRepository.save(entity);
    }

    @Override
    @Transactional
    public void deleteByUserId(UserId userId) {
        jpaRepository.deleteByUserId(userId.getValue());
    }

    @Override
    @Transactional
    public void deleteExpiredTokens() {
        jpaRepository.deleteExpiredTokens(Instant.now());
    }
}
