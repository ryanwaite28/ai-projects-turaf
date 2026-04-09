package com.turaf.bff.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

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
            .build();
    }
}
