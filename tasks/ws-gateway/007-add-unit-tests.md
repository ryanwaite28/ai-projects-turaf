# Task: Add Unit Tests

**Service**: WebSocket Gateway  
**Type**: Testing  
**Priority**: High  
**Estimated Time**: 2 hours  
**Dependencies**: 002-006 (all implementation tasks)

---

## Objective

Add comprehensive unit tests for gateways, services, and guards with >80% code coverage.

---

## Acceptance Criteria

- [ ] ChatGateway tests
- [ ] TypingGateway tests
- [ ] WsAuthGuard tests
- [ ] SqsPublisherService tests
- [ ] RedisPubSubService tests
- [ ] Test coverage >80%
- [ ] All tests pass

---

## Test Structure

```
src/
├── gateways/
│   ├── chat.gateway.spec.ts
│   └── typing.gateway.spec.ts
├── auth/
│   └── ws-auth.guard.spec.ts
└── services/
    ├── sqs-publisher.service.spec.ts
    └── redis-pub-sub.service.spec.ts
```

---

## Key Test Cases

### Gateway Tests
- Connection/disconnection handling
- Event handling (join, leave, send_message)
- Authorization checks
- Error handling

### Service Tests
- SQS message publishing
- Redis pub/sub operations
- Configuration handling
- Error scenarios

### Guard Tests
- Valid JWT acceptance
- Invalid JWT rejection
- Missing token handling
- Token extraction from different sources

---

## Example Test Suite

**File**: `src/services/redis-pub-sub.service.spec.ts`

```typescript
import { Test, TestingModule } from '@nestjs/testing';
import { ConfigService } from '@nestjs/config';
import { RedisPubSubService } from './redis-pub-sub.service';
import { createClient } from 'redis';

jest.mock('redis');

describe('RedisPubSubService', () => {
  let service: RedisPubSubService;
  let mockPubClient: any;
  let mockSubClient: any;

  beforeEach(async () => {
    mockPubClient = {
      connect: jest.fn().mockResolvedValue(undefined),
      publish: jest.fn().mockResolvedValue(1),
      quit: jest.fn().mockResolvedValue(undefined),
      on: jest.fn(),
    };

    mockSubClient = {
      connect: jest.fn().mockResolvedValue(undefined),
      subscribe: jest.fn().mockResolvedValue(undefined),
      quit: jest.fn().mockResolvedValue(undefined),
      on: jest.fn(),
      duplicate: jest.fn().mockReturnValue(mockSubClient),
    };

    (createClient as jest.Mock).mockReturnValue(mockPubClient);
    mockPubClient.duplicate = jest.fn().mockReturnValue(mockSubClient);

    const module: TestingModule = await Test.createTestingModule({
      providers: [
        RedisPubSubService,
        {
          provide: ConfigService,
          useValue: {
            get: jest.fn().mockReturnValue('redis://localhost:6379'),
          },
        },
      ],
    }).compile();

    service = module.get<RedisPubSubService>(RedisPubSubService);
  });

  it('should publish message to Redis', async () => {
    await service.publish('test-channel', { data: 'test' });
    
    expect(mockPubClient.publish).toHaveBeenCalledWith(
      'test-channel',
      JSON.stringify({ data: 'test' }),
    );
  });

  it('should subscribe to channel', async () => {
    const callback = jest.fn();
    
    await service.subscribe('test-channel', callback);
    
    expect(mockSubClient.subscribe).toHaveBeenCalled();
  });
});
```

---

## Running Tests

```bash
# Run all tests
npm test

# Run tests with coverage
npm run test:cov

# Run tests in watch mode
npm run test:watch
```

---

## Coverage Requirements

- **Statements**: >80%
- **Branches**: >75%
- **Functions**: >80%
- **Lines**: >80%

---

## References

- **Testing Guide**: `specs/ws-gateway.md` (Testing Strategy section)
- **NestJS Testing**: https://docs.nestjs.com/fundamentals/testing
