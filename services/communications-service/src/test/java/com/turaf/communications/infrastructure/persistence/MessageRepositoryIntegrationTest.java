package com.turaf.communications.infrastructure.persistence;

import com.turaf.communications.domain.model.Conversation;
import com.turaf.communications.domain.model.Message;
import com.turaf.communications.domain.repository.ConversationRepository;
import com.turaf.communications.domain.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class MessageRepositoryIntegrationTest {
    
    @Autowired
    private MessageRepository messageRepository;
    
    @Autowired
    private ConversationRepository conversationRepository;
    
    private String conversationId;
    
    @BeforeEach
    void setUp() {
        Conversation conversation = Conversation.createDirect("user1", "user2");
        conversationRepository.save(conversation);
        conversationId = conversation.getId();
        
        Message msg1 = Message.create(conversationId, "user1", "Hello");
        Message msg2 = Message.create(conversationId, "user2", "Hi there");
        Message msg3 = Message.create(conversationId, "user1", "How are you?");
        
        messageRepository.save(msg1);
        messageRepository.save(msg2);
        messageRepository.save(msg3);
    }
    
    @Test
    void findByConversationIdOrderByCreatedAtDesc_shouldReturnMessagesInDescendingOrder() {
        Pageable pageable = PageRequest.of(0, 10);
        
        Page<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, pageable);
        
        assertEquals(3, messages.getTotalElements());
        assertEquals("How are you?", messages.getContent().get(0).getContent());
        assertEquals("Hello", messages.getContent().get(2).getContent());
    }
    
    @Test
    void findByConversationIdOrderByCreatedAtDesc_shouldSupportPagination() {
        Pageable pageable = PageRequest.of(0, 2);
        
        Page<Message> page1 = messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, pageable);
        
        assertEquals(3, page1.getTotalElements());
        assertEquals(2, page1.getContent().size());
        assertEquals(2, page1.getTotalPages());
        
        Pageable pageable2 = PageRequest.of(1, 2);
        Page<Message> page2 = messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, pageable2);
        
        assertEquals(1, page2.getContent().size());
    }
    
    @Test
    void findByConversationIdOrderByCreatedAtDesc_shouldReturnEmptyForNonExistentConversation() {
        Pageable pageable = PageRequest.of(0, 10);
        
        Page<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtDesc("non-existent", pageable);
        
        assertTrue(messages.isEmpty());
    }
    
    @Test
    void save_shouldPersistMessage() {
        Message newMessage = Message.create(conversationId, "user2", "New message");
        
        Message saved = messageRepository.save(newMessage);
        
        assertNotNull(saved.getId());
        assertEquals("New message", saved.getContent());
        assertEquals("user2", saved.getSenderId());
        assertNotNull(saved.getCreatedAt());
    }
}
