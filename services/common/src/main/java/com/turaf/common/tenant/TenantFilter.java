package com.turaf.common.tenant;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Servlet filter that extracts tenant information from the request and sets up the tenant context.
 * 
 * This filter should be configured with the highest precedence to ensure the tenant context
 * is available for all subsequent filters and request processing.
 * 
 * The filter extracts the organization ID and user ID from request headers (typically set by
 * the authentication layer after JWT validation) and creates a TenantContext.
 * 
 * The context is automatically cleared after the request completes to prevent memory leaks.
 */
public class TenantFilter implements Filter {
    
    private static final Logger logger = LoggerFactory.getLogger(TenantFilter.class);
    
    private static final String ORGANIZATION_ID_HEADER = "X-Organization-Id";
    private static final String USER_ID_HEADER = "X-User-Id";
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        
        try {
            String organizationId = extractOrganizationId(httpRequest);
            String userId = extractUserId(httpRequest);
            
            if (organizationId != null && userId != null) {
                TenantContext context = new TenantContext(organizationId, userId);
                TenantContextHolder.setContext(context);
                logger.debug("Tenant context set: organizationId={}, userId={}", organizationId, userId);
            } else {
                logger.debug("No tenant context set - missing headers. URI: {}", httpRequest.getRequestURI());
            }
            
            chain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
            logger.trace("Tenant context cleared");
        }
    }
    
    /**
     * Extracts the organization ID from the request.
     * Override this method to customize extraction logic (e.g., from JWT claims).
     *
     * @param request The HTTP request
     * @return The organization ID, or null if not available
     */
    protected String extractOrganizationId(HttpServletRequest request) {
        return request.getHeader(ORGANIZATION_ID_HEADER);
    }
    
    /**
     * Extracts the user ID from the request.
     * Override this method to customize extraction logic (e.g., from JWT claims).
     *
     * @param request The HTTP request
     * @return The user ID, or null if not available
     */
    protected String extractUserId(HttpServletRequest request) {
        return request.getHeader(USER_ID_HEADER);
    }
}
