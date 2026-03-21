package com.turaf.common.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * Service for tracking processed events to ensure idempotent event processing.
 * 
 * This component uses DynamoDB to track which events have been processed, preventing
 * duplicate processing when the same event is delivered multiple times (at-least-once delivery).
 * 
 * Key features:
 * - Check if an event has already been processed
 * - Mark events as processed with conditional writes
 * - Automatic cleanup via DynamoDB TTL (30 days)
 * - Retrieve processing records for audit/debugging
 * 
 * Following SOLID principles:
 * - Single Responsibility: Only handles idempotency tracking
 * - Dependency Inversion: Depends on DynamoDbClient abstraction
 * 
 * Following event-driven architecture best practices:
 * - Prevents duplicate processing in at-least-once delivery systems
 * - Uses conditional writes to prevent race conditions
 * - TTL ensures records don't accumulate indefinitely
 */
@Component
public class IdempotencyService {
    
    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);
    private static final int TTL_DAYS = 30;
    
    private final DynamoDbClient dynamoDbClient;
    private final String tableName;
    
    /**
     * Constructs an IdempotencyService with required dependencies.
     * 
     * @param dynamoDbClient AWS DynamoDB client for table operations
     * @param tableName name of the DynamoDB idempotency table
     */
    public IdempotencyService(
            DynamoDbClient dynamoDbClient,
            @Value("${aws.dynamodb.idempotency-table}") String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
        
        log.info("IdempotencyService initialized with table: {}", tableName);
    }
    
    /**
     * Checks if an event has already been processed.
     * 
     * This method performs a DynamoDB GetItem operation to check for the event ID.
     * It's designed to be fast and efficient for the common case of checking
     * before processing an event.
     * 
     * @param eventId the unique identifier of the event
     * @return true if the event has been processed, false otherwise
     */
    public boolean isProcessed(String eventId) {
        try {
            log.debug("Checking if event is already processed: {}", eventId);
            
            GetItemRequest request = GetItemRequest.builder()
                .tableName(tableName)
                .key(Map.of("eventId", AttributeValue.builder().s(eventId).build()))
                .projectionExpression("eventId") // Only fetch the key to minimize data transfer
                .build();
            
            GetItemResponse response = dynamoDbClient.getItem(request);
            boolean processed = response.hasItem();
            
            if (processed) {
                log.info("Event already processed: {}", eventId);
            }
            
            return processed;
            
        } catch (Exception e) {
            log.error("Error checking idempotency for event: {}", eventId, e);
            // Fail open: if we can't check, allow processing to prevent blocking
            // The conditional write in markProcessed will still prevent duplicates
            return false;
        }
    }
    
    /**
     * Marks an event as processed in the idempotency table.
     * 
     * This method uses a conditional write (attribute_not_exists) to ensure that
     * only the first attempt to mark an event as processed succeeds. If multiple
     * handlers try to process the same event concurrently, only one will succeed.
     * 
     * The record includes a TTL attribute set to 30 days from now, ensuring
     * automatic cleanup of old records.
     * 
     * @param eventId the unique identifier of the event
     * @param eventType the type of the event
     * @param handler the name of the handler/service processing the event
     */
    public void markProcessed(String eventId, String eventType, String handler) {
        try {
            log.debug("Marking event as processed: {} by handler: {}", eventId, handler);
            
            long ttl = getTtl();
            
            PutItemRequest request = PutItemRequest.builder()
                .tableName(tableName)
                .item(Map.of(
                    "eventId", AttributeValue.builder().s(eventId).build(),
                    "eventType", AttributeValue.builder().s(eventType).build(),
                    "handler", AttributeValue.builder().s(handler).build(),
                    "processedAt", AttributeValue.builder().s(Instant.now().toString()).build(),
                    "ttl", AttributeValue.builder().n(String.valueOf(ttl)).build()
                ))
                .conditionExpression("attribute_not_exists(eventId)")
                .build();
            
            dynamoDbClient.putItem(request);
            
            log.info("Successfully marked event as processed: {} by handler: {}", eventId, handler);
            
        } catch (ConditionalCheckFailedException e) {
            // This is expected if the event was already processed
            log.warn("Event already processed (concurrent processing detected): {}", eventId);
        } catch (Exception e) {
            log.error("Error marking event as processed: {}", eventId, e);
            throw new RuntimeException("Failed to mark event as processed: " + eventId, e);
        }
    }
    
    /**
     * Retrieves the idempotency record for a specific event.
     * 
     * This method is useful for debugging and auditing to see when and by whom
     * an event was processed.
     * 
     * @param eventId the unique identifier of the event
     * @return the IdempotencyRecord if found, null otherwise
     */
    public IdempotencyRecord getRecord(String eventId) {
        try {
            log.debug("Retrieving idempotency record for event: {}", eventId);
            
            GetItemRequest request = GetItemRequest.builder()
                .tableName(tableName)
                .key(Map.of("eventId", AttributeValue.builder().s(eventId).build()))
                .build();
            
            GetItemResponse response = dynamoDbClient.getItem(request);
            
            if (!response.hasItem()) {
                log.debug("No idempotency record found for event: {}", eventId);
                return null;
            }
            
            Map<String, AttributeValue> item = response.item();
            
            IdempotencyRecord record = new IdempotencyRecord(
                item.get("eventId").s(),
                item.get("eventType").s(),
                item.get("handler").s(),
                Instant.parse(item.get("processedAt").s())
            );
            
            log.debug("Retrieved idempotency record: {}", record);
            return record;
            
        } catch (Exception e) {
            log.error("Error retrieving idempotency record for event: {}", eventId, e);
            return null;
        }
    }
    
    /**
     * Calculates the TTL (Time To Live) value for DynamoDB records.
     * 
     * DynamoDB TTL uses epoch seconds. This method returns a timestamp
     * 30 days in the future, after which DynamoDB will automatically
     * delete the record.
     * 
     * @return epoch seconds for 30 days from now
     */
    private long getTtl() {
        return Instant.now().plus(TTL_DAYS, ChronoUnit.DAYS).getEpochSecond();
    }
}
