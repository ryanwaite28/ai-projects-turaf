package com.turaf.communications.application.service;

import com.turaf.communications.domain.model.ReadState;
import com.turaf.communications.domain.repository.MessageRepository;
import com.turaf.communications.domain.repository.ReadStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UnreadCountService {
    
    private final ReadStateRepository readStateRepository;
    private final MessageRepository messageRepository;
    
    @Transactional(readOnly = true)
    public int getUnreadCount(String userId, String conversationId) {
        log.debug("Calculating unread count: userId={}, conversationId={}", userId, conversationId);
        return messageRepository.countUnreadMessages(userId, conversationId);
    }
    
    @Transactional(readOnly = true)
    public Map<String, Integer> getAllUnreadCounts(String userId) {
        log.debug("Calculating all unread counts for user: {}", userId);
        
        List<ReadState> readStates = readStateRepository.findByUserId(userId);
        Map<String, Integer> unreadCounts = new HashMap<>();
        
        for (ReadState readState : readStates) {
            int count = messageRepository.countUnreadMessages(userId, readState.getConversationId());
            unreadCounts.put(readState.getConversationId(), count);
        }
        
        return unreadCounts;
    }
    
    @Transactional
    public void markAsRead(String userId, String conversationId, String lastMessageId) {
        log.info("Marking messages as read: userId={}, conversationId={}, lastMessageId={}", 
                 userId, conversationId, lastMessageId);
        
        ReadState readState = readStateRepository
            .findByUserIdAndConversationId(userId, conversationId)
            .orElseGet(() -> ReadState.create(userId, conversationId));
        
        readState.markAsRead(lastMessageId);
        readStateRepository.save(readState);
        
        log.info("Read state updated");
    }
    
    @Transactional
    public void initializeReadState(String userId, String conversationId) {
        log.info("Initializing read state: userId={}, conversationId={}", userId, conversationId);
        
        if (readStateRepository.findByUserIdAndConversationId(userId, conversationId).isEmpty()) {
            ReadState readState = ReadState.create(userId, conversationId);
            readStateRepository.save(readState);
        }
    }
}
