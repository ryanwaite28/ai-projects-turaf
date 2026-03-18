package com.turaf.identity.infrastructure.persistence;

import com.turaf.identity.domain.RefreshToken;
import com.turaf.identity.domain.RefreshTokenRepository;
import com.turaf.identity.domain.UserId;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Repository
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpaRepository;

    public RefreshTokenRepositoryImpl(RefreshTokenJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return jpaRepository.findByToken(token)
            .map(RefreshTokenJpaEntity::toDomain);
    }

    @Override
    public void save(RefreshToken refreshToken) {
        RefreshTokenJpaEntity entity = RefreshTokenJpaEntity.fromDomain(refreshToken);
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
