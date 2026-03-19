package com.turaf.organization.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;

/**
 * Configuration for AWS EventBridge client.
 */
@Configuration
public class EventBridgeConfig {
    
    @Value("${aws.region:us-east-1}")
    private String awsRegion;
    
    @Bean
    @Profile("!test")
    public EventBridgeClient eventBridgeClient() {
        return EventBridgeClient.builder()
            .region(Region.of(awsRegion))
            .build();
    }
    
    /**
     * Mock EventBridge client for testing.
     * In production, this would be replaced by the actual client.
     */
    @Bean
    @Profile("test")
    public EventBridgeClient testEventBridgeClient() {
        // For testing, we'll use a mock or LocalStack
        return EventBridgeClient.builder()
            .region(Region.US_EAST_1)
            .build();
    }
}
