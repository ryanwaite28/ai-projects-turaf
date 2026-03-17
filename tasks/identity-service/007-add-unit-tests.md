# Task: Add Unit Tests

**Service**: Identity Service  
**Phase**: 2  
**Estimated Time**: 3 hours  

## Objective

Create comprehensive unit tests for domain model, application services, and infrastructure components.

## Prerequisites

- [x] All identity-service implementation tasks completed

## Scope

**Test Files to Create**:
- Domain tests (already created in task 001)
- `AuthenticationServiceTest.java`
- `TokenServiceTest.java`
- `JwtTokenProviderTest.java`
- `UserRepositoryImplTest.java`

## Implementation Details

### Authentication Service Test

```java
@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @InjectMocks
    private AuthenticationService authenticationService;
    
    @Test
    void register_WithValidData_ShouldCreateUser() {
        RegisterRequest request = new RegisterRequest("test@example.com", "password123", "Test User");
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        
        UserDto result = authenticationService.register(request);
        
        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
        verify(userRepository).save(any(User.class));
    }
    
    @Test
    void register_WithDuplicateEmail_ShouldThrowException() {
        RegisterRequest request = new RegisterRequest("test@example.com", "password123", "Test User");
        when(userRepository.existsByEmail(any())).thenReturn(true);
        
        assertThrows(UserAlreadyExistsException.class, () -> 
            authenticationService.register(request));
    }
    
    @Test
    void login_WithValidCredentials_ShouldReturnUser() {
        LoginRequest request = new LoginRequest("test@example.com", "password123");
        User user = createTestUser();
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(any(), any())).thenReturn(true);
        
        UserDto result = authenticationService.login(request);
        
        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
    }
    
    @Test
    void login_WithInvalidPassword_ShouldThrowException() {
        LoginRequest request = new LoginRequest("test@example.com", "wrongpassword");
        User user = createTestUser();
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);
        
        assertThrows(InvalidCredentialsException.class, () -> 
            authenticationService.login(request));
    }
}
```

### Token Service Test

```java
@ExtendWith(MockitoExtension.class)
class TokenServiceTest {
    @Mock
    private JwtTokenProvider tokenProvider;
    
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private TokenService tokenService;
    
    @Test
    void generateTokens_ShouldCreateAccessAndRefreshTokens() {
        UserId userId = UserId.generate();
        User user = createTestUser(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tokenProvider.generateAccessToken(any(), any(), any())).thenReturn("access-token");
        when(tokenProvider.generateRefreshToken()).thenReturn("refresh-token");
        
        TokenResponse result = tokenService.generateTokens(userId, "org-id");
        
        assertNotNull(result);
        assertEquals("access-token", result.getAccessToken());
        assertEquals("refresh-token", result.getRefreshToken());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }
    
    @Test
    void refreshAccessToken_WithValidToken_ShouldReturnNewAccessToken() {
        RefreshToken refreshToken = createTestRefreshToken();
        User user = createTestUser(refreshToken.getUserId());
        when(refreshTokenRepository.findByToken(any())).thenReturn(Optional.of(refreshToken));
        when(userRepository.findById(any())).thenReturn(Optional.of(user));
        when(tokenProvider.generateAccessToken(any(), any(), any())).thenReturn("new-access-token");
        
        RefreshTokenRequest request = new RefreshTokenRequest("refresh-token");
        TokenResponse result = tokenService.refreshAccessToken(request);
        
        assertNotNull(result);
        assertEquals("new-access-token", result.getAccessToken());
    }
    
    @Test
    void refreshAccessToken_WithExpiredToken_ShouldThrowException() {
        RefreshToken expiredToken = createExpiredRefreshToken();
        when(refreshTokenRepository.findByToken(any())).thenReturn(Optional.of(expiredToken));
        
        RefreshTokenRequest request = new RefreshTokenRequest("expired-token");
        
        assertThrows(InvalidTokenException.class, () -> 
            tokenService.refreshAccessToken(request));
    }
}
```

## Acceptance Criteria

- [ ] All domain model tests pass
- [ ] All application service tests pass
- [ ] All infrastructure tests pass
- [ ] Code coverage > 80%
- [ ] All edge cases covered
- [ ] Mock dependencies properly
- [ ] Tests are isolated and independent

## Testing Requirements

**Unit Test Coverage**:
- Domain entities and value objects
- Application services
- JWT token provider
- Repository implementations
- Exception scenarios

## References

- Specification: `specs/identity-service.md`
- Related Tasks: 008-add-integration-tests
