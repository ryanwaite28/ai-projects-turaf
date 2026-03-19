# Task: Implement Rate Limiting

**Service**: BFF API  
**Phase**: 5  
**Estimated Time**: 2-3 hours  

## Objective

Implement rate limiting to protect the BFF API and downstream services from abuse.

## Prerequisites

- [x] Task 001: BFF API project setup complete
- [x] Resilience4j dependencies added

## Scope

**Files to Create**:
- `src/main/java/com/turaf/bff/config/RateLimitConfig.java`
- `src/main/java/com/turaf/bff/filter/RateLimitFilter.java`
- `src/test/java/com/turaf/bff/filter/RateLimitFilterTest.java`

## Implementation Details

### Rate Limit Configuration

```java
package com.turaf.bff.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Slf4j
@Configuration
public class RateLimitConfig {
    
    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        RateLimiterConfig defaultConfig = RateLimiterConfig.custom()
            .limitForPeriod(100)
            .limitRefreshPeriod(Duration.ofMinutes(1))
            .timeoutDuration(Duration.ofSeconds(5))
            .build();
        
        RateLimiterConfig publicEndpointConfig = RateLimiterConfig.custom()
            .limitForPeriod(10)
            .limitRefreshPeriod(Duration.ofMinutes(1))
            .timeoutDuration(Duration.ofSeconds(5))
            .build();
        
        RateLimiterRegistry registry = RateLimiterRegistry.of(defaultConfig);
        
        // Register specific rate limiters
        registry.rateLimiter("default", defaultConfig);
        registry.rateLimiter("public", publicEndpointConfig);
        
        log.info("Rate limiter registry configured");
        return registry;
    }
}
```

### Rate Limit Filter

```java
package com.turaf.bff.filter;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {
    
    private final RateLimiterRegistry rateLimiterRegistry;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        
        String rateLimiterKey = getRateLimiterKey(request);
        RateLimiter rateLimiter = getRateLimiter(request, rateLimiterKey);
        
        try {
            rateLimiter.acquirePermission();
            filterChain.doFilter(request, response);
        } catch (RequestNotPermitted e) {
            log.warn("Rate limit exceeded for key: {}", rateLimiterKey);
            response.setStatus(HttpServletResponse.SC_TOO_MANY_REQUESTS);
            response.setContentType("application/json");
            response.getWriter().write(String.format(
                "{\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Please try again later.\",\"retryAfter\":60}"
            ));
        }
    }
    
    private String getRateLimiterKey(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated()) {
            // Use user ID for authenticated requests
            return "user:" + authentication.getName();
        } else {
            // Use IP address for unauthenticated requests
            return "ip:" + getClientIpAddress(request);
        }
    }
    
    private RateLimiter getRateLimiter(HttpServletRequest request, String key) {
        String path = request.getRequestURI();
        
        // Use stricter rate limit for public endpoints
        if (path.startsWith("/api/v1/auth/login") || path.startsWith("/api/v1/auth/register")) {
            return rateLimiterRegistry.rateLimiter(key + ":public", "public");
        }
        
        // Default rate limit for other endpoints
        return rateLimiterRegistry.rateLimiter(key, "default");
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
```

### Update Application Configuration

Add rate limiting configuration to `application.yml`:

```yaml
resilience4j:
  ratelimiter:
    configs:
      default:
        limitForPeriod: 100
        limitRefreshPeriod: 1m
        timeoutDuration: 5s
      public:
        limitForPeriod: 10
        limitRefreshPeriod: 1m
        timeoutDuration: 5s
    instances:
      default:
        baseConfig: default
      public:
        baseConfig: public
```

### Add Rate Limit Headers

Update the filter to add rate limit headers to responses:

```java
private void addRateLimitHeaders(HttpServletResponse response, RateLimiter rateLimiter) {
    RateLimiter.Metrics metrics = rateLimiter.getMetrics();
    
    response.setHeader("X-RateLimit-Limit", String.valueOf(
        rateLimiter.getRateLimiterConfig().getLimitForPeriod()));
    response.setHeader("X-RateLimit-Remaining", String.valueOf(
        metrics.getAvailablePermissions()));
    response.setHeader("X-RateLimit-Reset", String.valueOf(
        System.currentTimeMillis() + 60000)); // 1 minute from now
}
```

Update `doFilterInternal`:

```java
try {
    rateLimiter.acquirePermission();
    addRateLimitHeaders(response, rateLimiter);
    filterChain.doFilter(request, response);
} catch (RequestNotPermitted e) {
    // ... existing error handling
}
```

## Acceptance Criteria

- [x] Rate limiter registry configured with default and public configs
- [x] Rate limit filter intercepts all requests
- [x] Per-user rate limiting for authenticated requests
- [x] Per-IP rate limiting for unauthenticated requests
- [x] Stricter limits for public endpoints (login, register)
- [x] 429 Too Many Requests returned when limit exceeded
- [x] Rate limit headers added to responses (X-RateLimit-*)
- [x] Retry-After header included in 429 responses
- [x] Rate limiter metrics exposed via Actuator
- [x] Unit tests verify rate limiting logic
- [x] Integration tests verify rate limit enforcement

## Testing Requirements

**Unit Tests**:
```java
@SpringBootTest
class RateLimitFilterTest {
    
    @Autowired
    private RateLimitFilter rateLimitFilter;
    
    @Mock
    private HttpServletRequest request;
    
    @Mock
    private HttpServletResponse response;
    
    @Mock
    private FilterChain filterChain;
    
    @Test
    void testRateLimitNotExceeded() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/experiments");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        
        rateLimitFilter.doFilterInternal(request, response, filterChain);
        
        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }
    
    @Test
    void testRateLimitExceeded() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        
        // Make requests until rate limit exceeded
        for (int i = 0; i < 15; i++) {
            rateLimitFilter.doFilterInternal(request, response, filterChain);
        }
        
        verify(response, atLeastOnce()).setStatus(429);
    }
}
```

**Integration Tests**:
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class RateLimitIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void testRateLimitHeaders() throws Exception {
        mockMvc.perform(get("/api/v1/experiments")
                .header("Authorization", "Bearer token"))
            .andExpect(header().exists("X-RateLimit-Limit"))
            .andExpect(header().exists("X-RateLimit-Remaining"))
            .andExpect(header().exists("X-RateLimit-Reset"));
    }
    
    @Test
    void testPublicEndpointRateLimit() throws Exception {
        // Make 11 requests (limit is 10)
        for (int i = 0; i < 11; i++) {
            ResultActions result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@example.com\",\"password\":\"password\"}"));
            
            if (i < 10) {
                result.andExpect(status().isOk());
            } else {
                result.andExpect(status().isTooManyRequests());
            }
        }
    }
}
```

## Rate Limit Strategies

### Per-User Limits
- **Authenticated requests**: 100 requests per minute per user
- **Key format**: `user:{userId}`

### Per-IP Limits
- **Unauthenticated requests**: 10 requests per minute per IP
- **Key format**: `ip:{ipAddress}`

### Endpoint-Specific Limits
- **Public endpoints** (login, register): 10 requests per minute
- **Protected endpoints**: 100 requests per minute
- **Orchestration endpoints**: 50 requests per minute (optional)

## References

- Specification: `specs/bff-api.md` (Rate Limiting section)
- Resilience4j Rate Limiter Documentation
- RFC 6585 (Additional HTTP Status Codes)
