package com.turaf.communications.interfaces.mapper;

import com.turaf.communications.application.dto.ConversationDTO;
import com.turaf.communications.domain.model.Conversation;
import com.turaf.communications.domain.model.ConversationType;
import com.turaf.communications.domain.model.ParticipantRole;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConversationMapperTest {
    
    private final ConversationMapper mapper = Mappers.getMapper(ConversationMapper.class);
    
    @Test
    void toDTO_shouldMapDirectConversationCorrectly() {
        Conversation conversation = Conversation.createDirect("user1", "user2");
        
        ConversationDTO dto = mapper.toDTO(conversation);
        
        assertNotNull(dto);
        assertEquals(conversation.getId(), dto.getId());
        assertEquals(ConversationType.DIRECT, dto.getType());
        assertNull(dto.getName());
        assertNotNull(dto.getCreatedAt());
        assertNotNull(dto.getUpdatedAt());
        assertEquals(2, dto.getParticipants().size());
    }
    
    @Test
    void toDTO_shouldMapGroupConversationCorrectly() {
        Conversation conversation = Conversation.createGroup("Test Group", List.of("user2", "user3"), "user1");
        
        ConversationDTO dto = mapper.toDTO(conversation);
        
        assertNotNull(dto);
        assertEquals(conversation.getId(), dto.getId());
        assertEquals(ConversationType.GROUP, dto.getType());
        assertEquals("Test Group", dto.getName());
        assertEquals(3, dto.getParticipants().size());
    }
    
    @Test
    void toDTO_shouldHandleNullConversation() {
        ConversationDTO dto = mapper.toDTO((Conversation) null);
        
        assertNull(dto);
    }
    
    @Test
    void toDTO_shouldMapAllParticipants() {
        Conversation conversation = Conversation.createGroup("Group", List.of("user2", "user3"), "user1");
        
        ConversationDTO dto = mapper.toDTO(conversation);
        
        assertNotNull(dto.getParticipants());
        assertEquals(3, dto.getParticipants().size());
        
        boolean hasAdmin = dto.getParticipants().stream()
            .anyMatch(p -> p.getRole() == ParticipantRole.ADMIN);
        assertTrue(hasAdmin, "Should have at least one admin");
        
        boolean hasMembers = dto.getParticipants().stream()
            .anyMatch(p -> p.getRole() == ParticipantRole.MEMBER);
        assertTrue(hasMembers, "Should have members");
    }
    
    @Test
    void toDTOList_shouldMapMultipleConversations() {
        Conversation conv1 = Conversation.createDirect("user1", "user2");
        Conversation conv2 = Conversation.createGroup("Group", List.of("user2"), "user1");
        
        List<ConversationDTO> dtos = mapper.toDTOList(List.of(conv1, conv2));
        
        assertNotNull(dtos);
        assertEquals(2, dtos.size());
        assertEquals(ConversationType.DIRECT, dtos.get(0).getType());
        assertEquals(ConversationType.GROUP, dtos.get(1).getType());
    }
    
    @Test
    void toDTOList_shouldHandleEmptyList() {
        List<ConversationDTO> dtos = mapper.toDTOList(List.of());
        
        assertNotNull(dtos);
        assertTrue(dtos.isEmpty());
    }
}
