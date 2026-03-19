package com.turaf.organization.infrastructure.tenant;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Servlet filter that extracts tenant context from request headers.
 * Sets the tenant context in TenantContextHolder for the duration of the request.
 */
public class TenantFilter implements Filter {
    
    private static final Logger log = LoggerFactory.getLogger(TenantFilter.class);
    
    private static final String ORGANIZATION_ID_HEADER = "X-Organization-Id";
    private static final String USER_ID_HEADER = "X-User-Id";
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        
        try {
            String organizationId = httpRequest.getHeader(ORGANIZATION_ID_HEADER);
            String userId = httpRequest.getHeader(USER_ID_HEADER);
            
            if (organizationId != null || userId != null) {
                TenantContext context = new TenantContext(organizationId, userId);
                TenantContextHolder.setContext(context);
                
                log.debug("Tenant context set - Organization: {}, User: {}", organizationId, userId);
            } else {
                log.debug("No tenant context headers found in request");
            }
            
            chain.doFilter(request, response);
            
        } finally {
            // Always clear context after request to prevent memory leaks
            TenantContextHolder.clear();
            log.debug("Tenant context cleared");
        }
    }
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("TenantFilter initialized");
    }
    
    @Override
    public void destroy() {
        log.info("TenantFilter destroyed");
    }
}
