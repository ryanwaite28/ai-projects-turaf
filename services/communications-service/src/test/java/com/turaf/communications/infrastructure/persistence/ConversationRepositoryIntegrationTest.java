package com.turaf.communications.infrastructure.persistence;

import com.turaf.communications.domain.model.Conversation;
import com.turaf.communications.domain.model.ConversationType;
import com.turaf.communications.domain.repository.ConversationRepository;
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
class ConversationRepositoryIntegrationTest {
    
    @Autowired
    private ConversationRepository conversationRepository;
    
    private Conversation directConversation;
    private Conversation groupConversation;
    
    @BeforeEach
    void setUp() {
        directConversation = Conversation.createDirect("user1", "user2");
        conversationRepository.save(directConversation);
        
        groupConversation = Conversation.createGroup("Test Group", List.of("user2", "user3"), "user1");
        conversationRepository.save(groupConversation);
    }
    
    @Test
    void findByUserId_shouldReturnUserConversations() {
        List<Conversation> conversations = conversationRepository.findByUserId("user1");
        
        assertEquals(2, conversations.size());
        assertTrue(conversations.stream().anyMatch(c -> c.getType() == ConversationType.DIRECT));
        assertTrue(conversations.stream().anyMatch(c -> c.getType() == ConversationType.GROUP));
    }
    
    @Test
    void findByUserId_shouldReturnEmptyForNonParticipant() {
        List<Conversation> conversations = conversationRepository.findByUserId("user999");
        
        assertTrue(conversations.isEmpty());
    }
    
    @Test
    void findDirectConversation_shouldFindExistingConversation() {
        Optional<Conversation> found = conversationRepository.findDirectConversation("user1", "user2");
        
        assertTrue(found.isPresent());
        assertEquals(directConversation.getId(), found.get().getId());
        assertEquals(ConversationType.DIRECT, found.get().getType());
    }
    
    @Test
    void findDirectConversation_shouldWorkWithReversedUserIds() {
        Optional<Conversation> found = conversationRepository.findDirectConversation("user2", "user1");
        
        assertTrue(found.isPresent());
        assertEquals(directConversation.getId(), found.get().getId());
    }
    
    @Test
    void findDirectConversation_shouldReturnEmptyForNonExistent() {
        Optional<Conversation> found = conversationRepository.findDirectConversation("user1", "user999");
        
        assertFalse(found.isPresent());
    }
    
    @Test
    void findDirectConversation_shouldNotReturnGroupConversations() {
        Optional<Conversation> found = conversationRepository.findDirectConversation("user1", "user3");
        
        assertFalse(found.isPresent());
    }
    
    @Test
    void save_shouldPersistConversationWithParticipants() {
        Conversation newConversation = Conversation.createDirect("user4", "user5");
        
        Conversation saved = conversationRepository.save(newConversation);
        
        assertNotNull(saved.getId());
        assertEquals(2, saved.getParticipants().size());
        
        Optional<Conversation> retrieved = conversationRepository.findById(saved.getId());
        assertTrue(retrieved.isPresent());
        assertEquals(2, retrieved.get().getParticipants().size());
    }
    
    @Test
    void delete_shouldRemoveConversationAndParticipants() {
        String conversationId = directConversation.getId();
        
        conversationRepository.delete(conversationId);
        
        Optional<Conversation> found = conversationRepository.findById(conversationId);
        assertFalse(found.isPresent());
    }
}
