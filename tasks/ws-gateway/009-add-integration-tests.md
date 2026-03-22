# Task: Add Integration Tests

**Service**: WebSocket Gateway  
**Phase**: 8  
**Estimated Time**: 3 hours  

## Objective

Create integration tests for the NestJS WebSocket Gateway that verify real-time messaging, Redis pub/sub, and SQS integration.

## Prerequisites

- [x] All ws-gateway implementation tasks completed
- [x] Task 008: E2E tests added

## Scope

**Test Files to Create**:
- `ChatGatewayIntegrationTest.ts`
- `TypingGatewayIntegrationTest.ts`
- `RedisPubSubIntegrationTest.ts`
- `SqsPublisherIntegrationTest.ts`

## Testing Strategy

**Follow the hybrid AWS approach** (PROJECT.md Section 23a, specs/testing-strategy.md):

- **Use Testcontainers** for:
  - Redis for pub/sub and adapter (free, not AWS)
  - WebSocket connection testing
  
- **Use Testcontainers + LocalStack** for:
  - SQS for message publishing (free tier)
  
- **No mocks needed** for:
  - WebSocket connections (test with real clients)
  - Redis operations (use Testcontainers)

**Example Configuration**:
```typescript
import { Test } from '@nestjs/testing';
import { GenericContainer } from 'testcontainers';
import { LocalstackContainer } from '@testcontainers/localstack';

describe('WebSocket Gateway Integration', () => {
  let redisContainer: GenericContainer;
  let localstackContainer: LocalstackContainer;
  
  beforeAll(async () => {
    // Start Redis container
    redisContainer = await new GenericContainer('redis:7-alpine')
      .withExposedPorts(6379)
      .start();
    
    // Start LocalStack for SQS
    localstackContainer = await new LocalstackContainer()
      .withServices('sqs')
      .start();
    
    const moduleRef = await Test.createTestingModule({
      imports: [AppModule],
    })
      .overrideProvider('REDIS_HOST')
      .useValue(redisContainer.getHost())
      .overrideProvider('REDIS_PORT')
      .useValue(redisContainer.getMappedPort(6379))
      .overrideProvider('AWS_SQS_ENDPOINT')
      .useValue(localstackContainer.getConnectionUri())
      .compile();
  });
});
```

---

## Implementation Details

Test complete flow:
1. Connect WebSocket clients
2. Authenticate with JWT
3. Join chat rooms (Redis pub/sub)
4. Send messages (broadcast via Redis)
5. Publish to SQS (LocalStack)
6. Test typing indicators
7. Test connection scaling (multiple clients)
8. Verify message delivery across instances

## Acceptance Criteria

- [x] WebSocket connections tested
- [x] JWT authentication verified
- [x] Redis pub/sub tested with Testcontainers
- [x] SQS publishing verified with LocalStack
- [x] Multi-client scenarios tested
- [x] Typing indicators verified
- [x] Connection lifecycle tested
- [x] All integration tests pass

## Testing Requirements

**Integration Test Coverage**:
- WebSocket connection lifecycle
- JWT authentication
- Redis adapter functionality
- SQS message publishing
- Multi-client broadcasting
- Typing indicators
- Room management
- Error scenarios (disconnections, auth failures)

## References

- **Testing Strategy**: `specs/testing-strategy.md` (comprehensive guide)
- **PROJECT.md**: Section 23a - Testing Strategy
- **CI/CD Pipeline**: `specs/ci-cd-pipelines.md` (integration test stage)
- **WebSocket Gateway**: `specs/ws-gateway.md`
- **Testcontainers Node**: https://node.testcontainers.org/
- **Related Tasks**: All ws-gateway tasks
