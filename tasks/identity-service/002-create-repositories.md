# Task: Create Identity Service Repositories

**Service**: Identity Service  
**Phase**: 2  
**Estimated Time**: 2-3 hours  

## Objective

Implement repository interfaces for User and RefreshToken entities using Spring Data JPA, including JPA entity mappings and database schema.

## Prerequisites

- [x] Task 001: Domain model created

## Scope

**Files to Create**:
- `services/identity-service/src/main/java/com/turaf/identity/infrastructure/persistence/UserJpaEntity.java`
- `services/identity-service/src/main/java/com/turaf/identity/infrastructure/persistence/RefreshTokenJpaEntity.java`
- `services/identity-service/src/main/java/com/turaf/identity/infrastructure/persistence/UserJpaRepository.java`
- `services/identity-service/src/main/java/com/turaf/identity/infrastructure/persistence/RefreshTokenJpaRepository.java`
- `services/identity-service/src/main/java/com/turaf/identity/infrastructure/persistence/UserRepositoryImpl.java`
- `services/identity-service/src/main/java/com/turaf/identity/infrastructure/persistence/RefreshTokenRepositoryImpl.java`
- `services/identity-service/src/main/resources/db/migration/V001__create_users_table.sql`
- `services/identity-service/src/main/resources/db/migration/V002__create_refresh_tokens_table.sql`

## Implementation Details

### JPA Entities

```java
@Entity
@Table(name = "users")
public class UserJpaEntity {
    @Id
    private String id;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String password;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private Instant createdAt;
    
    @Column(nullable = false)
    private Instant updatedAt;
    
    // Constructors, getters, setters
    
    public User toDomain(PasswordEncoder encoder) {
        return new User(
            new UserId(id),
            new Email(email),
            Password.fromHashed(password, encoder),
            name
        );
    }
    
    public static UserJpaEntity fromDomain(User user) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.setId(user.getId().getValue());
        entity.setEmail(user.getEmail().getValue());
        entity.setPassword(user.getPassword().getHashedValue());
        entity.setName(user.getName());
        entity.setCreatedAt(user.getCreatedAt());
        entity.setUpdatedAt(user.getUpdatedAt());
        return entity;
    }
}
```

### Repository Implementation

```java
@Repository
public class UserRepositoryImpl implements UserRepository {
    private final UserJpaRepository jpaRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    public Optional<User> findById(UserId id) {
        return jpaRepository.findById(id.getValue())
            .map(entity -> entity.toDomain(passwordEncoder));
    }
    
    @Override
    public Optional<User> findByEmail(Email email) {
        return jpaRepository.findByEmail(email.getValue())
            .map(entity -> entity.toDomain(passwordEncoder));
    }
    
    @Override
    public User save(User user) {
        UserJpaEntity entity = UserJpaEntity.fromDomain(user);
        UserJpaEntity saved = jpaRepository.save(entity);
        return saved.toDomain(passwordEncoder);
    }
    
    @Override
    public boolean existsByEmail(Email email) {
        return jpaRepository.existsByEmail(email.getValue());
    }
}
```

### Database Migrations

```sql
-- V001__create_users_table.sql
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_users_email ON users(email);

-- V002__create_refresh_tokens_table.sql
CREATE TABLE refresh_tokens (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    token VARCHAR(500) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
```

## Acceptance Criteria

- [ ] JPA entities map to domain entities
- [ ] Repository implementations delegate to Spring Data JPA
- [ ] Database migrations create proper schema
- [ ] Indexes created for performance
- [ ] Foreign key constraints enforced
- [ ] Domain-to-JPA and JPA-to-domain conversions work correctly
- [ ] Integration tests pass

## Testing Requirements

**Integration Tests**:
- Test save and retrieve user
- Test find user by email
- Test check email exists
- Test save and retrieve refresh token
- Test delete expired tokens

**Test Files to Create**:
- `UserRepositoryImplTest.java`
- `RefreshTokenRepositoryImplTest.java`

## References

- Specification: `specs/identity-service.md` (Database Schema section)
- Related Tasks: 001-create-domain-model, 003-implement-authentication-service
