package com.turaf.communications.application.service;

import com.turaf.communications.application.dto.MessageCreatedDTO;
import com.turaf.communications.application.dto.MessageDTO;
import com.turaf.communications.application.dto.PaginatedMessages;
import com.turaf.communications.domain.exception.ConversationNotFoundException;
import com.turaf.communications.domain.exception.UnauthorizedParticipantException;
import com.turaf.communications.domain.model.Conversation;
import com.turaf.communications.domain.model.Message;
import com.turaf.communications.domain.repository.ConversationRepository;
import com.turaf.communications.domain.repository.MessageRepository;
import com.turaf.communications.infrastructure.messaging.EventBridgePublisher;
import com.turaf.communications.interfaces.mapper.MessageMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {
    
    @Mock
    private MessageRepository messageRepository;
    
    @Mock
    private ConversationRepository conversationRepository;
    
    @Mock
    private EventBridgePublisher eventBridgePublisher;
    
    @Mock
    private MessageMapper messageMapper;
    
    @InjectMocks
    private MessageService messageService;
    
    @Test
    void processMessage_shouldSaveMessageAndPublishEvent() {
        MessageCreatedDTO dto = MessageCreatedDTO.builder()
            .id("msg-1")
            .conversationId("conv-1")
            .senderId("user1")
            .content("Hello World")
            .build();
        
        Conversation conversation = Conversation.createDirect("user1", "user2");
        Message message = Message.create("conv-1", "user1", "Hello World");
        
        when(conversationRepository.findById("conv-1"))
            .thenReturn(Optional.of(conversation));
        when(messageRepository.save(any(Message.class)))
            .thenReturn(message);
        
        messageService.processMessage(dto);
        
        verify(messageRepository).save(any(Message.class));
        verify(eventBridgePublisher).publishMessageDelivered(any());
    }
    
    @Test
    void processMessage_shouldThrowWhenConversationNotFound() {
        MessageCreatedDTO dto = MessageCreatedDTO.builder()
            .id("msg-1")
            .conversationId("conv-1")
            .senderId("user1")
            .content("Hello World")
            .build();
        
        when(conversationRepository.findById("conv-1"))
            .thenReturn(Optional.empty());
        
        assertThrows(ConversationNotFoundException.class, () -> 
            messageService.processMessage(dto)
        );
    }
    
    @Test
    void processMessage_shouldThrowWhenSenderNotParticipant() {
        MessageCreatedDTO dto = MessageCreatedDTO.builder()
            .id("msg-1")
            .conversationId("conv-1")
            .senderId("user3")
            .content("Hello World")
            .build();
        
        Conversation conversation = Conversation.createDirect("user1", "user2");
        
        when(conversationRepository.findById("conv-1"))
            .thenReturn(Optional.of(conversation));
        
        assertThrows(UnauthorizedParticipantException.class, () -> 
            messageService.processMessage(dto)
        );
    }
    
    @Test
    void getMessages_shouldReturnPaginatedMessages() {
        String conversationId = "conv-1";
        String userId = "user1";
        Pageable pageable = PageRequest.of(0, 20);
        
        Conversation conversation = Conversation.createDirect("user1", "user2");
        Message message = Message.create(conversationId, userId, "Hello");
        Page<Message> messagePage = new PageImpl<>(List.of(message), pageable, 1);
        
        when(conversationRepository.findById(conversationId))
            .thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, pageable))
            .thenReturn(messagePage);
        when(messageMapper.toDTOList(anyList()))
            .thenReturn(List.of(new MessageDTO()));
        
        PaginatedMessages result = messageService.getMessages(conversationId, userId, pageable);
        
        assertNotNull(result);
        assertEquals(1, result.getMessages().size());
        assertEquals(0, result.getPage());
        assertEquals(20, result.getSize());
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getTotalPages());
    }
    
    @Test
    void getMessages_shouldThrowWhenConversationNotFound() {
        String conversationId = "conv-1";
        String userId = "user1";
        Pageable pageable = PageRequest.of(0, 20);
        
        when(conversationRepository.findById(conversationId))
            .thenReturn(Optional.empty());
        
        assertThrows(ConversationNotFoundException.class, () -> 
            messageService.getMessages(conversationId, userId, pageable)
        );
    }
    
    @Test
    void getMessages_shouldThrowWhenUserNotParticipant() {
        String conversationId = "conv-1";
        String userId = "user3";
        Pageable pageable = PageRequest.of(0, 20);
        
        Conversation conversation = Conversation.createDirect("user1", "user2");
        
        when(conversationRepository.findById(conversationId))
            .thenReturn(Optional.of(conversation));
        
        assertThrows(UnauthorizedParticipantException.class, () -> 
            messageService.getMessages(conversationId, userId, pageable)
        );
    }
    
    @Test
    void processMessage_shouldExcludeSenderFromRecipients() {
        MessageCreatedDTO dto = MessageCreatedDTO.builder()
            .id("msg-1")
            .conversationId("conv-1")
            .senderId("user1")
            .content("Hello World")
            .build();
        
        Conversation conversation = Conversation.createDirect("user1", "user2");
        Message message = Message.create("conv-1", "user1", "Hello World");
        
        when(conversationRepository.findById("conv-1"))
            .thenReturn(Optional.of(conversation));
        when(messageRepository.save(any(Message.class)))
            .thenReturn(message);
        
        messageService.processMessage(dto);
        
        verify(eventBridgePublisher).publishMessageDelivered(argThat(event -> 
            event.getRecipientIds().size() == 1 && 
            event.getRecipientIds().contains("user2") &&
            !event.getRecipientIds().contains("user1")
        ));
    }
}
