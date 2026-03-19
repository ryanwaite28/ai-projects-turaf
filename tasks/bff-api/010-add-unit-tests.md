# Task: Add Unit Tests

**Service**: BFF API  
**Phase**: 5  
**Estimated Time**: 3-4 hours  

## Objective

Create comprehensive unit tests for all BFF API components to ensure code quality and reliability.

## Prerequisites

- [x] All previous tasks (001-009) completed
- [x] JUnit 5 and testing dependencies added

## Scope

**Test Coverage Goals**:
- Controllers: 90%+
- Service Clients: 90%+
- Security Components: 95%+
- Filters: 90%+
- Configuration: 80%+

## Implementation Details

### Controller Tests

**AuthControllerTest.java**:
```java
package com.turaf.bff.controllers;

import com.turaf.bff.clients.IdentityServiceClient;
import com.turaf.bff.dto.LoginRequest;
import com.turaf.bff.dto.UserDto;
import com.turaf.bff.security.JwtTokenValidator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private IdentityServiceClient identityServiceClient;
    
    @MockBean
    private JwtTokenValidator jwtTokenValidator;
    
    @Test
    void testLogin_Success() throws Exception {
        UserDto userDto = UserDto.builder()
            .id("user-123")
            .email("test@example.com")
            .token("jwt-token")
            .build();
        
        when(identityServiceClient.login(any(LoginRequest.class)))
            .thenReturn(Mono.just(userDto));
        
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@example.com\",\"password\":\"password\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("user-123"))
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.token").value("jwt-token"));
    }
    
    @Test
    void testLogin_InvalidCredentials() throws Exception {
        when(identityServiceClient.login(any(LoginRequest.class)))
            .thenReturn(Mono.error(new RuntimeException("Invalid credentials")));
        
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@example.com\",\"password\":\"wrong\"}"))
            .andExpect(status().is5xxServerError());
    }
    
    @Test
    @WithMockUser
    void testGetCurrentUser_Success() throws Exception {
        UserDto userDto = UserDto.builder()
            .id("user-123")
            .email("test@example.com")
            .build();
        
        when(identityServiceClient.getCurrentUser(any(String.class)))
            .thenReturn(Mono.just(userDto));
        
        mockMvc.perform(get("/api/v1/auth/me")
                .header("Authorization", "Bearer token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("user-123"));
    }
}
```

### Service Client Tests

**IdentityServiceClientTest.java**:
```java
package com.turaf.bff.clients;

import com.turaf.bff.dto.LoginRequest;
import com.turaf.bff.dto.UserDto;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class IdentityServiceClientTest {
    
    private MockWebServer mockWebServer;
    private IdentityServiceClient client;
    
    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        
        WebClient webClient = WebClient.builder()
            .baseUrl(mockWebServer.url("/").toString())
            .build();
        
        client = new IdentityServiceClient(webClient);
    }
    
    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }
    
    @Test
    void testLogin_Success() {
        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"id\":\"user-123\",\"email\":\"test@example.com\",\"token\":\"jwt-token\"}")
            .addHeader("Content-Type", "application/json"));
        
        LoginRequest request = new LoginRequest("test@example.com", "password");
        UserDto user = client.login(request).block();
        
        assertNotNull(user);
        assertEquals("user-123", user.getId());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("jwt-token", user.getToken());
    }
    
    @Test
    void testLogin_ServerError() {
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(500)
            .setBody("{\"error\":\"Internal Server Error\"}"));
        
        LoginRequest request = new LoginRequest("test@example.com", "password");
        
        assertThrows(Exception.class, () -> client.login(request).block());
    }
    
    @Test
    void testGetCurrentUser_Success() {
        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"id\":\"user-123\",\"email\":\"test@example.com\"}")
            .addHeader("Content-Type", "application/json"));
        
        UserDto user = client.getCurrentUser("token").block();
        
        assertNotNull(user);
        assertEquals("user-123", user.getId());
    }
}
```

### Security Tests

**JwtTokenValidatorTest.java**:
```java
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
    
    @BeforeEach
    void setUp() {
        String secret = "test-secret-key-that-is-long-enough-for-hmac-sha256";
        secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        validator = new JwtTokenValidator(secret);
    }
    
    @Test
    void testValidateToken_ValidToken() {
        String token = generateValidToken();
        assertTrue(validator.validateToken(token));
    }
    
    @Test
    void testValidateToken_ExpiredToken() {
        String token = generateExpiredToken();
        assertFalse(validator.validateToken(token));
    }
    
    @Test
    void testValidateToken_InvalidSignature() {
        String token = "invalid.token.signature";
        assertFalse(validator.validateToken(token));
    }
    
    @Test
    void testExtractUserContext() {
        String token = generateValidToken();
        UserContext context = validator.extractUserContext(token);
        
        assertNotNull(context);
        assertEquals("user-123", context.getUserId());
        assertEquals("org-456", context.getOrganizationId());
        assertEquals("test@example.com", context.getEmail());
    }
    
    private String generateValidToken() {
        return Jwts.builder()
            .subject("user-123")
            .claim("organizationId", "org-456")
            .claim("email", "test@example.com")
            .claim("name", "Test User")
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + 86400000))
            .signWith(secretKey)
            .compact();
    }
    
    private String generateExpiredToken() {
        return Jwts.builder()
            .subject("user-123")
            .issuedAt(new Date(System.currentTimeMillis() - 86400000))
            .expiration(new Date(System.currentTimeMillis() - 3600000))
            .signWith(secretKey)
            .compact();
    }
}
```

### Filter Tests

**CorrelationIdFilterTest.java**:
```java
package com.turaf.bff.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CorrelationIdFilterTest {
    
    @InjectMocks
    private CorrelationIdFilter filter;
    
    @Mock
    private HttpServletRequest request;
    
    @Mock
    private HttpServletResponse response;
    
    @Mock
    private FilterChain filterChain;
    
    @Test
    void testGeneratesCorrelationId() throws Exception {
        when(request.getHeader("X-Correlation-Id")).thenReturn(null);
        
        filter.doFilterInternal(request, response, filterChain);
        
        verify(response).setHeader(eq("X-Correlation-Id"), anyString());
        verify(filterChain).doFilter(request, response);
    }
    
    @Test
    void testUsesExistingCorrelationId() throws Exception {
        String existingId = "existing-correlation-id";
        when(request.getHeader("X-Correlation-Id")).thenReturn(existingId);
        
        filter.doFilterInternal(request, response, filterChain);
        
        verify(response).setHeader("X-Correlation-Id", existingId);
    }
    
    @Test
    void testClearsMDCAfterRequest() throws Exception {
        when(request.getHeader("X-Correlation-Id")).thenReturn("test-id");
        
        filter.doFilterInternal(request, response, filterChain);
        
        assertNull(MDC.get("correlationId"));
    }
}
```

### Integration Tests

**BffApiIntegrationTest.java**:
```java
package com.turaf.bff;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class BffApiIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }
    
    @Test
    void testUnauthorizedAccessToProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/experiments"))
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    void testCorsHeaders() throws Exception {
        mockMvc.perform(options("/api/v1/experiments")
                .header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "GET"))
            .andExpect(status().isOk())
            .andExpect(header().exists("Access-Control-Allow-Origin"));
    }
    
    @Test
    void testCorrelationIdPropagation() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@example.com\",\"password\":\"password\"}"))
            .andExpect(header().exists("X-Correlation-Id"));
    }
}
```

## Acceptance Criteria

- [x] All controllers have unit tests with 90%+ coverage
- [x] All service clients have unit tests with MockWebServer
- [x] Security components tested (JWT validation, filters)
- [x] Filter tests verify correlation ID, logging, rate limiting
- [x] Configuration tests verify beans created correctly
- [x] Integration tests verify end-to-end flows
- [x] All tests pass consistently
- [ ] Test coverage report generated
- [ ] No flaky tests
- [ ] Tests run in CI/CD pipeline

## Test Execution

**Run all tests**:
```bash
mvn test
```

**Run with coverage**:
```bash
mvn test jacoco:report
```

**Run specific test class**:
```bash
mvn test -Dtest=AuthControllerTest
```

**Run integration tests only**:
```bash
mvn verify -DskipUnitTests
```

## Test Organization

```
src/test/java/com/turaf/bff/
├── controllers/
│   ├── AuthControllerTest.java
│   ├── OrganizationControllerTest.java
│   ├── ExperimentControllerTest.java
│   ├── MetricsControllerTest.java
│   └── DashboardControllerTest.java
├── clients/
│   ├── IdentityServiceClientTest.java
│   ├── OrganizationServiceClientTest.java
│   ├── ExperimentServiceClientTest.java
│   └── MetricsServiceClientTest.java
├── security/
│   ├── JwtTokenValidatorTest.java
│   ├── JwtAuthenticationFilterTest.java
│   └── SecurityConfigTest.java
├── filter/
│   ├── CorrelationIdFilterTest.java
│   ├── RequestLoggingFilterTest.java
│   └── RateLimitFilterTest.java
├── exception/
│   └── GlobalExceptionHandlerTest.java
└── BffApiIntegrationTest.java
```

## References

- JUnit 5 Documentation
- Mockito Documentation
- Spring Boot Testing Documentation
- MockWebServer Documentation
