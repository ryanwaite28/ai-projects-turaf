package com.turaf.identity.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserJpaRepository extends JpaRepository<UserJpaEntity, String> {
    
    Optional<UserJpaEntity> findByEmail(String email);
    
    boolean existsByEmail(String email);
}
