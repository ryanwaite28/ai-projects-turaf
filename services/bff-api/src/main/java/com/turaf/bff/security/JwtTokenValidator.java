package com.turaf.bff.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenValidator {
    
    private final SecretKey secretKey;
    
    public JwtTokenValidator(@Value("${jwt.secret-key}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
    
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }
    
    public UserContext extractUserContext(String token) {
        Claims claims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
        
        String userId = claims.getSubject();
        String organizationId = claims.get("organizationId", String.class);
        String email = claims.get("email", String.class);
        String username = claims.get("username", String.class);
        String firstName = claims.get("firstName", String.class);
        String lastName = claims.get("lastName", String.class);
        
        return UserContext.builder()
            .userId(userId)
            .organizationId(organizationId)
            .email(email)
            .username(username)
            .firstName(firstName)
            .lastName(lastName)
            .build();
    }
    
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
            
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
    
    public String extractToken(String bearerToken) {
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * Returns the expiry of the token as an {@link Instant}, or
     * {@link Instant#now()} if the token cannot be parsed (treat as already expired).
     */
    public Instant extractExpiry(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
            return claims.getExpiration().toInstant();
        } catch (Exception e) {
            return Instant.now();
        }
    }
}
