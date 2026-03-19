package com.turaf.bff.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenValidatorTest {
    
    private JwtTokenValidator validator;
    private SecretKey secretKey;
    private static final String SECRET = "test-secret-key-that-is-at-least-256-bits-long-for-hs256";
    
    @BeforeEach
    void setUp() {
        validator = new JwtTokenValidator(SECRET);
        secretKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }
    
    @Test
    void testValidateToken_ValidToken() {
        String token = createValidToken("user-123", "org-123", "test@example.com", "Test User");
        
        assertTrue(validator.validateToken(token));
    }
    
    @Test
    void testValidateToken_InvalidToken() {
        String invalidToken = "invalid.token.here";
        
        assertFalse(validator.validateToken(invalidToken));
    }
    
    @Test
    void testExtractUserContext_Success() {
        String token = createValidToken("user-456", "org-456", "john@example.com", "John Doe");
        
        UserContext context = validator.extractUserContext(token);
        
        assertNotNull(context);
        assertEquals("user-456", context.getUserId());
        assertEquals("org-456", context.getOrganizationId());
        assertEquals("john@example.com", context.getEmail());
        assertEquals("John Doe", context.getName());
    }
    
    @Test
    void testIsTokenExpired_NotExpired() {
        String token = createValidToken("user-789", "org-789", "active@example.com", "Active User");
        
        assertFalse(validator.isTokenExpired(token));
    }
    
    @Test
    void testIsTokenExpired_Expired() {
        String expiredToken = createExpiredToken("user-999", "org-999");
        
        assertTrue(validator.isTokenExpired(expiredToken));
    }
    
    @Test
    void testExtractToken_ValidBearerToken() {
        String bearerToken = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9";
        
        String token = validator.extractToken(bearerToken);
        
        assertEquals("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9", token);
    }
    
    @Test
    void testExtractToken_NoBearerPrefix() {
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9";
        
        String extracted = validator.extractToken(token);
        
        assertNull(extracted);
    }
    
    @Test
    void testExtractToken_NullToken() {
        String extracted = validator.extractToken(null);
        
        assertNull(extracted);
    }
    
    private String createValidToken(String userId, String orgId, String email, String name) {
        return Jwts.builder()
            .subject(userId)
            .claim("organizationId", orgId)
            .claim("email", email)
            .claim("name", name)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + 86400000)) // 24 hours
            .signWith(secretKey)
            .compact();
    }
    
    private String createExpiredToken(String userId, String orgId) {
        return Jwts.builder()
            .subject(userId)
            .claim("organizationId", orgId)
            .issuedAt(new Date(System.currentTimeMillis() - 172800000)) // 2 days ago
            .expiration(new Date(System.currentTimeMillis() - 86400000)) // 1 day ago
            .signWith(secretKey)
            .compact();
    }
}
