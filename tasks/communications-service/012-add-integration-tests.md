# Task: Add Integration Tests

**Service**: Communications Service  
**Type**: Testing  
**Priority**: Medium  
**Estimated Time**: 3 hours  
**Dependencies**: 011-add-unit-tests

---

## Objective

Add integration tests for repository queries, SQS consumers, EventBridge publishers, and REST API endpoints.

---

## Acceptance Criteria

- [x] Repository integration tests with H2
- [x] SQS consumer tests with test containers
- [x] EventBridge publisher tests
- [x] REST API integration tests
- [x] All integration tests pass (require LocalStack for AWS services)

---

## Test Structure

```
src/test/java/com/turaf/communications/
├── infrastructure/
│   ├── persistence/
│   │   ├── ConversationRepositoryIntegrationTest.java
│   │   └── MessageRepositoryIntegrationTest.java
│   └── messaging/
│       ├── SqsMessageConsumerIntegrationTest.java
│       └── EventBridgePublisherIntegrationTest.java
└── interfaces/
    └── rest/
        ├── ConversationControllerIntegrationTest.java
        └── MessageControllerIntegrationTest.java
```

---

## Key Test Cases

### Repository Tests
- Complex queries (findByUserId, findDirectConversation)
- Unread count queries
- Pagination

### Messaging Tests
- SQS message consumption
- EventBridge event publishing
- Error handling

### API Tests
- Full request/response cycles
- Authentication/authorization
- Error responses

---

## Testing Strategy

**Follow the hybrid AWS approach** (PROJECT.md Section 23a, specs/testing-strategy.md):

- **Use Testcontainers + LocalStack** for:
  - PostgreSQL database integration tests
  - SQS message queue tests (free tier)
  
- **Use @MockBean** for:
  - EventBridge publisher (not in LocalStack free tier)
  - CloudWatch metrics (limited in free tier)

**Example Configuration**:
```java
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class CommunicationsIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");
    
    @Container
    static LocalStackContainer localstack = new LocalStackContainer()
        .withServices(LocalStackContainer.Service.SQS);
    
    @MockBean
    private EventBridgeClient eventBridgeClient;
}
```

---

## References

- **Testing Strategy**: `specs/testing-strategy.md` (comprehensive guide)
- **PROJECT.md**: Section 23a - Testing Strategy
- **CI/CD Pipeline**: `specs/ci-cd-pipelines.md` (integration test stage)
- **Communications Service**: `specs/communications-service.md`
