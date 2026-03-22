package com.turaf.communications.infrastructure.persistence;

import com.turaf.communications.domain.model.Conversation;
import com.turaf.communications.domain.model.Message;
import com.turaf.communications.domain.model.ReadState;
import com.turaf.communications.domain.repository.ConversationRepository;
import com.turaf.communications.domain.repository.MessageRepository;
import com.turaf.communications.domain.repository.ReadStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class ReadStateRepositoryIntegrationTest {
    
    @Autowired
    private ReadStateRepository readStateRepository;
    
    @Autowired
    private ConversationRepository conversationRepository;
    
    @Autowired
    private MessageRepository messageRepository;
    
    private String conversationId;
    private String messageId;
    
    @BeforeEach
    void setUp() {
        Conversation conversation = Conversation.createDirect("user1", "user2");
        conversationRepository.save(conversation);
        conversationId = conversation.getId();
        
        Message message = Message.create(conversationId, "user1", "Test message");
        messageRepository.save(message);
        messageId = message.getId();
        
        ReadState readState = ReadState.create("user2", conversationId);
        readState.markAsRead(messageId);
        readStateRepository.save(readState);
    }
    
    @Test
    void findByUserIdAndConversationId_shouldReturnReadState() {
        Optional<ReadState> found = readStateRepository.findByUserIdAndConversationId("user2", conversationId);
        
        assertTrue(found.isPresent());
        assertEquals("user2", found.get().getUserId());
        assertEquals(conversationId, found.get().getConversationId());
        assertEquals(messageId, found.get().getLastReadMessageId());
    }
    
    @Test
    void findByUserIdAndConversationId_shouldReturnEmptyForNonExistent() {
        Optional<ReadState> found = readStateRepository.findByUserIdAndConversationId("user999", conversationId);
        
        assertFalse(found.isPresent());
    }
    
    @Test
    void findByUserId_shouldReturnAllUserReadStates() {
        Conversation conversation2 = Conversation.createDirect("user2", "user3");
        conversationRepository.save(conversation2);
        
        Message message2 = Message.create(conversation2.getId(), "user3", "Another message");
        messageRepository.save(message2);
        
        ReadState readState2 = ReadState.create("user2", conversation2.getId());
        readState2.markAsRead(message2.getId());
        readStateRepository.save(readState2);
        
        List<ReadState> readStates = readStateRepository.findByUserId("user2");
        
        assertEquals(2, readStates.size());
    }
    
    @Test
    void save_shouldPersistReadState() {
        ReadState newReadState = ReadState.create("user1", conversationId);
        newReadState.markAsRead(messageId);
        
        ReadState saved = readStateRepository.save(newReadState);
        
        assertNotNull(saved.getId());
        assertEquals("user1", saved.getUserId());
        assertNotNull(saved.getUpdatedAt());
    }
    
    @Test
    void save_shouldUpdateExistingReadState() {
        Message newMessage = Message.create(conversationId, "user1", "Newer message");
        messageRepository.save(newMessage);
        
        Optional<ReadState> existing = readStateRepository.findByUserIdAndConversationId("user2", conversationId);
        assertTrue(existing.isPresent());
        
        ReadState readState = existing.get();
        readState.markAsRead(newMessage.getId());
        
        ReadState updated = readStateRepository.save(readState);
        
        assertEquals(newMessage.getId(), updated.getLastReadMessageId());
    }
}
