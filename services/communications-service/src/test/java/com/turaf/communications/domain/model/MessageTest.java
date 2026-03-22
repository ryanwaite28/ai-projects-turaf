package com.turaf.communications.domain.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MessageTest {
    
    @Test
    void create_shouldCreateMessageWithValidContent() {
        Message message = Message.create("conv-123", "user-1", "Hello World");
        
        assertNotNull(message.getId());
        assertEquals("conv-123", message.getConversationId());
        assertEquals("user-1", message.getSenderId());
        assertEquals("Hello World", message.getContent());
        assertNotNull(message.getCreatedAt());
    }
    
    @Test
    void create_shouldThrowExceptionWhenContentIsEmpty() {
        assertThrows(IllegalArgumentException.class, () -> 
            Message.create("conv-123", "user-1", "")
        );
    }
    
    @Test
    void create_shouldThrowExceptionWhenContentIsNull() {
        assertThrows(IllegalArgumentException.class, () -> 
            Message.create("conv-123", "user-1", null)
        );
    }
    
    @Test
    void create_shouldThrowExceptionWhenContentExceedsMaxLength() {
        String longContent = "a".repeat(10001);
        
        assertThrows(IllegalArgumentException.class, () -> 
            Message.create("conv-123", "user-1", longContent)
        );
    }
    
    @Test
    void create_shouldAcceptContentAtMaxLength() {
        String maxContent = "a".repeat(10000);
        
        Message message = Message.create("conv-123", "user-1", maxContent);
        
        assertNotNull(message);
        assertEquals(10000, message.getContent().length());
    }
}
