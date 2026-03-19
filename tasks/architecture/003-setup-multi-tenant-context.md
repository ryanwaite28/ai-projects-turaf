# Task: Setup Multi-Tenant Context

**Service**: Architecture Foundation  
**Phase**: 1  
**Estimated Time**: 3-4 hours  

## Objective

Implement the multi-tenant context management infrastructure that ensures tenant isolation across all services using organizationId as the tenant identifier.

## Prerequisites

- [x] Task 001: Clean Architecture layers established
- [x] Task 002: DDD patterns implemented
- [ ] Understanding of multi-tenancy patterns

## Scope

**Files to Create**:
- `services/common/src/main/java/com/turaf/common/tenant/TenantContext.java`
- `services/common/src/main/java/com/turaf/common/tenant/TenantContextHolder.java`
- `services/common/src/main/java/com/turaf/common/tenant/TenantFilter.java`
- `services/common/src/main/java/com/turaf/common/tenant/TenantInterceptor.java`
- `services/common/src/main/java/com/turaf/common/tenant/TenantAware.java`
- `services/common/src/main/java/com/turaf/common/tenant/TenantException.java`

## Implementation Details

### Tenant Context

```java
public class TenantContext {
    private final String organizationId;
    private final String userId;
    
    public TenantContext(String organizationId, String userId) {
        this.organizationId = Objects.requireNonNull(organizationId);
        this.userId = Objects.requireNonNull(userId);
    }
    
    public String getOrganizationId() {
        return organizationId;
    }
    
    public String getUserId() {
        return userId;
    }
}
```

### Tenant Context Holder

```java
public class TenantContextHolder {
    private static final ThreadLocal<TenantContext> contextHolder = new ThreadLocal<>();
    
    public static void setContext(TenantContext context) {
        contextHolder.set(context);
    }
    
    public static TenantContext getContext() {
        TenantContext context = contextHolder.get();
        if (context == null) {
            throw new TenantException("No tenant context available");
        }
        return context;
    }
    
    public static String getOrganizationId() {
        return getContext().getOrganizationId();
    }
    
    public static String getUserId() {
        return getContext().getUserId();
    }
    
    public static void clear() {
        contextHolder.remove();
    }
}
```

### Tenant Filter

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TenantFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            
            // Extract organizationId and userId from JWT token
            String organizationId = extractOrganizationId(httpRequest);
            String userId = extractUserId(httpRequest);
            
            TenantContext context = new TenantContext(organizationId, userId);
            TenantContextHolder.setContext(context);
            
            chain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }
    
    private String extractOrganizationId(HttpServletRequest request) {
        // Extract from JWT claims
        // Implementation depends on JWT structure
        return "org-id-from-jwt";
    }
    
    private String extractUserId(HttpServletRequest request) {
        // Extract from JWT claims
        return "user-id-from-jwt";
    }
}
```

### Tenant Interceptor (for JPA)

```java
@Component
public class TenantInterceptor implements Interceptor {
    
    @Override
    public boolean onSave(Object entity, Serializable id, Object[] state, 
                          String[] propertyNames, Type[] types) {
        if (entity instanceof TenantAware) {
            String organizationId = TenantContextHolder.getOrganizationId();
            ((TenantAware) entity).setOrganizationId(organizationId);
            
            // Update state array
            for (int i = 0; i < propertyNames.length; i++) {
                if ("organizationId".equals(propertyNames[i])) {
                    state[i] = organizationId;
                    return true;
                }
            }
        }
        return false;
    }
}
```

### Tenant Aware Interface

```java
public interface TenantAware {
    String getOrganizationId();
    void setOrganizationId(String organizationId);
}
```

### Tenant Exception

```java
public class TenantException extends RuntimeException {
    public TenantException(String message) {
        super(message);
    }
    
    public TenantException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

### JPA Configuration

Add to application configuration:
```java
@Configuration
public class TenantConfiguration {
    
    @Bean
    public FilterRegistrationBean<TenantFilter> tenantFilter() {
        FilterRegistrationBean<TenantFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new TenantFilter());
        registration.addUrlPatterns("/api/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
```

## Acceptance Criteria

- [x] TenantContext stores organizationId and userId
- [x] TenantContextHolder provides thread-safe access to context
- [x] TenantFilter extracts tenant info from JWT and sets context
- [x] TenantInterceptor automatically sets organizationId on entities
- [x] TenantAware interface defined for entities
- [x] Context is cleared after each request
- [x] TenantException thrown when context is missing
- [x] All tests pass
- [x] Documentation explains multi-tenant strategy

## Testing Requirements

**Unit Tests**:
- Test TenantContextHolder set/get/clear operations
- Test TenantFilter context extraction and cleanup
- Test TenantInterceptor sets organizationId on save
- Test TenantException is thrown when context missing

**Integration Tests**:
- Test that organizationId is automatically set on entity save
- Test that context is properly isolated between threads
- Test that context is cleared after request completion

**Test Files to Create**:
- `TenantContextHolderTest.java`
- `TenantFilterTest.java`
- `TenantInterceptorTest.java`

## References

- Specification: `specs/architecture.md` (Multi-Tenant Architecture section)
- PROJECT.md: Section 9 (Multi-Tenant Architecture)
- Related Tasks: All service implementations depend on this for tenant isolation
