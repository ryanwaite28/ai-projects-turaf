# Task: Implement Message Service

**Service**: Communications Service  
**Type**: Application Layer  
**Priority**: High  
**Estimated Time**: 3 hours  
**Dependencies**: 002-create-domain-model, 003-create-repositories

---

## Objective

Implement the MessageService to process incoming messages from SQS, persist them to the database, and publish MessageDelivered events to EventBridge.

---

## Acceptance Criteria

- [x] MessageService processes messages from SQS DTOs
- [x] Messages validated and persisted to database
- [x] MessageDelivered events published to EventBridge
- [x] Pagination support for message retrieval
- [x] Authorization checks for message access
- [x] Unit tests pass with >80% coverage

---

## Implementation

**File**: `application/service/MessageService.java`

```java
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
import java.time.Instant;
import java.util.List;
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
        
        // Validate conversation exists
        Conversation conversation = conversationRepository.findById(dto.getConversationId())
            .orElseThrow(() -> new ConversationNotFoundException(dto.getConversationId()));
        
        // Validate sender is a participant
        if (!conversation.isParticipant(dto.getSenderId())) {
            throw new UnauthorizedParticipantException("Sender is not a participant in this conversation");
        }
        
        // Create and save message
        Message message = Message.create(
            dto.getConversationId(),
            dto.getSenderId(),
            dto.getContent()
        );
        
        Message saved = messageRepository.save(message);
        log.info("Message persisted: id={}", saved.getId());
        
        // Publish MessageDelivered event
        publishMessageDeliveredEvent(saved, conversation);
    }
    
    private void publishMessageDeliveredEvent(Message message, Conversation conversation) {
        List<String> recipientIds = conversation.getParticipants().stream()
            .map(p -> p.getUserId())
            .filter(userId -> !userId.equals(message.getSenderId()))
            .collect(Collectors.toList());
        
        MessageDeliveredEvent event = new MessageDeliveredEvent(
            message.getId(),
            message.getConversationId(),
            message.getSenderId(),
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
        
        // Verify user is a participant
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
```

**File**: `application/dto/MessageDTO.java`

```java
package com.turaf.communications.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDTO {
    private String id;
    private String conversationId;
    private String senderId;
    private String content;
    private Instant createdAt;
}
```

**File**: `application/dto/PaginatedMessages.java`

```java
package com.turaf.communications.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginatedMessages {
    private List<MessageDTO> messages;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
```

**File**: `interfaces/mapper/MessageMapper.java`

```java
package com.turaf.communications.interfaces.mapper;

import com.turaf.communications.application.dto.MessageDTO;
import com.turaf.communications.domain.model.Message;
import org.mapstruct.Mapper;
import java.util.List;

@Mapper(componentModel = "spring")
public interface MessageMapper {
    MessageDTO toDTO(Message message);
    List<MessageDTO> toDTOList(List<Message> messages);
}
```

---

## References

- **Spec**: `specs/communications-service.md`
- **Domain Events**: `specs/communications-event-schemas.md`
