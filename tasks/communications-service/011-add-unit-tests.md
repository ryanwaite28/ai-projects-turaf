# Task: Add Unit Tests

**Service**: Communications Service  
**Type**: Testing  
**Priority**: High  
**Estimated Time**: 3 hours  
**Dependencies**: 002-009 (all implementation tasks)

---

## Objective

Add comprehensive unit tests for domain logic, services, and mappers with >80% code coverage.

---

## Acceptance Criteria

- [x] Domain model tests (Conversation, Message, ReadState)
- [x] Service layer tests (ConversationService, MessageService, UnreadCountService)
- [x] Mapper tests
- [x] Test coverage >80%
- [x] All tests pass

---

## Test Structure

```
src/test/java/com/turaf/communications/
├── domain/
│   ├── model/
│   │   ├── ConversationTest.java
│   │   ├── MessageTest.java
│   │   └── ReadStateTest.java
├── application/
│   └── service/
│       ├── ConversationServiceTest.java
│       ├── MessageServiceTest.java
│       └── UnreadCountServiceTest.java
└── interfaces/
    └── mapper/
        ├── ConversationMapperTest.java
        └── MessageMapperTest.java
```

---

## Key Test Cases

### Domain Tests
- Conversation creation (DIRECT/GROUP)
- Participant management
- Invariant validation
- Message validation

### Service Tests
- Conversation CRUD operations
- Message processing
- Unread count calculations
- Authorization checks

---

## References

- **Testing Strategy**: `specs/communications-service.md`
- **Example**: `services/experiment-service/src/test/`
