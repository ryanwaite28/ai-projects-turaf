package com.turaf.communications.application.service;

import com.turaf.communications.application.dto.MessageCreatedDTO;
import com.turaf.communications.application.dto.MessageDTO;
import com.turaf.communications.application.dto.PaginatedMessages;
import com.turaf.communications.domain.event.MessageDeliveredEvent;
import com.turaf.communications.domain.exception.ConversationNotFoundException;
import com.turaf.communications.domain.exception.UnauthorizedParticipantException;
import com.turaf.communications.domain.model.Conversation;
import com.turaf.communications.domain.model.Message;
import com.turaf.communications.domain.repository.ConversationRepository;
import com.turaf.communications.domain.repository.MessageRepository;
import com.turaf.communications.infrastructure.messaging.EventBridgePublisher;
import com.turaf.communications.interfaces.mapper.MessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {
    
    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final EventBridgePublisher eventBridgePublisher;
    private final MessageMapper messageMapper;
    
    @Transactional
    public void processMessage(MessageCreatedDTO dto) {
        log.info("Processing message: id={}, conversationId={}", dto.getId(), dto.getConversationId());
        
        Conversation conversation = conversationRepository.findById(dto.getConversationId())
            .orElseThrow(() -> new ConversationNotFoundException(dto.getConversationId()));
        
        if (!conversation.isParticipant(dto.getSenderId())) {
            throw new UnauthorizedParticipantException("Sender is not a participant in this conversation");
        }
        
        Message message = Message.create(
            dto.getConversationId(),
            dto.getSenderId(),
            dto.getContent()
        );
        
        Message saved = messageRepository.save(message);
        log.info("Message persisted: id={}", saved.getId());
        
        publishMessageDeliveredEvent(saved, conversation);
    }
    
    private void publishMessageDeliveredEvent(Message message, Conversation conversation) {
        List<String> recipientIds = conversation.getParticipants().stream()
            .map(p -> p.getUserId())
            .filter(userId -> !userId.equals(message.getSenderId()))
            .collect(Collectors.toList());
        
        MessageDeliveredEvent event = new MessageDeliveredEvent(
            UUID.randomUUID().toString(),
            message.getId(),
            message.getConversationId(),
            message.getSenderId(),
            conversation.getOrganizationId(),
            conversation.getType(),
            recipientIds,
            message.getContent(),
            message.getCreatedAt()
        );
        
        eventBridgePublisher.publishMessageDelivered(event);
        log.info("MessageDelivered event published: messageId={}", message.getId());
    }
    
    @Transactional(readOnly = true)
    public PaginatedMessages getMessages(String conversationId, String userId, Pageable pageable) {
        log.debug("Fetching messages: conversationId={}, userId={}, page={}", 
                  conversationId, userId, pageable.getPageNumber());
        
        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new ConversationNotFoundException(conversationId));
        
        if (!conversation.isParticipant(userId)) {
            throw new UnauthorizedParticipantException("User is not a participant in this conversation");
        }
        
        Page<Message> messagePage = messageRepository.findByConversationIdOrderByCreatedAtDesc(
            conversationId, 
            pageable
        );
        
        List<MessageDTO> messages = messageMapper.toDTOList(messagePage.getContent());
        
        return PaginatedMessages.builder()
            .messages(messages)
            .page(messagePage.getNumber())
            .size(messagePage.getSize())
            .totalElements(messagePage.getTotalElements())
            .totalPages(messagePage.getTotalPages())
            .build();
    }
}
