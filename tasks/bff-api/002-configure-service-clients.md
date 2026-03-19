# Task: Configure Service Clients

**Service**: BFF API  
**Phase**: 5  
**Estimated Time**: 3-4 hours  

## Objective

Create HTTP clients for communicating with microservices via the internal Application Load Balancer.

## Prerequisites

- [x] Task 001: BFF API project setup complete
- [ ] Internal ALB deployed and accessible
- [ ] Microservices registered with internal ALB target groups

## Scope

**Files to Create**:
- `src/main/java/com/turaf/bff/config/WebClientConfig.java`
- `src/main/java/com/turaf/bff/clients/IdentityServiceClient.java`
- `src/main/java/com/turaf/bff/clients/OrganizationServiceClient.java`
- `src/main/java/com/turaf/bff/clients/ExperimentServiceClient.java`
- `src/main/java/com/turaf/bff/clients/MetricsServiceClient.java`
- `src/main/java/com/turaf/bff/dto/` (DTOs for service communication)
- `src/test/java/com/turaf/bff/clients/` (Client tests)

## Implementation Details

### WebClient Configuration

```java
package com.turaf.bff.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
public class WebClientConfig {
    
    @Value("${internal.alb.url}")
    private String internalAlbUrl;
    
    @Bean
    public WebClient webClient() {
        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
            .responseTimeout(Duration.ofSeconds(10))
            .doOnConnected(conn -> 
                conn.addHandlerLast(new ReadTimeoutHandler(10, TimeUnit.SECONDS))
                    .addHandlerLast(new WriteTimeoutHandler(10, TimeUnit.SECONDS)));
        
        return WebClient.builder()
            .baseUrl(internalAlbUrl)
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
    }
}
```

### Identity Service Client

```java
package com.turaf.bff.clients;

import com.turaf.bff.dto.LoginRequest;
import com.turaf.bff.dto.RegisterRequest;
import com.turaf.bff.dto.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdentityServiceClient {
    
    private final WebClient webClient;
    private static final String SERVICE_PATH = "/identity";
    
    public Mono<UserDto> login(LoginRequest request) {
        log.debug("Calling Identity Service: POST /auth/login");
        return webClient.post()
            .uri(SERVICE_PATH + "/auth/login")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(UserDto.class)
            .doOnError(error -> log.error("Failed to login", error));
    }
    
    public Mono<UserDto> register(RegisterRequest request) {
        log.debug("Calling Identity Service: POST /auth/register");
        return webClient.post()
            .uri(SERVICE_PATH + "/auth/register")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(UserDto.class)
            .doOnError(error -> log.error("Failed to register", error));
    }
    
    public Mono<UserDto> getCurrentUser(String token) {
        log.debug("Calling Identity Service: GET /auth/me");
        return webClient.get()
            .uri(SERVICE_PATH + "/auth/me")
            .header("Authorization", "Bearer " + token)
            .retrieve()
            .bodyToMono(UserDto.class)
            .doOnError(error -> log.error("Failed to get current user", error));
    }
    
    public Mono<Void> logout(String token) {
        log.debug("Calling Identity Service: POST /auth/logout");
        return webClient.post()
            .uri(SERVICE_PATH + "/auth/logout")
            .header("Authorization", "Bearer " + token)
            .retrieve()
            .bodyToMono(Void.class)
            .doOnError(error -> log.error("Failed to logout", error));
    }
}
```

### Organization Service Client

```java
package com.turaf.bff.clients;

import com.turaf.bff.dto.OrganizationDto;
import com.turaf.bff.dto.CreateOrganizationRequest;
import com.turaf.bff.dto.MemberDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrganizationServiceClient {
    
    private final WebClient webClient;
    private static final String SERVICE_PATH = "/organization";
    
    public Flux<OrganizationDto> getOrganizations(String userId) {
        log.debug("Calling Organization Service: GET /organizations");
        return webClient.get()
            .uri(SERVICE_PATH + "/organizations")
            .header("X-User-Id", userId)
            .retrieve()
            .bodyToFlux(OrganizationDto.class)
            .doOnError(error -> log.error("Failed to get organizations", error));
    }
    
    public Mono<OrganizationDto> createOrganization(CreateOrganizationRequest request, String userId) {
        log.debug("Calling Organization Service: POST /organizations");
        return webClient.post()
            .uri(SERVICE_PATH + "/organizations")
            .header("X-User-Id", userId)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(OrganizationDto.class)
            .doOnError(error -> log.error("Failed to create organization", error));
    }
    
    public Mono<OrganizationDto> getOrganization(String id, String userId) {
        log.debug("Calling Organization Service: GET /organizations/{}", id);
        return webClient.get()
            .uri(SERVICE_PATH + "/organizations/{id}", id)
            .header("X-User-Id", userId)
            .retrieve()
            .bodyToMono(OrganizationDto.class)
            .doOnError(error -> log.error("Failed to get organization {}", id, error));
    }
    
    public Flux<MemberDto> getMembers(String organizationId, String userId) {
        log.debug("Calling Organization Service: GET /organizations/{}/members", organizationId);
        return webClient.get()
            .uri(SERVICE_PATH + "/organizations/{id}/members", organizationId)
            .header("X-User-Id", userId)
            .retrieve()
            .bodyToFlux(MemberDto.class)
            .doOnError(error -> log.error("Failed to get members for org {}", organizationId, error));
    }
}
```

### Experiment Service Client

```java
package com.turaf.bff.clients;

import com.turaf.bff.dto.ExperimentDto;
import com.turaf.bff.dto.CreateExperimentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExperimentServiceClient {
    
    private final WebClient webClient;
    private static final String SERVICE_PATH = "/experiment";
    
    public Flux<ExperimentDto> getExperiments(String organizationId, String userId) {
        log.debug("Calling Experiment Service: GET /experiments");
        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path(SERVICE_PATH + "/experiments")
                .queryParam("organizationId", organizationId)
                .build())
            .header("X-User-Id", userId)
            .header("X-Organization-Id", organizationId)
            .retrieve()
            .bodyToFlux(ExperimentDto.class)
            .doOnError(error -> log.error("Failed to get experiments", error));
    }
    
    public Mono<ExperimentDto> createExperiment(CreateExperimentRequest request, String userId, String organizationId) {
        log.debug("Calling Experiment Service: POST /experiments");
        return webClient.post()
            .uri(SERVICE_PATH + "/experiments")
            .header("X-User-Id", userId)
            .header("X-Organization-Id", organizationId)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(ExperimentDto.class)
            .doOnError(error -> log.error("Failed to create experiment", error));
    }
    
    public Mono<ExperimentDto> getExperiment(String id, String userId, String organizationId) {
        log.debug("Calling Experiment Service: GET /experiments/{}", id);
        return webClient.get()
            .uri(SERVICE_PATH + "/experiments/{id}", id)
            .header("X-User-Id", userId)
            .header("X-Organization-Id", organizationId)
            .retrieve()
            .bodyToMono(ExperimentDto.class)
            .doOnError(error -> log.error("Failed to get experiment {}", id, error));
    }
    
    public Mono<ExperimentDto> startExperiment(String id, String userId, String organizationId) {
        log.debug("Calling Experiment Service: POST /experiments/{}/start", id);
        return webClient.post()
            .uri(SERVICE_PATH + "/experiments/{id}/start", id)
            .header("X-User-Id", userId)
            .header("X-Organization-Id", organizationId)
            .retrieve()
            .bodyToMono(ExperimentDto.class)
            .doOnError(error -> log.error("Failed to start experiment {}", id, error));
    }
    
    public Mono<ExperimentDto> completeExperiment(String id, String userId, String organizationId) {
        log.debug("Calling Experiment Service: POST /experiments/{}/complete", id);
        return webClient.post()
            .uri(SERVICE_PATH + "/experiments/{id}/complete", id)
            .header("X-User-Id", userId)
            .header("X-Organization-Id", organizationId)
            .retrieve()
            .bodyToMono(ExperimentDto.class)
            .doOnError(error -> log.error("Failed to complete experiment {}", id, error));
    }
}
```

### Metrics Service Client

```java
package com.turaf.bff.clients;

import com.turaf.bff.dto.MetricDto;
import com.turaf.bff.dto.RecordMetricRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class MetricsServiceClient {
    
    private final WebClient webClient;
    private static final String SERVICE_PATH = "/metrics";
    
    public Mono<MetricDto> recordMetric(RecordMetricRequest request, String userId, String organizationId) {
        log.debug("Calling Metrics Service: POST /metrics");
        return webClient.post()
            .uri(SERVICE_PATH + "/metrics")
            .header("X-User-Id", userId)
            .header("X-Organization-Id", organizationId)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(MetricDto.class)
            .doOnError(error -> log.error("Failed to record metric", error));
    }
    
    public Flux<MetricDto> getExperimentMetrics(String experimentId, String userId, String organizationId) {
        log.debug("Calling Metrics Service: GET /experiments/{}/metrics", experimentId);
        return webClient.get()
            .uri(SERVICE_PATH + "/experiments/{id}/metrics", experimentId)
            .header("X-User-Id", userId)
            .header("X-Organization-Id", organizationId)
            .retrieve()
            .bodyToFlux(MetricDto.class)
            .doOnError(error -> log.error("Failed to get metrics for experiment {}", experimentId, error));
    }
    
    public Mono<MetricDto> getMetric(String id, String userId, String organizationId) {
        log.debug("Calling Metrics Service: GET /metrics/{}", id);
        return webClient.get()
            .uri(SERVICE_PATH + "/metrics/{id}", id)
            .header("X-User-Id", userId)
            .header("X-Organization-Id", organizationId)
            .retrieve()
            .bodyToMono(MetricDto.class)
            .doOnError(error -> log.error("Failed to get metric {}", id, error));
    }
}
```

### Sample DTOs

Create DTOs in `src/main/java/com/turaf/bff/dto/`:
- `LoginRequest.java`
- `RegisterRequest.java`
- `UserDto.java`
- `OrganizationDto.java`
- `CreateOrganizationRequest.java`
- `MemberDto.java`
- `ExperimentDto.java`
- `CreateExperimentRequest.java`
- `MetricDto.java`
- `RecordMetricRequest.java`

## Acceptance Criteria

- [x] WebClient configured with internal ALB base URL
- [x] Connection timeout and read timeout configured
- [x] IdentityServiceClient created with all auth endpoints
- [x] OrganizationServiceClient created with CRUD operations
- [x] ExperimentServiceClient created with lifecycle operations
- [x] MetricsServiceClient created with metric operations
- [x] All clients use proper service paths (/identity, /organization, etc.)
- [x] User context headers (X-User-Id, X-Organization-Id) added to requests
- [x] Error logging implemented for all client methods
- [x] DTOs created for all request/response types
- [x] Unit tests created using MockWebServer
- [ ] All tests passing

## Testing Requirements

**Unit Tests**:
```java
@SpringBootTest
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
    void testLogin() {
        // Mock response
        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"id\":\"user-123\",\"email\":\"test@example.com\"}")
            .addHeader("Content-Type", "application/json"));
        
        // Test
        LoginRequest request = new LoginRequest("test@example.com", "password");
        UserDto user = client.login(request).block();
        
        // Verify
        assertNotNull(user);
        assertEquals("user-123", user.getId());
        assertEquals("test@example.com", user.getEmail());
    }
}
```

## References

- Specification: `specs/bff-api.md` (Service Communication section)
- Spring WebClient Documentation
- Resilience4j Documentation
