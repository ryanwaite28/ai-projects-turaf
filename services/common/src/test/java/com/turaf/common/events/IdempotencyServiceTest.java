package com.turaf.common.events;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for IdempotencyService.
 * 
 * Tests cover:
 * - Checking if events are processed
 * - Marking events as processed
 * - Duplicate prevention with conditional writes
 * - TTL calculation
 * - Record retrieval
 * - Error handling
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IdempotencyService")
class IdempotencyServiceTest {
    
    @Mock
    private DynamoDbClient dynamoDbClient;
    
    private IdempotencyService service;
    private String tableName = "test-idempotency-table";
    private String eventId;
    private String eventType = "test.EventOccurred";
    private String handler = "test-handler";
    
    @BeforeEach
    void setUp() {
        service = new IdempotencyService(dynamoDbClient, tableName);
        eventId = UUID.randomUUID().toString();
    }
    
    @Nested
    @DisplayName("isProcessed()")
    class IsProcessedTests {
        
        @Test
        @DisplayName("should return true when event exists in table")
        void shouldReturnTrueWhenEventExists() {
            // Arrange
            GetItemResponse response = GetItemResponse.builder()
                .item(Map.of("eventId", AttributeValue.builder().s(eventId).build()))
                .build();
            when(dynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(response);
            
            // Act
            boolean result = service.isProcessed(eventId);
            
            // Assert
            assertTrue(result);
            verify(dynamoDbClient).getItem(any(GetItemRequest.class));
        }
        
        @Test
        @DisplayName("should return false when event does not exist in table")
        void shouldReturnFalseWhenEventDoesNotExist() {
            // Arrange
            GetItemResponse response = GetItemResponse.builder().build();
            when(dynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(response);
            
            // Act
            boolean result = service.isProcessed(eventId);
            
            // Assert
            assertFalse(result);
            verify(dynamoDbClient).getItem(any(GetItemRequest.class));
        }
        
        @Test
        @DisplayName("should create correct GetItemRequest")
        void shouldCreateCorrectGetItemRequest() {
            // Arrange
            GetItemResponse response = GetItemResponse.builder().build();
            when(dynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(response);
            
            ArgumentCaptor<GetItemRequest> requestCaptor = ArgumentCaptor.forClass(GetItemRequest.class);
            
            // Act
            service.isProcessed(eventId);
            
            // Assert
            verify(dynamoDbClient).getItem(requestCaptor.capture());
            GetItemRequest request = requestCaptor.getValue();
            
            assertEquals(tableName, request.tableName());
            assertEquals(eventId, request.key().get("eventId").s());
            assertEquals("eventId", request.projectionExpression());
        }
        
        @Test
        @DisplayName("should return false on DynamoDB error (fail open)")
        void shouldReturnFalseOnError() {
            // Arrange
            when(dynamoDbClient.getItem(any(GetItemRequest.class)))
                .thenThrow(new RuntimeException("DynamoDB error"));
            
            // Act
            boolean result = service.isProcessed(eventId);
            
            // Assert
            assertFalse(result); // Fail open to prevent blocking
        }
    }
    
    @Nested
    @DisplayName("markProcessed()")
    class MarkProcessedTests {
        
        @Test
        @DisplayName("should successfully mark event as processed")
        void shouldMarkEventAsProcessed() {
            // Arrange
            PutItemResponse response = PutItemResponse.builder().build();
            when(dynamoDbClient.putItem(any(PutItemRequest.class))).thenReturn(response);
            
            // Act
            service.markProcessed(eventId, eventType, handler);
            
            // Assert
            verify(dynamoDbClient).putItem(any(PutItemRequest.class));
        }
        
        @Test
        @DisplayName("should create correct PutItemRequest with all fields")
        void shouldCreateCorrectPutItemRequest() {
            // Arrange
            PutItemResponse response = PutItemResponse.builder().build();
            when(dynamoDbClient.putItem(any(PutItemRequest.class))).thenReturn(response);
            
            ArgumentCaptor<PutItemRequest> requestCaptor = ArgumentCaptor.forClass(PutItemRequest.class);
            
            // Act
            service.markProcessed(eventId, eventType, handler);
            
            // Assert
            verify(dynamoDbClient).putItem(requestCaptor.capture());
            PutItemRequest request = requestCaptor.getValue();
            
            assertEquals(tableName, request.tableName());
            assertEquals("attribute_not_exists(eventId)", request.conditionExpression());
            
            Map<String, AttributeValue> item = request.item();
            assertEquals(eventId, item.get("eventId").s());
            assertEquals(eventType, item.get("eventType").s());
            assertEquals(handler, item.get("handler").s());
            assertNotNull(item.get("processedAt").s());
            assertNotNull(item.get("ttl").n());
        }
        
        @Test
        @DisplayName("should include TTL attribute set to 30 days from now")
        void shouldIncludeTtlAttribute() {
            // Arrange
            PutItemResponse response = PutItemResponse.builder().build();
            when(dynamoDbClient.putItem(any(PutItemRequest.class))).thenReturn(response);
            
            ArgumentCaptor<PutItemRequest> requestCaptor = ArgumentCaptor.forClass(PutItemRequest.class);
            Instant before = Instant.now().plus(29, ChronoUnit.DAYS);
            
            // Act
            service.markProcessed(eventId, eventType, handler);
            
            // Assert
            verify(dynamoDbClient).putItem(requestCaptor.capture());
            PutItemRequest request = requestCaptor.getValue();
            
            long ttl = Long.parseLong(request.item().get("ttl").n());
            Instant ttlInstant = Instant.ofEpochSecond(ttl);
            Instant after = Instant.now().plus(31, ChronoUnit.DAYS);
            
            assertTrue(ttlInstant.isAfter(before));
            assertTrue(ttlInstant.isBefore(after));
        }
        
        @Test
        @DisplayName("should handle ConditionalCheckFailedException gracefully")
        void shouldHandleConditionalCheckFailedException() {
            // Arrange
            when(dynamoDbClient.putItem(any(PutItemRequest.class)))
                .thenThrow(ConditionalCheckFailedException.builder()
                    .message("Condition not met")
                    .build());
            
            // Act & Assert - Should not throw exception
            assertDoesNotThrow(() -> service.markProcessed(eventId, eventType, handler));
        }
        
        @Test
        @DisplayName("should throw RuntimeException on other DynamoDB errors")
        void shouldThrowOnOtherErrors() {
            // Arrange
            when(dynamoDbClient.putItem(any(PutItemRequest.class)))
                .thenThrow(new RuntimeException("Network error"));
            
            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class, () -> 
                service.markProcessed(eventId, eventType, handler)
            );
            
            assertTrue(exception.getMessage().contains("Failed to mark event as processed"));
        }
        
        @Test
        @DisplayName("should use conditional write to prevent race conditions")
        void shouldUseConditionalWrite() {
            // Arrange
            PutItemResponse response = PutItemResponse.builder().build();
            when(dynamoDbClient.putItem(any(PutItemRequest.class))).thenReturn(response);
            
            ArgumentCaptor<PutItemRequest> requestCaptor = ArgumentCaptor.forClass(PutItemRequest.class);
            
            // Act
            service.markProcessed(eventId, eventType, handler);
            
            // Assert
            verify(dynamoDbClient).putItem(requestCaptor.capture());
            PutItemRequest request = requestCaptor.getValue();
            
            assertEquals("attribute_not_exists(eventId)", request.conditionExpression());
        }
    }
    
    @Nested
    @DisplayName("getRecord()")
    class GetRecordTests {
        
        @Test
        @DisplayName("should retrieve idempotency record successfully")
        void shouldRetrieveRecord() {
            // Arrange
            Instant processedAt = Instant.now();
            GetItemResponse response = GetItemResponse.builder()
                .item(Map.of(
                    "eventId", AttributeValue.builder().s(eventId).build(),
                    "eventType", AttributeValue.builder().s(eventType).build(),
                    "handler", AttributeValue.builder().s(handler).build(),
                    "processedAt", AttributeValue.builder().s(processedAt.toString()).build()
                ))
                .build();
            when(dynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(response);
            
            // Act
            IdempotencyRecord record = service.getRecord(eventId);
            
            // Assert
            assertNotNull(record);
            assertEquals(eventId, record.getEventId());
            assertEquals(eventType, record.getEventType());
            assertEquals(handler, record.getHandler());
            assertEquals(processedAt, record.getProcessedAt());
        }
        
        @Test
        @DisplayName("should return null when record does not exist")
        void shouldReturnNullWhenRecordDoesNotExist() {
            // Arrange
            GetItemResponse response = GetItemResponse.builder().build();
            when(dynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(response);
            
            // Act
            IdempotencyRecord record = service.getRecord(eventId);
            
            // Assert
            assertNull(record);
        }
        
        @Test
        @DisplayName("should return null on DynamoDB error")
        void shouldReturnNullOnError() {
            // Arrange
            when(dynamoDbClient.getItem(any(GetItemRequest.class)))
                .thenThrow(new RuntimeException("DynamoDB error"));
            
            // Act
            IdempotencyRecord record = service.getRecord(eventId);
            
            // Assert
            assertNull(record);
        }
        
        @Test
        @DisplayName("should create correct GetItemRequest")
        void shouldCreateCorrectGetItemRequest() {
            // Arrange
            GetItemResponse response = GetItemResponse.builder().build();
            when(dynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(response);
            
            ArgumentCaptor<GetItemRequest> requestCaptor = ArgumentCaptor.forClass(GetItemRequest.class);
            
            // Act
            service.getRecord(eventId);
            
            // Assert
            verify(dynamoDbClient).getItem(requestCaptor.capture());
            GetItemRequest request = requestCaptor.getValue();
            
            assertEquals(tableName, request.tableName());
            assertEquals(eventId, request.key().get("eventId").s());
        }
    }
    
    @Nested
    @DisplayName("Integration Scenarios")
    class IntegrationScenarioTests {
        
        @Test
        @DisplayName("should handle complete idempotency flow")
        void shouldHandleCompleteFlow() {
            // Arrange - Event not processed yet
            GetItemResponse notFoundResponse = GetItemResponse.builder().build();
            when(dynamoDbClient.getItem(any(GetItemRequest.class)))
                .thenReturn(notFoundResponse);
            
            PutItemResponse putResponse = PutItemResponse.builder().build();
            when(dynamoDbClient.putItem(any(PutItemRequest.class)))
                .thenReturn(putResponse);
            
            // Act - Check and mark as processed
            boolean isProcessedBefore = service.isProcessed(eventId);
            service.markProcessed(eventId, eventType, handler);
            
            // Arrange - Event now processed
            Instant processedAt = Instant.now();
            GetItemResponse foundResponse = GetItemResponse.builder()
                .item(Map.of(
                    "eventId", AttributeValue.builder().s(eventId).build(),
                    "eventType", AttributeValue.builder().s(eventType).build(),
                    "handler", AttributeValue.builder().s(handler).build(),
                    "processedAt", AttributeValue.builder().s(processedAt.toString()).build()
                ))
                .build();
            when(dynamoDbClient.getItem(any(GetItemRequest.class)))
                .thenReturn(foundResponse);
            
            boolean isProcessedAfter = service.isProcessed(eventId);
            IdempotencyRecord record = service.getRecord(eventId);
            
            // Assert
            assertFalse(isProcessedBefore);
            assertTrue(isProcessedAfter);
            assertNotNull(record);
            assertEquals(eventId, record.getEventId());
        }
        
        @Test
        @DisplayName("should prevent duplicate processing with concurrent requests")
        void shouldPreventDuplicateProcessing() {
            // Arrange - First request succeeds
            PutItemResponse successResponse = PutItemResponse.builder().build();
            when(dynamoDbClient.putItem(any(PutItemRequest.class)))
                .thenReturn(successResponse)
                .thenThrow(ConditionalCheckFailedException.builder()
                    .message("Item already exists")
                    .build());
            
            // Act - Two concurrent attempts to mark as processed
            service.markProcessed(eventId, eventType, "handler-1");
            service.markProcessed(eventId, eventType, "handler-2"); // Should be gracefully ignored
            
            // Assert - Both calls complete without exception
            verify(dynamoDbClient, times(2)).putItem(any(PutItemRequest.class));
        }
    }
}
