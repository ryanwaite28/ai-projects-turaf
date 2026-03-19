# Task: Implement Orchestration Endpoints

**Service**: BFF API  
**Phase**: 5  
**Estimated Time**: 4-5 hours  

## Objective

Implement composite endpoints that aggregate data from multiple microservices in parallel.

## Prerequisites

- [x] Task 001: BFF API project setup complete
- [x] Task 002: Service clients configured
- [x] Task 003: Authentication implemented
- [x] Task 004: Proxy endpoints implemented

## Scope

**Files to Create**:
- `src/main/java/com/turaf/bff/controllers/DashboardController.java`
- `src/main/java/com/turaf/bff/dto/DashboardOverviewDto.java`
- `src/main/java/com/turaf/bff/dto/ExperimentFullDto.java`
- `src/main/java/com/turaf/bff/dto/OrganizationSummaryDto.java`
- `src/test/java/com/turaf/bff/controllers/DashboardControllerTest.java`

## Implementation Details

### Dashboard Controller

```java
package com.turaf.bff.controllers;

import com.turaf.bff.clients.ExperimentServiceClient;
import com.turaf.bff.clients.IdentityServiceClient;
import com.turaf.bff.clients.MetricsServiceClient;
import com.turaf.bff.clients.OrganizationServiceClient;
import com.turaf.bff.dto.*;
import com.turaf.bff.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    
    private final IdentityServiceClient identityServiceClient;
    private final OrganizationServiceClient organizationServiceClient;
    private final ExperimentServiceClient experimentServiceClient;
    private final MetricsServiceClient metricsServiceClient;
    
    @GetMapping("/overview")
    public Mono<ResponseEntity<DashboardOverviewDto>> getDashboardOverview(
            @AuthenticationPrincipal UserContext userContext,
            @RequestHeader("Authorization") String authHeader) {
        
        log.info("Get dashboard overview for user: {}", userContext.getUserId());
        String token = authHeader.substring(7);
        
        // Execute all calls in parallel
        Mono<UserDto> userMono = identityServiceClient.getCurrentUser(token)
            .doOnError(error -> log.error("Failed to get user", error))
            .onErrorReturn(new UserDto()); // Fallback to empty user
        
        Mono<java.util.List<OrganizationDto>> organizationsMono = 
            organizationServiceClient.getOrganizations(userContext.getUserId())
                .collectList()
                .doOnError(error -> log.error("Failed to get organizations", error))
                .onErrorReturn(java.util.Collections.emptyList());
        
        Mono<java.util.List<ExperimentDto>> experimentsMono = 
            experimentServiceClient.getExperiments(
                    userContext.getOrganizationId(), 
                    userContext.getUserId())
                .filter(exp -> "RUNNING".equals(exp.getStatus()))
                .collectList()
                .doOnError(error -> log.error("Failed to get experiments", error))
                .onErrorReturn(java.util.Collections.emptyList());
        
        // Combine all results
        return Mono.zip(userMono, organizationsMono, experimentsMono)
            .map(tuple -> {
                DashboardOverviewDto overview = DashboardOverviewDto.builder()
                    .user(tuple.getT1())
                    .organizations(tuple.getT2())
                    .activeExperiments(tuple.getT3())
                    .build();
                return ResponseEntity.ok(overview);
            })
            .doOnSuccess(response -> log.info("Dashboard overview retrieved"))
            .doOnError(error -> log.error("Failed to get dashboard overview", error));
    }
}
```

### Dashboard Overview DTO

```java
package com.turaf.bff.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardOverviewDto {
    private UserDto user;
    private List<OrganizationDto> organizations;
    private List<ExperimentDto> activeExperiments;
}
```

### Experiment Full Endpoint

Add to DashboardController:

```java
@GetMapping("/experiments/{id}/full")
public Mono<ResponseEntity<ExperimentFullDto>> getExperimentFull(
        @PathVariable String id,
        @AuthenticationPrincipal UserContext userContext) {
    
    log.info("Get full experiment details: {}", id);
    
    // Execute calls in parallel
    Mono<ExperimentDto> experimentMono = experimentServiceClient.getExperiment(
            id, 
            userContext.getUserId(), 
            userContext.getOrganizationId())
        .doOnError(error -> log.error("Failed to get experiment", error));
    
    Mono<java.util.List<MetricDto>> metricsMono = 
        metricsServiceClient.getExperimentMetrics(
                id, 
                userContext.getUserId(), 
                userContext.getOrganizationId())
            .collectList()
            .doOnError(error -> log.error("Failed to get metrics", error))
            .onErrorReturn(java.util.Collections.emptyList());
    
    // Combine results
    return Mono.zip(experimentMono, metricsMono)
        .map(tuple -> {
            ExperimentDto experiment = tuple.getT1();
            List<MetricDto> metrics = tuple.getT2();
            
            // Calculate metrics summary
            double avgValue = metrics.stream()
                .mapToDouble(MetricDto::getValue)
                .average()
                .orElse(0.0);
            
            MetricsSummary summary = MetricsSummary.builder()
                .count(metrics.size())
                .avgValue(avgValue)
                .build();
            
            ExperimentFullDto fullDto = ExperimentFullDto.builder()
                .experiment(experiment)
                .metrics(metrics)
                .metricsSummary(summary)
                .build();
            
            return ResponseEntity.ok(fullDto);
        })
        .doOnSuccess(response -> log.info("Full experiment details retrieved"))
        .doOnError(error -> log.error("Failed to get full experiment {}", id, error));
}
```

### Experiment Full DTO

```java
package com.turaf.bff.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentFullDto {
    private ExperimentDto experiment;
    private List<MetricDto> metrics;
    private MetricsSummary metricsSummary;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricsSummary {
        private int count;
        private double avgValue;
    }
}
```

### Organization Summary Endpoint

Add to DashboardController:

```java
@GetMapping("/organizations/{id}/summary")
public Mono<ResponseEntity<OrganizationSummaryDto>> getOrganizationSummary(
        @PathVariable String id,
        @AuthenticationPrincipal UserContext userContext) {
    
    log.info("Get organization summary: {}", id);
    
    // Execute calls in parallel
    Mono<OrganizationDto> organizationMono = organizationServiceClient.getOrganization(
            id, 
            userContext.getUserId())
        .doOnError(error -> log.error("Failed to get organization", error));
    
    Mono<java.util.List<MemberDto>> membersMono = 
        organizationServiceClient.getMembers(id, userContext.getUserId())
            .collectList()
            .doOnError(error -> log.error("Failed to get members", error))
            .onErrorReturn(java.util.Collections.emptyList());
    
    Mono<Long> experimentCountMono = 
        experimentServiceClient.getExperiments(id, userContext.getUserId())
            .count()
            .doOnError(error -> log.error("Failed to count experiments", error))
            .onErrorReturn(0L);
    
    Mono<Long> activeExperimentCountMono = 
        experimentServiceClient.getExperiments(id, userContext.getUserId())
            .filter(exp -> "RUNNING".equals(exp.getStatus()))
            .count()
            .doOnError(error -> log.error("Failed to count active experiments", error))
            .onErrorReturn(0L);
    
    // Combine results
    return Mono.zip(organizationMono, membersMono, experimentCountMono, activeExperimentCountMono)
        .map(tuple -> {
            OrganizationSummaryDto summary = OrganizationSummaryDto.builder()
                .organization(tuple.getT1())
                .members(tuple.getT2())
                .experimentCount(tuple.getT3().intValue())
                .activeExperimentCount(tuple.getT4().intValue())
                .build();
            
            return ResponseEntity.ok(summary);
        })
        .doOnSuccess(response -> log.info("Organization summary retrieved"))
        .doOnError(error -> log.error("Failed to get organization summary {}", id, error));
}
```

### Organization Summary DTO

```java
package com.turaf.bff.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationSummaryDto {
    private OrganizationDto organization;
    private List<MemberDto> members;
    private int experimentCount;
    private int activeExperimentCount;
}
```

## Acceptance Criteria

- [x] Dashboard overview endpoint aggregates user, organizations, and active experiments
- [x] Experiment full endpoint aggregates experiment details and metrics
- [x] Organization summary endpoint aggregates organization, members, and experiment counts
- [x] All orchestration endpoints execute service calls in parallel using Mono.zip
- [x] Partial failures handled gracefully (fallback to empty data)
- [x] Metrics summary calculated correctly (count, average)
- [x] Request logging includes correlation IDs
- [x] Response times optimized through parallel execution
- [x] Unit tests verify orchestration logic
- [x] Integration tests verify end-to-end aggregation

## Testing Requirements

**Unit Tests**:
```java
@WebMvcTest(DashboardController.class)
class DashboardControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private IdentityServiceClient identityServiceClient;
    
    @MockBean
    private OrganizationServiceClient organizationServiceClient;
    
    @MockBean
    private ExperimentServiceClient experimentServiceClient;
    
    @MockBean
    private MetricsServiceClient metricsServiceClient;
    
    @Test
    @WithMockUser
    void testGetDashboardOverview() {
        // Mock responses
        when(identityServiceClient.getCurrentUser(anyString()))
            .thenReturn(Mono.just(new UserDto()));
        when(organizationServiceClient.getOrganizations(anyString()))
            .thenReturn(Flux.just(new OrganizationDto()));
        when(experimentServiceClient.getExperiments(anyString(), anyString()))
            .thenReturn(Flux.just(new ExperimentDto()));
        
        // Test
        mockMvc.perform(get("/api/v1/dashboard/overview")
                .header("Authorization", "Bearer token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user").exists())
            .andExpect(jsonPath("$.organizations").isArray())
            .andExpect(jsonPath("$.activeExperiments").isArray());
    }
    
    @Test
    @WithMockUser
    void testGetExperimentFull() {
        // Mock responses
        when(experimentServiceClient.getExperiment(anyString(), anyString(), anyString()))
            .thenReturn(Mono.just(new ExperimentDto()));
        when(metricsServiceClient.getExperimentMetrics(anyString(), anyString(), anyString()))
            .thenReturn(Flux.just(new MetricDto()));
        
        // Test
        mockMvc.perform(get("/api/v1/dashboard/experiments/exp-123/full")
                .header("Authorization", "Bearer token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.experiment").exists())
            .andExpect(jsonPath("$.metrics").isArray())
            .andExpect(jsonPath("$.metricsSummary.count").exists());
    }
}
```

## Performance Considerations

1. **Parallel Execution**: Use `Mono.zip` to execute service calls in parallel
2. **Timeouts**: Configure appropriate timeouts for composite calls
3. **Fallbacks**: Provide fallback values for partial failures
4. **Caching**: Consider caching frequently accessed aggregated data
5. **Circuit Breakers**: Ensure circuit breakers protect each service call

## References

- Specification: `specs/bff-api.md` (Orchestration Endpoints section)
- Project Reactor Documentation
- Spring WebFlux Documentation
