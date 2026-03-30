# Task 003: Implement Wait Helpers

**Status**: Pending  
**Assignee**: TBD  
**Estimated Time**: 4 hours  
**Related Spec**: [Architecture Testing](../../specs/architecture-testing.md)

---

## Objective

Implement Java helper classes for waiting on asynchronous processes (API polling, EventBridge, Lambda, S3).

---

## Prerequisites

- Task 001 and 002 completed
- Understanding of Awaitility library
- AWS SDK knowledge

---

## Tasks

### 1. Create WaitHelper Class

Create `src/test/java/com/turaf/architecture/helpers/WaitHelper.java`:

```java
package com.turaf.architecture.helpers;

import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;

import java.time.Duration;
import java.util.concurrent.Callable;

public class WaitHelper {
    
    /**
     * Wait for condition to be true
     */
    public static boolean waitForCondition(Callable<Boolean> condition, int timeoutSeconds) {
        try {
            Awaitility.await()
                .atMost(Duration.ofSeconds(timeoutSeconds))
                .pollInterval(Duration.ofSeconds(1))
                .until(condition);
            return true;
        } catch (ConditionTimeoutException e) {
            return false;
        }
    }
    
    /**
     * Wait for report generation to complete
     */
    public static boolean waitForReport(String experimentId, int timeoutMs) {
        int timeoutSeconds = timeoutMs / 1000;
        return waitForCondition(() -> {
            // This would make actual HTTP call to check report status
            // For now, return placeholder
            return checkReportExists(experimentId);
        }, timeoutSeconds);
    }
    
    /**
     * Poll endpoint until specific field has expected value
     */
    public static boolean waitForFieldValue(String endpoint, String jsonPath, Object expectedValue, int timeoutSeconds) {
        return waitForCondition(() -> {
            // Make HTTP request and check field value
            return checkFieldValue(endpoint, jsonPath, expectedValue);
        }, timeoutSeconds);
    }
    
    private static boolean checkReportExists(String experimentId) {
        // Implementation will be added when integrating with actual API
        return true;
    }
    
    private static boolean checkFieldValue(String endpoint, String jsonPath, Object expectedValue) {
        // Implementation will be added when integrating with actual API
        return true;
    }
}
```

### 2. Create EventHelper Class

Create `src/test/java/com/turaf/architecture/helpers/EventHelper.java`:

```java
package com.turaf.architecture.helpers;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;
import org.awaitility.Awaitility;

import java.time.Duration;
import java.util.List;

public class EventHelper {
    
    private static SqsClient sqsClient;
    
    static {
        sqsClient = SqsClient.builder().build();
    }
    
    /**
     * Wait for EventBridge event to be processed
     */
    public static void waitForEventProcessing(String eventType, int timeoutSeconds) {
        Awaitility.await()
            .atMost(Duration.ofSeconds(timeoutSeconds))
            .pollInterval(Duration.ofSeconds(1))
            .until(() -> {
                // Check if event was processed
                // This is a placeholder - actual implementation depends on event tracking
                return true;
            });
    }
    
    /**
     * Get SQS message from queue
     */
    public static String getSqsMessage(String queueUrl) {
        try {
            ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(1)
                .waitTimeSeconds(5)
                .build();
                
            ReceiveMessageResponse response = sqsClient.receiveMessage(request);
            List<Message> messages = response.messages();
            
            return messages.isEmpty() ? null : messages.get(0).body();
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Wait for Lambda execution to complete
     */
    public static void waitForLambdaExecution(String functionName, int timeoutSeconds) {
        Awaitility.await()
            .atMost(Duration.ofSeconds(timeoutSeconds))
            .pollInterval(Duration.ofSeconds(2))
            .until(() -> {
                // Check CloudWatch logs or Lambda invocation status
                // Placeholder implementation
                return true;
            });
    }
    
    /**
     * Get notification for organization event
     */
    public static Object getNotification(String orgId, String eventType) {
        // Placeholder - would query notification service or SQS
        return new Object();
    }
}
```

### 3. Create AwsHelper Class

Create `src/test/java/com/turaf/architecture/helpers/AwsHelper.java`:

```java
package com.turaf.architecture.helpers;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import org.awaitility.Awaitility;

import java.time.Duration;

public class AwsHelper {
    
    private static S3Client s3Client;
    
    static {
        s3Client = S3Client.builder().build();
    }
    
    /**
     * Verify S3 object exists
     */
    public static boolean verifyS3Object(String bucket, String key) {
        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
                
            s3Client.headObject(request);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Wait for S3 object to exist
     */
    public static boolean waitForS3Object(String bucket, String key, int timeoutSeconds) {
        try {
            Awaitility.await()
                .atMost(Duration.ofSeconds(timeoutSeconds))
                .pollInterval(Duration.ofSeconds(2))
                .until(() -> verifyS3Object(bucket, key));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```

### 4. Create TokenHelper Class

Create `src/test/java/com/turaf/architecture/helpers/TokenHelper.java`:

```java
package com.turaf.architecture.helpers;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class TokenHelper {
    
    /**
     * Extract user ID from JWT token
     */
    public static String extractUserId(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return null;
            }
            
            String payload = new String(Base64.getDecoder().decode(parts[1]));
            // Parse JSON and extract userId
            // Simplified implementation
            return "user-id";
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Check if token is expired
     */
    public static boolean isTokenExpired(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return true;
            }
            
            String payload = new String(Base64.getDecoder().decode(parts[1]));
            // Parse JSON and check exp claim
            // Simplified implementation
            return false;
        } catch (Exception e) {
            return true;
        }
    }
}
```

---

## Acceptance Criteria

- [ ] WaitHelper class created with polling methods
- [ ] EventHelper class created with SQS and event methods
- [ ] AwsHelper class created with S3 verification
- [ ] TokenHelper class created with JWT utilities
- [ ] All classes compile without errors
- [ ] Helper methods can be called from Karate tests

---

## Verification

```bash
mvn clean compile
# Should compile without errors
```

---

## Notes

- Helpers use Awaitility for robust polling
- AWS SDK clients are initialized statically
- Actual implementations will be enhanced as tests are written
- These are foundation classes that will evolve
