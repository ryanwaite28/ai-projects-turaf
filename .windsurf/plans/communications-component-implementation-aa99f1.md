# Communications Component Implementation Plan

Add real-time messaging capabilities to the Turaf platform with direct messaging, group chats, unread indicators, and WebSocket-based real-time updates using a hybrid NestJS/Spring Boot architecture, following the established workflow for PROJECT.md updates, specs generation, and task creation.

---

## Pre-Implementation: Documentation & Planning

### Step 0.1: Update PROJECT.md

**Objective:** Document the Communications component in the authoritative system design.

**Files to Modify:**
- `PROJECT.md`

**Changes Required:**

1. **Add to Core Features (Section 5):**
   ```markdown
   ## Communications
   
   Real-time messaging for team collaboration.
   
   Features:
   - Direct messaging (1-on-1 conversations)
   - Group chat conversations
   - Unread message indicators
   - Typing indicators
   - Message history and persistence
   ```

2. **Add to Domain Model (Section 6):**
   ```markdown
   Conversation
   Message
   Participant
   ReadState
   ```

3. **Add to Service Boundaries (Section 11):**
   ```markdown
   Communications Service (Spring Boot)
   WebSocket Gateway (NestJS)
   ```

4. **Update System Architecture Diagram (Section with Mermaid):**
   - Add `CommunicationsService` node
   - Add `WSGateway` node
   - Add `Redis` for Pub/Sub
   - Add `SQS FIFO Queues` for message ordering

5. **Add to Database Strategy (Multi-Schema section):**
   ```markdown
   - **communications_schema**: Conversations, messages, participants, read state
   ```

6. **Add to Database Users:**
   ```markdown
   - **communications_user** → Full access to `communications_schema` only
   ```

7. **Update Internal ALB Routing:**
   ```markdown
   - `/communications/*` → Communications Service target group
   - `/ws/*` → WebSocket Gateway target group (WebSocket upgrade support)
   ```

**Changelog Entry:**
- Create `changelog/2026-03-21-add-communications-component.md`
- Document all changes made to PROJECT.md
- Include rationale for architectural decisions

---

### Step 0.2: Generate Communications Specifications

**Objective:** Create detailed specifications for the Communications component following the established pattern.

**Files to Create:**

1. **`specs/communications-service.md`**
   - Service overview and responsibilities
   - Domain model (Conversation, Message, Participant, ReadState)
   - API endpoints (REST)
   - Database schema (communications_schema tables)
   - SQS consumer configuration
   - EventBridge event publishing
   - Read-state strategy for unread counts
   - Clean Architecture layers

2. **`specs/ws-gateway.md`**
   - WebSocket gateway overview
   - NestJS architecture
   - JWT authentication strategy
   - Redis Pub/Sub for horizontal scaling
   - SQS FIFO queue publishing
   - WebSocket events (send_message, typing indicators, join/leave)
   - Configuration-based Redis adapter
   - Connection management

3. **`specs/communications-domain-model.md`**
   - Conversation aggregate (root)
   - Message entity
   - Participant entity
   - ReadState entity
   - ConversationType value object (DIRECT, GROUP)
   - ParticipantRole value object (MEMBER, ADMIN)
   - Domain events (MessageDeliveredEvent)
   - Business rules and invariants

4. **`specs/communications-event-schemas.md`**
   - MessageDeliveredEvent schema
   - Event envelope structure
   - Integration with existing EventBridge bus
   - Event versioning

**Update Existing Files:**
- `specs/README.md` - Add communications specs to the list
- `specs/event-schemas.md` - Add MessageDeliveredEvent

---

### Step 0.3: Generate Communications Tasks

**Objective:** Break down the Communications component into granular, actionable tasks.

**Directory to Create:**
- `tasks/communications-service/` (10-12 tasks)
- `tasks/ws-gateway/` (6-8 tasks)

**Task Files to Create:**

**Communications Service Tasks:**
1. `001-setup-project-structure.md` - Maven project, pom.xml, directory structure
2. `002-create-domain-model.md` - Entities, value objects, aggregates
3. `003-create-repositories.md` - JPA repositories with custom queries
4. `004-implement-conversation-service.md` - Business logic for conversations
5. `005-implement-message-service.md` - Message processing and persistence
6. `006-implement-unread-count-service.md` - Read-state tracking
7. `007-implement-sqs-consumers.md` - Listen to both FIFO queues
8. `008-implement-eventbridge-publisher.md` - Publish MessageDelivered events
9. `009-implement-rest-controllers.md` - REST API endpoints
10. `010-create-database-migrations.md` - Flyway migration scripts
11. `011-add-unit-tests.md` - Domain and application layer tests
12. `012-add-integration-tests.md` - Repository and API tests

**WebSocket Gateway Tasks:**
1. `001-setup-nestjs-project.md` - Initialize NestJS with WebSocket support
2. `002-implement-jwt-authentication.md` - Auth guard and JWT strategy
3. `003-implement-redis-adapter.md` - Configuration-based Redis IoAdapter
4. `004-implement-chat-gateway.md` - WebSocket gateway for messages
5. `005-implement-typing-gateway.md` - Typing indicator handling
6. `006-implement-sqs-publisher.md` - Publish to FIFO queues
7. `007-add-unit-tests.md` - Service and gateway tests
8. `008-add-e2e-tests.md` - WebSocket integration tests

**Infrastructure Tasks:**
- `tasks/infrastructure/014-add-redis-infrastructure.md` - ElastiCache setup
- `tasks/infrastructure/015-add-sqs-fifo-queues.md` - Queue configuration
- `tasks/infrastructure/016-update-alb-routing.md` - Add communications paths

**Frontend Tasks:**
- `tasks/frontend/015-implement-communications-module.md` - Full chat UI

**Update Existing Files:**
- `tasks/README.md` - Add communications tasks to organization and count

---

## Architecture Overview

### New Services
1. **ws-gateway** (NestJS) - WebSocket gateway for real-time connections
2. **communications-service** (Spring Boot) - Core business logic and persistence
3. **Redis** (ElastiCache/Container) - Pub/Sub backplane for horizontal scaling

### Integration Points
- **SQS FIFO Queues**: Two queues for ordered message delivery
  - `communications-direct-messages.fifo` - Direct/1-on-1 messages
  - `communications-group-messages.fifo` - Group chat messages
- **EventBridge**: Emit `MessageDelivered` events to existing event bus
- **PostgreSQL**: New `communications_schema` in existing database
- **Internal ALB**: Add routing rules for `/communications/*` and `/ws/*`

---

## Implementation Steps

### Step 1: Database Schema & Migrations (PostgreSQL)

**Files to Create:**
- `infrastructure/docker/postgres/init-db.sh` - Add communications_schema and communications_user
- `services/communications-service/src/main/resources/db/migration/V001__create_conversations_table.sql`
- `services/communications-service/src/main/resources/db/migration/V002__create_messages_table.sql`
- `services/communications-service/src/main/resources/db/migration/V003__create_participants_table.sql`
- `services/communications-service/src/main/resources/db/migration/V004__create_read_state_table.sql`

**Schema Design:**
```sql
-- conversations: id, type (DIRECT/GROUP), name, created_at, updated_at
-- messages: id, conversation_id, sender_id, content, created_at
-- participants: id, conversation_id, user_id, role, joined_at
-- read_state: user_id, conversation_id, last_read_message_id, updated_at
```

**Tasks:**
- [ ] Update `init-db.sh` to create `communications_schema` and `communications_user`
- [ ] Create Flyway migration scripts for all tables
- [ ] Add indexes for performance (conversation_id, sender_id, created_at)
- [ ] Update docker-compose.yml with COMMUNICATIONS_USER_PASSWORD env var

---

### Step 2: Communications Service (Spring Boot)

**Directory Structure:**
```
services/communications-service/
├── pom.xml
├── src/main/java/com/turaf/communications/
│   ├── domain/
│   │   ├── entities/
│   │   │   ├── Conversation.java
│   │   │   ├── Message.java
│   │   │   ├── Participant.java
│   │   │   └── ReadState.java
│   │   ├── events/
│   │   │   └── MessageDeliveredEvent.java
│   │   └── valueobjects/
│   │       └── ConversationType.java
│   ├── application/
│   │   ├── services/
│   │   │   ├── ConversationService.java
│   │   │   ├── MessageService.java
│   │   │   └── UnreadCountService.java
│   │   └── dto/
│   │       ├── CreateMessageRequest.java
│   │       └── MessageResponse.java
│   ├── infrastructure/
│   │   ├── persistence/
│   │   │   ├── ConversationRepository.java
│   │   │   ├── MessageRepository.java
│   │   │   ├── ParticipantRepository.java
│   │   │   └── ReadStateRepository.java
│   │   ├── messaging/
│   │   │   ├── SqsMessageConsumer.java
│   │   │   └── EventBridgePublisher.java
│   │   └── config/
│   │       ├── SqsConfig.java
│   │       └── EventBridgeConfig.java
│   └── interfaces/
│       └── rest/
│           ├── ConversationController.java
│           └── MessageController.java
└── src/main/resources/
    ├── application.yml
    ├── application-docker.yml
    └── db/migration/
```

**Key Components:**

**Domain Layer:**
- `Conversation` entity with DDD aggregate root pattern
- `Message` entity with sender, content, timestamp
- `Participant` entity for conversation membership
- `ReadState` entity tracking last_read_message_id per user/conversation
- `MessageDeliveredEvent` domain event

**Application Layer:**
- `MessageService.processMessage()` - Core business logic
- `UnreadCountService.calculateUnread()` - Compute unread counts
- `ConversationService.createConversation()` - Create direct/group chats

**Infrastructure Layer:**
- `SqsMessageConsumer` - Listen to both FIFO queues
  - `@SqsListener(value = "communications-direct-messages.fifo")`
  - `@SqsListener(value = "communications-group-messages.fifo")`
- `EventBridgePublisher` - Publish MessageDelivered events
- JPA repositories with custom queries for unread counts

**REST API Endpoints:**
- `GET /conversations` - List user's conversations
- `GET /conversations/{id}` - Get conversation details
- `POST /conversations` - Create new conversation
- `GET /conversations/{id}/messages` - Get message history (paginated)
- `POST /conversations/{id}/read` - Mark messages as read
- `GET /conversations/{id}/unread-count` - Get unread count

**Tasks:**
- [ ] Create Maven pom.xml with Spring Boot, JPA, SQS, EventBridge dependencies
- [ ] Implement domain entities following DDD patterns
- [ ] Create application services with business logic
- [ ] Implement SQS consumers for both queues
- [ ] Create EventBridge publisher for MessageDelivered events
- [ ] Build REST controllers for conversation management
- [ ] Add Flyway configuration
- [ ] Create application.yml with database and AWS config
- [ ] Add to parent pom.xml

---

### Step 3: WebSocket Gateway (NestJS)

**Directory Structure:**
```
services/ws-gateway/
├── package.json
├── tsconfig.json
├── nest-cli.json
├── src/
│   ├── main.ts
│   ├── app.module.ts
│   ├── config/
│   │   ├── redis.config.ts
│   │   └── sqs.config.ts
│   ├── auth/
│   │   ├── auth.guard.ts
│   │   └── jwt.strategy.ts
│   ├── gateways/
│   │   ├── chat.gateway.ts
│   │   └── typing.gateway.ts
│   ├── services/
│   │   ├── redis-pub-sub.service.ts
│   │   ├── sqs-publisher.service.ts
│   │   └── identity-client.service.ts
│   └── dto/
│       ├── message.dto.ts
│       └── typing-indicator.dto.ts
└── Dockerfile
```

**Key Components:**

**WebSocket Gateway:**
- `@WebSocketGateway()` with JWT authentication
- Handle client connections via `@SubscribeMessage('join_conversation')`
- Emit events: `message_received`, `typing_started`, `typing_stopped`
- Connection validation using Identity Service JWT logic

**Redis Adapter:**
- Use `@nestjs/platform-socket.io` with Redis adapter
- Configuration-based switching:
  - Local: Redis container (`redis://localhost:6379`)
  - AWS: ElastiCache endpoint from environment variable
- Pub/Sub for cross-instance message broadcasting

**SQS Publisher:**
- Publish to appropriate FIFO queue based on conversation type
- Include MessageGroupId for ordering (conversation_id)
- Include MessageDeduplicationId for idempotency

**Authentication:**
- Extract JWT from WebSocket handshake
- Validate using existing Identity Service logic
- Store user context in socket connection

**Events Handled:**
- `send_message` → Validate → Publish to SQS → Broadcast via Redis
- `start_typing` → Broadcast typing indicator via Redis
- `stop_typing` → Broadcast typing stopped via Redis
- `join_conversation` → Subscribe to conversation room
- `leave_conversation` → Unsubscribe from conversation room

**Tasks:**
- [ ] Initialize NestJS project with WebSocket support
- [ ] Install dependencies: @nestjs/websockets, @nestjs/platform-socket.io, socket.io-redis, aws-sdk
- [ ] Create Redis IoAdapter with environment-based configuration
- [ ] Implement JWT authentication guard for WebSocket connections
- [ ] Build ChatGateway for message handling
- [ ] Build TypingGateway for typing indicators
- [ ] Create SQS publisher service
- [ ] Add Redis Pub/Sub service for cross-instance communication
- [ ] Create Dockerfile for containerization
- [ ] Add health check endpoint

---

### Step 4: Infrastructure Updates

**Docker Compose (Local Development):**

Add to `docker-compose.yml`:
```yaml
# Redis for WebSocket scaling
redis:
  image: redis:7-alpine
  container_name: turaf-redis
  ports:
    - "6379:6379"
  networks:
    - turaf-network
  healthcheck:
    test: ["CMD", "redis-cli", "ping"]

# WebSocket Gateway
ws-gateway:
  build:
    context: ./services/ws-gateway
    dockerfile: Dockerfile
  container_name: turaf-ws-gateway
  environment:
    - NODE_ENV=docker
    - REDIS_URL=redis://redis:6379
    - JWT_SECRET=${JWT_SECRET}
    - SQS_DIRECT_QUEUE_URL=http://localstack:4566/000000000000/communications-direct-messages.fifo
    - SQS_GROUP_QUEUE_URL=http://localstack:4566/000000000000/communications-group-messages.fifo
  ports:
    - "3000:3000"
  depends_on:
    - redis
    - localstack

# Communications Service
communications-service:
  build:
    context: .
    dockerfile: infrastructure/docker/spring-boot/Dockerfile
    args:
      SERVICE_NAME: communications-service
  container_name: turaf-communications-service
  environment:
    - SPRING_PROFILES_ACTIVE=docker
    - DB_NAME=${DB_NAME:-turaf}
    - DB_USERNAME=communications_user
    - COMMUNICATIONS_USER_PASSWORD=${COMMUNICATIONS_USER_PASSWORD}
  ports:
    - "8085:8085"
```

**LocalStack Initialization:**

Update `infrastructure/docker/localstack/init-aws.sh`:
```bash
# Create SQS FIFO queues
awslocal sqs create-queue \
  --queue-name communications-direct-messages.fifo \
  --attributes FifoQueue=true,ContentBasedDeduplication=true

awslocal sqs create-queue \
  --queue-name communications-group-messages.fifo \
  --attributes FifoQueue=true,ContentBasedDeduplication=true
```

**Internal ALB Routing:**

Add path-based routing rules:
- `/communications/*` → Communications Service target group
- `/ws/*` → WebSocket Gateway target group (with WebSocket upgrade support)

**Tasks:**
- [ ] Add Redis service to docker-compose.yml
- [ ] Add ws-gateway service to docker-compose.yml
- [ ] Add communications-service to docker-compose.yml
- [ ] Update LocalStack init script for SQS FIFO queues
- [ ] Update .env.example with new environment variables
- [ ] Document Internal ALB routing updates (for Terraform later)

---

### Step 5: Frontend Chat Module (Angular)

**Directory Structure:**
```
frontend/src/app/features/communications/
├── communications.module.ts
├── communications-routing.module.ts
├── services/
│   ├── websocket.service.ts
│   ├── conversations.service.ts
│   └── messages.service.ts
├── store/
│   ├── communications.state.ts
│   ├── communications.actions.ts
│   ├── communications.reducer.ts
│   ├── communications.effects.ts
│   └── communications.selectors.ts
├── components/
│   ├── conversation-list/
│   │   ├── conversation-list.component.ts
│   │   ├── conversation-list.component.html
│   │   └── conversation-list.component.scss
│   ├── chat-window/
│   │   ├── chat-window.component.ts
│   │   ├── chat-window.component.html
│   │   └── chat-window.component.scss
│   ├── message-input/
│   │   ├── message-input.component.ts
│   │   ├── message-input.component.html
│   │   └── message-input.component.scss
│   └── typing-indicator/
│       ├── typing-indicator.component.ts
│       ├── typing-indicator.component.html
│       └── typing-indicator.component.scss
└── models/
    ├── conversation.model.ts
    ├── message.model.ts
    └── participant.model.ts
```

**Key Features:**

**WebSocket Service:**
- Connect to ws-gateway with JWT authentication
- Handle reconnection with exponential backoff
- Emit/listen to socket events
- Observable streams for messages and typing indicators

**NgRx Store:**
- State: conversations list, active conversation, messages, typing users, unread counts
- Actions: load conversations, send message, receive message, mark as read, typing events
- Effects: API calls, WebSocket event handling
- Selectors: filtered conversations, sorted messages, unread count per conversation

**UI Components:**

1. **ConversationListComponent:**
   - Display all conversations with last message preview
   - Show unread count badges
   - Click to open conversation
   - Search/filter conversations

2. **ChatWindowComponent:**
   - Display message history (virtual scrolling for performance)
   - Auto-scroll to bottom on new messages
   - Show sender name and timestamp
   - Group messages by date

3. **MessageInputComponent:**
   - Text input with send button
   - Emit typing indicators on input
   - Handle Enter key to send
   - Character count/limit

4. **TypingIndicatorComponent:**
   - Show "User is typing..." when active
   - Display multiple users typing
   - Auto-hide after timeout

**Routing:**
- `/communications` - Conversation list view
- `/communications/:id` - Chat window for specific conversation

**Tasks:**
- [ ] Create CommunicationsModule with lazy loading
- [ ] Implement WebSocketService with socket.io-client
- [ ] Create NgRx store (state, actions, reducer, effects, selectors)
- [ ] Build ConversationListComponent with Material UI
- [ ] Build ChatWindowComponent with virtual scrolling
- [ ] Build MessageInputComponent with typing detection
- [ ] Build TypingIndicatorComponent
- [ ] Create ConversationsService for REST API calls
- [ ] Add routing configuration
- [ ] Style components with SCSS (match existing design)
- [ ] Add to main app navigation menu

---

## Clean Architecture Alignment

### Domain-Driven Design (DDD)

**Aggregates:**
- `Conversation` (aggregate root)
  - Contains: Messages, Participants
  - Invariants: At least 2 participants, unique participants

**Value Objects:**
- `ConversationType` (DIRECT, GROUP)
- `ParticipantRole` (MEMBER, ADMIN)

**Domain Events:**
- `MessageDeliveredEvent` - Published to EventBridge after persistence

**Bounded Context:**
- Communications context is isolated from Experiment/Metrics contexts
- No cross-schema foreign keys
- Integration via events and REST APIs

### Clean Architecture Layers

**Domain Layer (Inner):**
- Pure business logic
- No framework dependencies
- Entities, value objects, domain events

**Application Layer:**
- Use cases and orchestration
- Service interfaces
- DTOs for data transfer

**Infrastructure Layer:**
- Database repositories (JPA)
- External integrations (SQS, EventBridge)
- Framework-specific code

**Interface Layer (Outer):**
- REST controllers
- WebSocket gateways
- Request/response models

---

## Testing Strategy

### Communications Service (Spring Boot)
- Unit tests for domain logic
- Integration tests for repositories
- SQS consumer tests with LocalStack
- REST API tests with MockMvc

### WebSocket Gateway (NestJS)
- Unit tests for services
- Integration tests for WebSocket events
- Mock Redis and SQS for testing
- E2E tests with socket.io-client

### Frontend (Angular)
- Unit tests for components (Jasmine/Karma)
- NgRx store tests (actions, reducers, effects, selectors)
- WebSocket service tests with mock socket
- E2E tests with Cypress for chat flows

---

## Deployment Considerations

### AWS Infrastructure (Future Terraform)
- ElastiCache Redis cluster (Multi-AZ)
- SQS FIFO queues in each environment
- ECS Fargate tasks for both services
- Internal ALB listener rules
- Security groups for service communication
- IAM roles for SQS/EventBridge access

### Environment Variables
- `REDIS_URL` - Redis/ElastiCache endpoint
- `SQS_DIRECT_QUEUE_URL` - Direct messages queue
- `SQS_GROUP_QUEUE_URL` - Group messages queue
- `EVENTBRIDGE_BUS_NAME` - Event bus name
- `JWT_SECRET` - Shared secret for validation
- `COMMUNICATIONS_USER_PASSWORD` - Database password

### Monitoring & Observability
- CloudWatch metrics for SQS queue depth
- WebSocket connection count metrics
- Message delivery latency tracking
- Redis connection pool monitoring
- Structured logging (JSON format)

---

## Success Criteria

- [ ] Users can send/receive direct messages in real-time
- [ ] Users can create and participate in group chats
- [ ] Unread message counts display correctly
- [ ] Typing indicators work across multiple users
- [ ] Messages persist to database via SQS
- [ ] MessageDelivered events publish to EventBridge
- [ ] WebSocket gateway scales horizontally with Redis
- [ ] All tests pass (unit, integration, E2E)
- [ ] Clean Architecture principles maintained
- [ ] DDD patterns followed in domain layer

---

## Migration Path

1. **Phase 1 (Local Development):**
   - Docker Compose with all services
   - LocalStack for AWS mocking
   - Redis container for Pub/Sub

2. **Phase 2 (AWS Deployment):**
   - Terraform for infrastructure
   - ElastiCache for Redis
   - Real SQS FIFO queues
   - ECS Fargate deployment

3. **Phase 3 (Production Hardening):**
   - Rate limiting on WebSocket connections
   - Message size limits
   - Conversation participant limits
   - Archive old messages strategy

---

## Dependencies

**Communications Service (Spring Boot):**
- spring-boot-starter-web
- spring-boot-starter-data-jpa
- spring-cloud-aws-messaging (SQS)
- spring-cloud-aws-autoconfigure (EventBridge)
- postgresql
- flyway-core
- lombok
- mapstruct

**WebSocket Gateway (NestJS):**
- @nestjs/websockets
- @nestjs/platform-socket.io
- socket.io-redis
- aws-sdk (SQS client)
- @nestjs/jwt
- @nestjs/passport
- passport-jwt

**Frontend (Angular):**
- socket.io-client
- @ngrx/store
- @ngrx/effects
- @angular/material
- rxjs

---

## Timeline Estimate

- Step 1 (Database): 2-3 hours
- Step 2 (Communications Service): 8-10 hours
- Step 3 (WebSocket Gateway): 6-8 hours
- Step 4 (Infrastructure): 2-3 hours
- Step 5 (Frontend): 10-12 hours
- Testing & Integration: 4-6 hours

**Total: ~32-42 hours**

---

## Notes

- Follow existing code patterns from experiment-service and identity-service
- Maintain schema isolation (no cross-schema joins)
- Use configuration-based adapter pattern for Redis (local vs AWS)
- Ensure WebSocket gateway is stateless for horizontal scaling
- Implement idempotency for message processing
- Consider message ordering guarantees with FIFO queues
- Plan for future features: file attachments, reactions, message editing
