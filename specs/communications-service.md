# Communications Service Specification

**Service Type**: Spring Boot Microservice  
**Language**: Java 17  
**Framework**: Spring Boot 3.x  
**Database**: PostgreSQL (communications_schema)  
**Messaging**: AWS SQS (Consumer), AWS EventBridge (Publisher)

---

## Overview

The Communications Service manages real-time messaging functionality for the Turaf platform. It handles the persistence, retrieval, and business logic for conversations, messages, participants, and read state tracking. The service consumes messages from SQS FIFO queues (published by the WebSocket Gateway) and publishes domain events to EventBridge.

---

## Responsibilities

- Persist messages to the database
- Manage conversation lifecycle (create, list, retrieve)
- Track conversation participants and roles
- Calculate unread message counts using read-state strategy
- Publish MessageDelivered events to EventBridge
- Provide REST API for conversation and message queries
- Enforce business rules and invariants

---

## Domain Model

### Entities

#### Conversation (Aggregate Root)

```java
@Entity
@Table(name = "conversations", schema = "communications_schema")
public class Conversation {
    @Id
    private String id;  // UUID
    
    @Enumerated(EnumType.STRING)
    private ConversationType type;  // DIRECT, GROUP
    
    private String name;  // Optional, for group chats
    
    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL)
    private List<Participant> participants;
    
    @OneToMany(mappedBy = "conversation")
    private List<Message> messages;
    
    private Instant createdAt;
    private Instant updatedAt;
    
    // Business methods
    public void addParticipant(String userId, ParticipantRole role);
    public void removeParticipant(String userId);
    public boolean isParticipant(String userId);
    public void validateInvariants();  // At least 2 participants, unique participants
}
```

#### Message

```java
@Entity
@Table(name = "messages", schema = "communications_schema")
public class Message {
    @Id
    private String id;  // UUID
    
    @ManyToOne
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;
    
    private String senderId;  // User ID from identity service
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    private Instant createdAt;
    
    // Future: attachments, reactions, edited flag
}
```

#### Participant

```java
@Entity
@Table(name = "participants", schema = "communications_schema")
public class Participant {
    @Id
    private String id;  // UUID
    
    @ManyToOne
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;
    
    private String userId;  // User ID from identity service
    
    @Enumerated(EnumType.STRING)
    private ParticipantRole role;  // MEMBER, ADMIN
    
    private Instant joinedAt;
}
```

#### ReadState

```java
@Entity
@Table(name = "read_state", schema = "communications_schema")
public class ReadState {
    @Id
    private String id;  // UUID
    
    private String userId;
    private String conversationId;
    private String lastReadMessageId;  // Last message the user has read
    
    private Instant updatedAt;
    
    @UniqueConstraint(columnNames = {"user_id", "conversation_id"})
}
```

### Value Objects

#### ConversationType

```java
public enum ConversationType {
    DIRECT,   // 1-on-1 conversation
    GROUP     // Multi-user conversation
}
```

#### ParticipantRole

```java
public enum ParticipantRole {
    MEMBER,   // Regular participant
    ADMIN     // Can add/remove participants, manage conversation
}
```

### Domain Events

#### MessageDeliveredEvent

```java
public class MessageDeliveredEvent extends DomainEvent {
    private String messageId;
    private String conversationId;
    private String senderId;
    private ConversationType conversationType;
    private List<String> recipientIds;
    private Instant deliveredAt;
}
```

---

## Clean Architecture Layers

### Domain Layer (`domain/`)

**Entities**:
- `Conversation.java` - Aggregate root
- `Message.java`
- `Participant.java`
- `ReadState.java`

**Value Objects**:
- `ConversationType.java`
- `ParticipantRole.java`

**Domain Events**:
- `MessageDeliveredEvent.java`

**Repository Interfaces**:
- `ConversationRepository.java`
- `MessageRepository.java`
- `ParticipantRepository.java`
- `ReadStateRepository.java`

**Exceptions**:
- `ConversationNotFoundException.java`
- `UnauthorizedParticipantException.java`
- `InvalidConversationStateException.java`

---

### Application Layer (`application/`)

**Services**:

```java
@Service
public class ConversationService {
    public ConversationDTO createConversation(CreateConversationRequest request);
    public List<ConversationDTO> getUserConversations(String userId);
    public ConversationDTO getConversation(String conversationId, String userId);
    public void addParticipant(String conversationId, String userId, String newParticipantId);
    public void removeParticipant(String conversationId, String userId, String participantId);
}

@Service
public class MessageService {
    public void processMessage(MessageCreatedDTO messageDto);  // Called by SQS consumer
    public PaginatedMessages getMessages(String conversationId, String userId, Pageable pageable);
}

@Service
public class UnreadCountService {
    public int getUnreadCount(String userId, String conversationId);
    public Map<String, Integer> getAllUnreadCounts(String userId);
    public void markAsRead(String userId, String conversationId, String lastMessageId);
}
```

**DTOs**:
- `ConversationDTO.java`
- `MessageDTO.java`
- `CreateConversationRequest.java`
- `MessageCreatedDTO.java` (from SQS)
- `PaginatedMessages.java`
- `UnreadCountResponse.java`

---

### Infrastructure Layer (`infrastructure/`)

**Persistence**:

```java
@Repository
public interface ConversationRepository extends JpaRepository<Conversation, String> {
    @Query("SELECT c FROM Conversation c JOIN c.participants p WHERE p.userId = :userId")
    List<Conversation> findByUserId(@Param("userId") String userId);
}

@Repository
public interface MessageRepository extends JpaRepository<Message, String> {
    Page<Message> findByConversationIdOrderByCreatedAtDesc(String conversationId, Pageable pageable);
    
    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversation.id = :conversationId " +
           "AND m.createdAt > (SELECT rs.lastReadMessageId FROM ReadState rs " +
           "WHERE rs.userId = :userId AND rs.conversationId = :conversationId)")
    int countUnreadMessages(@Param("conversationId") String conversationId, 
                           @Param("userId") String userId);
}

@Repository
public interface ReadStateRepository extends JpaRepository<ReadState, String> {
    Optional<ReadState> findByUserIdAndConversationId(String userId, String conversationId);
    List<ReadState> findByUserId(String userId);
}
```

**Messaging**:

```java
@Component
public class SqsMessageConsumer {
    
    @SqsListener(value = "${aws.sqs.direct-messages-queue}")
    public void consumeDirectMessage(MessageCreatedDTO message) {
        messageService.processMessage(message);
    }
    
    @SqsListener(value = "${aws.sqs.group-messages-queue}")
    public void consumeGroupMessage(MessageCreatedDTO message) {
        messageService.processMessage(message);
    }
}

@Component
public class EventBridgePublisher {
    
    public void publishMessageDelivered(MessageDeliveredEvent event) {
        PutEventsRequest request = PutEventsRequest.builder()
            .entries(PutEventsRequestEntry.builder()
                .source("communications-service")
                .detailType("MessageDelivered")
                .detail(objectMapper.writeValueAsString(event))
                .eventBusName(eventBusName)
                .build())
            .build();
        
        eventBridgeClient.putEvents(request);
    }
}
```

**Configuration**:

```java
@Configuration
public class SqsConfig {
    @Value("${aws.sqs.direct-messages-queue}")
    private String directMessagesQueue;
    
    @Value("${aws.sqs.group-messages-queue}")
    private String groupMessagesQueue;
    
    @Bean
    public SqsAsyncClient sqsAsyncClient() {
        return SqsAsyncClient.builder()
            .region(Region.US_EAST_1)
            .build();
    }
}

@Configuration
public class EventBridgeConfig {
    @Bean
    public EventBridgeClient eventBridgeClient() {
        return EventBridgeClient.builder()
            .region(Region.US_EAST_1)
            .build();
    }
}
```

---

### Interface Layer (`interfaces/`)

**REST Controllers**:

```java
@RestController
@RequestMapping("/conversations")
public class ConversationController {
    
    @GetMapping
    public ResponseEntity<List<ConversationDTO>> getUserConversations(
        @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(conversationService.getUserConversations(userId));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ConversationDTO> getConversation(
        @PathVariable String id,
        @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(conversationService.getConversation(id, userId));
    }
    
    @PostMapping
    public ResponseEntity<ConversationDTO> createConversation(
        @RequestBody CreateConversationRequest request,
        @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(conversationService.createConversation(request));
    }
}

@RestController
@RequestMapping("/conversations/{conversationId}/messages")
public class MessageController {
    
    @GetMapping
    public ResponseEntity<PaginatedMessages> getMessages(
        @PathVariable String conversationId,
        @RequestHeader("X-User-Id") String userId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(messageService.getMessages(conversationId, userId, pageable));
    }
    
    @PostMapping("/read")
    public ResponseEntity<Void> markAsRead(
        @PathVariable String conversationId,
        @RequestHeader("X-User-Id") String userId,
        @RequestBody MarkAsReadRequest request) {
        
        unreadCountService.markAsRead(userId, conversationId, request.getLastMessageId());
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/unread-count")
    public ResponseEntity<Integer> getUnreadCount(
        @PathVariable String conversationId,
        @RequestHeader("X-User-Id") String userId) {
        
        return ResponseEntity.ok(unreadCountService.getUnreadCount(userId, conversationId));
    }
}
```

---

## Database Schema

### Tables

**conversations**:
```sql
CREATE TABLE communications_schema.conversations (
    id VARCHAR(36) PRIMARY KEY,
    type VARCHAR(20) NOT NULL,  -- DIRECT, GROUP
    name VARCHAR(200),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_conversations_type ON communications_schema.conversations(type);
CREATE INDEX idx_conversations_updated_at ON communications_schema.conversations(updated_at);
```

**messages**:
```sql
CREATE TABLE communications_schema.messages (
    id VARCHAR(36) PRIMARY KEY,
    conversation_id VARCHAR(36) NOT NULL,
    sender_id VARCHAR(36) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (conversation_id) REFERENCES communications_schema.conversations(id) ON DELETE CASCADE
);

CREATE INDEX idx_messages_conversation_id ON communications_schema.messages(conversation_id);
CREATE INDEX idx_messages_created_at ON communications_schema.messages(created_at);
CREATE INDEX idx_messages_sender_id ON communications_schema.messages(sender_id);
```

**participants**:
```sql
CREATE TABLE communications_schema.participants (
    id VARCHAR(36) PRIMARY KEY,
    conversation_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    role VARCHAR(20) NOT NULL,  -- MEMBER, ADMIN
    joined_at TIMESTAMP NOT NULL,
    FOREIGN KEY (conversation_id) REFERENCES communications_schema.conversations(id) ON DELETE CASCADE,
    UNIQUE(conversation_id, user_id)
);

CREATE INDEX idx_participants_conversation_id ON communications_schema.participants(conversation_id);
CREATE INDEX idx_participants_user_id ON communications_schema.participants(user_id);
```

**read_state**:
```sql
CREATE TABLE communications_schema.read_state (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    conversation_id VARCHAR(36) NOT NULL,
    last_read_message_id VARCHAR(36),
    updated_at TIMESTAMP NOT NULL,
    UNIQUE(user_id, conversation_id)
);

CREATE INDEX idx_read_state_user_id ON communications_schema.read_state(user_id);
CREATE INDEX idx_read_state_conversation_id ON communications_schema.read_state(conversation_id);
```

---

## Business Rules

1. **Conversation Invariants**:
   - Must have at least 2 participants
   - DIRECT conversations must have exactly 2 participants
   - GROUP conversations can have 2+ participants
   - Participant user IDs must be unique within a conversation

2. **Message Rules**:
   - Sender must be a participant in the conversation
   - Content cannot be empty
   - Messages are immutable once created (future: support editing)

3. **Read State**:
   - One read state entry per user/conversation pair
   - last_read_message_id must reference a valid message in the conversation
   - Unread count = messages created after last_read_message_id

4. **Authorization**:
   - Only participants can view conversation details
   - Only participants can view messages
   - Only admins can add/remove participants in GROUP conversations
   - Participants can leave conversations voluntarily

---

## Configuration

**application.yml**:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/turaf
    username: communications_user
    password: ${COMMUNICATIONS_USER_PASSWORD}
  jpa:
    properties:
      hibernate:
        default_schema: communications_schema

aws:
  region: us-east-1
  sqs:
    direct-messages-queue: communications-direct-messages.fifo
    group-messages-queue: communications-group-messages.fifo
  eventbridge:
    bus-name: turaf-event-bus
```

---

## Dependencies (pom.xml)

```xml
<dependencies>
    <!-- Spring Boot -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    
    <!-- Database -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
    </dependency>
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-core</artifactId>
    </dependency>
    
    <!-- AWS -->
    <dependency>
        <groupId>io.awspring.cloud</groupId>
        <artifactId>spring-cloud-aws-messaging</artifactId>
    </dependency>
    <dependency>
        <groupId>software.amazon.awssdk</groupId>
        <artifactId>eventbridge</artifactId>
    </dependency>
    
    <!-- Utilities -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
    </dependency>
    <dependency>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct</artifactId>
    </dependency>
</dependencies>
```

---

## Testing Strategy

**Unit Tests**:
- Domain entity business logic
- Service layer use cases
- DTO mappings

**Integration Tests**:
- Repository queries
- SQS message consumption
- EventBridge event publishing

**API Tests**:
- REST endpoint responses
- Authorization checks
- Pagination

---

## References

- **PROJECT.md**: Sections 5, 6, 11, 27
- **Domain Model**: `specs/domain-model.md`
- **Event Schemas**: `specs/communications-event-schemas.md`
- **Architecture**: `specs/architecture.md`
