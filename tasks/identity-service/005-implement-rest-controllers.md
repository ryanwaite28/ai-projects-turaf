# Task: Implement REST Controllers

**Service**: Identity Service  
**Phase**: 2  
**Estimated Time**: 3 hours  

## Objective

Implement REST API endpoints for authentication operations including register, login, refresh token, get user profile, change password, and logout.

## Prerequisites

- [x] Task 003: Authentication service implemented
- [x] Task 004: JWT token service implemented

## Scope

**Files to Create**:
- `services/identity-service/src/main/java/com/turaf/identity/interfaces/rest/AuthController.java`
- `services/identity-service/src/main/java/com/turaf/identity/interfaces/rest/UserController.java`
- `services/identity-service/src/main/java/com/turaf/identity/interfaces/rest/dto/RegisterRequestDto.java`
- `services/identity-service/src/main/java/com/turaf/identity/interfaces/rest/dto/LoginRequestDto.java`
- `services/identity-service/src/main/java/com/turaf/identity/interfaces/rest/dto/LoginResponseDto.java`
- `services/identity-service/src/main/java/com/turaf/identity/interfaces/rest/GlobalExceptionHandler.java`

## Implementation Details

### Auth Controller

```java
@RestController
@RequestMapping("/api/v1/auth")
@Validated
public class AuthController {
    private final AuthenticationService authenticationService;
    private final TokenService tokenService;
    
    @PostMapping("/register")
    public ResponseEntity<LoginResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
        UserDto user = authenticationService.register(toApplicationDto(request));
        TokenResponse tokens = tokenService.generateTokens(
            new UserId(user.getId()),
            request.getOrganizationId()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new LoginResponseDto(user, tokens));
    }
    
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        UserDto user = authenticationService.login(toApplicationDto(request));
        TokenResponse tokens = tokenService.generateTokens(
            new UserId(user.getId()),
            request.getOrganizationId()
        );
        
        return ResponseEntity.ok(new LoginResponseDto(user, tokens));
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        TokenResponse tokens = tokenService.refreshAccessToken(request);
        return ResponseEntity.ok(tokens);
    }
    
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UserPrincipal principal) {
        tokenService.revokeRefreshToken(new UserId(principal.getUserId()));
        return ResponseEntity.noContent().build();
    }
}
```

### User Controller

```java
@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("isAuthenticated()")
public class UserController {
    private final AuthenticationService authenticationService;
    
    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        UserDto user = authenticationService.getUserById(new UserId(principal.getUserId()));
        return ResponseEntity.ok(user);
    }
    
    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        authenticationService.changePassword(new UserId(principal.getUserId()), request);
        return ResponseEntity.noContent().build();
    }
}
```

### Global Exception Handler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        ErrorResponse error = new ErrorResponse("USER_ALREADY_EXISTS", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
    
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        ErrorResponse error = new ErrorResponse("INVALID_CREDENTIALS", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }
    
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(InvalidTokenException ex) {
        ErrorResponse error = new ErrorResponse("INVALID_TOKEN", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.toList());
        
        ErrorResponse error = new ErrorResponse("VALIDATION_ERROR", errors.toString());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}
```

### DTOs

```java
public class LoginResponseDto {
    private UserDto user;
    private String accessToken;
    private String refreshToken;
    private long expiresIn;
    
    public LoginResponseDto(UserDto user, TokenResponse tokens) {
        this.user = user;
        this.accessToken = tokens.getAccessToken();
        this.refreshToken = tokens.getRefreshToken();
        this.expiresIn = tokens.getExpiresIn();
    }
}

public class ErrorResponse {
    private String code;
    private String message;
    private Instant timestamp;
    
    public ErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
        this.timestamp = Instant.now();
    }
}
```

## Acceptance Criteria

- [x] POST /api/v1/auth/register endpoint works
- [x] POST /api/v1/auth/login endpoint works
- [x] POST /api/v1/auth/refresh endpoint works
- [x] POST /api/v1/auth/logout endpoint works
- [x] GET /api/v1/users/me endpoint works
- [x] PUT /api/v1/users/me/password endpoint works
- [x] Validation errors return 400 with details
- [x] Authentication errors return 401
- [x] Duplicate user returns 409
- [ ] All endpoints documented with OpenAPI

## Testing Requirements

**Integration Tests**:
- Test register endpoint with valid data
- Test register with duplicate email
- Test login with valid credentials
- Test login with invalid credentials
- Test refresh token endpoint
- Test get current user
- Test change password

**Test Files to Create**:
- `AuthControllerTest.java`
- `UserControllerTest.java`

## References

- Specification: `specs/identity-service.md` (API Endpoints section)
- Related Tasks: 006-add-security-configuration
