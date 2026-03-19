# Task: Implement Authentication

**Service**: BFF API  
**Phase**: 5  
**Estimated Time**: 3-4 hours  

## Objective

Implement JWT token validation and user context extraction for securing BFF API endpoints.

## Prerequisites

- [x] Task 001: BFF API project setup complete
- [x] Task 002: Service clients configured
- [ ] JWT secret key configured in environment

## Scope

**Files to Create**:
- `src/main/java/com/turaf/bff/security/JwtTokenValidator.java`
- `src/main/java/com/turaf/bff/security/JwtAuthenticationFilter.java`
- `src/main/java/com/turaf/bff/security/UserContext.java`
- `src/main/java/com/turaf/bff/security/SecurityConfig.java`
- `src/main/java/com/turaf/bff/security/UserContextInterceptor.java`
- `src/test/java/com/turaf/bff/security/` (Security tests)

## Implementation Details

### JWT Token Validator

```java
package com.turaf.bff.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
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
        String name = claims.get("name", String.class);
        
        return UserContext.builder()
            .userId(userId)
            .organizationId(organizationId)
            .email(email)
            .name(name)
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
}
```

### User Context

```java
package com.turaf.bff.security;

import lombok.Builder;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Collections;

@Data
@Builder
public class UserContext {
    private String userId;
    private String organizationId;
    private String email;
    private String name;
    
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }
}
```

### JWT Authentication Filter

```java
package com.turaf.bff.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtTokenValidator jwtTokenValidator;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        
        String token = extractToken(request);
        
        if (token != null && jwtTokenValidator.validateToken(token)) {
            try {
                UserContext userContext = jwtTokenValidator.extractUserContext(token);
                
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(
                        userContext, 
                        null, 
                        userContext.getAuthorities()
                    );
                
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                log.debug("Authenticated user: {}", userContext.getUserId());
            } catch (Exception e) {
                log.error("Failed to authenticate user", e);
            }
        }
        
        filterChain.doFilter(request, response);
    }
    
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
```

### Security Configuration

```java
package com.turaf.bff.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/login", "/api/v1/auth/register").permitAll()
                .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers("/actuator/**").authenticated()
                .requestMatchers("/api/v1/**").authenticated()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
```

### User Context Interceptor (for WebClient)

```java
package com.turaf.bff.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class UserContextInterceptor implements ExchangeFilterFunction {
    
    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.getPrincipal() instanceof UserContext) {
            UserContext userContext = (UserContext) authentication.getPrincipal();
            
            ClientRequest modifiedRequest = ClientRequest.from(request)
                .header("X-User-Id", userContext.getUserId())
                .header("X-Organization-Id", userContext.getOrganizationId())
                .build();
            
            log.debug("Added user context headers: userId={}, orgId={}", 
                userContext.getUserId(), userContext.getOrganizationId());
            
            return next.exchange(modifiedRequest);
        }
        
        return next.exchange(request);
    }
}
```

### Update WebClientConfig

```java
@Bean
public WebClient webClient(UserContextInterceptor userContextInterceptor) {
    HttpClient httpClient = HttpClient.create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
        .responseTimeout(Duration.ofSeconds(10));
    
    return WebClient.builder()
        .baseUrl(internalAlbUrl)
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .filter(userContextInterceptor)
        .build();
}
```

## Acceptance Criteria

- [x] JwtTokenValidator validates JWT signatures correctly
- [x] JwtTokenValidator extracts user context (userId, organizationId, email, name)
- [x] JwtAuthenticationFilter intercepts requests and validates tokens
- [x] UserContext stored in SecurityContext for authenticated requests
- [x] SecurityConfig permits public endpoints (login, register, health)
- [x] SecurityConfig requires authentication for all /api/v1/* endpoints
- [x] UserContextInterceptor adds X-User-Id and X-Organization-Id headers
- [x] Invalid tokens return 401 Unauthorized
- [x] Missing tokens on protected endpoints return 401 Unauthorized
- [x] Unit tests verify token validation logic
- [x] Integration tests verify end-to-end authentication flow

## Testing Requirements

**Unit Tests**:
```java
@SpringBootTest
class JwtTokenValidatorTest {
    
    @Autowired
    private JwtTokenValidator validator;
    
    @Test
    void testValidateValidToken() {
        String token = generateValidToken();
        assertTrue(validator.validateToken(token));
    }
    
    @Test
    void testValidateExpiredToken() {
        String token = generateExpiredToken();
        assertFalse(validator.validateToken(token));
    }
    
    @Test
    void testExtractUserContext() {
        String token = generateValidToken();
        UserContext context = validator.extractUserContext(token);
        
        assertNotNull(context);
        assertEquals("user-123", context.getUserId());
        assertEquals("org-456", context.getOrganizationId());
    }
}
```

**Integration Tests**:
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class AuthenticationIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void testAccessProtectedEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/experiments"))
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    void testAccessProtectedEndpointWithValidToken() throws Exception {
        String token = generateValidToken();
        
        mockMvc.perform(get("/api/v1/experiments")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }
    
    @Test
    void testAccessPublicEndpoint() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@example.com\",\"password\":\"password\"}"))
            .andExpect(status().isOk());
    }
}
```

## References

- Specification: `specs/bff-api.md` (Authentication Flow section)
- Spring Security Documentation
- JWT.io Documentation
