# Task: Create Domain Model

**Service**: Communications Service  
**Type**: Domain Layer Implementation  
**Priority**: High  
**Estimated Time**: 4 hours  
**Dependencies**: 001-setup-project-structure

---

## Objective

Implement the domain model for the Communications bounded context following DDD principles, including entities, value objects, aggregates, and domain events.

---

## Acceptance Criteria

- [x] Conversation aggregate root implemented with invariants
- [x] Message, Participant, ReadState entities created
- [x] ConversationType and ParticipantRole value objects implemented
- [x] Domain events defined (MessageDeliveredEvent, ConversationCreatedEvent)
- [x] Domain exceptions created
- [x] Repository interfaces defined
- [x] All domain logic is testable and follows DDD patterns

---

## Implementation

### 1. Value Objects

**File**: `domain/model/ConversationType.java`

```java
package com.turaf.communications.domain.model;

public enum ConversationType {
    DIRECT,
    GROUP;
    
    public void validateParticipantCount(int count) {
        if (this == DIRECT && count != 2) {
            throw new IllegalArgumentException("Direct conversations must have exactly 2 participants");
        }
        if (this == GROUP && count < 2) {
            throw new IllegalArgumentException("Group conversations must have at least 2 participants");
        }
    }
}
```

**File**: `domain/model/ParticipantRole.java`

```java
package com.turaf.communications.domain.model;

public enum ParticipantRole {
    MEMBER,
    ADMIN;
    
    public boolean canAddParticipants() {
        return this == ADMIN;
    }
    
    public boolean canRemoveParticipants() {
        return this == ADMIN;
    }
}
```

### 2. Entities

**File**: `domain/model/Conversation.java`

```java
package com.turaf.communications.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "conversations", schema = "communications_schema")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Conversation {
    
    @Id
    private String id;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConversationType type;
    
    private String name;
    
    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Participant> participants = new ArrayList<>();
    
    @Column(nullable = false)
    private Instant createdAt;
    
    @Column(nullable = false)
    private Instant updatedAt;
    
    public static Conversation createDirect(String user1Id, String user2Id) {
        Conversation conversation = new Conversation();
        conversation.id = UUID.randomUUID().toString();
        conversation.type = ConversationType.DIRECT;
        conversation.createdAt = Instant.now();
        conversation.updatedAt = Instant.now();
        
        conversation.addParticipant(user1Id, ParticipantRole.MEMBER);
        conversation.addParticipant(user2Id, ParticipantRole.MEMBER);
        
        conversation.validateInvariants();
        return conversation;
    }
    
    public static Conversation createGroup(String name, List<String> userIds, String creatorId) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Group conversation name is required");
        }
        
        Conversation conversation = new Conversation();
        conversation.id = UUID.randomUUID().toString();
        conversation.type = ConversationType.GROUP;
        conversation.name = name;
        conversation.createdAt = Instant.now();
        conversation.updatedAt = Instant.now();
        
        conversation.addParticipant(creatorId, ParticipantRole.ADMIN);
        userIds.stream()
            .filter(userId -> !userId.equals(creatorId))
            .forEach(userId -> conversation.addParticipant(userId, ParticipantRole.MEMBER));
        
        conversation.validateInvariants();
        return conversation;
    }
    
    public void addParticipant(String userId, ParticipantRole role) {
        if (isParticipant(userId)) {
            throw new IllegalArgumentException("User is already a participant");
        }
        
        Participant participant = new Participant(
            UUID.randomUUID().toString(),
            this,
            userId,
            role,
            Instant.now()
        );
        participants.add(participant);
        this.updatedAt = Instant.now();
    }
    
    public void removeParticipant(String userId) {
        participants.removeIf(p -> p.getUserId().equals(userId));
        this.updatedAt = Instant.now();
        validateInvariants();
    }
    
    public boolean isParticipant(String userId) {
        return participants.stream()
            .anyMatch(p -> p.getUserId().equals(userId));
    }
    
    public boolean isAdmin(String userId) {
        return participants.stream()
            .anyMatch(p -> p.getUserId().equals(userId) && p.getRole() == ParticipantRole.ADMIN);
    }
    
    public void validateInvariants() {
        type.validateParticipantCount(participants.size());
        
        long uniqueUserIds = participants.stream()
            .map(Participant::getUserId)
            .distinct()
            .count();
        
        if (uniqueUserIds != participants.size()) {
            throw new IllegalStateException("Duplicate participants not allowed");
        }
        
        if (type == ConversationType.GROUP) {
            boolean hasAdmin = participants.stream()
                .anyMatch(p -> p.getRole() == ParticipantRole.ADMIN);
            if (!hasAdmin) {
                throw new IllegalStateException("Group conversation must have at least one admin");
            }
        }
    }
}
```

**File**: `domain/model/Message.java`

```java
package com.turaf.communications.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "messages", schema = "communications_schema")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Message {
    
    @Id
    private String id;
    
    @Column(name = "conversation_id", nullable = false)
    private String conversationId;
    
    @Column(name = "sender_id", nullable = false)
    private String senderId;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
    
    @Column(nullable = false)
    private Instant createdAt;
    
    public static Message create(String conversationId, String senderId, String content) {
        validateContent(content);
        
        return new Message(
            UUID.randomUUID().toString(),
            conversationId,
            senderId,
            content,
            Instant.now()
        );
    }
    
    private static void validateContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Message content cannot be empty");
        }
        if (content.length() > 10000) {
            throw new IllegalArgumentException("Message content exceeds maximum length of 10,000 characters");
        }
    }
}
```

**File**: `domain/model/Participant.java`

```java
package com.turaf.communications.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "participants", schema = "communications_schema",
       uniqueConstraints = @UniqueConstraint(columnNames = {"conversation_id", "user_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Participant {
    
    @Id
    private String id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;
    
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParticipantRole role;
    
    @Column(nullable = false)
    private Instant joinedAt;
}
```

**File**: `domain/model/ReadState.java`

```java
package com.turaf.communications.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "read_state", schema = "communications_schema",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "conversation_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ReadState {
    
    @Id
    private String id;
    
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @Column(name = "conversation_id", nullable = false)
    private String conversationId;
    
    @Column(name = "last_read_message_id")
    private String lastReadMessageId;
    
    @Column(nullable = false)
    private Instant updatedAt;
    
    public static ReadState create(String userId, String conversationId) {
        return new ReadState(
            UUID.randomUUID().toString(),
            userId,
            conversationId,
            null,
            Instant.now()
        );
    }
    
    public void markAsRead(String messageId) {
        this.lastReadMessageId = messageId;
        this.updatedAt = Instant.now();
    }
}
```

### 3. Domain Events

**File**: `domain/event/DomainEvent.java`

```java
package com.turaf.communications.domain.event;

import lombok.Getter;
import java.time.Instant;
import java.util.UUID;

@Getter
public abstract class DomainEvent {
    private final String eventId;
    private final Instant occurredAt;
    
    protected DomainEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.occurredAt = Instant.now();
    }
}
```

**File**: `domain/event/MessageDeliveredEvent.java`

```java
package com.turaf.communications.domain.event;

import com.turaf.communications.domain.model.ConversationType;
import lombok.Getter;
import java.time.Instant;
import java.util.List;

@Getter
public class MessageDeliveredEvent extends DomainEvent {
    private final String messageId;
    private final String conversationId;
    private final String senderId;
    private final ConversationType conversationType;
    private final List<String> recipientIds;
    private final String content;
    private final Instant deliveredAt;
    
    public MessageDeliveredEvent(
        String messageId,
        String conversationId,
        String senderId,
        ConversationType conversationType,
        List<String> recipientIds,
        String content,
        Instant deliveredAt
    ) {
        super();
        this.messageId = messageId;
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.conversationType = conversationType;
        this.recipientIds = recipientIds;
        this.content = content;
        this.deliveredAt = deliveredAt;
    }
}
```

### 4. Domain Exceptions

**File**: `domain/exception/ConversationNotFoundException.java`

```java
package com.turaf.communications.domain.exception;

public class ConversationNotFoundException extends RuntimeException {
    public ConversationNotFoundException(String conversationId) {
        super("Conversation not found: " + conversationId);
    }
}
```

**File**: `domain/exception/UnauthorizedParticipantException.java`

```java
package com.turaf.communications.domain.exception;

public class UnauthorizedParticipantException extends RuntimeException {
    public UnauthorizedParticipantException(String message) {
        super(message);
    }
}
```

### 5. Repository Interfaces

**File**: `domain/repository/ConversationRepository.java`

```java
package com.turaf.communications.domain.repository;

import com.turaf.communications.domain.model.Conversation;
import java.util.List;
import java.util.Optional;

public interface ConversationRepository {
    Conversation save(Conversation conversation);
    Optional<Conversation> findById(String id);
    List<Conversation> findByUserId(String userId);
    Optional<Conversation> findDirectConversation(String user1Id, String user2Id);
    void delete(String id);
}
```

**File**: `domain/repository/MessageRepository.java`

```java
package com.turaf.communications.domain.repository;

import com.turaf.communications.domain.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

public interface MessageRepository {
    Message save(Message message);
    Optional<Message> findById(String id);
    Page<Message> findByConversationId(String conversationId, Pageable pageable);
    int countUnreadMessages(String userId, String conversationId);
}
```

**File**: `domain/repository/ReadStateRepository.java`

```java
package com.turaf.communications.domain.repository;

import com.turaf.communications.domain.model.ReadState;
import java.util.List;
import java.util.Optional;

public interface ReadStateRepository {
    ReadState save(ReadState readState);
    Optional<ReadState> findByUserIdAndConversationId(String userId, String conversationId);
    List<ReadState> findByUserId(String userId);
}
```

---

## Testing

Create unit tests for domain logic:

**File**: `src/test/java/com/turaf/communications/domain/model/ConversationTest.java`

```java
package com.turaf.communications.domain.model;

import org.junit.jupiter.api.Test;
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
    }
}
```

---

## Verification

- [ ] All domain classes compile without errors
- [ ] Unit tests pass
- [ ] Domain invariants are enforced
- [ ] No infrastructure dependencies in domain layer

---

## References

- **Spec**: `specs/communications-domain-model.md`
- **Spec**: `specs/communications-service.md`
- **PROJECT.md**: Section 6 (Domain Model)
