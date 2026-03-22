package com.turaf.communications.domain.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReadStateTest {
    
    @Test
    void create_shouldCreateReadStateWithNullLastReadMessage() {
        ReadState readState = ReadState.create("user-1", "conv-123");
        
        assertNotNull(readState.getId());
        assertEquals("user-1", readState.getUserId());
        assertEquals("conv-123", readState.getConversationId());
        assertNull(readState.getLastReadMessageId());
        assertNotNull(readState.getUpdatedAt());
    }
    
    @Test
    void markAsRead_shouldUpdateLastReadMessageId() {
        ReadState readState = ReadState.create("user-1", "conv-123");
        
        readState.markAsRead("msg-456");
        
        assertEquals("msg-456", readState.getLastReadMessageId());
        assertNotNull(readState.getUpdatedAt());
    }
    
    @Test
    void markAsRead_shouldUpdateTimestamp() {
        ReadState readState = ReadState.create("user-1", "conv-123");
        var initialTime = readState.getUpdatedAt();
        
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        readState.markAsRead("msg-456");
        
        assertTrue(readState.getUpdatedAt().isAfter(initialTime));
    }
}
