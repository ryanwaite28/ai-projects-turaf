package com.turaf.bff.config;

import com.turaf.bff.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RestClientConfig {
    
    private final ServiceUrlsConfig serviceUrlsConfig;
    
    @Bean(name = "identityRestClient")
    public RestClient identityRestClient() {
        String baseUrl = serviceUrlsConfig.getIdentityUrl();
        log.info("Configuring Identity Service RestClient with base URL: {}", baseUrl);
        return createRestClient(baseUrl);
    }
    
    @Bean(name = "organizationRestClient")
    public RestClient organizationRestClient() {
        String baseUrl = serviceUrlsConfig.getOrganizationUrl();
        log.info("Configuring Organization Service RestClient with base URL: {}", baseUrl);
        return createRestClient(baseUrl);
    }
    
    @Bean(name = "experimentRestClient")
    public RestClient experimentRestClient() {
        String baseUrl = serviceUrlsConfig.getExperimentUrl();
        log.info("Configuring Experiment Service RestClient with base URL: {}", baseUrl);
        return createRestClient(baseUrl);
    }
    
    @Bean(name = "metricsRestClient")
    public RestClient metricsRestClient() {
        String baseUrl = serviceUrlsConfig.getMetricsUrl();
        log.info("Configuring Metrics Service RestClient with base URL: {}", baseUrl);
        return createRestClient(baseUrl);
    }
    
    private RestClient createRestClient(String baseUrl) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
            .withConnectTimeout(Duration.ofSeconds(10))
            .withReadTimeout(Duration.ofSeconds(20));
        
        return RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(ClientHttpRequestFactories.get(settings))
            .requestInterceptor((request, body, execution) -> {
                // Centralized logging for all HTTP client calls
                log.debug("→ {} {}", request.getMethod(), request.getURI());
                
                // Forward Authorization header from incoming request to downstream services
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attributes != null) {
                    HttpServletRequest httpRequest = attributes.getRequest();
                    String authHeader = httpRequest.getHeader("Authorization");
                    if (authHeader != null && !authHeader.isEmpty()) {
                        request.getHeaders().set("Authorization", authHeader);
                        log.trace("Forwarding Authorization header to downstream service");
                    }
                }
                
                // Extract UserContext from SecurityContext and add tenant headers
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.getPrincipal() instanceof UserContext) {
                    UserContext userContext = (UserContext) authentication.getPrincipal();
                    
                    if (userContext.getOrganizationId() != null) {
                        request.getHeaders().set("X-Organization-Id", userContext.getOrganizationId());
                        log.trace("Added X-Organization-Id header: {}", userContext.getOrganizationId());
                    }
                    
                    if (userContext.getUserId() != null) {
                        request.getHeaders().set("X-User-Id", userContext.getUserId());
                        log.trace("Added X-User-Id header: {}", userContext.getUserId());
                    }
                }
                
                var response = execution.execute(request, body);
                log.debug("← {} {}", response.getStatusCode(), request.getURI());
                return response;
            })
            .build();
    }
}
