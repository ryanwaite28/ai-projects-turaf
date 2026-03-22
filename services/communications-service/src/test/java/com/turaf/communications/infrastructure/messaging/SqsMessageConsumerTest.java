package com.turaf.communications.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turaf.communications.application.dto.MessageCreatedDTO;
import com.turaf.communications.application.service.MessageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SqsMessageConsumerTest {
    
    @Mock
    private MessageService messageService;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @InjectMocks
    private SqsMessageConsumer consumer;
    
    @Test
    void consumeDirectMessage_shouldProcessMessage() throws Exception {
        String json = "{\"id\":\"msg-1\",\"conversationId\":\"conv-1\",\"senderId\":\"user-1\",\"content\":\"Hello\"}";
        MessageCreatedDTO dto = MessageCreatedDTO.builder()
            .id("msg-1")
            .conversationId("conv-1")
            .senderId("user-1")
            .content("Hello")
            .build();
        
        when(objectMapper.readValue(json, MessageCreatedDTO.class)).thenReturn(dto);
        
        consumer.consumeDirectMessage(json);
        
        verify(messageService).processMessage(dto);
    }
    
    @Test
    void consumeDirectMessage_shouldThrowExceptionOnError() throws Exception {
        String json = "{\"invalid\":\"json\"}";
        
        when(objectMapper.readValue(json, MessageCreatedDTO.class))
            .thenThrow(new RuntimeException("Parse error"));
        
        assertThrows(RuntimeException.class, () -> 
            consumer.consumeDirectMessage(json)
        );
        
        verify(messageService, never()).processMessage(any());
    }
    
    @Test
    void consumeGroupMessage_shouldProcessMessage() throws Exception {
        String json = "{\"id\":\"msg-2\",\"conversationId\":\"conv-2\",\"senderId\":\"user-2\",\"content\":\"Hello Group\"}";
        MessageCreatedDTO dto = MessageCreatedDTO.builder()
            .id("msg-2")
            .conversationId("conv-2")
            .senderId("user-2")
            .content("Hello Group")
            .build();
        
        when(objectMapper.readValue(json, MessageCreatedDTO.class)).thenReturn(dto);
        
        consumer.consumeGroupMessage(json);
        
        verify(messageService).processMessage(dto);
    }
    
    @Test
    void consumeGroupMessage_shouldThrowExceptionOnError() throws Exception {
        String json = "{\"invalid\":\"json\"}";
        
        when(objectMapper.readValue(json, MessageCreatedDTO.class))
            .thenThrow(new RuntimeException("Parse error"));
        
        assertThrows(RuntimeException.class, () -> 
            consumer.consumeGroupMessage(json)
        );
        
        verify(messageService, never()).processMessage(any());
    }
    
    @Test
    void consumeDirectMessage_shouldLogSuccessfulProcessing() throws Exception {
        String json = "{\"id\":\"msg-1\",\"conversationId\":\"conv-1\",\"senderId\":\"user-1\",\"content\":\"Hello\"}";
        MessageCreatedDTO dto = MessageCreatedDTO.builder()
            .id("msg-1")
            .conversationId("conv-1")
            .senderId("user-1")
            .content("Hello")
            .build();
        
        when(objectMapper.readValue(json, MessageCreatedDTO.class)).thenReturn(dto);
        doNothing().when(messageService).processMessage(dto);
        
        consumer.consumeDirectMessage(json);
        
        verify(messageService).processMessage(dto);
    }
}
