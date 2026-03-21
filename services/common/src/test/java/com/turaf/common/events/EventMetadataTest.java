package com.turaf.common.events;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EventMetadata.
 * 
 * Tests cover:
 * - Metadata creation and initialization
 * - Correlation and causation ID handling
 * - Custom metadata operations
 * - Equality and hash code contracts
 */
@DisplayName("EventMetadata")
class EventMetadataTest {
    
    private EventMetadata metadata;
    
    @BeforeEach
    void setUp() {
        metadata = new EventMetadata();
    }
    
    @Nested
    @DisplayName("Constructor")
    class ConstructorTests {
        
        @Test
        @DisplayName("should create metadata with default constructor")
        void shouldCreateWithDefaultConstructor() {
            EventMetadata meta = new EventMetadata();
            
            assertNotNull(meta);
            assertNull(meta.getCorrelationId());
            assertNull(meta.getCausationId());
            assertNotNull(meta.getAllMetadata());
            assertTrue(meta.getAllMetadata().isEmpty());
        }
        
        @Test
        @DisplayName("should create metadata with correlation and causation IDs")
        void shouldCreateWithCorrelationAndCausation() {
            EventMetadata meta = new EventMetadata("correlation-123", "causation-456");
            
            assertNotNull(meta);
            assertEquals("correlation-123", meta.getCorrelationId());
            assertEquals("causation-456", meta.getCausationId());
            assertNotNull(meta.getAllMetadata());
            assertTrue(meta.getAllMetadata().isEmpty());
        }
    }
    
    @Nested
    @DisplayName("Correlation and Causation IDs")
    class CorrelationCausationTests {
        
        @Test
        @DisplayName("should set and get correlation ID")
        void shouldSetAndGetCorrelationId() {
            metadata.setCorrelationId("correlation-789");
            
            assertEquals("correlation-789", metadata.getCorrelationId());
        }
        
        @Test
        @DisplayName("should set and get causation ID")
        void shouldSetAndGetCausationId() {
            metadata.setCausationId("causation-789");
            
            assertEquals("causation-789", metadata.getCausationId());
        }
        
        @Test
        @DisplayName("should allow null correlation ID")
        void shouldAllowNullCorrelationId() {
            metadata.setCorrelationId(null);
            
            assertNull(metadata.getCorrelationId());
        }
        
        @Test
        @DisplayName("should allow null causation ID")
        void shouldAllowNullCausationId() {
            metadata.setCausationId(null);
            
            assertNull(metadata.getCausationId());
        }
    }
    
    @Nested
    @DisplayName("Custom Metadata")
    class CustomMetadataTests {
        
        @Test
        @DisplayName("should add custom metadata")
        void shouldAddCustomMetadata() {
            metadata.addMetadata("key1", "value1");
            metadata.addMetadata("key2", "value2");
            
            assertEquals("value1", metadata.getMetadata("key1"));
            assertEquals("value2", metadata.getMetadata("key2"));
        }
        
        @Test
        @DisplayName("should return null for non-existent key")
        void shouldReturnNullForNonExistentKey() {
            assertNull(metadata.getMetadata("nonexistent"));
        }
        
        @Test
        @DisplayName("should throw NullPointerException when key is null")
        void shouldThrowWhenKeyIsNull() {
            assertThrows(NullPointerException.class, () -> 
                metadata.addMetadata(null, "value")
            );
        }
        
        @Test
        @DisplayName("should allow null value")
        void shouldAllowNullValue() {
            metadata.addMetadata("key", null);
            
            assertNull(metadata.getMetadata("key"));
        }
        
        @Test
        @DisplayName("should overwrite existing metadata")
        void shouldOverwriteExistingMetadata() {
            metadata.addMetadata("key", "value1");
            metadata.addMetadata("key", "value2");
            
            assertEquals("value2", metadata.getMetadata("key"));
        }
        
        @Test
        @DisplayName("should return all metadata as unmodifiable map")
        void shouldReturnAllMetadataAsUnmodifiableMap() {
            metadata.addMetadata("key1", "value1");
            metadata.addMetadata("key2", "value2");
            
            Map<String, String> allMetadata = metadata.getAllMetadata();
            
            assertNotNull(allMetadata);
            assertEquals(2, allMetadata.size());
            assertEquals("value1", allMetadata.get("key1"));
            assertEquals("value2", allMetadata.get("key2"));
            
            // Verify map is unmodifiable
            assertThrows(UnsupportedOperationException.class, () -> 
                allMetadata.put("key3", "value3")
            );
        }
    }
    
    @Nested
    @DisplayName("Equality and HashCode")
    class EqualityTests {
        
        @Test
        @DisplayName("should be equal when all fields match")
        void shouldBeEqualWhenFieldsMatch() {
            EventMetadata meta1 = new EventMetadata("correlation-123", "causation-456");
            meta1.addMetadata("key1", "value1");
            
            EventMetadata meta2 = new EventMetadata("correlation-123", "causation-456");
            meta2.addMetadata("key1", "value1");
            
            assertEquals(meta1, meta2);
            assertEquals(meta1.hashCode(), meta2.hashCode());
        }
        
        @Test
        @DisplayName("should not be equal when correlation ID differs")
        void shouldNotBeEqualWhenCorrelationIdDiffers() {
            EventMetadata meta1 = new EventMetadata("correlation-123", "causation-456");
            EventMetadata meta2 = new EventMetadata("correlation-789", "causation-456");
            
            assertNotEquals(meta1, meta2);
        }
        
        @Test
        @DisplayName("should not be equal when causation ID differs")
        void shouldNotBeEqualWhenCausationIdDiffers() {
            EventMetadata meta1 = new EventMetadata("correlation-123", "causation-456");
            EventMetadata meta2 = new EventMetadata("correlation-123", "causation-789");
            
            assertNotEquals(meta1, meta2);
        }
        
        @Test
        @DisplayName("should not be equal when custom metadata differs")
        void shouldNotBeEqualWhenCustomMetadataDiffers() {
            EventMetadata meta1 = new EventMetadata();
            meta1.addMetadata("key1", "value1");
            
            EventMetadata meta2 = new EventMetadata();
            meta2.addMetadata("key1", "value2");
            
            assertNotEquals(meta1, meta2);
        }
        
        @Test
        @DisplayName("should handle null in equals")
        void shouldHandleNullInEquals() {
            assertNotEquals(null, metadata);
        }
        
        @Test
        @DisplayName("should be equal to itself")
        void shouldBeEqualToItself() {
            assertEquals(metadata, metadata);
        }
    }
    
    @Nested
    @DisplayName("toString()")
    class ToStringTests {
        
        @Test
        @DisplayName("should return string representation with all fields")
        void shouldReturnStringRepresentation() {
            metadata.setCorrelationId("correlation-123");
            metadata.setCausationId("causation-456");
            metadata.addMetadata("key1", "value1");
            
            String result = metadata.toString();
            
            assertNotNull(result);
            assertTrue(result.contains("EventMetadata"));
            assertTrue(result.contains("correlation-123"));
            assertTrue(result.contains("causation-456"));
        }
    }
}
