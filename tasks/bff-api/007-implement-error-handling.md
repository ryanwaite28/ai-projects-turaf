# Task: Implement Error Handling

**Service**: BFF API  
**Phase**: 5  
**Estimated Time**: 2-3 hours  

## Objective

Implement global exception handling and circuit breakers to handle service failures gracefully.

## Prerequisites

- [x] Task 001: BFF API project setup complete
- [x] Task 002: Service clients configured
- [x] Resilience4j dependencies added

## Scope

**Files to Create**:
- `src/main/java/com/turaf/bff/exception/GlobalExceptionHandler.java`
- `src/main/java/com/turaf/bff/exception/ErrorResponse.java`
- `src/main/java/com/turaf/bff/exception/ServiceUnavailableException.java`
- `src/main/java/com/turaf/bff/config/CircuitBreakerConfig.java`
- `src/test/java/com/turaf/bff/exception/GlobalExceptionHandlerTest.java`

## Implementation Details

### Error Response DTO

```java
package com.turaf.bff.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    
    private int status;
    private String error;
    private String message;
    private String path;
    private String correlationId;
}
```

### Custom Exceptions

```java
package com.turaf.bff.exception;

public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(String message) {
        super(message);
    }
    
    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

### Global Exception Handler

```java
package com.turaf.bff.exception;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleServiceUnavailable(
            ServiceUnavailableException ex, HttpServletRequest request) {
        
        log.error("Service unavailable: {}", ex.getMessage(), ex);
        
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
    
    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<ErrorResponse> handleCircuitBreakerOpen(
            CallNotPermittedException ex, HttpServletRequest request) {
        
        log.error("Circuit breaker open: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.SERVICE_UNAVAILABLE.value())
            .error("Service Unavailable")
            .message("Service is temporarily unavailable. Please try again later.")
            .path(request.getRequestURI())
            .correlationId(MDC.get("correlationId"))
            .build();
            
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }
    
    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<ErrorResponse> handleWebClientResponseException(
            WebClientResponseException ex, HttpServletRequest request) {
        
        log.error("Downstream service error: {} - {}", ex.getStatusCode(), ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(ex.getStatusCode().value())
            .error(ex.getStatusText())
            .message("Downstream service error: " + ex.getMessage())
            .path(request.getRequestURI())
            .correlationId(MDC.get("correlationId"))
            .build();
            
        return ResponseEntity.status(ex.getStatusCode()).body(error);
    }
    
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {
        
        log.error("Authentication failed: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.UNAUTHORIZED.value())
            .error("Unauthorized")
            .message("Authentication failed: " + ex.getMessage())
            .path(request.getRequestURI())
            .correlationId(MDC.get("correlationId"))
            .build();
            
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {
        
        log.error("Access denied: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.FORBIDDEN.value())
            .error("Forbidden")
            .message("Access denied: " + ex.getMessage())
            .path(request.getRequestURI())
            .correlationId(MDC.get("correlationId"))
            .build();
            
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));
        
        log.error("Validation failed: {}", message);
        
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Bad Request")
            .message("Validation failed: " + message)
            .path(request.getRequestURI())
            .correlationId(MDC.get("correlationId"))
            .build();
            
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error("Internal Server Error")
            .message("An unexpected error occurred")
            .path(request.getRequestURI())
            .correlationId(MDC.get("correlationId"))
            .build();
            
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
```

### Circuit Breaker Configuration

Update `application.yml` with circuit breaker instances:

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
        eventConsumerBufferSize: 10
        
      organizationService:
        registerHealthIndicator: true
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        permittedNumberOfCallsInHalfOpenState: 3
        waitDurationInOpenState: 10s
        failureRateThreshold: 50
        
      experimentService:
        registerHealthIndicator: true
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        permittedNumberOfCallsInHalfOpenState: 3
        waitDurationInOpenState: 10s
        failureRateThreshold: 50
        
      metricsService:
        registerHealthIndicator: true
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        permittedNumberOfCallsInHalfOpenState: 3
        waitDurationInOpenState: 10s
        failureRateThreshold: 50
```

### Apply Circuit Breaker to Service Clients

Update service clients to use circuit breakers:

```java
@Service
@RequiredArgsConstructor
public class ExperimentServiceClient {
    
    private final WebClient webClient;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    
    public Mono<ExperimentDto> getExperiment(String id, String userId, String organizationId) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("experimentService");
        
        return webClient.get()
            .uri(SERVICE_PATH + "/experiments/{id}", id)
            .header("X-User-Id", userId)
            .header("X-Organization-Id", organizationId)
            .retrieve()
            .bodyToMono(ExperimentDto.class)
            .transform(CircuitBreakerOperator.of(circuitBreaker))
            .doOnError(error -> log.error("Failed to get experiment {}", id, error))
            .onErrorMap(throwable -> new ServiceUnavailableException(
                "Experiment service unavailable", throwable));
    }
}

## Acceptance Criteria

- [x] ErrorResponse DTO created with timestamp, status, error, message, path
- [x] Custom exceptions created (ServiceUnavailableException, etc.)
- [x] GlobalExceptionHandler handles all exception types
- [x] Circuit breaker exceptions return 503 Service Unavailable
- [x] WebClient exceptions properly mapped to HTTP status codes
- [x] Validation exceptions return 400 Bad Request with field errors
- [x] Authentication exceptions return 401 Unauthorized
- [x] Access denied exceptions return 403 Forbidden
- [x] Generic exceptions return 500 Internal Server Error
- [x] All errors include correlation ID from MDC
- [x] Error responses logged with appropriate level
- [x] Unit tests verify all exception handlers
- [x] Integration tests verify circuit breaker behavior

## Testing Requirements

**Unit Tests**:
```java
@SpringBootTest
class GlobalExceptionHandlerTest {
    
    @Autowired
    private GlobalExceptionHandler exceptionHandler;
    
    @Mock
    private HttpServletRequest request;
    
    @BeforeEach
    void setUp() {
        when(request.getRequestURI()).thenReturn("/api/v1/test");
        MDC.put("correlationId", "test-correlation-id");
    }
    
    @Test
    void testHandleServiceUnavailable() {
        ServiceUnavailableException ex = new ServiceUnavailableException("Service down");
        
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleServiceUnavailable(ex, request);
        
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("Service down", response.getBody().getMessage());
        assertEquals("test-correlation-id", response.getBody().getCorrelationId());
    }
    
    @Test
    void testHandleCircuitBreakerOpen() {
        CallNotPermittedException ex = CallNotPermittedException.createCallNotPermittedException(
            CircuitBreaker.ofDefaults("test"));
        
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleCircuitBreakerOpen(ex, request);
        
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertTrue(response.getBody().getMessage().contains("temporarily unavailable"));
    }
}
```

## References

- Specification: `specs/bff-api.md` (Error Handling section)
- Resilience4j Documentation
- Spring Exception Handling Documentation
