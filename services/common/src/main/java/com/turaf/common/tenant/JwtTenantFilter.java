package com.turaf.common.tenant;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Filter that extracts tenant context from JWT token instead of HTTP headers.
 * This is more secure as the tenant information is cryptographically signed.
 * 
 * The JWT token should contain:
 * - sub: User ID
 * - organizationId: Organization/Tenant ID
 * 
 * Falls back to header-based extraction if JWT is not present (for backward compatibility).
 */
@Component
public class JwtTenantFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtTenantFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ORGANIZATION_ID_CLAIM = "organizationId";
    
    // Fallback headers for backward compatibility
    private static final String HEADER_ORGANIZATION_ID = "X-Organization-Id";
    private static final String HEADER_USER_ID = "X-User-Id";
    
    private final SecretKey secretKey;
    
    public JwtTenantFilter(@Value("${jwt.secret}") String jwtSecret) {
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        try {
            String organizationId = null;
            String userId = null;
            
            // Try to extract from JWT first
            String token = extractJwtToken(request);
            if (token != null) {
                try {
                    Claims claims = parseJwt(token);
                    organizationId = claims.get(ORGANIZATION_ID_CLAIM, String.class);
                    userId = claims.getSubject();
                    
                    logger.debug("Extracted tenant context from JWT - Org: {}, User: {}", 
                        organizationId, userId);
                } catch (Exception e) {
                    logger.warn("Failed to parse JWT token: {}", e.getMessage());
                    // Fall through to header-based extraction
                }
            }
            
            // Fallback to header-based extraction if JWT extraction failed
            if (organizationId == null) {
                organizationId = request.getHeader(HEADER_ORGANIZATION_ID);
                userId = request.getHeader(HEADER_USER_ID);
                
                if (organizationId != null) {
                    logger.debug("Extracted tenant context from headers - Org: {}, User: {}", 
                        organizationId, userId);
                }
            }
            
            // Set tenant context if we have organization ID
            if (organizationId != null && !organizationId.isEmpty()) {
                TenantContext context = new TenantContext(organizationId, userId);
                TenantContextHolder.setContext(context);
                
                logger.trace("Tenant context set for request: {}", context);
            } else {
                logger.debug("No tenant context found in request");
            }
            
            filterChain.doFilter(request, response);
            
        } finally {
            // Always clear context after request
            TenantContextHolder.clear();
            logger.trace("Tenant context cleared");
        }
    }
    
    /**
     * Extracts JWT token from Authorization header.
     *
     * @param request HTTP request
     * @return JWT token string or null if not found
     */
    private String extractJwtToken(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }
        
        return null;
    }
    
    /**
     * Parses JWT token and extracts claims.
     *
     * @param token JWT token string
     * @return Claims from the token
     * @throws io.jsonwebtoken.JwtException if token is invalid
     */
    private Claims parseJwt(String token) {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // Skip filter for public endpoints
        return path.startsWith("/actuator/") ||
               path.startsWith("/swagger-ui/") ||
               path.startsWith("/v3/api-docs/") ||
               path.equals("/api/v1/auth/register") ||
               path.equals("/api/v1/auth/login") ||
               path.equals("/api/v1/auth/refresh");
    }
}
