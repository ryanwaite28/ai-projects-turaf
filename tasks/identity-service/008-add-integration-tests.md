# Task: Add Integration Tests

**Service**: Identity Service  
**Phase**: 2  
**Estimated Time**: 3 hours  

## Objective

Create integration tests that verify the complete authentication flow from API endpoints through to database.

## Prerequisites

- [x] All identity-service implementation tasks completed
- [x] Task 007: Unit tests added

## Scope

**Test Files to Create**:
- `AuthControllerIntegrationTest.java`
- `UserControllerIntegrationTest.java`
- `AuthenticationFlowIntegrationTest.java`

## Implementation Details

### Auth Controller Integration Test

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase
@Transactional
class AuthControllerIntegrationTest {
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private UserRepository userRepository;
    
    @Test
    void register_WithValidData_ShouldCreateUserAndReturnTokens() {
        RegisterRequestDto request = new RegisterRequestDto(
            "newuser@example.com",
            "SecurePass123!",
            "New User",
            "org-123"
        );
        
        ResponseEntity<LoginResponseDto> response = restTemplate.postForEntity(
            "/api/v1/auth/register",
            request,
            LoginResponseDto.class
        );
        
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getAccessToken());
        assertNotNull(response.getBody().getRefreshToken());
        assertEquals("newuser@example.com", response.getBody().getUser().getEmail());
        
        // Verify user saved in database
        Optional<User> savedUser = userRepository.findByEmail(new Email("newuser@example.com"));
        assertTrue(savedUser.isPresent());
    }
    
    @Test
    void register_WithDuplicateEmail_ShouldReturn409() {
        // Create existing user
        createTestUser("existing@example.com");
        
        RegisterRequestDto request = new RegisterRequestDto(
            "existing@example.com",
            "SecurePass123!",
            "Duplicate User",
            "org-123"
        );
        
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
            "/api/v1/auth/register",
            request,
            ErrorResponse.class
        );
        
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("USER_ALREADY_EXISTS", response.getBody().getCode());
    }
    
    @Test
    void login_WithValidCredentials_ShouldReturnTokens() {
        createTestUser("user@example.com", "password123");
        
        LoginRequestDto request = new LoginRequestDto(
            "user@example.com",
            "password123",
            "org-123"
        );
        
        ResponseEntity<LoginResponseDto> response = restTemplate.postForEntity(
            "/api/v1/auth/login",
            request,
            LoginResponseDto.class
        );
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody().getAccessToken());
        assertNotNull(response.getBody().getRefreshToken());
    }
    
    @Test
    void login_WithInvalidPassword_ShouldReturn401() {
        createTestUser("user@example.com", "password123");
        
        LoginRequestDto request = new LoginRequestDto(
            "user@example.com",
            "wrongpassword",
            "org-123"
        );
        
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
            "/api/v1/auth/login",
            request,
            ErrorResponse.class
        );
        
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("INVALID_CREDENTIALS", response.getBody().getCode());
    }
    
    @Test
    void refreshToken_WithValidToken_ShouldReturnNewAccessToken() {
        LoginResponseDto loginResponse = loginTestUser();
        
        RefreshTokenRequest request = new RefreshTokenRequest(loginResponse.getRefreshToken());
        
        ResponseEntity<TokenResponse> response = restTemplate.postForEntity(
            "/api/v1/auth/refresh",
            request,
            TokenResponse.class
        );
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody().getAccessToken());
    }
}
```

### Authentication Flow Integration Test

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase
@Transactional
class AuthenticationFlowIntegrationTest {
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void completeAuthenticationFlow_ShouldWork() {
        // 1. Register
        RegisterRequestDto registerRequest = new RegisterRequestDto(
            "flowtest@example.com",
            "SecurePass123!",
            "Flow Test User",
            "org-123"
        );
        
        ResponseEntity<LoginResponseDto> registerResponse = restTemplate.postForEntity(
            "/api/v1/auth/register",
            registerRequest,
            LoginResponseDto.class
        );
        
        assertEquals(HttpStatus.CREATED, registerResponse.getStatusCode());
        String accessToken = registerResponse.getBody().getAccessToken();
        String refreshToken = registerResponse.getBody().getRefreshToken();
        
        // 2. Access protected endpoint
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        
        ResponseEntity<UserDto> meResponse = restTemplate.exchange(
            "/api/v1/users/me",
            HttpMethod.GET,
            entity,
            UserDto.class
        );
        
        assertEquals(HttpStatus.OK, meResponse.getStatusCode());
        assertEquals("flowtest@example.com", meResponse.getBody().getEmail());
        
        // 3. Refresh token
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(refreshToken);
        ResponseEntity<TokenResponse> refreshResponse = restTemplate.postForEntity(
            "/api/v1/auth/refresh",
            refreshRequest,
            TokenResponse.class
        );
        
        assertEquals(HttpStatus.OK, refreshResponse.getStatusCode());
        String newAccessToken = refreshResponse.getBody().getAccessToken();
        assertNotEquals(accessToken, newAccessToken);
        
        // 4. Logout
        headers.setBearerAuth(newAccessToken);
        HttpEntity<Void> logoutEntity = new HttpEntity<>(headers);
        
        ResponseEntity<Void> logoutResponse = restTemplate.exchange(
            "/api/v1/auth/logout",
            HttpMethod.POST,
            logoutEntity,
            Void.class
        );
        
        assertEquals(HttpStatus.NO_CONTENT, logoutResponse.getStatusCode());
        
        // 5. Verify refresh token no longer works
        ResponseEntity<ErrorResponse> invalidRefreshResponse = restTemplate.postForEntity(
            "/api/v1/auth/refresh",
            refreshRequest,
            ErrorResponse.class
        );
        
        assertEquals(HttpStatus.UNAUTHORIZED, invalidRefreshResponse.getStatusCode());
    }
}
```

## Acceptance Criteria

- [ ] All API endpoints tested end-to-end
- [ ] Database interactions verified
- [ ] Complete authentication flow tested
- [ ] Error scenarios tested
- [ ] Security constraints verified
- [ ] All integration tests pass
- [ ] Tests use test database
- [ ] Tests are transactional and isolated

## Testing Requirements

**Integration Test Coverage**:
- Registration flow
- Login flow
- Token refresh flow
- Logout flow
- Protected endpoint access
- Error scenarios (duplicate user, invalid credentials, expired tokens)

## References

- Specification: `specs/identity-service.md`
- Related Tasks: All identity-service tasks
