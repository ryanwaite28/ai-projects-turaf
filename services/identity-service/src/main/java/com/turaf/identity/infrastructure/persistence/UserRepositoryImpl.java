package com.turaf.identity.infrastructure.persistence;

import com.turaf.identity.domain.*;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository jpaRepository;
    private final PasswordEncoder passwordEncoder;

    public UserRepositoryImpl(UserJpaRepository jpaRepository, PasswordEncoder passwordEncoder) {
        this.jpaRepository = jpaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Optional<User> findById(UserId id) {
        return jpaRepository.findById(id.getValue())
            .map(entity -> entity.toDomain(passwordEncoder));
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity = UserJpaEntity.fromDomain(user);
        UserJpaEntity saved = jpaRepository.save(entity);
        return saved.toDomain(passwordEncoder);
    }

    @Override
    public void delete(User user) {
        jpaRepository.deleteById(user.getId().getValue());
    }

    @Override
    public List<User> findAll() {
        return jpaRepository.findAll().stream()
            .map(entity -> entity.toDomain(passwordEncoder))
            .collect(Collectors.toList());
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return jpaRepository.findByEmail(email.getValue())
            .map(entity -> entity.toDomain(passwordEncoder));
    }

    @Override
    public boolean existsByEmail(Email email) {
        return jpaRepository.existsByEmail(email.getValue());
    }
}
