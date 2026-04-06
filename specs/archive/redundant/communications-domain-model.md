# Communications Domain Model Specification

**Bounded Context**: Communications  
**Domain**: Real-time messaging and collaboration  
**Design Pattern**: Domain-Driven Design (DDD)

---

## Overview

The Communications domain model defines the entities, value objects, aggregates, and business rules for the messaging system. It follows DDD principles with clear aggregate boundaries, invariants, and domain events.

---

## Aggregates

### Conversation (Aggregate Root)

**Purpose**: Represents a chat conversation between two or more users.

**Invariants**:
- Must have at least 2 participants
- DIRECT conversations must have exactly 2 participants
- GROUP conversations can have 2+ participants
- Participant user IDs must be unique within a conversation
- At least one participant must have ADMIN role in GROUP conversations

**State**:
```java
public class Conversation {
    private ConversationId id;
    private ConversationType type;
    private String name;  // Optional, required for GROUP
    private List<Participant> participants;
    private Instant createdAt;
    private Instant updatedAt;
}
```

**Business Methods**:
```java
// Factory method
public static Conversation createDirect(UserId user1, UserId user2) {
    validateDirectConversation(user1, user2);
    // Create conversation with exactly 2 participants
}

public static Conversation createGroup(String name, List<UserId> userIds, UserId creatorId) {
    validateGroupConversation(name, userIds);
    // Create conversation with creator as ADMIN
}

// Participant management
public void addParticipant(UserId userId, ParticipantRole role, UserId addedBy) {
    validateCanAddParticipant(addedBy);
    validateUniqueParticipant(userId);
    // Add participant
}

public void removeParticipant(UserId userId, UserId removedBy) {
    validateCanRemoveParticipant(removedBy, userId);
    validateNotLastAdmin(userId);
    // Remove participant
}

public void promoteToAdmin(UserId userId, UserId promotedBy) {
    validateIsAdmin(promotedBy);
    // Promote participant to ADMIN
}

// Queries
public boolean isParticipant(UserId userId);
public boolean isAdmin(UserId userId);
public int getParticipantCount();
```

**Domain Events**:
- `ConversationCreated`
- `ParticipantAdded`
- `ParticipantRemoved`
- `ParticipantPromoted`

---

## Entities

### Message

**Purpose**: Represents a single message within a conversation.

**Characteristics**:
- Immutable once created (future: support editing with edit history)
- Always associated with a conversation
- Always has a sender (participant)

**State**:
```java
public class Message {
    private MessageId id;
    private ConversationId conversationId;
    private UserId senderId;
    private String content;
    private Instant createdAt;
    // Future: attachments, reactions, editedAt
}
```

**Business Rules**:
- Content cannot be empty or null
- Content max length: 10,000 characters
- Sender must be a participant in the conversation
- Messages are append-only (no deletion, future: soft delete)

**Validation**:
```java
public void validate() {
    if (content == null || content.trim().isEmpty()) {
        throw new InvalidMessageException("Content cannot be empty");
    }
    if (content.length() > 10000) {
        throw new InvalidMessageException("Content exceeds maximum length");
    }
}
```

---

### Participant

**Purpose**: Represents a user's membership in a conversation.

**State**:
```java
public class Participant {
    private ParticipantId id;
    private ConversationId conversationId;
    private UserId userId;
    private ParticipantRole role;
    private Instant joinedAt;
}
```

**Business Rules**:
- One participant entry per user per conversation
- Role can be MEMBER or ADMIN
- ADMIN can add/remove participants, promote members
- MEMBER can only leave voluntarily

---

### ReadState

**Purpose**: Tracks the last message a user has read in a conversation.

**State**:
```java
public class ReadState {
    private ReadStateId id;
    private UserId userId;
    private ConversationId conversationId;
    private MessageId lastReadMessageId;
    private Instant updatedAt;
}
```

**Business Rules**:
- One read state entry per user per conversation
- lastReadMessageId must reference a valid message in the conversation
- Automatically created when user joins conversation (null lastReadMessageId)
- Updated when user marks messages as read

**Unread Count Calculation**:
```java
public int calculateUnreadCount(List<Message> messages) {
    if (lastReadMessageId == null) {
        return messages.size();
    }
    
    // Count messages created after the last read message
    return (int) messages.stream()
        .filter(m -> m.getCreatedAt().isAfter(getLastReadMessageTimestamp()))
        .count();
}
```

---

## Value Objects

### ConversationId

```java
public class ConversationId {
    private final String value;  // UUID
    
    public static ConversationId generate() {
        return new ConversationId(UUID.randomUUID().toString());
    }
    
    public static ConversationId of(String value) {
        validateUUID(value);
        return new ConversationId(value);
    }
}
```

### MessageId

```java
public class MessageId {
    private final String value;  // UUID
    
    public static MessageId generate() {
        return new MessageId(UUID.randomUUID().toString());
    }
}
```

### ConversationType

```java
public enum ConversationType {
    DIRECT,   // 1-on-1 conversation
    GROUP;    // Multi-user conversation
    
    public void validateParticipantCount(int count) {
        if (this == DIRECT && count != 2) {
            throw new InvalidConversationException("Direct conversations must have exactly 2 participants");
        }
        if (this == GROUP && count < 2) {
            throw new InvalidConversationException("Group conversations must have at least 2 participants");
        }
    }
}
```

### ParticipantRole

```java
public enum ParticipantRole {
    MEMBER,   // Regular participant
    ADMIN;    // Can manage conversation
    
    public boolean canAddParticipants() {
        return this == ADMIN;
    }
    
    public boolean canRemoveParticipants() {
        return this == ADMIN;
    }
    
    public boolean canPromoteMembers() {
        return this == ADMIN;
    }
}
```

---

## Domain Events

### MessageDeliveredEvent

**Trigger**: When a message is successfully persisted to the database

**Payload**:
```java
public class MessageDeliveredEvent extends DomainEvent {
    private String eventId;
    private String messageId;
    private String conversationId;
    private String senderId;
    private ConversationType conversationType;
    private List<String> recipientIds;  // All participants except sender
    private Instant deliveredAt;
    private String version = "1.0";
}
```

**Consumers**:
- Notification Service (send push notifications)
- Analytics Service (track messaging activity)

---

### ConversationCreatedEvent

**Trigger**: When a new conversation is created

**Payload**:
```java
public class ConversationCreatedEvent extends DomainEvent {
    private String eventId;
    private String conversationId;
    private ConversationType type;
    private String name;
    private List<String> participantIds;
    private String createdBy;
    private Instant createdAt;
}
```

---

### ParticipantAddedEvent

**Trigger**: When a user is added to a conversation

**Payload**:
```java
public class ParticipantAddedEvent extends DomainEvent {
    private String eventId;
    private String conversationId;
    private String userId;
    private String addedBy;
    private ParticipantRole role;
    private Instant addedAt;
}
```

---

## Repository Interfaces

### ConversationRepository

```java
public interface ConversationRepository {
    Conversation save(Conversation conversation);
    Optional<Conversation> findById(ConversationId id);
    List<Conversation> findByUserId(UserId userId);
    Optional<Conversation> findDirectConversation(UserId user1, UserId user2);
    void delete(ConversationId id);
}
```

### MessageRepository

```java
public interface MessageRepository {
    Message save(Message message);
    Optional<Message> findById(MessageId id);
    Page<Message> findByConversationId(ConversationId conversationId, Pageable pageable);
    List<Message> findUnreadMessages(UserId userId, ConversationId conversationId);
    int countUnreadMessages(UserId userId, ConversationId conversationId);
}
```

### ParticipantRepository

```java
public interface ParticipantRepository {
    Participant save(Participant participant);
    List<Participant> findByConversationId(ConversationId conversationId);
    Optional<Participant> findByConversationIdAndUserId(ConversationId conversationId, UserId userId);
    void delete(Participant participant);
    boolean existsByConversationIdAndUserId(ConversationId conversationId, UserId userId);
}
```

### ReadStateRepository

```java
public interface ReadStateRepository {
    ReadState save(ReadState readState);
    Optional<ReadState> findByUserIdAndConversationId(UserId userId, ConversationId conversationId);
    List<ReadState> findByUserId(UserId userId);
}
```

---

## Business Rules

### Conversation Rules

1. **Creation**:
   - DIRECT: Exactly 2 participants, no name required
   - GROUP: 2+ participants, name required
   - Creator automatically becomes ADMIN in GROUP conversations

2. **Participant Management**:
   - Only ADMINs can add participants to GROUP conversations
   - Only ADMINs can remove participants (except self)
   - Any participant can leave voluntarily
   - Cannot remove the last ADMIN from a GROUP conversation
   - Participants must be unique (no duplicates)

3. **Deletion**:
   - Conversations can be soft-deleted (future feature)
   - Hard deletion removes all messages and read states (cascade)

### Message Rules

1. **Creation**:
   - Sender must be a participant in the conversation
   - Content cannot be empty
   - Content max length: 10,000 characters

2. **Immutability**:
   - Messages cannot be edited (future: edit history)
   - Messages cannot be deleted (future: soft delete with tombstone)

3. **Ordering**:
   - Messages are ordered by createdAt timestamp
   - SQS FIFO queues ensure ordering per conversation

### Read State Rules

1. **Initialization**:
   - Created when user joins conversation
   - Initial lastReadMessageId is null (all messages unread)

2. **Updates**:
   - Updated when user marks messages as read
   - lastReadMessageId must reference a valid message
   - Cannot mark future messages as read

3. **Unread Count**:
   - Calculated as messages created after lastReadMessageId
   - Efficient query using indexed timestamps

---

## Aggregate Boundaries

```
Conversation (Aggregate Root)
├── Participants (Entities within aggregate)
└── Messages (Separate aggregate, referenced by ID)

Message (Aggregate Root)
└── No child entities

ReadState (Aggregate Root)
└── No child entities
```

**Rationale**:
- **Conversation** owns **Participants** because they are tightly coupled
- **Messages** are separate to allow independent scaling and querying
- **ReadState** is separate for independent updates per user

---

## Domain Services

### ConversationService

```java
public class ConversationService {
    public Conversation createDirectConversation(UserId user1, UserId user2) {
        // Check if conversation already exists
        Optional<Conversation> existing = repository.findDirectConversation(user1, user2);
        if (existing.isPresent()) {
            return existing.get();
        }
        
        // Create new conversation
        Conversation conversation = Conversation.createDirect(user1, user2);
        return repository.save(conversation);
    }
    
    public Conversation createGroupConversation(String name, List<UserId> userIds, UserId creatorId) {
        Conversation conversation = Conversation.createGroup(name, userIds, creatorId);
        return repository.save(conversation);
    }
}
```

### UnreadCountService

```java
public class UnreadCountService {
    public int calculateUnreadCount(UserId userId, ConversationId conversationId) {
        ReadState readState = readStateRepository
            .findByUserIdAndConversationId(userId, conversationId)
            .orElseThrow();
        
        return messageRepository.countUnreadMessages(userId, conversationId);
    }
    
    public Map<ConversationId, Integer> calculateAllUnreadCounts(UserId userId) {
        List<Conversation> conversations = conversationRepository.findByUserId(userId);
        
        return conversations.stream()
            .collect(Collectors.toMap(
                Conversation::getId,
                conv -> calculateUnreadCount(userId, conv.getId())
            ));
    }
}
```

---

## Anti-Corruption Layer

Since the Communications domain references users from the Identity Service, we use an anti-corruption layer:

```java
public interface UserIdentityService {
    UserInfo getUserInfo(UserId userId);
    boolean userExists(UserId userId);
    List<UserInfo> getUsersInfo(List<UserId> userIds);
}

// Implementation calls Identity Service REST API
public class UserIdentityServiceImpl implements UserIdentityService {
    private final RestTemplate restTemplate;
    
    @Override
    public UserInfo getUserInfo(UserId userId) {
        // Call identity service API
        return restTemplate.getForObject(
            "http://internal-alb/identity/users/" + userId.getValue(),
            UserInfo.class
        );
    }
}
```

---

## Validation Rules

### Conversation Validation

```java
public class ConversationValidator {
    public void validate(Conversation conversation) {
        validateType(conversation);
        validateParticipants(conversation);
        validateName(conversation);
    }
    
    private void validateType(Conversation conversation) {
        if (conversation.getType() == null) {
            throw new ValidationException("Conversation type is required");
        }
    }
    
    private void validateParticipants(Conversation conversation) {
        int count = conversation.getParticipantCount();
        conversation.getType().validateParticipantCount(count);
        
        // Check for unique participants
        Set<UserId> uniqueIds = conversation.getParticipants().stream()
            .map(Participant::getUserId)
            .collect(Collectors.toSet());
        
        if (uniqueIds.size() != count) {
            throw new ValidationException("Duplicate participants not allowed");
        }
    }
    
    private void validateName(Conversation conversation) {
        if (conversation.getType() == ConversationType.GROUP) {
            if (conversation.getName() == null || conversation.getName().trim().isEmpty()) {
                throw new ValidationException("Group conversation name is required");
            }
        }
    }
}
```

---

## References

- **PROJECT.md**: Section 6 (Domain Model)
- **Architecture**: `specs/architecture.md`
- **Communications Service**: `specs/communications-service.md`
- **Event Schemas**: `specs/communications-event-schemas.md`
- **DDD Patterns**: Eric Evans - Domain-Driven Design
