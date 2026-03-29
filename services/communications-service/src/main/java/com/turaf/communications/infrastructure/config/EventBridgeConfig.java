package com.turaf.communications.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import java.net.URI;

@Configuration
public class EventBridgeConfig {
    
    @Value("${aws.region:us-east-1}")
    private String region;
    
    @Value("${aws.endpoint:}")
    private String endpoint;
    
    @Bean
    public EventBridgeClient eventBridgeClient() {
        var builder = EventBridgeClient.builder()
            .region(Region.of(region))
            .credentialsProvider(DefaultCredentialsProvider.create());
        
        if (endpoint != null && !endpoint.isEmpty()) {
            builder.endpointOverride(URI.create(endpoint));
        }
        
        return builder.build();
    }
}
