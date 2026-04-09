package com.turaf.common.security;

import com.turaf.common.tenant.TenantContext;
import com.turaf.common.tenant.TenantContextHolder;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Shared JWT authentication filter for downstream microservices.
 *
 * Parses the Bearer token from the Authorization header, validates it,
 * and sets the authenticated {@link UserPrincipal} in the Spring Security
 * context so that {@code @AuthenticationPrincipal} works in controllers.
 *
 * Falls back to {@code X-User-Id} / {@code X-Organization-Id} headers for
 * internal service-to-service calls that forward identity but not the full JWT.
 *
 * Also populates {@link TenantContextHolder} so tenant-scoped queries work
 * without a separate filter.
 *
 * Auto-registered as a servlet filter in every service that scans
 * {@code com.turaf} (all services except the BFF, which has its own filter).
 */
@Component
public class ServiceJwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ServiceJwtAuthenticationFilter.class);

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_ORGANIZATION_ID = "X-Organization-Id";
    private static final String CLAIM_ORGANIZATION_ID = "organizationId";

    private final SecretKey secretKey;

    public ServiceJwtAuthenticationFilter(@Value("${jwt.secret}") String jwtSecret) {
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            String userId = null;
            String organizationId = null;

            // --- 1. Try JWT Bearer token ---
            String authHeader = request.getHeader(AUTHORIZATION_HEADER);
            if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
                String token = authHeader.substring(BEARER_PREFIX.length());
                try {
                    Claims claims = Jwts.parser()
                            .verifyWith(secretKey)
                            .build()
                            .parseSignedClaims(token)
                            .getPayload();

                    userId = claims.getSubject();
                    organizationId = claims.get(CLAIM_ORGANIZATION_ID, String.class);
                    log.debug("JWT authenticated — userId={}, orgId={}", userId, organizationId);
                } catch (Exception e) {
                    log.debug("JWT parse failed ({}), trying headers", e.getMessage());
                }
            }

            // --- 2. Fallback to trusted headers (BFF → downstream) ---
            if (userId == null) {
                userId = request.getHeader(HEADER_USER_ID);
                organizationId = request.getHeader(HEADER_ORGANIZATION_ID);
                if (userId != null) {
                    log.debug("Header authenticated — userId={}, orgId={}", userId, organizationId);
                }
            }

            // --- 3. Populate security + tenant context ---
            if (userId != null && organizationId != null) {
                UserPrincipal principal = new UserPrincipal(userId, organizationId);

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);

                // Keep TenantContextHolder in sync so JPA tenant queries work
                TenantContextHolder.setContext(new TenantContext(organizationId, userId));
            }
        } catch (Exception e) {
            log.error("ServiceJwtAuthenticationFilter error: {}", e.getMessage());
        }

        try {
            chain.doFilter(request, response);
        } finally {
            // Clear only if we set it; JwtTenantFilter / TenantFilter also clean up theirs
            TenantContextHolder.clear();
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator")
                || path.equals("/api/v1/auth/login")
                || path.equals("/api/v1/auth/register")
                || path.equals("/api/v1/auth/refresh");
    }
}
