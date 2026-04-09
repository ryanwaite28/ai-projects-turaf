# BFF API Service Specification

**Source**: PROJECT.md (Section 40 - BFF API Service)

This specification defines the Backend for Frontend (BFF) API service that provides a unified REST API for the Angular frontend.

---

## Service Overview

**Type**: Backend for Frontend (BFF)  
**Technology**: Spring Boot 3.2.0 REST API (Java 17)  
**Pattern**: API Gateway + Orchestration  
**Deployment**: ECS Fargate (behind Public ALB)  
**Port**: 8080 (internal)

### Purpose

The BFF API serves as the single entry point for all frontend requests, providing:

1. **Unified REST API**: Single endpoint (`/api/v1/*`) for frontend
2. **Authentication**: JWT token validation and user context extraction
3. **Orchestration**: Aggregate data from multiple microservices
4. **Proxy**: Route requests to appropriate microservices via internal ALB
5. **Cross-Cutting Concerns**: CORS, rate limiting, logging, error handling

### Position in Architecture

```
Frontend (Angular) 
  ↓ HTTPS
Public ALB (api.turafapp.com)
  ↓ HTTP
BFF API (Spring Boot)
  ↓ HTTP
Internal ALB (internal-alb.turafapp.com)
  ↓ HTTP
Microservices (Identity, Organization, Experiment, Metrics)
```

---

## Technology Stack

### Core Dependencies

**Spring Boot**:
- `spring-boot-starter-web` - REST API framework
- `spring-boot-starter-security` - JWT validation
- `spring-boot-starter-actuator` - Health checks and metrics
- `spring-boot-starter-validation` - Request validation

**HTTP Client**:
- `spring-boot-starter-webflux` - WebClient for reactive HTTP calls
- Alternative: `RestTemplate` with connection pooling

**Resilience**:
- `resilience4j-spring-boot3` - Circuit breakers, retry, rate limiting
- `resilience4j-circuitbreaker` - Prevent cascade failures
- `resilience4j-retry` - Automatic retry logic

**Security**:
- `io.jsonwebtoken:jjwt-api` - JWT parsing
- `io.jsonwebtoken:jjwt-impl` - JWT implementation
- `io.jsonwebtoken:jjwt-jackson` - JSON processing

**Observability**:
- `micrometer-registry-prometheus` - Metrics export
- `spring-boot-starter-logging` - Structured logging

---

## API Endpoints

### Base Path

All endpoints are prefixed with `/api/v1`

### Authentication Endpoints (Proxy to Identity Service)

**POST /api/v1/auth/login**
- Description: User login
- Proxy to: `http://internal-alb/identity/auth/login`
- Request: `{ "email": "string", "password": "string" }`
- Response: `{ "token": "string", "user": {...} }`
- Auth: None (public endpoint)

**POST /api/v1/auth/register**
- Description: User registration
- Proxy to: `http://internal-alb/identity/auth/register`
- Request: `{ "email": "string", "password": "string", "name": "string" }`
- Response: `{ "token": "string", "user": {...} }`
- Auth: None (public endpoint)

**GET /api/v1/auth/me**
- Description: Get current user
- Proxy to: `http://internal-alb/identity/auth/me`
- Response: `{ "id": "string", "email": "string", "name": "string" }`
- Auth: Required (JWT token)

**POST /api/v1/auth/logout**
- Description: User logout
- Proxy to: `http://internal-alb/identity/auth/logout`
- Response: `{ "message": "Logged out successfully" }`
- Auth: Required (JWT token)

### Organization Endpoints (Proxy to Organization Service)

**GET /api/v1/organizations**
- Description: List user's organizations
- Proxy to: `http://internal-alb/organization/organizations`
- Response: `[{ "id": "string", "name": "string", ... }]`
- Auth: Required

**POST /api/v1/organizations**
- Description: Create organization
- Proxy to: `http://internal-alb/organization/organizations`
- Request: `{ "name": "string", "description": "string" }`
- Response: `{ "id": "string", "name": "string", ... }`
- Auth: Required

**GET /api/v1/organizations/{id}**
- Description: Get organization details
- Proxy to: `http://internal-alb/organization/organizations/{id}`
- Response: `{ "id": "string", "name": "string", "members": [...] }`
- Auth: Required

**PUT /api/v1/organizations/{id}**
- Description: Update organization
- Proxy to: `http://internal-alb/organization/organizations/{id}`
- Request: `{ "name": "string", "description": "string" }`
- Response: `{ "id": "string", "name": "string", ... }`
- Auth: Required

**DELETE /api/v1/organizations/{id}**
- Description: Delete organization
- Proxy to: `http://internal-alb/organization/organizations/{id}`
- Response: `{ "message": "Organization deleted" }`
- Auth: Required

**GET /api/v1/organizations/{id}/members**
- Description: List organization members
- Proxy to: `http://internal-alb/organization/organizations/{id}/members`
- Response: `[{ "userId": "string", "role": "string", ... }]`
- Auth: Required

**POST /api/v1/organizations/{id}/members**
- Description: Add member to organization
- Proxy to: `http://internal-alb/organization/organizations/{id}/members`
- Request: `{ "email": "string", "role": "string" }`
- Response: `{ "userId": "string", "role": "string", ... }`
- Auth: Required

### Experiment Endpoints (Proxy to Experiment Service)

**GET /api/v1/experiments**
- Description: List experiments
- Proxy to: `http://internal-alb/experiment/experiments`
- Query params: `organizationId`, `status`, `page`, `size`
- Response: `{ "content": [...], "page": {...} }`
- Auth: Required

**POST /api/v1/experiments**
- Description: Create experiment
- Proxy to: `http://internal-alb/experiment/experiments`
- Request: `{ "name": "string", "hypothesisId": "string", ... }`
- Response: `{ "id": "string", "name": "string", ... }`
- Auth: Required

**GET /api/v1/experiments/{id}**
- Description: Get experiment details
- Proxy to: `http://internal-alb/experiment/experiments/{id}`
- Response: `{ "id": "string", "name": "string", "status": "string", ... }`
- Auth: Required

**PUT /api/v1/experiments/{id}**
- Description: Update experiment
- Proxy to: `http://internal-alb/experiment/experiments/{id}`
- Request: `{ "name": "string", "description": "string" }`
- Response: `{ "id": "string", "name": "string", ... }`
- Auth: Required

**DELETE /api/v1/experiments/{id}**
- Description: Delete experiment
- Proxy to: `http://internal-alb/experiment/experiments/{id}`
- Response: `{ "message": "Experiment deleted" }`
- Auth: Required

**POST /api/v1/experiments/{id}/start**
- Description: Start experiment
- Proxy to: `http://internal-alb/experiment/experiments/{id}/start`
- Response: `{ "id": "string", "status": "RUNNING", ... }`
- Auth: Required

**POST /api/v1/experiments/{id}/complete**
- Description: Complete experiment
- Proxy to: `http://internal-alb/experiment/experiments/{id}/complete`
- Request: `{ "resultSummary": "string" }`
- Response: `{ "id": "string", "status": "COMPLETED", ... }`
- Auth: Required

### Metrics Endpoints (Proxy to Metrics Service)

**POST /api/v1/metrics**
- Description: Record metric
- Proxy to: `http://internal-alb/metrics/metrics`
- Request: `{ "experimentId": "string", "name": "string", "value": number, ... }`
- Response: `{ "id": "string", "experimentId": "string", ... }`
- Auth: Required

**GET /api/v1/experiments/{id}/metrics**
- Description: Get experiment metrics
- Proxy to: `http://internal-alb/metrics/experiments/{id}/metrics`
- Query params: `metricName`, `startDate`, `endDate`
- Response: `[{ "id": "string", "name": "string", "value": number, ... }]`
- Auth: Required

**GET /api/v1/metrics/{id}**
- Description: Get metric details
- Proxy to: `http://internal-alb/metrics/metrics/{id}`
- Response: `{ "id": "string", "experimentId": "string", "name": "string", ... }`
- Auth: Required

### Report Query Endpoints (Query S3/DynamoDB)

**Note**: Reports are generated asynchronously by the Reporting Service (Lambda function) when experiments are completed. The BFF API provides read-only query access to generated reports stored in S3.

**Event Flow**: Experiment Service publishes `ExperimentCompleted` event → EventBridge → Reporting Lambda → generates report → stores in S3 → publishes `ReportGenerated` event

**GET /api/v1/reports**
- Description: List reports (query S3 or DynamoDB metadata)
- Query params: `experimentId`, `type`, `status`, `organizationId`
- Response: `[{ "id": "string", "experimentId": "string", "type": "string", "format": "string", "status": "string", "downloadUrl": "string", "createdAt": "ISO-8601" }]`
- Auth: Required
- Implementation: Query DynamoDB for report metadata or list S3 objects

**GET /api/v1/reports/{id}**
- Description: Get report metadata
- Response: `{ "id": "string", "experimentId": "string", "type": "string", "format": "string", "status": "string", "downloadUrl": "string", "s3Location": "string", "createdAt": "ISO-8601" }`
- Auth: Required
- Implementation: Query DynamoDB or S3 metadata

**GET /api/v1/reports/{id}/download**
- Description: Download report file from S3
- Response: Binary file (PDF, CSV, etc.)
- Headers: `Content-Type: application/pdf`, `Content-Disposition: attachment; filename="report-{id}.pdf"`
- Auth: Required
- Implementation: Generate presigned S3 URL or stream file from S3

**Note**: Report creation is NOT exposed via REST API. Reports are created automatically by the Reporting Lambda when experiments complete, following event-driven architecture principles.

### Orchestration Endpoints (Composite Queries)

**GET /api/v1/dashboard/overview**
- Description: Dashboard overview with aggregated data
- Orchestration: Parallel calls to multiple services
- Response:
  ```json
  {
    "user": { "id": "string", "name": "string", "email": "string" },
    "organizations": [{ "id": "string", "name": "string", "memberCount": number }],
    "activeExperiments": [{ "id": "string", "name": "string", "status": "string" }],
    "recentMetrics": [{ "experimentId": "string", "count": number }]
  }
  ```
- Auth: Required
- Implementation:
  1. Call Identity Service for user info
  2. Call Organization Service for organizations
  3. Call Experiment Service for active experiments
  4. Call Metrics Service for recent metrics
  5. Aggregate and return combined response

**GET /api/v1/experiments/{id}/full**
- Description: Complete experiment details with metrics and report status
- Orchestration: Parallel calls to Experiment and Metrics services
- Response:
  ```json
  {
    "experiment": { "id": "string", "name": "string", "status": "string", ... },
    "metrics": [{ "id": "string", "name": "string", "value": number, ... }],
    "metricsSummary": { "count": number, "avgValue": number },
    "reportStatus": { "generated": boolean, "url": "string" }
  }
  ```
- Auth: Required
- Implementation:
  1. Call Experiment Service for experiment details
  2. Call Metrics Service for experiment metrics
  3. Calculate metrics summary
  4. Check report status (if available)
  5. Aggregate and return combined response

**GET /api/v1/organizations/{id}/summary**
- Description: Organization summary with members and experiment count
- Orchestration: Parallel calls to Organization and Experiment services
- Response:
  ```json
  {
    "organization": { "id": "string", "name": "string", ... },
    "members": [{ "userId": "string", "name": "string", "role": "string" }],
    "experimentCount": number,
    "activeExperimentCount": number
  }
  ```
- Auth: Required
- Implementation:
  1. Call Organization Service for organization details
  2. Call Organization Service for members
  3. Call Experiment Service for experiment counts
  4. Aggregate and return combined response

---

## Service Communication

### Internal ALB Configuration

**Base URL**: `http://internal-alb.{env}.turafapp.com`

**Service Paths**:
- Identity Service: `/identity/*`
- Organization Service: `/organization/*`
- Experiment Service: `/experiment/*`
- Metrics Service: `/metrics/*`

### Service Client Configuration

```java
@Configuration
public class ServiceClientConfig {
    
    @Value("${internal.alb.url}")
    private String internalAlbUrl;
    
    @Bean
    public WebClient webClient() {
        return WebClient.builder()
            .baseUrl(internalAlbUrl)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }
    
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
            .rootUri(internalAlbUrl)
            .setConnectTimeout(Duration.ofSeconds(5))
            .setReadTimeout(Duration.ofSeconds(10))
            .build();
    }
}
```

### Service Client Example

```java
@Service
@RequiredArgsConstructor
public class IdentityServiceClient {
    
    private final WebClient webClient;
    
    public Mono<UserDto> login(LoginRequest request) {
        return webClient.post()
            .uri("/identity/auth/login")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(UserDto.class);
    }
    
    public Mono<UserDto> getCurrentUser(String token) {
        return webClient.get()
            .uri("/identity/auth/me")
            .header("Authorization", "Bearer " + token)
            .retrieve()
            .bodyToMono(UserDto.class);
    }
}
```

---

## Authentication Flow

### JWT Token Validation

1. **Frontend Request**: Frontend sends request with JWT token in `Authorization` header
2. **BFF Validation**: BFF validates JWT signature and expiration
3. **User Context Extraction**: BFF extracts `userId` and `organizationId` from token
4. **Header Propagation**: BFF adds headers to downstream requests:
   - `X-User-Id`: User ID from token
   - `X-Organization-Id`: Organization ID from token
   - `Authorization`: Original JWT token
5. **Service Processing**: Microservices use headers for authorization and tenant isolation
6. **Response**: BFF returns response to frontend

### JWT Filter Implementation

```java
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
            UserContext userContext = jwtTokenValidator.extractUserContext(token);
            
            // Set user context in security context
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                userContext, null, userContext.getAuthorities()
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
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

### User Context Propagation

```java
@Component
@RequiredArgsConstructor
public class UserContextInterceptor implements ClientHttpRequestInterceptor {
    
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, 
                                        ClientHttpRequestExecution execution) throws IOException {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.getPrincipal() instanceof UserContext) {
            UserContext userContext = (UserContext) authentication.getPrincipal();
            
            request.getHeaders().add("X-User-Id", userContext.getUserId());
            request.getHeaders().add("X-Organization-Id", userContext.getOrganizationId());
        }
        
        return execution.execute(request, body);
    }
}
```

---

## Error Handling

### Standard Error Response

All errors return a consistent format:

```json
{
  "timestamp": "2024-03-19T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed for field 'email'",
  "path": "/api/v1/auth/register",
  "correlationId": "uuid"
}
```

### Error Types

**400 Bad Request**: Invalid request data
**401 Unauthorized**: Missing or invalid JWT token
**403 Forbidden**: User lacks permission
**404 Not Found**: Resource not found
**429 Too Many Requests**: Rate limit exceeded
**500 Internal Server Error**: Unexpected error
**503 Service Unavailable**: Downstream service unavailable

### Global Exception Handler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleServiceUnavailable(
            ServiceUnavailableException ex, HttpServletRequest request) {
        
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.SERVICE_UNAVAILABLE.value())
            .error("Service Unavailable")
            .message(ex.getMessage())
            .path(request.getRequestURI())
            .correlationId(MDC.get("correlationId"))
            .build();
            
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }
    
    @ExceptionHandler(JwtAuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleJwtAuthentication(
            JwtAuthenticationException ex, HttpServletRequest request) {
        
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.UNAUTHORIZED.value())
            .error("Unauthorized")
            .message("Invalid or expired token")
            .path(request.getRequestURI())
            .correlationId(MDC.get("correlationId"))
            .build();
            
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }
}
```

---

## Circuit Breaker Configuration

### Resilience4j Configuration

```yaml
resilience4j:
  circuitbreaker:
    instances:
      identityService:
        registerHealthIndicator: true
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        permittedNumberOfCallsInHalfOpenState: 3
        automaticTransitionFromOpenToHalfOpenEnabled: true
        waitDurationInOpenState: 10s
        failureRateThreshold: 50
        
      organizationService:
        registerHealthIndicator: true
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        failureRateThreshold: 50
        
      experimentService:
        registerHealthIndicator: true
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        failureRateThreshold: 50
        
      metricsService:
        registerHealthIndicator: true
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        failureRateThreshold: 50
  
  retry:
    instances:
      default:
        maxAttempts: 3
        waitDuration: 500ms
        retryExceptions:
          - org.springframework.web.client.ResourceAccessException
          - java.net.SocketTimeoutException
```

### Circuit Breaker Usage

```java
@Service
@RequiredArgsConstructor
public class ExperimentServiceClient {
    
    private final WebClient webClient;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    
    public Mono<ExperimentDto> getExperiment(String id) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("experimentService");
        
        return webClient.get()
            .uri("/experiment/experiments/{id}", id)
            .retrieve()
            .bodyToMono(ExperimentDto.class)
            .transform(CircuitBreakerOperator.of(circuitBreaker))
            .onErrorResume(this::handleError);
    }
    
    private Mono<ExperimentDto> handleError(Throwable throwable) {
        // Log error and return fallback response
        log.error("Failed to fetch experiment", throwable);
        return Mono.error(new ServiceUnavailableException("Experiment service unavailable"));
    }
}
```

---

## CORS Configuration

```java
@Configuration
public class CorsConfig {
    
    @Value("${cors.allowed-origins}")
    private String[] allowedOrigins;
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
```

**Environment-Specific Origins**:
- **dev**: `http://localhost:4200`, `https://app.dev.turafapp.com`
- **qa**: `https://app.qa.turafapp.com`
- **prod**: `https://app.turafapp.com`

---

## Rate Limiting

```java
@Configuration
public class RateLimitConfig {
    
    @Bean
    public RateLimiter rateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
            .limitForPeriod(100)
            .limitRefreshPeriod(Duration.ofMinutes(1))
            .timeoutDuration(Duration.ofSeconds(5))
            .build();
            
        return RateLimiter.of("bffApi", config);
    }
}
```

**Rate Limits**:
- **Per User**: 100 requests per minute
- **Per Organization**: 1000 requests per minute
- **Public Endpoints** (login, register): 10 requests per minute per IP

---

## Observability

### Logging

**Structured JSON Logging**:
```json
{
  "timestamp": "2024-03-19T12:00:00Z",
  "level": "INFO",
  "correlationId": "uuid",
  "userId": "user-123",
  "organizationId": "org-456",
  "method": "GET",
  "path": "/api/v1/experiments/123",
  "status": 200,
  "duration": 150,
  "message": "Request completed"
}
```

### Metrics

**Custom Metrics**:
- `bff_requests_total` - Total requests by endpoint and status
- `bff_request_duration_seconds` - Request duration histogram
- `bff_service_calls_total` - Service calls by target service
- `bff_service_call_duration_seconds` - Service call duration
- `bff_circuit_breaker_state` - Circuit breaker state by service

### Health Checks

**Actuator Endpoints**:
- `/actuator/health` - Overall health status
- `/actuator/health/liveness` - Liveness probe
- `/actuator/health/readiness` - Readiness probe
- `/actuator/metrics` - Prometheus metrics
- `/actuator/info` - Application info

**Health Indicators**:
- Database connectivity (if applicable)
- Internal ALB reachability
- Circuit breaker states
- Memory and CPU usage

---

## Deployment Configuration

### ECS Task Definition

```json
{
  "family": "turaf-bff-api-dev",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "512",
  "memory": "1024",
  "containerDefinitions": [{
    "name": "bff-api",
    "image": "{{ECR_REPOSITORY_URL}}:{{IMAGE_TAG}}",
    "portMappings": [{
      "containerPort": 8080,
      "protocol": "tcp"
    }],
    "environment": [
      { "name": "SPRING_PROFILES_ACTIVE", "value": "dev" },
      { "name": "INTERNAL_ALB_URL", "value": "http://internal-alb.dev.turafapp.com" }
    ],
    "secrets": [
      { "name": "JWT_SECRET_KEY", "valueFrom": "arn:aws:secretsmanager:..." }
    ],
    "logConfiguration": {
      "logDriver": "awslogs",
      "options": {
        "awslogs-group": "/ecs/turaf-bff-api",
        "awslogs-region": "us-east-1",
        "awslogs-stream-prefix": "ecs"
      }
    }
  }]
}
```

### Environment Variables

**Required**:
- `SPRING_PROFILES_ACTIVE` - Environment (dev/qa/prod)
- `INTERNAL_ALB_URL` - Internal ALB base URL
- `JWT_SECRET_KEY` - JWT signing secret (from Secrets Manager)

**Optional**:
- `CORS_ALLOWED_ORIGINS` - Comma-separated allowed origins
- `RATE_LIMIT_REQUESTS_PER_MINUTE` - Rate limit configuration
- `CIRCUIT_BREAKER_FAILURE_THRESHOLD` - Circuit breaker threshold

---

## Testing Strategy

### Unit Tests

- Service client tests with MockWebServer
- Controller tests with MockMvc
- JWT validation tests
- Error handling tests
- Circuit breaker tests

### Integration Tests

**Test Environment Setup**:
- Use WireMock for mocking downstream services
- Testcontainers for dependencies (Redis, etc.)
- Isolated test configuration with test-specific properties
- Dynamic port allocation for parallel test execution

**Authentication Flow Tests**:
- User login with valid credentials → verify JWT token
- User login with invalid credentials → verify 401 response
- Access protected endpoint with valid token → verify success
- Access protected endpoint without token → verify 401 response
- Token expiration handling → verify token refresh flow
- Logout flow → verify token invalidation

**Proxy Endpoint Tests**:
- Forward requests to Identity Service → verify headers and body
- Forward requests to Organization Service → verify response mapping
- Forward requests to Experiment Service → verify status codes
- Forward requests to Metrics Service → verify error handling
- Service timeout handling → verify timeout response
- Service unavailable → verify circuit breaker activation
- Correlation ID propagation → verify header forwarding

**Orchestration Tests**:
- Dashboard overview → verify parallel service calls
- Dashboard overview → verify data aggregation from multiple services
- Experiment full details → verify experiment + metrics combination
- Organization summary → verify org + members + experiments aggregation
- Partial service failure → verify graceful degradation
- All services fail → verify appropriate error response
- Performance under concurrent requests → verify < 500ms response time

**Security Tests**:
- CORS preflight requests → verify allowed origins
- CORS actual requests → verify headers in response
- Rate limiting on public endpoints → verify 429 after threshold
- Rate limiting on protected endpoints → verify per-user limits
- JWT signature validation → verify invalid tokens rejected
- Authorization header extraction → verify Bearer token parsing
- Actuator endpoints → verify excluded from authentication

**Cross-Cutting Concerns**:
- Correlation ID generation → verify unique ID per request
- Correlation ID propagation → verify ID in response headers
- Request logging → verify method, URI, status, duration logged
- Metrics collection → verify custom metrics recorded
- Error responses → verify consistent format with correlation ID
- Circuit breaker state → verify open/closed transitions

### Load Tests

- Throughput testing (requests per second)
- Latency testing (p50, p95, p99)
- Connection pool sizing
- Circuit breaker under load
- Auto-scaling validation

---

## Security Considerations

1. **JWT Validation**: Verify signature, expiration, issuer
2. **HTTPS Only**: All external communication over HTTPS
3. **Security Headers**: HSTS, X-Content-Type-Options, X-Frame-Options
4. **Input Validation**: Validate all request data
5. **Rate Limiting**: Prevent abuse and DDoS
6. **CORS**: Restrict to known origins only
7. **Secrets Management**: Use AWS Secrets Manager for sensitive data
8. **Audit Logging**: Log all authentication and authorization events

---

## Performance Optimization

1. **Connection Pooling**: Reuse HTTP connections to internal ALB
2. **Async Processing**: Use reactive WebClient for non-blocking I/O
3. **Response Caching**: Cache frequently accessed data (with TTL)
4. **Parallel Calls**: Execute orchestration calls in parallel
5. **Compression**: Enable gzip compression for responses
6. **Keep-Alive**: Enable HTTP keep-alive to internal ALB

---

## References

- PROJECT.md: Section 10a (Network Architecture), Section 40 (BFF API Service)
- specs/architecture.md: System architecture overview
- specs/identity-service.md: Authentication endpoints
- specs/organization-service.md: Organization endpoints
- specs/experiment-service.md: Experiment endpoints
- specs/metrics-service.md: Metrics endpoints
- tasks/bff-api/: Implementation task breakdown
