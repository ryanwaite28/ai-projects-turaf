# Changelog: Add Communications Component

**Date**: March 21, 2026  
**Type**: Feature Addition  
**Impact**: Architecture Extension

---

## Summary

Added a new **Communications Component** to the Turaf platform to support real-time messaging capabilities, including direct messaging, group chats, unread indicators, and typing indicators. This feature enables team collaboration within the platform.

---

## Changes to PROJECT.md

### 1. Core Features (Section 5)

**Added**: New "Communications" feature section

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

**Rationale**: Communications is a core collaboration feature that complements the existing problem-solving and experimentation workflows.

---

### 2. Domain Model (Section 6)

**Added**: Four new core entities

- `Conversation` - Aggregate root for chat conversations
- `Message` - Individual messages within conversations
- `Participant` - Users participating in conversations
- `ReadState` - Tracking read/unread message state per user

**Rationale**: These entities follow DDD principles and maintain clear bounded contexts separate from other domains.

---

### 3. Domain Events (Section 7)

**Added**: New domain event

- `MessageDelivered` - Published when a message is successfully persisted

**Rationale**: Maintains event-driven architecture consistency. Allows other services to react to messaging events (e.g., notifications).

---

### 4. Service Boundaries (Section 11)

**Added**: Two new services

- `Communications Service` (Spring Boot) - Core business logic and persistence
- `WebSocket Gateway` (NestJS) - Real-time WebSocket connections

**Rationale**: 
- **Communications Service**: Follows existing Spring Boot microservice pattern, handles persistence and business logic
- **WebSocket Gateway**: NestJS is better suited for WebSocket handling with built-in support and horizontal scaling via Redis

---

### 5. Database Strategy (Section 27)

**Added**: New schema and database user

Schema:
- `communications_schema` - Conversations, messages, participants, read state

Database User:
- `communications_user` → Full access to `communications_schema` only

**Rationale**: Maintains schema isolation principle. No cross-schema foreign keys. Service owns its data completely.

---

### 6. Internal ALB Routing (Section 10a)

**Added**: Two new path-based routing rules

- `/communications/*` → Communications Service target group
- `/ws/*` → WebSocket Gateway target group (with WebSocket upgrade support)

**Rationale**: Follows existing ALB routing pattern. WebSocket path requires upgrade support for persistent connections.

---

## Architectural Decisions

### Hybrid Technology Stack (Spring Boot + NestJS)

**Decision**: Use NestJS for WebSocket Gateway instead of Spring Boot

**Rationale**:
- NestJS has superior WebSocket support with `@nestjs/websockets` and `socket.io`
- Built-in Redis adapter for horizontal scaling
- TypeScript provides better type safety for real-time event handling
- Smaller footprint for stateless gateway service
- Spring Boot handles complex business logic and persistence

### Message Ordering with SQS FIFO

**Decision**: Use two SQS FIFO queues for message delivery

**Rationale**:
- FIFO queues guarantee message ordering per conversation
- Separate queues for direct vs group messages allows independent scaling
- Decouples WebSocket gateway from database writes
- Provides resilience and retry capabilities
- MessageGroupId = conversation_id ensures ordering

### Redis for Horizontal Scaling

**Decision**: Use Redis Pub/Sub as backplane for WebSocket gateway instances

**Rationale**:
- Enables multiple gateway instances to share real-time events
- Configuration-based adapter (local container vs ElastiCache)
- Low latency for typing indicators and presence
- Proven pattern for scaling WebSocket servers

### Read-State Strategy

**Decision**: Track `last_read_message_id` per user/conversation instead of per-message read receipts

**Rationale**:
- Simpler data model and queries
- Efficient unread count calculation
- Scales better than marking each message as read
- Sufficient for most use cases

---

## Impact Assessment

### New Infrastructure Components

- **Redis** (ElastiCache in AWS, container locally)
- **2 SQS FIFO Queues** (communications-direct-messages.fifo, communications-group-messages.fifo)
- **ECS Fargate Tasks** (2 new services)
- **Internal ALB Listener Rules** (2 new paths)

### Database Changes

- New schema: `communications_schema`
- New user: `communications_user`
- 4 new tables (via Flyway migrations)

### Service Dependencies

- **Communications Service** depends on:
  - PostgreSQL (communications_schema)
  - SQS (consumer)
  - EventBridge (publisher)

- **WebSocket Gateway** depends on:
  - Redis (Pub/Sub)
  - SQS (publisher)
  - Identity Service (JWT validation)

### Frontend Changes

- New feature module: `communications`
- WebSocket client integration
- NgRx store for chat state
- 4 new UI components

---

## Migration Path

1. **Phase 1**: Update PROJECT.md (✅ Complete)
2. **Phase 2**: Generate specifications
3. **Phase 3**: Generate task lists
4. **Phase 4**: Implement database migrations
5. **Phase 5**: Implement Communications Service
6. **Phase 6**: Implement WebSocket Gateway
7. **Phase 7**: Update infrastructure (Docker Compose, Terraform)
8. **Phase 8**: Implement frontend module

---

## Compatibility

- **Backward Compatible**: Yes
- **Breaking Changes**: None
- **Existing Services Affected**: None (new bounded context)

---

## Testing Strategy

- Unit tests for domain logic
- Integration tests for SQS consumers
- WebSocket integration tests
- E2E tests for chat flows
- Load testing for horizontal scaling

---

## Documentation Updates Required

- [ ] Generate `specs/communications-service.md`
- [ ] Generate `specs/ws-gateway.md`
- [ ] Generate `specs/communications-domain-model.md`
- [ ] Generate `specs/communications-event-schemas.md`
- [ ] Update `specs/README.md`
- [ ] Update `specs/event-schemas.md`
- [ ] Generate task files for both services
- [ ] Update `tasks/README.md`

---

## References

- **Workflow**: `.windsurf/workflows/project.md`
- **Implementation Plan**: `.windsurf/plans/communications-component-implementation-aa99f1.md`
- **Architecture Pattern**: Follows existing microservice patterns from experiment-service and identity-service

---

## Approval

**Status**: Approved  
**Reviewed By**: System Architect  
**Date**: March 21, 2026
