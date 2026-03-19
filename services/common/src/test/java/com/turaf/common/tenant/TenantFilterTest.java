package com.turaf.common.tenant;

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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantFilterTest {
    
    @Mock
    private HttpServletRequest request;
    
    @Mock
    private HttpServletResponse response;
    
    @Mock
    private FilterChain filterChain;
    
    private TenantFilter filter;
    
    @BeforeEach
    void setUp() {
        filter = new TenantFilter();
    }
    
    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }
    
    @Test
    void testDoFilter_SetsContextFromHeaders() throws ServletException, IOException {
        when(request.getHeader("X-Organization-Id")).thenReturn("org-123");
        when(request.getHeader("X-User-Id")).thenReturn("user-456");
        
        filter.doFilter(request, response, filterChain);
        
        verify(filterChain).doFilter(request, response);
        
        // Context should be cleared after filter chain completes
        assertThat(TenantContextHolder.hasContext()).isFalse();
    }
    
    @Test
    void testDoFilter_MissingHeaders_NoContextSet() throws ServletException, IOException {
        when(request.getHeader("X-Organization-Id")).thenReturn(null);
        when(request.getHeader("X-User-Id")).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/api/test");
        
        filter.doFilter(request, response, filterChain);
        
        verify(filterChain).doFilter(request, response);
        assertThat(TenantContextHolder.hasContext()).isFalse();
    }
    
    @Test
    void testDoFilter_ClearsContextAfterException() throws ServletException, IOException {
        when(request.getHeader("X-Organization-Id")).thenReturn("org-123");
        when(request.getHeader("X-User-Id")).thenReturn("user-456");
        doThrow(new ServletException("Test exception")).when(filterChain).doFilter(request, response);
        
        assertThatThrownBy(() -> filter.doFilter(request, response, filterChain))
            .isInstanceOf(ServletException.class);
        
        // Context should still be cleared even after exception
        assertThat(TenantContextHolder.hasContext()).isFalse();
    }
    
    @Test
    void testDoFilter_ContextAvailableDuringChain() throws ServletException, IOException {
        when(request.getHeader("X-Organization-Id")).thenReturn("org-123");
        when(request.getHeader("X-User-Id")).thenReturn("user-456");
        
        doAnswer(invocation -> {
            // During filter chain execution, context should be available
            assertThat(TenantContextHolder.hasContext()).isTrue();
            assertThat(TenantContextHolder.getOrganizationId()).isEqualTo("org-123");
            assertThat(TenantContextHolder.getUserId()).isEqualTo("user-456");
            return null;
        }).when(filterChain).doFilter(request, response);
        
        filter.doFilter(request, response, filterChain);
    }
}
