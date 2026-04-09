package com.turaf.bff.config;

import com.turaf.bff.clients.ExperimentServiceClient;
import com.turaf.bff.clients.HypothesisServiceClient;
import com.turaf.bff.clients.IdentityServiceClient;
import com.turaf.bff.clients.MetricsServiceClient;
import com.turaf.bff.clients.OrganizationServiceClient;
import com.turaf.bff.clients.ProblemServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * Configuration for Spring's declarative HTTP clients using @HttpExchange.
 * 
 * This configuration creates proxy implementations of HTTP client interfaces
 * at runtime, eliminating boilerplate REST client code.
 */
@Slf4j
@Configuration
public class HttpExchangeConfig {
    
    @Bean
    public IdentityServiceClient identityServiceClient(
            @Qualifier("identityRestClient") RestClient restClient) {
        log.info("Creating IdentityServiceClient HTTP exchange proxy");
        return createHttpServiceProxy(restClient, IdentityServiceClient.class);
    }
    
    @Bean
    public OrganizationServiceClient organizationServiceClient(
            @Qualifier("organizationRestClient") RestClient restClient) {
        log.info("Creating OrganizationServiceClient HTTP exchange proxy");
        return createHttpServiceProxy(restClient, OrganizationServiceClient.class);
    }
    
    @Bean
    public ExperimentServiceClient experimentServiceClient(
            @Qualifier("experimentRestClient") RestClient restClient) {
        log.info("Creating ExperimentServiceClient HTTP exchange proxy");
        return createHttpServiceProxy(restClient, ExperimentServiceClient.class);
    }
    
    @Bean
    public HypothesisServiceClient hypothesisServiceClient(
            @Qualifier("experimentRestClient") RestClient restClient) {
        log.info("Creating HypothesisServiceClient HTTP exchange proxy");
        return createHttpServiceProxy(restClient, HypothesisServiceClient.class);
    }
    
    @Bean
    public ProblemServiceClient problemServiceClient(
            @Qualifier("experimentRestClient") RestClient restClient) {
        log.info("Creating ProblemServiceClient HTTP exchange proxy");
        return createHttpServiceProxy(restClient, ProblemServiceClient.class);
    }
    
    /**
     * Creates a MetricsServiceClient proxy using HttpServiceProxyFactory.
     * 
     * @param restClient The configured RestClient for the Metrics Service
     * @return Proxy implementation of MetricsServiceClient
     */
    @Bean
    public MetricsServiceClient metricsServiceClient(
            @Qualifier("metricsRestClient") RestClient restClient) {
        log.info("Creating MetricsServiceClient HTTP exchange proxy");
        return createHttpServiceProxy(restClient, MetricsServiceClient.class);
    }
    
    /**
     * Generic method to create HTTP service proxies.
     * 
     * @param restClient The configured RestClient instance
     * @param clientClass The interface class to proxy
     * @param <T> The client interface type
     * @return Proxy implementation of the client interface
     */
    private <T> T createHttpServiceProxy(RestClient restClient, Class<T> clientClass) {
        HttpServiceProxyFactory factory = HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(restClient))
            .build();
        return factory.createClient(clientClass);
    }
}
