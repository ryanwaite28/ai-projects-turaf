# Task: Implement Webhook Service

**Service**: Notification Service  
**Phase**: 8  
**Estimated Time**: 3 hours  

## Objective

Implement webhook delivery service with retry logic for external integrations.

## Prerequisites

- [x] Task 002: Event handlers implemented

## Scope

**Files to Create**:
- `services/notification-service/services/webhook_service.py`
- `services/notification-service/services/webhook_signer.py`
- `services/notification-service/clients/webhook_client.py`

## Implementation Details

### Webhook Service

```python
import requests
import json
import hmac
import hashlib
import time
from typing import Dict, List
public class WebhookService {
    private final WebhookDeliveryService deliveryService;
    private final WebhookConfigService configService;
    
    public void sendWebhook(String organizationId, String eventType, Map<String, Object> payload) {
        // Get webhook configurations for organization
        List<WebhookConfig> configs = configService.getWebhooks(organizationId, eventType);
        
        for (WebhookConfig config : configs) {
            WebhookPayload webhookPayload = new WebhookPayload(
                eventType,
                payload,
                Instant.now()
            );
            
            deliveryService.deliver(config.getUrl(), webhookPayload, config.getSecret());
        }
    }
}
```

### Webhook Delivery Service

```java
public class WebhookDeliveryService {
    private final HttpClient httpClient;
    private static final int MAX_RETRIES = 3;
    private static final Duration RETRY_DELAY = Duration.ofSeconds(5);
    
    public void deliver(String url, WebhookPayload payload, String secret) {
        String jsonPayload = serializePayload(payload);
        String signature = generateSignature(jsonPayload, secret);
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .header("X-Webhook-Signature", signature)
            .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
            .timeout(Duration.ofSeconds(30))
            .build();
        
        deliverWithRetry(request, 0);
    }
    
    private void deliverWithRetry(HttpRequest request, int attempt) {
        try {
            HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
            );
            
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                // Success
                return;
            }
            
            if (attempt < MAX_RETRIES) {
                Thread.sleep(RETRY_DELAY.toMillis() * (attempt + 1));
                deliverWithRetry(request, attempt + 1);
            } else {
                throw new WebhookDeliveryException("Failed after " + MAX_RETRIES + " retries");
            }
        } catch (Exception e) {
            if (attempt < MAX_RETRIES) {
                try {
                    Thread.sleep(RETRY_DELAY.toMillis() * (attempt + 1));
                    deliverWithRetry(request, attempt + 1);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new WebhookDeliveryException("Delivery interrupted", ie);
                }
            } else {
                throw new WebhookDeliveryException("Failed to deliver webhook", e);
            }
        }
    }
    
    private String generateSignature(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(payload.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate signature", e);
        }
    }
    
    private String serializePayload(WebhookPayload payload) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize payload", e);
        }
    }
}
```

## Acceptance Criteria

- [x] Webhook delivery works
- [x] Retry logic implemented
- [x] Signature generation works
- [x] Timeout handling implemented
- [x] Unit tests pass

## Testing Requirements

**Unit Tests**:
- Test webhook delivery
- Test retry logic
- Test signature generation

**Test Files to Create**:
- `test_webhook_service.py`
- `WebhookDeliveryServiceTest.java`

## References

- Specification: `specs/notification-service.md` (Webhook Service section)
- Related Tasks: 006-implement-recipient-selection
