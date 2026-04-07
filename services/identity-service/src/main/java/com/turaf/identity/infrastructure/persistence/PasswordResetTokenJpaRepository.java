package com.turaf.identity.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface PasswordResetTokenJpaRepository extends JpaRepository<PasswordResetTokenJpaEntity, String> {

    Optional<PasswordResetTokenJpaEntity> findByToken(String token);

    void deleteByUserId(String userId);

    @Modifying
    @Query("DELETE FROM PasswordResetTokenJpaEntity prt WHERE prt.expiresAt < :now")
    void deleteExpiredTokens(@Param("now") Instant now);
}
