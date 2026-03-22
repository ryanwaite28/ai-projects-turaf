package com.turaf.communications.domain.model;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ConversationTest {
    
    @Test
    void createDirect_shouldCreateConversationWithTwoParticipants() {
        Conversation conversation = Conversation.createDirect("user1", "user2");
        
        assertNotNull(conversation.getId());
        assertEquals(ConversationType.DIRECT, conversation.getType());
        assertEquals(2, conversation.getParticipants().size());
        assertTrue(conversation.isParticipant("user1"));
        assertTrue(conversation.isParticipant("user2"));
    }
    
    @Test
    void createGroup_shouldCreateConversationWithCreatorAsAdmin() {
        Conversation conversation = Conversation.createGroup(
            "Test Group",
            List.of("user1", "user2", "user3"),
            "user1"
        );
        
        assertEquals(ConversationType.GROUP, conversation.getType());
        assertEquals("Test Group", conversation.getName());
        assertTrue(conversation.isAdmin("user1"));
        assertFalse(conversation.isAdmin("user2"));
        assertEquals(3, conversation.getParticipants().size());
    }
    
    @Test
    void createGroup_shouldThrowExceptionWhenNameIsEmpty() {
        assertThrows(IllegalArgumentException.class, () -> 
            Conversation.createGroup("", List.of("user1", "user2"), "user1")
        );
    }
    
    @Test
    void addParticipant_shouldThrowExceptionWhenUserAlreadyExists() {
        Conversation conversation = Conversation.createDirect("user1", "user2");
        
        assertThrows(IllegalArgumentException.class, () -> 
            conversation.addParticipant("user1", ParticipantRole.MEMBER)
        );
    }
    
    @Test
    void removeParticipant_shouldRemoveParticipantFromConversation() {
        Conversation conversation = Conversation.createGroup(
            "Test Group",
            List.of("user1", "user2", "user3"),
            "user1"
        );
        
        conversation.removeParticipant("user3");
        
        assertEquals(2, conversation.getParticipants().size());
        assertFalse(conversation.isParticipant("user3"));
    }
    
    @Test
    void validateInvariants_shouldThrowExceptionWhenGroupHasNoAdmin() {
        Conversation conversation = Conversation.createGroup(
            "Test Group",
            List.of("user1", "user2", "user3"),
            "user1"
        );
        
        // Removing the admin should throw because no admin would remain
        assertThrows(IllegalStateException.class, () -> 
            conversation.removeParticipant("user1")
        );
    }
    
    @Test
    void isAdmin_shouldReturnTrueForAdminUser() {
        Conversation conversation = Conversation.createGroup(
            "Test Group",
            List.of("user1", "user2"),
            "user1"
        );
        
        assertTrue(conversation.isAdmin("user1"));
        assertFalse(conversation.isAdmin("user2"));
    }
}
