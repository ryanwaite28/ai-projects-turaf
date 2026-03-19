# Task: Implement Proxy Endpoints

**Service**: BFF API  
**Phase**: 5  
**Estimated Time**: 4-5 hours  

## Objective

Implement REST controllers that proxy requests to microservices via internal ALB.

## Prerequisites

- [x] Task 001: BFF API project setup complete
- [x] Task 002: Service clients configured
- [x] Task 003: Authentication implemented

## Scope

**Files to Create**:
- `src/main/java/com/turaf/bff/controllers/AuthController.java`
- `src/main/java/com/turaf/bff/controllers/OrganizationController.java`
- `src/main/java/com/turaf/bff/controllers/ExperimentController.java`
- `src/main/java/com/turaf/bff/controllers/MetricsController.java`
- `src/test/java/com/turaf/bff/controllers/` (Controller tests)

## Implementation Details

### Auth Controller

```java
package com.turaf.bff.controllers;

import com.turaf.bff.clients.IdentityServiceClient;
import com.turaf.bff.dto.LoginRequest;
import com.turaf.bff.dto.RegisterRequest;
import com.turaf.bff.dto.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final IdentityServiceClient identityServiceClient;
    
    @PostMapping("/login")
    public Mono<ResponseEntity<UserDto>> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request for email: {}", request.getEmail());
        return identityServiceClient.login(request)
            .map(ResponseEntity::ok)
            .doOnSuccess(response -> log.info("Login successful"))
            .doOnError(error -> log.error("Login failed", error));
    }
    
    @PostMapping("/register")
    public Mono<ResponseEntity<UserDto>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Register request for email: {}", request.getEmail());
        return identityServiceClient.register(request)
            .map(ResponseEntity::ok)
            .doOnSuccess(response -> log.info("Registration successful"))
            .doOnError(error -> log.error("Registration failed", error));
    }
    
    @GetMapping("/me")
    public Mono<ResponseEntity<UserDto>> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7); // Remove "Bearer "
        log.info("Get current user request");
        return identityServiceClient.getCurrentUser(token)
            .map(ResponseEntity::ok)
            .doOnError(error -> log.error("Failed to get current user", error));
    }
    
    @PostMapping("/logout")
    public Mono<ResponseEntity<Void>> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        log.info("Logout request");
        return identityServiceClient.logout(token)
            .map(ResponseEntity::ok)
            .doOnSuccess(response -> log.info("Logout successful"))
            .doOnError(error -> log.error("Logout failed", error));
    }
}
```

### Organization Controller

```java
package com.turaf.bff.controllers;

import com.turaf.bff.clients.OrganizationServiceClient;
import com.turaf.bff.dto.CreateOrganizationRequest;
import com.turaf.bff.dto.MemberDto;
import com.turaf.bff.dto.OrganizationDto;
import com.turaf.bff.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
public class OrganizationController {
    
    private final OrganizationServiceClient organizationServiceClient;
    
    @GetMapping
    public Flux<OrganizationDto> getOrganizations(@AuthenticationPrincipal UserContext userContext) {
        log.info("Get organizations for user: {}", userContext.getUserId());
        return organizationServiceClient.getOrganizations(userContext.getUserId())
            .doOnComplete(() -> log.info("Retrieved organizations"))
            .doOnError(error -> log.error("Failed to get organizations", error));
    }
    
    @PostMapping
    public Mono<ResponseEntity<OrganizationDto>> createOrganization(
            @Valid @RequestBody CreateOrganizationRequest request,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Create organization: {}", request.getName());
        return organizationServiceClient.createOrganization(request, userContext.getUserId())
            .map(ResponseEntity::ok)
            .doOnSuccess(response -> log.info("Organization created"))
            .doOnError(error -> log.error("Failed to create organization", error));
    }
    
    @GetMapping("/{id}")
    public Mono<ResponseEntity<OrganizationDto>> getOrganization(
            @PathVariable String id,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Get organization: {}", id);
        return organizationServiceClient.getOrganization(id, userContext.getUserId())
            .map(ResponseEntity::ok)
            .doOnError(error -> log.error("Failed to get organization {}", id, error));
    }
    
    @GetMapping("/{id}/members")
    public Flux<MemberDto> getMembers(
            @PathVariable String id,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Get members for organization: {}", id);
        return organizationServiceClient.getMembers(id, userContext.getUserId())
            .doOnComplete(() -> log.info("Retrieved members"))
            .doOnError(error -> log.error("Failed to get members", error));
    }
}
```

### Experiment Controller

```java
package com.turaf.bff.controllers;

import com.turaf.bff.clients.ExperimentServiceClient;
import com.turaf.bff.dto.CreateExperimentRequest;
import com.turaf.bff.dto.ExperimentDto;
import com.turaf.bff.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/v1/experiments")
@RequiredArgsConstructor
public class ExperimentController {
    
    private final ExperimentServiceClient experimentServiceClient;
    
    @GetMapping
    public Flux<ExperimentDto> getExperiments(
            @RequestParam String organizationId,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Get experiments for organization: {}", organizationId);
        return experimentServiceClient.getExperiments(organizationId, userContext.getUserId())
            .doOnComplete(() -> log.info("Retrieved experiments"))
            .doOnError(error -> log.error("Failed to get experiments", error));
    }
    
    @PostMapping
    public Mono<ResponseEntity<ExperimentDto>> createExperiment(
            @Valid @RequestBody CreateExperimentRequest request,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Create experiment: {}", request.getName());
        return experimentServiceClient.createExperiment(
                request, 
                userContext.getUserId(), 
                userContext.getOrganizationId())
            .map(ResponseEntity::ok)
            .doOnSuccess(response -> log.info("Experiment created"))
            .doOnError(error -> log.error("Failed to create experiment", error));
    }
    
    @GetMapping("/{id}")
    public Mono<ResponseEntity<ExperimentDto>> getExperiment(
            @PathVariable String id,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Get experiment: {}", id);
        return experimentServiceClient.getExperiment(
                id, 
                userContext.getUserId(), 
                userContext.getOrganizationId())
            .map(ResponseEntity::ok)
            .doOnError(error -> log.error("Failed to get experiment {}", id, error));
    }
    
    @PostMapping("/{id}/start")
    public Mono<ResponseEntity<ExperimentDto>> startExperiment(
            @PathVariable String id,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Start experiment: {}", id);
        return experimentServiceClient.startExperiment(
                id, 
                userContext.getUserId(), 
                userContext.getOrganizationId())
            .map(ResponseEntity::ok)
            .doOnSuccess(response -> log.info("Experiment started"))
            .doOnError(error -> log.error("Failed to start experiment {}", id, error));
    }
    
    @PostMapping("/{id}/complete")
    public Mono<ResponseEntity<ExperimentDto>> completeExperiment(
            @PathVariable String id,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Complete experiment: {}", id);
        return experimentServiceClient.completeExperiment(
                id, 
                userContext.getUserId(), 
                userContext.getOrganizationId())
            .map(ResponseEntity::ok)
            .doOnSuccess(response -> log.info("Experiment completed"))
            .doOnError(error -> log.error("Failed to complete experiment {}", id, error));
    }
}
```

### Metrics Controller

```java
package com.turaf.bff.controllers;

import com.turaf.bff.clients.MetricsServiceClient;
import com.turaf.bff.dto.MetricDto;
import com.turaf.bff.dto.RecordMetricRequest;
import com.turaf.bff.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/v1/metrics")
@RequiredArgsConstructor
public class MetricsController {
    
    private final MetricsServiceClient metricsServiceClient;
    
    @PostMapping
    public Mono<ResponseEntity<MetricDto>> recordMetric(
            @Valid @RequestBody RecordMetricRequest request,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Record metric for experiment: {}", request.getExperimentId());
        return metricsServiceClient.recordMetric(
                request, 
                userContext.getUserId(), 
                userContext.getOrganizationId())
            .map(ResponseEntity::ok)
            .doOnSuccess(response -> log.info("Metric recorded"))
            .doOnError(error -> log.error("Failed to record metric", error));
    }
    
    @GetMapping("/experiments/{experimentId}/metrics")
    public Flux<MetricDto> getExperimentMetrics(
            @PathVariable String experimentId,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Get metrics for experiment: {}", experimentId);
        return metricsServiceClient.getExperimentMetrics(
                experimentId, 
                userContext.getUserId(), 
                userContext.getOrganizationId())
            .doOnComplete(() -> log.info("Retrieved metrics"))
            .doOnError(error -> log.error("Failed to get metrics", error));
    }
    
    @GetMapping("/{id}")
    public Mono<ResponseEntity<MetricDto>> getMetric(
            @PathVariable String id,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Get metric: {}", id);
        return metricsServiceClient.getMetric(
                id, 
                userContext.getUserId(), 
                userContext.getOrganizationId())
            .map(ResponseEntity::ok)
            .doOnError(error -> log.error("Failed to get metric {}", id, error));
    }
}
```

## Acceptance Criteria

- [x] AuthController proxies login, register, me, logout to Identity Service
- [x] OrganizationController proxies CRUD operations to Organization Service
- [x] ExperimentController proxies lifecycle operations to Experiment Service
- [x] MetricsController proxies metric operations to Metrics Service
- [x] All controllers use @AuthenticationPrincipal to access UserContext
- [x] User context (userId, organizationId) passed to service clients
- [x] Request/response logging implemented
- [x] Validation annotations on request DTOs
- [x] Proper HTTP status codes returned
- [x] Error handling delegates to GlobalExceptionHandler
- [x] Unit tests with MockMvc verify controller behavior
- [x] Integration tests verify end-to-end proxy flow

## Testing Requirements

**Controller Tests**:
```java
@WebMvcTest(ExperimentController.class)
class ExperimentControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private ExperimentServiceClient experimentServiceClient;
    
    @MockBean
    private JwtTokenValidator jwtTokenValidator;
    
    @Test
    @WithMockUser
    void testGetExperiments() throws Exception {
        // Mock
        when(experimentServiceClient.getExperiments(anyString(), anyString()))
            .thenReturn(Flux.just(new ExperimentDto()));
        
        // Test
        mockMvc.perform(get("/api/v1/experiments")
                .param("organizationId", "org-123")
                .header("Authorization", "Bearer token"))
            .andExpect(status().isOk());
        
        // Verify
        verify(experimentServiceClient).getExperiments("org-123", "user-123");
    }
}
```

## References

- Specification: `specs/bff-api.md` (API Endpoints section)
- Spring WebFlux Documentation
- Spring MVC Testing Documentation
