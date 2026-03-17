# Task: Create Identity Service Domain Model

**Service**: Identity Service  
**Phase**: 2  
**Estimated Time**: 3 hours  

## Objective

Create the domain model for the Identity Service including User entity, value objects, and domain logic for user management and authentication.

## Prerequisites

- [x] Task 001: Clean Architecture layers established
- [x] Task 002: DDD patterns implemented
- [x] Task 003: Multi-tenant context setup

## Scope

**Files to Create**:
- `services/identity-service/src/main/java/com/turaf/identity/domain/User.java`
- `services/identity-service/src/main/java/com/turaf/identity/domain/UserId.java`
- `services/identity-service/src/main/java/com/turaf/identity/domain/Email.java`
- `services/identity-service/src/main/java/com/turaf/identity/domain/Password.java`
- `services/identity-service/src/main/java/com/turaf/identity/domain/RefreshToken.java`
- `services/identity-service/src/main/java/com/turaf/identity/domain/UserRepository.java`
- `services/identity-service/src/main/java/com/turaf/identity/domain/RefreshTokenRepository.java`

## Implementation Details

### User Entity (Aggregate Root)

```java
public class User extends AggregateRoot<UserId> {
    private Email email;
    private Password password;
    private String name;
    private Instant createdAt;
    private Instant updatedAt;
    
    public User(UserId id, Email email, Password password, String name) {
        super(id);
        this.email = Objects.requireNonNull(email);
        this.password = Objects.requireNonNull(password);
        this.name = Objects.requireNonNull(name);
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
    
    public void updatePassword(Password newPassword) {
        this.password = Objects.requireNonNull(newPassword);
        this.updatedAt = Instant.now();
    }
    
    public void updateProfile(String name) {
        this.name = Objects.requireNonNull(name);
        this.updatedAt = Instant.now();
    }
    
    public boolean verifyPassword(String rawPassword) {
        return password.matches(rawPassword);
    }
    
    // Getters
}
```

### UserId Value Object

```java
public class UserId extends ValueObject {
    private final String value;
    
    public UserId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("UserId cannot be null or blank");
        }
        this.value = value;
    }
    
    public static UserId generate() {
        return new UserId(UUID.randomUUID().toString());
    }
    
    @Override
    protected List<Object> getEqualityComponents() {
        return List.of(value);
    }
    
    public String getValue() {
        return value;
    }
}
```

### Email Value Object

```java
public class Email extends ValueObject {
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    
    private final String value;
    
    public Email(String value) {
        if (value == null || !EMAIL_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid email format");
        }
        this.value = value.toLowerCase();
    }
    
    @Override
    protected List<Object> getEqualityComponents() {
        return List.of(value);
    }
    
    public String getValue() {
        return value;
    }
}
```

### Password Value Object

```java
public class Password extends ValueObject {
    private final String hashedValue;
    private final PasswordEncoder encoder;
    
    private Password(String hashedValue, PasswordEncoder encoder) {
        this.hashedValue = hashedValue;
        this.encoder = encoder;
    }
    
    public static Password fromRaw(String rawPassword, PasswordEncoder encoder) {
        validatePasswordStrength(rawPassword);
        String hashed = encoder.encode(rawPassword);
        return new Password(hashed, encoder);
    }
    
    public static Password fromHashed(String hashedValue, PasswordEncoder encoder) {
        return new Password(hashedValue, encoder);
    }
    
    public boolean matches(String rawPassword) {
        return encoder.matches(rawPassword, hashedValue);
    }
    
    private static void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }
        // Add more validation rules
    }
    
    @Override
    protected List<Object> getEqualityComponents() {
        return List.of(hashedValue);
    }
    
    public String getHashedValue() {
        return hashedValue;
    }
}
```

### RefreshToken Entity

```java
public class RefreshToken extends Entity<String> {
    private UserId userId;
    private String token;
    private Instant expiresAt;
    private Instant createdAt;
    
    public RefreshToken(String id, UserId userId, String token, Instant expiresAt) {
        super(id);
        this.userId = Objects.requireNonNull(userId);
        this.token = Objects.requireNonNull(token);
        this.expiresAt = Objects.requireNonNull(expiresAt);
        this.createdAt = Instant.now();
    }
    
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
    
    // Getters
}
```

### Repository Interfaces

```java
public interface UserRepository extends Repository<User, UserId> {
    Optional<User> findByEmail(Email email);
    boolean existsByEmail(Email email);
}

public interface RefreshTokenRepository {
    Optional<RefreshToken> findByToken(String token);
    void save(RefreshToken refreshToken);
    void deleteByUserId(UserId userId);
    void deleteExpiredTokens();
}
```

## Acceptance Criteria

- [ ] User entity extends AggregateRoot
- [ ] UserId value object with UUID generation
- [ ] Email value object with validation
- [ ] Password value object with hashing and strength validation
- [ ] RefreshToken entity with expiration logic
- [ ] Repository interfaces defined
- [ ] All domain invariants enforced
- [ ] No infrastructure dependencies in domain layer
- [ ] Unit tests pass

## Testing Requirements

**Unit Tests**:
- Test User creation and password verification
- Test Email validation (valid/invalid formats)
- Test Password strength validation
- Test RefreshToken expiration logic
- Test UserId generation and equality

**Test Files to Create**:
- `UserTest.java`
- `EmailTest.java`
- `PasswordTest.java`
- `RefreshTokenTest.java`

## References

- Specification: `specs/identity-service.md` (Domain Model section)
- Specification: `specs/domain-model.md` (User entity)
- PROJECT.md: Section 40 (Identity Service)
- Related Tasks: 002-create-repositories
