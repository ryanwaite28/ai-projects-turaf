package com.turaf.communications.application.service;

import com.turaf.communications.domain.model.ReadState;
import com.turaf.communications.domain.repository.MessageRepository;
import com.turaf.communications.domain.repository.ReadStateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UnreadCountServiceTest {
    
    @Mock
    private ReadStateRepository readStateRepository;
    
    @Mock
    private MessageRepository messageRepository;
    
    @InjectMocks
    private UnreadCountService unreadCountService;
    
    @Test
    void getUnreadCount_shouldReturnCount() {
        String userId = "user1";
        String conversationId = "conv1";
        
        when(messageRepository.countUnreadMessages(userId, conversationId))
            .thenReturn(5);
        
        int result = unreadCountService.getUnreadCount(userId, conversationId);
        
        assertEquals(5, result);
        verify(messageRepository).countUnreadMessages(userId, conversationId);
    }
    
    @Test
    void getAllUnreadCounts_shouldReturnMapOfCounts() {
        String userId = "user1";
        ReadState readState1 = ReadState.create(userId, "conv1");
        ReadState readState2 = ReadState.create(userId, "conv2");
        
        when(readStateRepository.findByUserId(userId))
            .thenReturn(List.of(readState1, readState2));
        when(messageRepository.countUnreadMessages(userId, "conv1"))
            .thenReturn(3);
        when(messageRepository.countUnreadMessages(userId, "conv2"))
            .thenReturn(7);
        
        Map<String, Integer> result = unreadCountService.getAllUnreadCounts(userId);
        
        assertEquals(2, result.size());
        assertEquals(3, result.get("conv1"));
        assertEquals(7, result.get("conv2"));
    }
    
    @Test
    void markAsRead_shouldUpdateExistingReadState() {
        String userId = "user1";
        String conversationId = "conv1";
        String messageId = "msg1";
        ReadState existingReadState = ReadState.create(userId, conversationId);
        
        when(readStateRepository.findByUserIdAndConversationId(userId, conversationId))
            .thenReturn(Optional.of(existingReadState));
        when(readStateRepository.save(any(ReadState.class)))
            .thenReturn(existingReadState);
        
        unreadCountService.markAsRead(userId, conversationId, messageId);
        
        verify(readStateRepository).save(any(ReadState.class));
        assertEquals(messageId, existingReadState.getLastReadMessageId());
    }
    
    @Test
    void markAsRead_shouldCreateNewReadStateIfNotExists() {
        String userId = "user1";
        String conversationId = "conv1";
        String messageId = "msg1";
        
        when(readStateRepository.findByUserIdAndConversationId(userId, conversationId))
            .thenReturn(Optional.empty());
        when(readStateRepository.save(any(ReadState.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        unreadCountService.markAsRead(userId, conversationId, messageId);
        
        verify(readStateRepository).save(argThat(readState -> 
            readState.getUserId().equals(userId) &&
            readState.getConversationId().equals(conversationId) &&
            readState.getLastReadMessageId().equals(messageId)
        ));
    }
    
    @Test
    void initializeReadState_shouldCreateNewReadState() {
        String userId = "user1";
        String conversationId = "conv1";
        
        when(readStateRepository.findByUserIdAndConversationId(userId, conversationId))
            .thenReturn(Optional.empty());
        when(readStateRepository.save(any(ReadState.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        unreadCountService.initializeReadState(userId, conversationId);
        
        verify(readStateRepository).save(argThat(readState -> 
            readState.getUserId().equals(userId) &&
            readState.getConversationId().equals(conversationId)
        ));
    }
    
    @Test
    void initializeReadState_shouldNotCreateIfAlreadyExists() {
        String userId = "user1";
        String conversationId = "conv1";
        ReadState existingReadState = ReadState.create(userId, conversationId);
        
        when(readStateRepository.findByUserIdAndConversationId(userId, conversationId))
            .thenReturn(Optional.of(existingReadState));
        
        unreadCountService.initializeReadState(userId, conversationId);
        
        verify(readStateRepository, never()).save(any());
    }
    
    @Test
    void getAllUnreadCounts_shouldReturnEmptyMapWhenNoReadStates() {
        String userId = "user1";
        
        when(readStateRepository.findByUserId(userId))
            .thenReturn(List.of());
        
        Map<String, Integer> result = unreadCountService.getAllUnreadCounts(userId);
        
        assertTrue(result.isEmpty());
    }
}
