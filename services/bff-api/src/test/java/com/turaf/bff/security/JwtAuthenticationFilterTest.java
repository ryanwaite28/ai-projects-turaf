package com.turaf.bff.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {
    
    @Mock
    private JwtTokenValidator jwtTokenValidator;
    
    @Mock
    private HttpServletRequest request;
    
    @Mock
    private HttpServletResponse response;
    
    @Mock
    private FilterChain filterChain;
    
    private JwtAuthenticationFilter filter;
    
    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtTokenValidator);
        SecurityContextHolder.clearContext();
    }
    
    @Test
    void testDoFilterInternal_ValidToken() throws ServletException, IOException {
        String token = "valid-jwt-token";
        String bearerToken = "Bearer " + token;
        UserContext userContext = UserContext.builder()
            .userId("user-123")
            .organizationId("org-123")
            .email("test@example.com")
            .username("testuser")
            .firstName("Test")
            .lastName("User")
            .build();
        
        when(request.getHeader("Authorization")).thenReturn(bearerToken);
        when(request.getRequestURI()).thenReturn("/api/v1/organizations");
        when(jwtTokenValidator.extractToken(bearerToken)).thenReturn(token);
        when(jwtTokenValidator.validateToken(token)).thenReturn(true);
        when(jwtTokenValidator.extractUserContext(token)).thenReturn(userContext);
        
        filter.doFilterInternal(request, response, filterChain);
        
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(userContext, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        verify(filterChain).doFilter(request, response);
    }
    
    @Test
    void testDoFilterInternal_NoAuthHeader() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/api/v1/organizations");
        
        filter.doFilterInternal(request, response, filterChain);
        
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verify(jwtTokenValidator, never()).validateToken(anyString());
    }
    
    @Test
    void testDoFilterInternal_InvalidToken() throws ServletException, IOException {
        String token = "invalid-token";
        String bearerToken = "Bearer " + token;
        
        when(request.getHeader("Authorization")).thenReturn(bearerToken);
        when(request.getRequestURI()).thenReturn("/api/v1/organizations");
        when(jwtTokenValidator.extractToken(bearerToken)).thenReturn(token);
        when(jwtTokenValidator.validateToken(token)).thenReturn(false);
        
        filter.doFilterInternal(request, response, filterChain);
        
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verify(jwtTokenValidator, never()).extractUserContext(anyString());
    }
    
    @Test
    void testShouldNotFilter_ActuatorEndpoint() throws ServletException {
        when(request.getRequestURI()).thenReturn("/actuator/health");
        
        assertTrue(filter.shouldNotFilter(request));
    }
    
    @Test
    void testShouldNotFilter_LoginEndpoint() throws ServletException {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        
        assertTrue(filter.shouldNotFilter(request));
    }
    
    @Test
    void testShouldNotFilter_RegisterEndpoint() throws ServletException {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/register");
        
        assertTrue(filter.shouldNotFilter(request));
    }
    
    @Test
    void testShouldNotFilter_ProtectedEndpoint() throws ServletException {
        when(request.getRequestURI()).thenReturn("/api/v1/organizations");
        
        assertFalse(filter.shouldNotFilter(request));
    }
}
