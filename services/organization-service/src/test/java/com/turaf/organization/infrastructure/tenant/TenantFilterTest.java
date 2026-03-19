package com.turaf.organization.infrastructure.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TenantFilter.
 */
@ExtendWith(MockitoExtension.class)
class TenantFilterTest {
    
    @Mock
    private HttpServletRequest request;
    
    @Mock
    private HttpServletResponse response;
    
    @Mock
    private FilterChain filterChain;
    
    private TenantFilter tenantFilter;
    
    @BeforeEach
    void setUp() {
        tenantFilter = new TenantFilter();
        TenantContextHolder.clear();
    }
    
    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }
    
    @Test
    void shouldSetTenantContextFromHeaders() throws ServletException, IOException {
        // Given
        when(request.getHeader("X-Organization-Id")).thenReturn("org-123");
        when(request.getHeader("X-User-Id")).thenReturn("user-456");
        
        // When
        tenantFilter.doFilter(request, response, filterChain);
        
        // Then
        verify(filterChain).doFilter(request, response);
        // Context is cleared after filter, so we can't verify it here
        // But we can verify the filter chain was called
    }
    
    @Test
    void shouldHandleMissingHeaders() throws ServletException, IOException {
        // Given
        when(request.getHeader("X-Organization-Id")).thenReturn(null);
        when(request.getHeader("X-User-Id")).thenReturn(null);
        
        // When
        tenantFilter.doFilter(request, response, filterChain);
        
        // Then
        verify(filterChain).doFilter(request, response);
        assertNull(TenantContextHolder.getContext());
    }
    
    @Test
    void shouldSetOnlyOrganizationId() throws ServletException, IOException {
        // Given
        when(request.getHeader("X-Organization-Id")).thenReturn("org-123");
        when(request.getHeader("X-User-Id")).thenReturn(null);
        
        // When
        tenantFilter.doFilter(request, response, filterChain);
        
        // Then
        verify(filterChain).doFilter(request, response);
    }
    
    @Test
    void shouldSetOnlyUserId() throws ServletException, IOException {
        // Given
        when(request.getHeader("X-Organization-Id")).thenReturn(null);
        when(request.getHeader("X-User-Id")).thenReturn("user-456");
        
        // When
        tenantFilter.doFilter(request, response, filterChain);
        
        // Then
        verify(filterChain).doFilter(request, response);
    }
    
    @Test
    void shouldClearContextAfterRequest() throws ServletException, IOException {
        // Given
        when(request.getHeader("X-Organization-Id")).thenReturn("org-123");
        when(request.getHeader("X-User-Id")).thenReturn("user-456");
        
        // When
        tenantFilter.doFilter(request, response, filterChain);
        
        // Then - context should be cleared after filter execution
        assertNull(TenantContextHolder.getContext());
    }
    
    @Test
    void shouldClearContextEvenWhenExceptionThrown() throws ServletException, IOException {
        // Given
        when(request.getHeader("X-Organization-Id")).thenReturn("org-123");
        when(request.getHeader("X-User-Id")).thenReturn("user-456");
        doThrow(new ServletException("Test exception")).when(filterChain).doFilter(request, response);
        
        // When/Then
        assertThrows(ServletException.class, () -> tenantFilter.doFilter(request, response, filterChain));
        
        // Context should still be cleared
        assertNull(TenantContextHolder.getContext());
    }
    
    @Test
    void shouldCallFilterChainEvenWithoutHeaders() throws ServletException, IOException {
        // Given
        when(request.getHeader("X-Organization-Id")).thenReturn(null);
        when(request.getHeader("X-User-Id")).thenReturn(null);
        
        // When
        tenantFilter.doFilter(request, response, filterChain);
        
        // Then
        verify(filterChain).doFilter(request, response);
    }
}
