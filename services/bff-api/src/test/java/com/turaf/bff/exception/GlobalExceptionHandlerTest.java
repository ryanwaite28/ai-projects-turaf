package com.turaf.bff.exception;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {
    
    private GlobalExceptionHandler exceptionHandler;
    
    @Mock
    private HttpServletRequest request;
    
    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        when(request.getRequestURI()).thenReturn("/api/v1/test");
    }
    
    @Test
    void testHandleServiceUnavailable() {
        ServiceUnavailableException exception = new ServiceUnavailableException("Service is down");
        
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleServiceUnavailable(exception, request);
        
        assertNotNull(response);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(503, response.getBody().getStatus());
        assertEquals("Service Unavailable", response.getBody().getError());
        assertEquals("Service is down", response.getBody().getMessage());
        assertEquals("/api/v1/test", response.getBody().getPath());
    }
    
    @Test
    void testHandleResourceNotFound() {
        ResourceNotFoundException exception = new ResourceNotFoundException("Resource not found");
        
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleResourceNotFound(exception, request);
        
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().getStatus());
        assertEquals("Not Found", response.getBody().getError());
        assertEquals("Resource not found", response.getBody().getMessage());
    }
    
    @Test
    void testHandleCircuitBreakerOpen() {
        CallNotPermittedException exception = CallNotPermittedException.createCallNotPermittedException(
            io.github.resilience4j.circuitbreaker.CircuitBreaker.ofDefaults("test"));
        
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleCircuitBreakerOpen(exception, request);
        
        assertNotNull(response);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(503, response.getBody().getStatus());
        assertEquals("Service Unavailable", response.getBody().getError());
        assertTrue(response.getBody().getMessage().contains("temporarily unavailable"));
    }
    
    @Test
    void testHandleHttpClientError() {
        HttpClientErrorException exception = new HttpClientErrorException(
            HttpStatus.NOT_FOUND, 
            "Not Found");
        
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleHttpClientError(exception, request);
        
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().getStatus());
        assertEquals("Downstream service error", response.getBody().getMessage());
    }
    
    @Test
    void testHandleHttpServerError() {
        HttpServerErrorException exception = new HttpServerErrorException(
            HttpStatus.INTERNAL_SERVER_ERROR, 
            "Internal Server Error");
        
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleHttpServerError(exception, request);
        
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getStatus());
        assertEquals("Downstream service error", response.getBody().getMessage());
    }
    
    @Test
    void testHandleResourceAccessException() {
        ResourceAccessException exception = new ResourceAccessException("Connection refused");
        
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleResourceAccessException(exception, request);
        
        assertNotNull(response);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(503, response.getBody().getStatus());
        assertEquals("Unable to connect to downstream service", response.getBody().getMessage());
    }
    
    @Test
    void testHandleValidationException() {
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("object", "field", "Field is required");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);
        
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationException(exception, request);
        
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Validation Failed", response.getBody().getError());
        assertTrue(response.getBody().getMessage().contains("Field is required"));
    }
    
    @Test
    void testHandleUnauthorized() {
        UnauthorizedException exception = new UnauthorizedException("Invalid credentials");
        
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleUnauthorized(exception, request);
        
        assertNotNull(response);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(401, response.getBody().getStatus());
        assertEquals("Unauthorized", response.getBody().getError());
        assertEquals("Invalid credentials", response.getBody().getMessage());
    }
    
    @Test
    void testHandleAccessDenied() {
        AccessDeniedException exception = new AccessDeniedException("Access denied");
        
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleAccessDenied(exception, request);
        
        assertNotNull(response);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(403, response.getBody().getStatus());
        assertEquals("Forbidden", response.getBody().getError());
    }
    
    @Test
    void testHandleGenericException() {
        Exception exception = new RuntimeException("Unexpected error");
        
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(exception, request);
        
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getStatus());
        assertEquals("Internal Server Error", response.getBody().getError());
        assertEquals("An unexpected error occurred", response.getBody().getMessage());
    }
}
