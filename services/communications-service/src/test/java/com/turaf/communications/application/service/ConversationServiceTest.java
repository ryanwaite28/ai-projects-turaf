package com.turaf.communications.application.service;

import com.turaf.communications.application.dto.AddParticipantRequest;
import com.turaf.communications.application.dto.ConversationDTO;
import com.turaf.communications.application.dto.CreateConversationRequest;
import com.turaf.communications.domain.exception.ConversationNotFoundException;
import com.turaf.communications.domain.exception.UnauthorizedParticipantException;
import com.turaf.communications.domain.model.Conversation;
import com.turaf.communications.domain.model.ConversationType;
import com.turaf.communications.domain.model.ParticipantRole;
import com.turaf.communications.domain.repository.ConversationRepository;
import com.turaf.communications.interfaces.mapper.ConversationMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {
    
    @Mock
    private ConversationRepository conversationRepository;
    
    @Mock
    private ConversationMapper conversationMapper;
    
    @InjectMocks
    private ConversationService conversationService;
    
    @Test
    void createDirectConversation_shouldCreateNewConversation() {
        CreateConversationRequest request = new CreateConversationRequest(
            ConversationType.DIRECT,
            null,
            List.of("user2")
        );
        
        Conversation conversation = Conversation.createDirect("user1", "user2");
        when(conversationRepository.findDirectConversation("user1", "user2"))
            .thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class)))
            .thenReturn(conversation);
        when(conversationMapper.toDTO(any(Conversation.class)))
            .thenReturn(new ConversationDTO());
        
        ConversationDTO result = conversationService.createConversation(request, "user1");
        
        assertNotNull(result);
        verify(conversationRepository).save(any(Conversation.class));
    }
    
    @Test
    void createDirectConversation_shouldReturnExistingWhenAlreadyExists() {
        CreateConversationRequest request = new CreateConversationRequest(
            ConversationType.DIRECT,
            null,
            List.of("user2")
        );
        
        Conversation existing = Conversation.createDirect("user1", "user2");
        when(conversationRepository.findDirectConversation("user1", "user2"))
            .thenReturn(Optional.of(existing));
        when(conversationRepository.save(any(Conversation.class)))
            .thenReturn(existing);
        when(conversationMapper.toDTO(any(Conversation.class)))
            .thenReturn(new ConversationDTO());
        
        ConversationDTO result = conversationService.createConversation(request, "user1");
        
        assertNotNull(result);
        verify(conversationRepository).save(any(Conversation.class));
    }
    
    @Test
    void createDirectConversation_shouldThrowWhenMultipleParticipants() {
        CreateConversationRequest request = new CreateConversationRequest(
            ConversationType.DIRECT,
            null,
            List.of("user2", "user3")
        );
        
        assertThrows(IllegalArgumentException.class, () -> 
            conversationService.createConversation(request, "user1")
        );
    }
    
    @Test
    void createGroupConversation_shouldRequireName() {
        CreateConversationRequest request = new CreateConversationRequest(
            ConversationType.GROUP,
            null,
            List.of("user2", "user3")
        );
        
        assertThrows(IllegalArgumentException.class, () -> 
            conversationService.createConversation(request, "user1")
        );
    }
    
    @Test
    void createGroupConversation_shouldCreateWithName() {
        CreateConversationRequest request = new CreateConversationRequest(
            ConversationType.GROUP,
            "Test Group",
            List.of("user2", "user3")
        );
        
        Conversation conversation = Conversation.createGroup("Test Group", List.of("user1", "user2", "user3"), "user1");
        when(conversationRepository.save(any(Conversation.class)))
            .thenReturn(conversation);
        when(conversationMapper.toDTO(any(Conversation.class)))
            .thenReturn(new ConversationDTO());
        
        ConversationDTO result = conversationService.createConversation(request, "user1");
        
        assertNotNull(result);
        verify(conversationRepository).save(any(Conversation.class));
    }
    
    @Test
    void getUserConversations_shouldReturnUserConversations() {
        List<Conversation> conversations = List.of(
            Conversation.createDirect("user1", "user2")
        );
        
        when(conversationRepository.findByUserId("user1"))
            .thenReturn(conversations);
        when(conversationMapper.toDTOList(conversations))
            .thenReturn(List.of(new ConversationDTO()));
        
        List<ConversationDTO> result = conversationService.getUserConversations("user1");
        
        assertNotNull(result);
        assertEquals(1, result.size());
    }
    
    @Test
    void getConversation_shouldReturnConversationForParticipant() {
        String conversationId = "conv-1";
        Conversation conversation = Conversation.createDirect("user1", "user2");
        
        when(conversationRepository.findById(conversationId))
            .thenReturn(Optional.of(conversation));
        when(conversationMapper.toDTO(conversation))
            .thenReturn(new ConversationDTO());
        
        ConversationDTO result = conversationService.getConversation(conversationId, "user1");
        
        assertNotNull(result);
    }
    
    @Test
    void getConversation_shouldThrowWhenNotFound() {
        String conversationId = "conv-1";
        
        when(conversationRepository.findById(conversationId))
            .thenReturn(Optional.empty());
        
        assertThrows(ConversationNotFoundException.class, () -> 
            conversationService.getConversation(conversationId, "user1")
        );
    }
    
    @Test
    void getConversation_shouldThrowWhenNotParticipant() {
        String conversationId = "conv-1";
        Conversation conversation = Conversation.createDirect("user1", "user2");
        
        when(conversationRepository.findById(conversationId))
            .thenReturn(Optional.of(conversation));
        
        assertThrows(UnauthorizedParticipantException.class, () -> 
            conversationService.getConversation(conversationId, "user3")
        );
    }
    
    @Test
    void addParticipant_shouldAddToGroupConversation() {
        String conversationId = "conv-1";
        Conversation conversation = Conversation.createGroup("Test Group", List.of("user1", "user2"), "user1");
        AddParticipantRequest request = new AddParticipantRequest("user3", ParticipantRole.MEMBER);
        
        when(conversationRepository.findById(conversationId))
            .thenReturn(Optional.of(conversation));
        when(conversationRepository.save(any(Conversation.class)))
            .thenReturn(conversation);
        
        conversationService.addParticipant(conversationId, "user1", request);
        
        verify(conversationRepository).save(any(Conversation.class));
    }
    
    @Test
    void addParticipant_shouldThrowWhenNotAdmin() {
        String conversationId = "conv-1";
        Conversation conversation = Conversation.createGroup("Test Group", List.of("user1", "user2"), "user1");
        AddParticipantRequest request = new AddParticipantRequest("user3", ParticipantRole.MEMBER);
        
        when(conversationRepository.findById(conversationId))
            .thenReturn(Optional.of(conversation));
        
        assertThrows(UnauthorizedParticipantException.class, () -> 
            conversationService.addParticipant(conversationId, "user2", request)
        );
    }
    
    @Test
    void addParticipant_shouldThrowForDirectConversation() {
        String conversationId = "conv-1";
        Conversation conversation = Conversation.createDirect("user1", "user2");
        AddParticipantRequest request = new AddParticipantRequest("user3", ParticipantRole.MEMBER);
        
        when(conversationRepository.findById(conversationId))
            .thenReturn(Optional.of(conversation));
        
        assertThrows(IllegalArgumentException.class, () -> 
            conversationService.addParticipant(conversationId, "user1", request)
        );
    }
    
    @Test
    void removeParticipant_shouldAllowSelfRemoval() {
        String conversationId = "conv-1";
        Conversation conversation = Conversation.createGroup("Test Group", List.of("user1", "user2", "user3"), "user1");
        
        when(conversationRepository.findById(conversationId))
            .thenReturn(Optional.of(conversation));
        when(conversationRepository.save(any(Conversation.class)))
            .thenReturn(conversation);
        
        conversationService.removeParticipant(conversationId, "user2", "user2");
        
        verify(conversationRepository).save(any(Conversation.class));
    }
    
    @Test
    void removeParticipant_shouldAllowAdminToRemoveOthers() {
        String conversationId = "conv-1";
        Conversation conversation = Conversation.createGroup("Test Group", List.of("user1", "user2", "user3"), "user1");
        
        when(conversationRepository.findById(conversationId))
            .thenReturn(Optional.of(conversation));
        when(conversationRepository.save(any(Conversation.class)))
            .thenReturn(conversation);
        
        conversationService.removeParticipant(conversationId, "user1", "user2");
        
        verify(conversationRepository).save(any(Conversation.class));
    }
    
    @Test
    void removeParticipant_shouldThrowWhenNonAdminRemovesOthers() {
        String conversationId = "conv-1";
        Conversation conversation = Conversation.createGroup("Test Group", List.of("user1", "user2", "user3"), "user1");
        
        when(conversationRepository.findById(conversationId))
            .thenReturn(Optional.of(conversation));
        
        assertThrows(UnauthorizedParticipantException.class, () -> 
            conversationService.removeParticipant(conversationId, "user2", "user3")
        );
    }
}
