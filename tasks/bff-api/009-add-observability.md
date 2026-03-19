# Task: Add Observability

**Service**: BFF API  
**Phase**: 5  
**Estimated Time**: 2-3 hours  

## Objective

Implement comprehensive observability including logging, metrics, and health checks for the BFF API.

## Prerequisites

- [x] Task 001: BFF API project setup complete
- [x] Spring Boot Actuator dependency added
- [x] Micrometer Prometheus dependency added

## Scope

**Files to Create**:
- `src/main/java/com/turaf/bff/config/LoggingConfig.java`
- `src/main/java/com/turaf/bff/filter/CorrelationIdFilter.java`
- `src/main/java/com/turaf/bff/metrics/CustomMetrics.java`
- `src/main/resources/logback-spring.xml`

## Implementation Details

### Correlation ID Filter

```java
package com.turaf.bff.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {
    
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }
        
        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);
        
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CORRELATION_ID_MDC_KEY);
        }
    }
}
```

### Request Logging Filter

```java
package com.turaf.bff.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

@Slf4j
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        
        long startTime = System.currentTimeMillis();
        
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        
        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            
            log.info("Request: method={}, uri={}, status={}, duration={}ms",
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                duration);
            
            responseWrapper.copyBodyToResponse();
        }
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator/");
    }
}
```

### Custom Metrics

```java
package com.turaf.bff.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class CustomMetrics {
    
    private final MeterRegistry meterRegistry;
    
    public void recordServiceCall(String serviceName, long durationMs, boolean success) {
        Timer.builder("bff.service.call.duration")
            .tag("service", serviceName)
            .tag("success", String.valueOf(success))
            .description("Duration of service calls from BFF")
            .register(meterRegistry)
            .record(durationMs, TimeUnit.MILLISECONDS);
        
        Counter.builder("bff.service.call.total")
            .tag("service", serviceName)
            .tag("success", String.valueOf(success))
            .description("Total number of service calls from BFF")
            .register(meterRegistry)
            .increment();
    }
    
    public void recordOrchestrationCall(String endpoint, long durationMs) {
        Timer.builder("bff.orchestration.duration")
            .tag("endpoint", endpoint)
            .description("Duration of orchestration calls")
            .register(meterRegistry)
            .record(durationMs, TimeUnit.MILLISECONDS);
    }
    
    public void recordAuthenticationAttempt(boolean success) {
        Counter.builder("bff.authentication.attempts")
            .tag("success", String.valueOf(success))
            .description("Number of authentication attempts")
            .register(meterRegistry)
            .increment();
    }
    
    public void recordRateLimitExceeded(String endpoint) {
        Counter.builder("bff.rate.limit.exceeded")
            .tag("endpoint", endpoint)
            .description("Number of rate limit exceeded events")
            .register(meterRegistry)
            .increment();
    }
}
```

### Structured Logging Configuration

Create `src/main/resources/logback-spring.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
    
    <springProperty scope="context" name="applicationName" source="spring.application.name"/>
    <springProperty scope="context" name="environment" source="spring.profiles.active"/>
    
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <customFields>{"application":"${applicationName}","environment":"${environment}"}</customFields>
            <includeMdcKeyName>correlationId</includeMdcKeyName>
            <includeMdcKeyName>userId</includeMdcKeyName>
            <includeMdcKeyName>organizationId</includeMdcKeyName>
        </encoder>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>
    
    <logger name="com.turaf.bff" level="DEBUG"/>
    <logger name="org.springframework.web" level="INFO"/>
    <logger name="org.springframework.security" level="INFO"/>
</configuration>
```

### Health Indicators

```java
package com.turaf.bff.health;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Component("internalAlb")
@RequiredArgsConstructor
public class InternalAlbHealthIndicator implements HealthIndicator {
    
    private final WebClient webClient;
    
    @Override
    public Health health() {
        try {
            // Simple connectivity check to internal ALB
            webClient.get()
                .uri("/health")
                .retrieve()
                .toBodilessEntity()
                .timeout(Duration.ofSeconds(5))
                .block();
            
            return Health.up()
                .withDetail("internalAlb", "reachable")
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("internalAlb", "unreachable")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

### Update Application Configuration

Add observability configuration to `application.yml`:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true
  health:
    circuitbreakers:
      enabled: true
    ratelimiters:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: ${spring.application.name}
      environment: ${spring.profiles.active}
    distribution:
      percentiles-histogram:
        http.server.requests: true
      slo:
        http.server.requests: 100ms,200ms,500ms,1s,2s

logging:
  level:
    com.turaf.bff: DEBUG
    org.springframework.web: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

## Acceptance Criteria

- [x] Correlation ID filter generates and propagates correlation IDs
- [x] Request logging filter logs all requests with duration
- [x] Custom metrics track service calls, orchestration, authentication
- [x] Structured JSON logging configured with Logback
- [x] Health indicators check internal ALB connectivity
- [x] Actuator endpoints exposed (health, metrics, prometheus)
- [x] Circuit breaker and rate limiter health indicators enabled
- [x] Metrics include percentile histograms for request duration
- [x] MDC context includes correlationId, userId, organizationId
- [x] Prometheus metrics endpoint accessible
- [x] All logs include correlation IDs
- [x] Integration tests verify observability features

## Testing Requirements

**Integration Tests**:
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ObservabilityIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void testCorrelationIdGenerated() throws Exception {
        mockMvc.perform(get("/api/v1/experiments")
                .header("Authorization", "Bearer token"))
            .andExpect(header().exists("X-Correlation-Id"));
    }
    
    @Test
    void testCorrelationIdPropagated() throws Exception {
        String correlationId = "test-correlation-id";
        
        mockMvc.perform(get("/api/v1/experiments")
                .header("Authorization", "Bearer token")
                .header("X-Correlation-Id", correlationId))
            .andExpect(header().string("X-Correlation-Id", correlationId));
    }
    
    @Test
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }
    
    @Test
    void testPrometheusMetrics() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("bff_service_call_total")));
    }
}
```

## Metrics to Monitor

### Request Metrics
- `http_server_requests_seconds` - Request duration
- `http_server_requests_total` - Total requests

### Custom Metrics
- `bff_service_call_duration_seconds` - Service call duration
- `bff_service_call_total` - Total service calls
- `bff_orchestration_duration_seconds` - Orchestration duration
- `bff_authentication_attempts_total` - Authentication attempts
- `bff_rate_limit_exceeded_total` - Rate limit exceeded count

### Circuit Breaker Metrics
- `resilience4j_circuitbreaker_state` - Circuit breaker state
- `resilience4j_circuitbreaker_calls_total` - Circuit breaker calls

### Rate Limiter Metrics
- `resilience4j_ratelimiter_available_permissions` - Available permissions
- `resilience4j_ratelimiter_waiting_threads` - Waiting threads

## References

- Specification: `specs/bff-api.md` (Observability section)
- Spring Boot Actuator Documentation
- Micrometer Documentation
- Logback Documentation
