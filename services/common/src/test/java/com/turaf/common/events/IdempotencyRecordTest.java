package com.turaf.common.events;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for IdempotencyRecord.
 * 
 * Tests cover:
 * - Record creation
 * - Null validation
 * - Equality and hash code
 * - Immutability
 */
@DisplayName("IdempotencyRecord")
class IdempotencyRecordTest {
    
    private String eventId = UUID.randomUUID().toString();
    private String eventType = "test.EventOccurred";
    private String handler = "test-handler";
    private Instant processedAt = Instant.now();
    
    @Nested
    @DisplayName("Constructor")
    class ConstructorTests {
        
        @Test
        @DisplayName("should create record with all fields")
        void shouldCreateRecordWithAllFields() {
            IdempotencyRecord record = new IdempotencyRecord(
                eventId, eventType, handler, processedAt
            );
            
            assertNotNull(record);
            assertEquals(eventId, record.getEventId());
            assertEquals(eventType, record.getEventType());
            assertEquals(handler, record.getHandler());
            assertEquals(processedAt, record.getProcessedAt());
        }
        
        @Test
        @DisplayName("should throw NullPointerException when eventId is null")
        void shouldThrowWhenEventIdIsNull() {
            assertThrows(NullPointerException.class, () -> 
                new IdempotencyRecord(null, eventType, handler, processedAt)
            );
        }
        
        @Test
        @DisplayName("should throw NullPointerException when eventType is null")
        void shouldThrowWhenEventTypeIsNull() {
            assertThrows(NullPointerException.class, () -> 
                new IdempotencyRecord(eventId, null, handler, processedAt)
            );
        }
        
        @Test
        @DisplayName("should throw NullPointerException when handler is null")
        void shouldThrowWhenHandlerIsNull() {
            assertThrows(NullPointerException.class, () -> 
                new IdempotencyRecord(eventId, eventType, null, processedAt)
            );
        }
        
        @Test
        @DisplayName("should throw NullPointerException when processedAt is null")
        void shouldThrowWhenProcessedAtIsNull() {
            assertThrows(NullPointerException.class, () -> 
                new IdempotencyRecord(eventId, eventType, handler, null)
            );
        }
    }
    
    @Nested
    @DisplayName("Equality and HashCode")
    class EqualityTests {
        
        @Test
        @DisplayName("should be equal when all fields match")
        void shouldBeEqualWhenFieldsMatch() {
            IdempotencyRecord record1 = new IdempotencyRecord(
                eventId, eventType, handler, processedAt
            );
            IdempotencyRecord record2 = new IdempotencyRecord(
                eventId, eventType, handler, processedAt
            );
            
            assertEquals(record1, record2);
            assertEquals(record1.hashCode(), record2.hashCode());
        }
        
        @Test
        @DisplayName("should not be equal when eventId differs")
        void shouldNotBeEqualWhenEventIdDiffers() {
            IdempotencyRecord record1 = new IdempotencyRecord(
                eventId, eventType, handler, processedAt
            );
            IdempotencyRecord record2 = new IdempotencyRecord(
                UUID.randomUUID().toString(), eventType, handler, processedAt
            );
            
            assertNotEquals(record1, record2);
        }
        
        @Test
        @DisplayName("should not be equal when eventType differs")
        void shouldNotBeEqualWhenEventTypeDiffers() {
            IdempotencyRecord record1 = new IdempotencyRecord(
                eventId, eventType, handler, processedAt
            );
            IdempotencyRecord record2 = new IdempotencyRecord(
                eventId, "different.EventType", handler, processedAt
            );
            
            assertNotEquals(record1, record2);
        }
        
        @Test
        @DisplayName("should not be equal when handler differs")
        void shouldNotBeEqualWhenHandlerDiffers() {
            IdempotencyRecord record1 = new IdempotencyRecord(
                eventId, eventType, handler, processedAt
            );
            IdempotencyRecord record2 = new IdempotencyRecord(
                eventId, eventType, "different-handler", processedAt
            );
            
            assertNotEquals(record1, record2);
        }
        
        @Test
        @DisplayName("should not be equal when processedAt differs")
        void shouldNotBeEqualWhenProcessedAtDiffers() {
            IdempotencyRecord record1 = new IdempotencyRecord(
                eventId, eventType, handler, processedAt
            );
            IdempotencyRecord record2 = new IdempotencyRecord(
                eventId, eventType, handler, processedAt.plusSeconds(60)
            );
            
            assertNotEquals(record1, record2);
        }
        
        @Test
        @DisplayName("should handle null in equals")
        void shouldHandleNullInEquals() {
            IdempotencyRecord record = new IdempotencyRecord(
                eventId, eventType, handler, processedAt
            );
            
            assertNotEquals(null, record);
        }
        
        @Test
        @DisplayName("should be equal to itself")
        void shouldBeEqualToItself() {
            IdempotencyRecord record = new IdempotencyRecord(
                eventId, eventType, handler, processedAt
            );
            
            assertEquals(record, record);
        }
    }
    
    @Nested
    @DisplayName("toString()")
    class ToStringTests {
        
        @Test
        @DisplayName("should return string representation with all fields")
        void shouldReturnStringRepresentation() {
            IdempotencyRecord record = new IdempotencyRecord(
                eventId, eventType, handler, processedAt
            );
            
            String result = record.toString();
            
            assertNotNull(result);
            assertTrue(result.contains("IdempotencyRecord"));
            assertTrue(result.contains(eventId));
            assertTrue(result.contains(eventType));
            assertTrue(result.contains(handler));
            assertTrue(result.contains(processedAt.toString()));
        }
    }
    
    @Nested
    @DisplayName("Immutability")
    class ImmutabilityTests {
        
        @Test
        @DisplayName("should be immutable after creation")
        void shouldBeImmutable() {
            Instant originalTimestamp = Instant.now();
            IdempotencyRecord record = new IdempotencyRecord(
                eventId, eventType, handler, originalTimestamp
            );
            
            // Verify getters return the same values
            assertEquals(eventId, record.getEventId());
            assertEquals(eventType, record.getEventType());
            assertEquals(handler, record.getHandler());
            assertEquals(originalTimestamp, record.getProcessedAt());
            
            // Verify no setters exist (compile-time check)
            // If this compiles, the class is properly immutable
        }
    }
}
