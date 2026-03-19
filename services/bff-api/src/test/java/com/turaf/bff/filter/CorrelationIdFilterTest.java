package com.turaf.bff.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CorrelationIdFilterTest {
    
    private CorrelationIdFilter filter;
    
    @Mock
    private HttpServletRequest request;
    
    @Mock
    private HttpServletResponse response;
    
    @Mock
    private FilterChain filterChain;
    
    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
        MDC.clear();
    }
    
    @Test
    void testCorrelationId_WhenProvidedInHeader() throws Exception {
        String existingCorrelationId = "existing-correlation-id";
        when(request.getHeader("X-Correlation-Id")).thenReturn(existingCorrelationId);
        
        filter.doFilterInternal(request, response, filterChain);
        
        verify(response).setHeader("X-Correlation-Id", existingCorrelationId);
        verify(filterChain).doFilter(request, response);
    }
    
    @Test
    void testCorrelationId_WhenNotProvided_GeneratesNew() throws Exception {
        when(request.getHeader("X-Correlation-Id")).thenReturn(null);
        
        filter.doFilterInternal(request, response, filterChain);
        
        verify(response).setHeader(eq("X-Correlation-Id"), anyString());
        verify(filterChain).doFilter(request, response);
    }
    
    @Test
    void testMDC_ClearedAfterRequest() throws Exception {
        when(request.getHeader("X-Correlation-Id")).thenReturn("test-id");
        
        filter.doFilterInternal(request, response, filterChain);
        
        assertNull(MDC.get("correlationId"));
    }
    
    @Test
    void testMDC_SetDuringRequest() throws Exception {
        String correlationId = "test-correlation-id";
        when(request.getHeader("X-Correlation-Id")).thenReturn(correlationId);
        
        doAnswer(invocation -> {
            assertEquals(correlationId, MDC.get("correlationId"));
            return null;
        }).when(filterChain).doFilter(request, response);
        
        filter.doFilterInternal(request, response, filterChain);
    }
}
