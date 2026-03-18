# Task: Add Security Configuration

**Service**: Identity Service  
**Phase**: 2  
**Estimated Time**: 2-3 hours  

## Objective

Configure Spring Security with JWT authentication, password encoding, and CORS settings.

## Prerequisites

- [x] Task 004: JWT token service implemented
- [x] Task 005: REST controllers implemented

## Scope

**Files to Create**:
- `services/identity-service/src/main/java/com/turaf/identity/infrastructure/security/SecurityConfig.java`
- `services/identity-service/src/main/java/com/turaf/identity/infrastructure/security/JwtAuthenticationFilter.java`
- `services/identity-service/src/main/java/com/turaf/identity/infrastructure/security/UserPrincipal.java`
- `services/identity-service/src/main/java/com/turaf/identity/infrastructure/security/CorsConfig.java`

## Implementation Details

### Security Configuration

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login", 
                                "/api/v1/auth/refresh").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:4200", "https://app.turaf.com"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
```

### JWT Authentication Filter

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider tokenProvider;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = extractJwtFromRequest(request);
            
            if (jwt != null && tokenProvider.validateToken(jwt)) {
                UserId userId = tokenProvider.getUserIdFromToken(jwt);
                String organizationId = tokenProvider.getOrganizationIdFromToken(jwt);
                
                UserPrincipal principal = new UserPrincipal(userId.getValue(), organizationId);
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(principal, null, List.of());
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            logger.error("Could not set user authentication", ex);
        }
        
        filterChain.doFilter(request, response);
    }
    
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
```

### User Principal

```java
public class UserPrincipal {
    private final String userId;
    private final String organizationId;
    
    public UserPrincipal(String userId, String organizationId) {
        this.userId = userId;
        this.organizationId = organizationId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public String getOrganizationId() {
        return organizationId;
    }
}
```

## Acceptance Criteria

- [x] Spring Security configured with JWT
- [x] BCrypt password encoder configured
- [x] Public endpoints accessible without authentication
- [x] Protected endpoints require valid JWT
- [x] JWT extracted from Authorization header
- [x] User principal set in security context
- [x] CORS configured for frontend origins
- [x] Security tests pass

## Testing Requirements

**Integration Tests**:
- Test public endpoints accessible without token
- Test protected endpoints require valid token
- Test invalid token rejected
- Test expired token rejected
- Test CORS headers present

**Test Files to Create**:
- `SecurityConfigTest.java`
- `JwtAuthenticationFilterTest.java`

## References

- Specification: `specs/identity-service.md` (Security Configuration section)
- Related Tasks: 007-add-unit-tests
