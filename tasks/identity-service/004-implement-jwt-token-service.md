# Task: Implement JWT Token Service

**Service**: Identity Service  
**Phase**: 2  
**Estimated Time**: 3 hours  

## Objective

Implement JWT token generation, validation, and refresh token management for authentication.

## Prerequisites

- [x] Task 001: Domain model created
- [x] Task 002: Repositories implemented

## Scope

**Files to Create**:
- `services/identity-service/src/main/java/com/turaf/identity/application/TokenService.java`
- `services/identity-service/src/main/java/com/turaf/identity/application/dto/TokenResponse.java`
- `services/identity-service/src/main/java/com/turaf/identity/application/dto/RefreshTokenRequest.java`
- `services/identity-service/src/main/java/com/turaf/identity/infrastructure/security/JwtTokenProvider.java`
- `services/identity-service/src/main/java/com/turaf/identity/application/exception/InvalidTokenException.java`

## Implementation Details

### JWT Token Provider

```java
@Component
public class JwtTokenProvider {
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;
    
    public String generateAccessToken(UserId userId, String email, String organizationId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpiration);
        
        return Jwts.builder()
            .setSubject(userId.getValue())
            .claim("email", email)
            .claim("organizationId", organizationId)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(SignatureAlgorithm.HS512, jwtSecret)
            .compact();
    }
    
    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }
    
    public UserId getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
            .setSigningKey(jwtSecret)
            .parseClaimsJws(token)
            .getBody();
        
        return new UserId(claims.getSubject());
    }
    
    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
```

### Token Service

```java
@Service
@Transactional
public class TokenService {
    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    
    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;
    
    public TokenResponse generateTokens(UserId userId, String organizationId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        String accessToken = tokenProvider.generateAccessToken(
            userId, 
            user.getEmail().getValue(),
            organizationId
        );
        
        String refreshTokenValue = tokenProvider.generateRefreshToken();
        Instant expiresAt = Instant.now().plusMillis(refreshTokenExpiration);
        
        RefreshToken refreshToken = new RefreshToken(
            UUID.randomUUID().toString(),
            userId,
            refreshTokenValue,
            expiresAt
        );
        
        refreshTokenRepository.save(refreshToken);
        
        return new TokenResponse(accessToken, refreshTokenValue, accessTokenExpiration / 1000);
    }
    
    public TokenResponse refreshAccessToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
            .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));
        
        if (refreshToken.isExpired()) {
            refreshTokenRepository.deleteByUserId(refreshToken.getUserId());
            throw new InvalidTokenException("Refresh token expired");
        }
        
        User user = userRepository.findById(refreshToken.getUserId())
            .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        // Generate new access token (keep same refresh token)
        String accessToken = tokenProvider.generateAccessToken(
            refreshToken.getUserId(),
            user.getEmail().getValue(),
            "organizationId" // TODO: Get from user's organization
        );
        
        return new TokenResponse(accessToken, request.getRefreshToken(), accessTokenExpiration / 1000);
    }
    
    public void revokeRefreshToken(UserId userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }
}
```

### DTOs

```java
public class TokenResponse {
    private String accessToken;
    private String refreshToken;
    private long expiresIn;
    
    // Constructor, getters, setters
}

public class RefreshTokenRequest {
    @NotBlank
    private String refreshToken;
    
    // Getters, setters
}
```

### Configuration

```yaml
# application.yml
jwt:
  secret: ${JWT_SECRET:your-secret-key-change-in-production}
  access-token-expiration: 900000  # 15 minutes
  refresh-token-expiration: 604800000  # 7 days
```

## Acceptance Criteria

- [ ] JWT access tokens generated with user claims
- [ ] Refresh tokens generated and stored
- [ ] Access token validation works
- [ ] Refresh token flow works correctly
- [ ] Expired refresh tokens rejected
- [ ] Refresh tokens can be revoked
- [ ] JWT secret configurable via environment
- [ ] Unit tests pass

## Testing Requirements

**Unit Tests**:
- Test access token generation
- Test refresh token generation
- Test token validation (valid/invalid/expired)
- Test refresh token flow
- Test expired refresh token rejection
- Test refresh token revocation

**Test Files to Create**:
- `JwtTokenProviderTest.java`
- `TokenServiceTest.java`

## References

- Specification: `specs/identity-service.md` (JWT Token Management section)
- Related Tasks: 005-implement-rest-controllers
