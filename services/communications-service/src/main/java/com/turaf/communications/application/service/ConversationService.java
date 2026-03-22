package com.turaf.communications.application.service;

import com.turaf.communications.application.dto.*;
import com.turaf.communications.domain.exception.ConversationNotFoundException;
import com.turaf.communications.domain.exception.UnauthorizedParticipantException;
import com.turaf.communications.domain.model.Conversation;
import com.turaf.communications.domain.model.ConversationType;
import com.turaf.communications.domain.repository.ConversationRepository;
import com.turaf.communications.interfaces.mapper.ConversationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationService {
    
    private final ConversationRepository conversationRepository;
    private final ConversationMapper conversationMapper;
    
    @Transactional
    public ConversationDTO createConversation(CreateConversationRequest request, String creatorId) {
        log.info("Creating conversation: type={}, creatorId={}", request.getType(), creatorId);
        
        Conversation conversation;
        
        if (request.getType() == ConversationType.DIRECT) {
            conversation = createDirectConversation(request, creatorId);
        } else {
            conversation = createGroupConversation(request, creatorId);
        }
        
        Conversation saved = conversationRepository.save(conversation);
        log.info("Conversation created: id={}", saved.getId());
        
        return conversationMapper.toDTO(saved);
    }
    
    private Conversation createDirectConversation(CreateConversationRequest request, String creatorId) {
        if (request.getParticipantIds().size() != 1) {
            throw new IllegalArgumentException("Direct conversation requires exactly 1 other participant");
        }
        
        String otherUserId = request.getParticipantIds().get(0);
        
        Optional<Conversation> existing = conversationRepository.findDirectConversation(creatorId, otherUserId);
        if (existing.isPresent()) {
            log.info("Direct conversation already exists between {} and {}", creatorId, otherUserId);
            return existing.get();
        }
        
        return Conversation.createDirect(creatorId, otherUserId);
    }
    
    private Conversation createGroupConversation(CreateConversationRequest request, String creatorId) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Group conversation name is required");
        }
        
        List<String> participantIds = new ArrayList<>(request.getParticipantIds());
        if (!participantIds.contains(creatorId)) {
            participantIds.add(creatorId);
        }
        
        return Conversation.createGroup(request.getName(), participantIds, creatorId);
    }
    
    @Transactional(readOnly = true)
    public List<ConversationDTO> getUserConversations(String userId) {
        log.debug("Fetching conversations for user: {}", userId);
        List<Conversation> conversations = conversationRepository.findByUserId(userId);
        return conversationMapper.toDTOList(conversations);
    }
    
    @Transactional(readOnly = true)
    public ConversationDTO getConversation(String conversationId, String userId) {
        log.debug("Fetching conversation: id={}, userId={}", conversationId, userId);
        
        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new ConversationNotFoundException(conversationId));
        
        if (!conversation.isParticipant(userId)) {
            throw new UnauthorizedParticipantException("User is not a participant in this conversation");
        }
        
        return conversationMapper.toDTO(conversation);
    }
    
    @Transactional
    public void addParticipant(String conversationId, String requesterId, AddParticipantRequest request) {
        log.info("Adding participant to conversation: conversationId={}, newUserId={}", 
                 conversationId, request.getUserId());
        
        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new ConversationNotFoundException(conversationId));
        
        if (conversation.getType() == ConversationType.GROUP) {
            if (!conversation.isAdmin(requesterId)) {
                throw new UnauthorizedParticipantException("Only admins can add participants to group conversations");
            }
        } else {
            throw new IllegalArgumentException("Cannot add participants to direct conversations");
        }
        
        conversation.addParticipant(request.getUserId(), request.getRole());
        conversationRepository.save(conversation);
        
        log.info("Participant added successfully");
    }
    
    @Transactional
    public void removeParticipant(String conversationId, String requesterId, String userIdToRemove) {
        log.info("Removing participant from conversation: conversationId={}, userId={}", 
                 conversationId, userIdToRemove);
        
        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new ConversationNotFoundException(conversationId));
        
        boolean isSelfRemoval = requesterId.equals(userIdToRemove);
        boolean isAdmin = conversation.isAdmin(requesterId);
        
        if (!isSelfRemoval && !isAdmin) {
            throw new UnauthorizedParticipantException("Only admins can remove other participants");
        }
        
        conversation.removeParticipant(userIdToRemove);
        conversationRepository.save(conversation);
        
        log.info("Participant removed successfully");
    }
}
