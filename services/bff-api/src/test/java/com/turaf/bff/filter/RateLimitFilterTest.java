package com.turaf.bff.filter;

import com.turaf.bff.security.UserContext;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {
    
    private RateLimitFilter rateLimitFilter;
    private RateLimiterRegistry rateLimiterRegistry;
    
    @Mock
    private HttpServletRequest request;
    
    @Mock
    private HttpServletResponse response;
    
    @Mock
    private FilterChain filterChain;
    
    @BeforeEach
    void setUp() {
        RateLimiterConfig config = RateLimiterConfig.custom()
            .limitForPeriod(2)
            .limitRefreshPeriod(Duration.ofMinutes(1))
            .timeoutDuration(Duration.ofSeconds(0))
            .build();
        
        rateLimiterRegistry = RateLimiterRegistry.of(config);
        rateLimitFilter = new RateLimitFilter(rateLimiterRegistry);
        SecurityContextHolder.clearContext();
    }
    
    @Test
    void testRateLimitNotExceeded_AllowsRequest() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/organizations");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        
        rateLimitFilter.doFilterInternal(request, response, filterChain);
        
        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(HttpServletResponse.SC_TOO_MANY_REQUESTS);
    }
    
    @Test
    void testRateLimitExceeded_ReturnsError() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/organizations");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(writer);
        
        rateLimitFilter.doFilterInternal(request, response, filterChain);
        rateLimitFilter.doFilterInternal(request, response, filterChain);
        rateLimitFilter.doFilterInternal(request, response, filterChain);
        
        verify(response, atLeastOnce()).setStatus(HttpServletResponse.SC_TOO_MANY_REQUESTS);
        verify(response, atLeastOnce()).setContentType("application/json");
        
        String responseBody = stringWriter.toString();
        assertTrue(responseBody.contains("Too Many Requests"));
        assertTrue(responseBody.contains("Rate limit exceeded"));
    }
    
    @Test
    void testAuthenticatedUser_UsesUserId() throws Exception {
        UserContext userContext = UserContext.builder()
            .userId("user-123")
            .email("test@example.com")
            .build();
        
        UsernamePasswordAuthenticationToken authentication = 
            new UsernamePasswordAuthenticationToken(userContext, null, userContext.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        when(request.getRequestURI()).thenReturn("/api/v1/organizations");
        
        rateLimitFilter.doFilterInternal(request, response, filterChain);
        
        verify(filterChain).doFilter(request, response);
    }
    
    @Test
    void testPublicEndpoint_UsesStricterLimit() throws Exception {
        RateLimiterConfig publicConfig = RateLimiterConfig.custom()
            .limitForPeriod(1)
            .limitRefreshPeriod(Duration.ofMinutes(1))
            .timeoutDuration(Duration.ofSeconds(0))
            .build();
        
        rateLimiterRegistry.rateLimiter("public", publicConfig);
        
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(request.getRemoteAddr()).thenReturn("192.168.1.2");
        
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(writer);
        
        rateLimitFilter.doFilterInternal(request, response, filterChain);
        rateLimitFilter.doFilterInternal(request, response, filterChain);
        
        verify(response, atLeastOnce()).setStatus(HttpServletResponse.SC_TOO_MANY_REQUESTS);
    }
    
    @Test
    void testXForwardedForHeader_UsesProxiedIp() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/experiments");
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1, 10.0.0.2");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        
        rateLimitFilter.doFilterInternal(request, response, filterChain);
        
        verify(filterChain).doFilter(request, response);
    }
    
    @Test
    void testActuatorEndpoint_SkipsRateLimiting() throws Exception {
        when(request.getRequestURI()).thenReturn("/actuator/health");
        
        boolean shouldFilter = !rateLimitFilter.shouldNotFilter(request);
        
        assertFalse(shouldFilter);
    }
}
