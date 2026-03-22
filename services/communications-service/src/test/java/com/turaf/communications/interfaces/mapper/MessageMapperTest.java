package com.turaf.communications.interfaces.mapper;

import com.turaf.communications.application.dto.MessageDTO;
import com.turaf.communications.domain.model.Message;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class MessageMapperTest {
    
    private final MessageMapper mapper = Mappers.getMapper(MessageMapper.class);
    
    @Test
    void toDTO_shouldMapMessageCorrectly() {
        Message message = Message.create("conv-1", "user1", "Hello, World!");
        
        MessageDTO dto = mapper.toDTO(message);
        
        assertNotNull(dto);
        assertEquals(message.getId(), dto.getId());
        assertEquals("conv-1", dto.getConversationId());
        assertEquals("user1", dto.getSenderId());
        assertEquals("Hello, World!", dto.getContent());
        assertNotNull(dto.getCreatedAt());
    }
    
    @Test
    void toDTO_shouldHandleNullMessage() {
        MessageDTO dto = mapper.toDTO(null);
        
        assertNull(dto);
    }
    
    @Test
    void toDTO_shouldPreserveTimestamp() {
        Message message = Message.create("conv-1", "user1", "Test message");
        Instant createdAt = message.getCreatedAt();
        
        MessageDTO dto = mapper.toDTO(message);
        
        assertNotNull(dto.getCreatedAt());
        assertEquals(createdAt, dto.getCreatedAt());
    }
    
    @Test
    void toDTO_shouldMapLongContent() {
        String longContent = "A".repeat(1000);
        Message message = Message.create("conv-1", "user1", longContent);
        
        MessageDTO dto = mapper.toDTO(message);
        
        assertNotNull(dto);
        assertEquals(longContent, dto.getContent());
        assertEquals(1000, dto.getContent().length());
    }
}
