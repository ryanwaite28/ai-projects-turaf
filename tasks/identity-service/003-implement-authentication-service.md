# Task: Implement Authentication Service

**Service**: Identity Service  
**Phase**: 2  
**Estimated Time**: 3-4 hours  

## Objective

Implement the application layer authentication service that handles user registration, login, and password management use cases.

## Prerequisites

- [x] Task 001: Domain model created
- [x] Task 002: Repositories implemented

## Scope

**Files to Create**:
- `services/identity-service/src/main/java/com/turaf/identity/application/AuthenticationService.java`
- `services/identity-service/src/main/java/com/turaf/identity/application/dto/RegisterRequest.java`
- `services/identity-service/src/main/java/com/turaf/identity/application/dto/LoginRequest.java`
- `services/identity-service/src/main/java/com/turaf/identity/application/dto/ChangePasswordRequest.java`
- `services/identity-service/src/main/java/com/turaf/identity/application/dto/UserDto.java`
- `services/identity-service/src/main/java/com/turaf/identity/application/exception/UserAlreadyExistsException.java`
- `services/identity-service/src/main/java/com/turaf/identity/application/exception/InvalidCredentialsException.java`

## Implementation Details

### Authentication Service

```java
@Service
@Transactional
public class AuthenticationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    public UserDto register(RegisterRequest request) {
        Email email = new Email(request.getEmail());
        
        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("User with email already exists");
        }
        
        UserId userId = UserId.generate();
        Password password = Password.fromRaw(request.getPassword(), passwordEncoder);
        
        User user = new User(userId, email, password, request.getName());
        User savedUser = userRepository.save(user);
        
        return UserDto.fromDomain(savedUser);
    }
    
    public UserDto login(LoginRequest request) {
        Email email = new Email(request.getEmail());
        
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));
        
        if (!user.verifyPassword(request.getPassword())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }
        
        return UserDto.fromDomain(user);
    }
    
    public void changePassword(UserId userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        if (!user.verifyPassword(request.getCurrentPassword())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }
        
        Password newPassword = Password.fromRaw(request.getNewPassword(), passwordEncoder);
        user.updatePassword(newPassword);
        userRepository.save(user);
    }
    
    public UserDto getUserById(UserId userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found"));
        return UserDto.fromDomain(user);
    }
}
```

### DTOs

```java
public class RegisterRequest {
    @NotBlank
    @Email
    private String email;
    
    @NotBlank
    @Size(min = 8)
    private String password;
    
    @NotBlank
    private String name;
    
    // Getters, setters
}

public class LoginRequest {
    @NotBlank
    private String email;
    
    @NotBlank
    private String password;
    
    // Getters, setters
}

public class UserDto {
    private String id;
    private String email;
    private String name;
    private Instant createdAt;
    
    public static UserDto fromDomain(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId().getValue());
        dto.setEmail(user.getEmail().getValue());
        dto.setName(user.getName());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }
}
```

## Acceptance Criteria

- [ ] Register creates new user with hashed password
- [ ] Register validates email uniqueness
- [ ] Login verifies credentials
- [ ] Login throws exception for invalid credentials
- [ ] Change password verifies current password
- [ ] Change password updates with new hashed password
- [ ] All validation rules enforced
- [ ] Unit tests pass

## Testing Requirements

**Unit Tests**:
- Test successful registration
- Test registration with duplicate email
- Test successful login
- Test login with invalid credentials
- Test change password with correct current password
- Test change password with incorrect current password

**Test Files to Create**:
- `AuthenticationServiceTest.java`

## References

- Specification: `specs/identity-service.md` (Application Services section)
- Related Tasks: 004-implement-jwt-token-service
