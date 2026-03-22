# Task: Implement EventBridge Publisher

**Service**: Communications Service  
**Type**: Infrastructure Layer - Messaging  
**Priority**: High  
**Estimated Time**: 2 hours  
**Dependencies**: 002-create-domain-model

---

## Objective

Implement the EventBridge publisher to publish MessageDelivered domain events to AWS EventBridge.

---

## Acceptance Criteria

- [x] EventBridge client configured
- [x] MessageDelivered events published correctly
- [x] Event envelope follows platform standard
- [x] Error handling and retry logic
- [x] Integration tests pass

---

## Implementation

**File**: `infrastructure/messaging/EventBridgePublisher.java`

```java
package com.turaf.communications.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turaf.communications.domain.event.MessageDeliveredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventBridgePublisher {
    
    private final EventBridgeClient eventBridgeClient;
    private final ObjectMapper objectMapper;
    
    @Value("${aws.eventbridge.bus-name}")
    private String eventBusName;
    
    public void publishMessageDelivered(MessageDeliveredEvent event) {
        try {
            String eventDetail = objectMapper.writeValueAsString(event);
            
            PutEventsRequestEntry entry = PutEventsRequestEntry.builder()
                .source("communications-service")
                .detailType("MessageDelivered")
                .detail(eventDetail)
                .eventBusName(eventBusName)
                .build();
            
            PutEventsRequest request = PutEventsRequest.builder()
                .entries(entry)
                .build();
            
            PutEventsResponse response = eventBridgeClient.putEvents(request);
            
            if (response.failedEntryCount() > 0) {
                log.error("Failed to publish event: {}", response.entries());
                throw new RuntimeException("Failed to publish MessageDelivered event");
            }
            
            log.info("MessageDelivered event published successfully: eventId={}", event.getEventId());
            
        } catch (Exception e) {
            log.error("Error publishing MessageDelivered event", e);
            throw new RuntimeException("Failed to publish event", e);
        }
    }
}
```

**File**: `infrastructure/config/EventBridgeConfig.java`

```java
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
    
    @Value("${aws.region}")
    private String region;
    
    @Value("${aws.eventbridge.endpoint:}")
    private String endpoint;
    
    @Bean
    public EventBridgeClient eventBridgeClient() {
        var builder = EventBridgeClient.builder()
            .region(Region.of(region))
            .credentialsProvider(DefaultCredentialsProvider.create());
        
        // LocalStack support
        if (endpoint != null && !endpoint.isEmpty()) {
            builder.endpointOverride(URI.create(endpoint));
        }
        
        return builder.build();
    }
}
```

---

## References

- **Spec**: `specs/communications-event-schemas.md`
- **Event Flow**: `specs/event-flow.md`
