# Task: Implement CORS Configuration

**Service**: BFF API  
**Phase**: 5  
**Estimated Time**: 1-2 hours  

## Objective

Configure Cross-Origin Resource Sharing (CORS) to allow the Angular frontend to communicate with the BFF API.

## Prerequisites

- [x] Task 001: BFF API project setup complete
- [ ] Frontend domain names configured per environment

## Scope

**Files to Create**:
- `src/main/java/com/turaf/bff/config/CorsConfig.java`
- `src/test/java/com/turaf/bff/config/CorsConfigTest.java`

## Implementation Details

### CORS Configuration

```java
package com.turaf.bff.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Configuration
public class CorsConfig {
    
    @Value("${cors.allowed-origins}")
    private String allowedOrigins;
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Parse allowed origins from comma-separated string
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        configuration.setAllowedOrigins(origins);
        
        log.info("CORS allowed origins: {}", origins);
        
        // Allow common HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
            "GET", 
            "POST", 
            "PUT", 
            "DELETE", 
            "OPTIONS", 
            "PATCH"
        ));
        
        // Allow all headers (can be restricted if needed)
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);
        
        // Cache preflight response for 1 hour
        configuration.setMaxAge(3600L);
        
        // Expose headers that frontend can access
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization",
            "X-Correlation-Id",
            "X-Request-Id"
        ));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        
        return source;
    }
}
```

### Update Security Config

Update `SecurityConfig.java` to use CORS configuration:

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(csrf -> csrf.disable())
        // ... rest of configuration
    
    return http.build();
}
```

### Environment-Specific CORS Origins

**application-dev.yml**:
```yaml
cors:
  allowed-origins: http://localhost:4200,https://app.dev.turafapp.com
```

**application-qa.yml**:
```yaml
cors:
  allowed-origins: https://app.qa.turafapp.com
```

**application-prod.yml**:
```yaml
cors:
  allowed-origins: https://app.turafapp.com
```

## Acceptance Criteria

- [x] CORS configuration bean created
- [x] Allowed origins configured per environment
- [x] Allowed methods include GET, POST, PUT, DELETE, OPTIONS, PATCH
- [x] Allowed headers set to all (*)
- [x] Credentials allowed (allowCredentials: true)
- [x] Preflight cache set to 1 hour
- [x] Exposed headers include Authorization, X-Correlation-Id
- [x] CORS applies to all /api/** endpoints
- [x] Security config integrates CORS configuration
- [x] Preflight OPTIONS requests return 200 OK
- [x] Frontend can make authenticated requests

## Testing Requirements

**Integration Tests**:
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class CorsConfigTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void testPreflightRequest() throws Exception {
        mockMvc.perform(options("/api/v1/experiments")
                .header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "Authorization"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"))
            .andExpect(header().string("Access-Control-Allow-Methods", containsString("GET")))
            .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }
    
    @Test
    void testCorsHeadersOnActualRequest() throws Exception {
        mockMvc.perform(get("/api/v1/auth/login")
                .header("Origin", "http://localhost:4200"))
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"))
            .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }
    
    @Test
    void testUnauthorizedOriginBlocked() throws Exception {
        mockMvc.perform(get("/api/v1/experiments")
                .header("Origin", "http://malicious-site.com"))
            .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
```

**Manual Testing**:
1. Start BFF API locally
2. Start Angular frontend on localhost:4200
3. Make API call from frontend
4. Verify no CORS errors in browser console
5. Verify Authorization header sent and received

## Security Considerations

1. **Restrict Origins**: Only allow known frontend domains
2. **Credentials**: Required for JWT tokens in Authorization header
3. **Methods**: Only allow necessary HTTP methods
4. **Headers**: Consider restricting to specific headers in production
5. **Max Age**: Balance between performance and security

## References

- Specification: `specs/bff-api.md` (CORS Configuration section)
- Spring Security CORS Documentation
- MDN CORS Documentation
