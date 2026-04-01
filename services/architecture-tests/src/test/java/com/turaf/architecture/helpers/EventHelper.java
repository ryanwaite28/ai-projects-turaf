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
