# Task: Integration Tests

**Service**: BFF API  
**Phase**: 5  
**Estimated Time**: 4-6 hours  

## Objective

Implement comprehensive integration tests to verify end-to-end functionality of the BFF API with all downstream services, security, and cross-cutting concerns.

## Prerequisites

- [x] Task 001-010: All BFF API components implemented
- [x] Docker installed for Testcontainers
- [x] WireMock dependency added for service mocking

## Scope

**Files to Create**:
- `src/test/java/com/turaf/bff/integration/AuthenticationIntegrationTest.java`
- `src/test/java/com/turaf/bff/integration/ProxyEndpointsIntegrationTest.java`
- `src/test/java/com/turaf/bff/integration/OrchestrationIntegrationTest.java`
- `src/test/java/com/turaf/bff/integration/SecurityIntegrationTest.java`
- `src/test/java/com/turaf/bff/integration/RateLimitingIntegrationTest.java`
- `src/test/java/com/turaf/bff/integration/PerformanceIntegrationTest.java`
- `src/test/resources/application-integration-test.yml`

## Implementation Details

### Authentication Integration Tests

```java
package com.turaf.bff.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class AuthenticationIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    private static WireMockServer wireMockServer;
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        wireMockServer = new WireMockServer(8089);
        wireMockServer.start();
        registry.add("services.identity.base-url", () -> wireMockServer.baseUrl());
    }
    
    @BeforeEach
    void setUp() {
        wireMockServer.resetAll();
    }
    
    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }
    
    @Test
    void testLoginFlow_Success() throws Exception {
        // Mock Identity Service response
        wireMockServer.stubFor(post(urlEqualTo("/api/v1/auth/login"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"id\":\"user-123\",\"email\":\"test@example.com\",\"token\":\"jwt-token\",\"name\":\"Test User\"}")));
        
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@example.com\",\"password\":\"password123\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("user-123"))
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.token").value("jwt-token"))
            .andExpect(header().exists("X-Correlation-Id"));
        
        // Verify request was made to Identity Service
        wireMockServer.verify(postRequestedFor(urlEqualTo("/api/v1/auth/login"))
            .withHeader("Content-Type", equalTo("application/json")));
    }
    
    @Test
    void testLoginFlow_InvalidCredentials() throws Exception {
        wireMockServer.stubFor(post(urlEqualTo("/api/v1/auth/login"))
            .willReturn(aResponse()
                .withStatus(401)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\":\"Unauthorized\",\"message\":\"Invalid credentials\"}")));
        
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@example.com\",\"password\":\"wrong\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").exists());
    }
    
    @Test
    void testProtectedEndpoint_WithValidToken() throws Exception {
        // This would require a valid JWT token
        // For integration tests, you may need to generate a test token
        mockMvc.perform(get("/api/v1/organizations")
                .header("Authorization", "Bearer valid-test-token"))
            .andExpect(status().isOk());
    }
    
    @Test
    void testProtectedEndpoint_WithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/organizations"))
            .andExpect(status().isUnauthorized());
    }
}
```

### Proxy Endpoints Integration Tests

```java
package com.turaf.bff.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ProxyEndpointsIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    private static WireMockServer wireMockServer;
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        wireMockServer = new WireMockServer(8089);
        wireMockServer.start();
        registry.add("services.organization.base-url", () -> wireMockServer.baseUrl());
        registry.add("services.experiment.base-url", () -> wireMockServer.baseUrl());
        registry.add("services.metrics.base-url", () -> wireMockServer.baseUrl());
    }
    
    @Test
    void testOrganizationProxy_GetAll() throws Exception {
        wireMockServer.stubFor(get(urlEqualTo("/api/v1/organizations"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("[{\"id\":\"org-1\",\"name\":\"Test Org\"}]")));
        
        mockMvc.perform(get("/api/v1/organizations")
                .header("Authorization", "Bearer test-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value("org-1"));
    }
    
    @Test
    void testExperimentProxy_Create() throws Exception {
        wireMockServer.stubFor(post(urlEqualTo("/api/v1/experiments"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"id\":\"exp-1\",\"name\":\"Test Experiment\",\"status\":\"DRAFT\"}")));
        
        mockMvc.perform(post("/api/v1/experiments")
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test Experiment\",\"organizationId\":\"org-1\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("exp-1"))
            .andExpect(jsonPath("$.status").value("DRAFT"));
    }
    
    @Test
    void testServiceTimeout_ReturnsError() throws Exception {
        wireMockServer.stubFor(get(urlEqualTo("/api/v1/organizations"))
            .willReturn(aResponse()
                .withFixedDelay(10000)
                .withStatus(200)));
        
        mockMvc.perform(get("/api/v1/organizations")
                .header("Authorization", "Bearer test-token"))
            .andExpect(status().is5xxServerError());
    }
}
```

### Orchestration Integration Tests

```java
package com.turaf.bff.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class OrchestrationIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    private static WireMockServer wireMockServer;
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        wireMockServer = new WireMockServer(8089);
        wireMockServer.start();
        registry.add("services.identity.base-url", () -> wireMockServer.baseUrl());
        registry.add("services.organization.base-url", () -> wireMockServer.baseUrl());
        registry.add("services.experiment.base-url", () -> wireMockServer.baseUrl());
    }
    
    @Test
    void testDashboardOverview_AggregatesMultipleServices() throws Exception {
        // Mock Identity Service
        wireMockServer.stubFor(get(urlMatching("/api/v1/auth/me.*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("{\"id\":\"user-1\",\"email\":\"test@example.com\"}")));
        
        // Mock Organization Service
        wireMockServer.stubFor(get(urlMatching("/api/v1/organizations.*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("[{\"id\":\"org-1\",\"name\":\"Org 1\"}]")));
        
        // Mock Experiment Service
        wireMockServer.stubFor(get(urlMatching("/api/v1/experiments.*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("[{\"id\":\"exp-1\",\"status\":\"RUNNING\"}]")));
        
        mockMvc.perform(get("/api/v1/dashboard/overview")
                .header("Authorization", "Bearer test-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.id").value("user-1"))
            .andExpect(jsonPath("$.organizations[0].id").value("org-1"))
            .andExpect(jsonPath("$.activeExperiments[0].id").value("exp-1"))
            .andExpect(jsonPath("$.totalOrganizations").value(1))
            .andExpect(jsonPath("$.totalActiveExperiments").value(1));
    }
    
    @Test
    void testDashboardOverview_PartialFailure() throws Exception {
        // Mock successful Identity Service
        wireMockServer.stubFor(get(urlMatching("/api/v1/auth/me.*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("{\"id\":\"user-1\",\"email\":\"test@example.com\"}")));
        
        // Mock failed Organization Service
        wireMockServer.stubFor(get(urlMatching("/api/v1/organizations.*"))
            .willReturn(aResponse()
                .withStatus(500)));
        
        // Mock successful Experiment Service
        wireMockServer.stubFor(get(urlMatching("/api/v1/experiments.*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("[]")));
        
        // Should still return 200 with partial data
        mockMvc.perform(get("/api/v1/dashboard/overview")
                .header("Authorization", "Bearer test-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.id").value("user-1"))
            .andExpect(jsonPath("$.organizations").isEmpty())
            .andExpect(jsonPath("$.activeExperiments").isEmpty());
    }
}
```

### Rate Limiting Integration Tests

```java
package com.turaf.bff.integration;

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
class RateLimitingIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void testRateLimit_PublicEndpoint() throws Exception {
        // Public endpoints have stricter limits (10 req/min)
        // Make 11 requests to trigger rate limit
        for (int i = 0; i < 11; i++) {
            var result = mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"test@example.com\",\"password\":\"password\"}"));
            
            if (i < 10) {
                result.andExpect(status().is5xxServerError()); // Service unavailable
            } else {
                result.andExpect(status().isTooManyRequests());
            }
        }
    }
    
    @Test
    void testRateLimit_ActuatorExcluded() throws Exception {
        // Actuator endpoints should not be rate limited
        for (int i = 0; i < 20; i++) {
            mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
        }
    }
}
```

### Performance Integration Tests

```java
package com.turaf.bff.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class PerformanceIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    private static WireMockServer wireMockServer;
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        wireMockServer = new WireMockServer(8089);
        wireMockServer.start();
        registry.add("services.organization.base-url", () -> wireMockServer.baseUrl());
    }
    
    @Test
    void testConcurrentRequests_Performance() throws Exception {
        wireMockServer.stubFor(get(urlEqualTo("/api/v1/organizations"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("[]")));
        
        int concurrentUsers = 100;
        CountDownLatch latch = new CountDownLatch(concurrentUsers);
        ExecutorService executor = Executors.newFixedThreadPool(concurrentUsers);
        AtomicInteger successCount = new AtomicInteger(0);
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < concurrentUsers; i++) {
            executor.submit(() -> {
                try {
                    mockMvc.perform(get("/api/v1/organizations")
                            .header("Authorization", "Bearer test-token"))
                        .andExpect(status().isOk());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // Log error
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(30, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;
        
        executor.shutdown();
        
        // Verify performance benchmarks
        assertTrue(successCount.get() >= 95, "At least 95% of requests should succeed");
        assertTrue(duration < 10000, "Should complete within 10 seconds");
    }
}
```

### Test Configuration

Create `src/test/resources/application-integration-test.yml`:

```yaml
spring:
  profiles:
    active: integration-test

cors:
  allowed-origins: http://localhost:4200

jwt:
  secret-key: test-secret-key-that-is-long-enough-for-hmac-sha256-algorithm

services:
  identity:
    base-url: http://localhost:8089
  organization:
    base-url: http://localhost:8089
  experiment:
    base-url: http://localhost:8089
  metrics:
    base-url: http://localhost:8089

resilience4j:
  circuitbreaker:
    instances:
      default:
        slidingWindowSize: 5
        minimumNumberOfCalls: 3
        failureRateThreshold: 50
  
  ratelimiter:
    instances:
      default:
        limitForPeriod: 100
        limitRefreshPeriod: 1m
      public:
        limitForPeriod: 10
        limitRefreshPeriod: 1m

logging:
  level:
    com.turaf.bff: DEBUG
    com.github.tomakehurst.wiremock: WARN
```

## Acceptance Criteria

- [x] Authentication flow integration tests created
- [x] Proxy endpoint integration tests verify all services
- [x] Orchestration tests verify parallel execution and aggregation
- [x] Security tests verify JWT validation and authorization
- [x] Rate limiting tests verify enforcement and exclusions
- [x] Performance tests verify response times and concurrency
- [x] CORS tests verify preflight and actual requests
- [x] Error handling tests verify proper status codes
- [x] Circuit breaker tests verify failure handling
- [x] Correlation ID tests verify propagation
- [x] All integration tests pass consistently
- [x] Test coverage includes happy path and error scenarios
- [x] WireMock used for downstream service mocking
- [x] Tests run in isolated environment
- [x] Performance benchmarks met (< 500ms for orchestration)

## Testing Requirements

**Run Integration Tests**:
```bash
mvn verify -Dspring.profiles.active=integration-test
```

**Run with Coverage**:
```bash
mvn verify jacoco:report -Dspring.profiles.active=integration-test
```

**Run Specific Test**:
```bash
mvn verify -Dtest=AuthenticationIntegrationTest -Dspring.profiles.active=integration-test
```

## Performance Benchmarks

- **Proxy Endpoints**: < 200ms response time
- **Orchestration Endpoints**: < 500ms response time
- **Concurrent Users**: Support 100+ concurrent requests
- **Success Rate**: > 95% under load
- **Circuit Breaker**: Opens after 50% failure rate
- **Rate Limit**: Enforced at configured thresholds

## Testing Strategy

**BFF API uses WireMock** for downstream service mocking (not Testcontainers/LocalStack):

- **Use WireMock** to mock:
  - Identity Service
  - Organization Service
  - Experiment Service
  - Metrics Service
  - Communications Service

- **No AWS services** needed for BFF API integration tests
- **Focus on**: Request routing, aggregation, circuit breakers, rate limiting

**Note**: This differs from microservice integration tests which use Testcontainers + LocalStack for database and AWS service testing.

---

## References

- **Testing Strategy**: `specs/testing-strategy.md` (comprehensive guide)
- **PROJECT.md**: Section 23a - Testing Strategy
- **CI/CD Pipeline**: `specs/ci-cd-pipelines.md` (integration test stage)
- **BFF API**: `specs/bff-api.md` (Integration Testing section)
- **WireMock Documentation**: https://wiremock.org/
- **Spring Boot Testing**: https://spring.io/guides/gs/testing-web/
