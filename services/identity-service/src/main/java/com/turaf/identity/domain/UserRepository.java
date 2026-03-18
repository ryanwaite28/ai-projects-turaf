package com.turaf.identity.domain;

import com.turaf.common.domain.Repository;

import java.util.Optional;

public interface UserRepository extends Repository<User, UserId> {
    
    Optional<User> findByEmail(Email email);
    
    boolean existsByEmail(Email email);
}
