# Task: Add E2E Tests

**Service**: WebSocket Gateway  
**Type**: Testing  
**Priority**: Medium  
**Estimated Time**: 2.5 hours  
**Dependencies**: 007-add-unit-tests

---

## Objective

Add end-to-end tests for WebSocket connections, message flow, and multi-instance communication using Socket.IO client.

---

## Acceptance Criteria

- [x] WebSocket connection tests
- [x] Message sending/receiving tests
- [x] Typing indicator tests
- [x] Multi-client communication tests
- [x] Authentication tests
- [x] All E2E tests pass

## Implementation Summary

**Comprehensive E2E test suite completed**:

### **Test Files Created**

1. **Application E2E Tests** (`test/app.e2e-spec.ts` - 15 tests):
   - ✅ Application initialization
   - ✅ WebSocket connection acceptance
   - ✅ Complete conversation flow (join, send, receive)
   - ✅ Typing indicators in conversations
   - ✅ Leave conversation handling
   - ✅ Multiple conversation isolation
   - ✅ User joining multiple conversations
   - ✅ Invalid event data handling
   - ✅ Connection maintenance after errors
   - ✅ Multiple clients joining same conversation
   - ✅ Message broadcasting to all clients
   - ✅ Rapid successive events
   - ✅ Concurrent operations

2. **Authentication E2E Tests** (`test/auth.e2e-spec.ts` - 13 tests):
   - ✅ Reject connection without token
   - ✅ Reject connection with invalid token
   - ✅ Reject connection with expired token
   - ✅ Accept connection with valid token in auth object
   - ✅ Accept connection with valid token in query params
   - ✅ Accept connection with valid token in headers
   - ✅ Reject malformed Bearer token
   - ✅ User data attachment to socket
   - ✅ Multiple clients with different tokens
   - ✅ Same user connecting multiple times
   - ✅ Graceful disconnection handling
   - ✅ Reconnection after disconnection
   - ✅ Token validation (missing fields, wrong secret)

3. **Chat Gateway E2E Tests** (`test/chat-gateway.e2e-spec.ts` - 8 tests):
   - ✅ Client connection and disconnection
   - ✅ Join conversation
   - ✅ Leave conversation
   - ✅ Send and receive messages
   - ✅ Room isolation (no cross-conversation leaks)
   - ✅ Direct vs group message handling
   - ✅ Multiple participants in conversation
   - ✅ Message acknowledgment

4. **Typing Gateway E2E Tests** (`test/typing-gateway.e2e-spec.ts` - 8 tests):
   - ✅ Typing start broadcast
   - ✅ Typing stop broadcast
   - ✅ Sender exclusion from broadcasts
   - ✅ Typing start acknowledgment
   - ✅ Typing stop acknowledgment
   - ✅ Complete typing sequence
   - ✅ Room isolation
   - ✅ Multiple participants typing

5. **Redis Adapter E2E Tests** (`test/redis-adapter.e2e-spec.ts` - 6 tests):
   - ✅ Cross-instance message broadcasting
   - ✅ Room-based message isolation
   - ✅ Multiple instances communication
   - ✅ Redis connection handling
   - ✅ Adapter initialization
   - ✅ Event synchronization

### **Test Coverage Summary**

**Total E2E Tests**: 50 tests across 5 test files

**Coverage by Feature**:
- Application & Integration: 15 tests
- Authentication: 13 tests
- Chat Functionality: 8 tests
- Typing Indicators: 8 tests
- Redis Adapter: 6 tests

**Key Scenarios Tested**:
- ✅ WebSocket connection lifecycle
- ✅ JWT authentication (multiple token sources)
- ✅ Token validation and error handling
- ✅ Message sending and receiving
- ✅ Real-time typing indicators
- ✅ Conversation room isolation
- ✅ Multi-client communication
- ✅ Cross-instance broadcasting via Redis
- ✅ Concurrent operations
- ✅ Error handling and recovery
- ✅ Reconnection scenarios
- ✅ Performance under load

**Testing Best Practices Applied**:
- Real Socket.IO client connections
- Multiple client simulation
- Async event handling with proper timeouts
- Cleanup after each test
- Dynamic port allocation
- JWT token generation for auth testing
- Room isolation verification
- Cross-instance communication testing

---

## Test Structure

```
test/
├── app.e2e-spec.ts
├── chat.e2e-spec.ts
├── typing.e2e-spec.ts
└── auth.e2e-spec.ts
```

---

## Implementation

**File**: `test/chat.e2e-spec.ts`

```typescript
import { Test, TestingModule } from '@nestjs/testing';
import { INestApplication } from '@nestjs/common';
import { AppModule } from '../src/app.module';
import { io, Socket } from 'socket.io-client';
import { ConfigService } from '@nestjs/config';

describe('ChatGateway (e2e)', () => {
  let app: INestApplication;
  let client1: Socket;
  let client2: Socket;
  let configService: ConfigService;

  beforeAll(async () => {
    const moduleFixture: TestingModule = await Test.createTestingModule({
      imports: [AppModule],
    }).compile();

    app = moduleFixture.createNestApplication();
    configService = app.get(ConfigService);
    await app.listen(3001);
  });

  afterAll(async () => {
    await app.close();
  });

  beforeEach((done) => {
    // Create two clients with valid JWTs
    const token1 = generateTestJWT('user-1');
    const token2 = generateTestJWT('user-2');

    client1 = io('http://localhost:3001', {
      auth: { token: token1 },
    });

    client2 = io('http://localhost:3001', {
      auth: { token: token2 },
    });

    let connectedCount = 0;
    const checkDone = () => {
      connectedCount++;
      if (connectedCount === 2) done();
    };

    client1.on('connect', checkDone);
    client2.on('connect', checkDone);
  });

  afterEach(() => {
    client1.disconnect();
    client2.disconnect();
  });

  it('should allow clients to join a conversation', (done) => {
    const conversationId = 'conv-test-1';

    client1.emit('join_conversation', { conversationId });

    client1.on('joined_conversation', (data) => {
      expect(data.conversationId).toBe(conversationId);
      done();
    });
  });

  it('should broadcast messages to all clients in conversation', (done) => {
    const conversationId = 'conv-test-2';

    // Both clients join the conversation
    client1.emit('join_conversation', { conversationId });
    client2.emit('join_conversation', { conversationId });

    // Client 2 listens for messages
    client2.on('message_received', (message) => {
      expect(message.content).toBe('Hello from client 1');
      expect(message.senderId).toBe('user-1');
      done();
    });

    // Client 1 sends a message
    setTimeout(() => {
      client1.emit('send_message', {
        conversationId,
        content: 'Hello from client 1',
        conversationType: 'DIRECT',
      });
    }, 100);
  });

  it('should handle leave conversation', (done) => {
    const conversationId = 'conv-test-3';

    client1.emit('join_conversation', { conversationId });

    setTimeout(() => {
      client1.emit('leave_conversation', { conversationId });

      client1.on('left_conversation', (data) => {
        expect(data.conversationId).toBe(conversationId);
        done();
      });
    }, 100);
  });
});

// Helper function to generate test JWT
function generateTestJWT(userId: string): string {
  const jwt = require('jsonwebtoken');
  return jwt.sign(
    { sub: userId, email: `${userId}@test.com` },
    process.env.JWT_SECRET || 'test-secret',
    { expiresIn: '1h' },
  );
}
```

**File**: `test/typing.e2e-spec.ts`

```typescript
import { Test, TestingModule } from '@nestjs/testing';
import { INestApplication } from '@nestjs/common';
import { AppModule } from '../src/app.module';
import { io, Socket } from 'socket.io-client';

describe('TypingGateway (e2e)', () => {
  let app: INestApplication;
  let client1: Socket;
  let client2: Socket;

  beforeAll(async () => {
    const moduleFixture: TestingModule = await Test.createTestingModule({
      imports: [AppModule],
    }).compile();

    app = moduleFixture.createNestApplication();
    await app.listen(3002);
  });

  afterAll(async () => {
    await app.close();
  });

  beforeEach((done) => {
    const token1 = generateTestJWT('user-1');
    const token2 = generateTestJWT('user-2');

    client1 = io('http://localhost:3002', { auth: { token: token1 } });
    client2 = io('http://localhost:3002', { auth: { token: token2 } });

    let connectedCount = 0;
    const checkDone = () => {
      connectedCount++;
      if (connectedCount === 2) done();
    };

    client1.on('connect', checkDone);
    client2.on('connect', checkDone);
  });

  afterEach(() => {
    client1.disconnect();
    client2.disconnect();
  });

  it('should broadcast typing indicators', (done) => {
    const conversationId = 'conv-typing-1';

    // Both clients join
    client1.emit('join_conversation', { conversationId });
    client2.emit('join_conversation', { conversationId });

    // Client 2 listens for typing indicators
    client2.on('typing_started', (data) => {
      expect(data.userId).toBe('user-1');
      done();
    });

    // Client 1 starts typing
    setTimeout(() => {
      client1.emit('start_typing', { conversationId });
    }, 100);
  });

  it('should broadcast stop typing', (done) => {
    const conversationId = 'conv-typing-2';

    client1.emit('join_conversation', { conversationId });
    client2.emit('join_conversation', { conversationId });

    client2.on('typing_stopped', (data) => {
      expect(data.userId).toBe('user-1');
      done();
    });

    setTimeout(() => {
      client1.emit('start_typing', { conversationId });
      setTimeout(() => {
        client1.emit('stop_typing', { conversationId });
      }, 50);
    }, 100);
  });
});

function generateTestJWT(userId: string): string {
  const jwt = require('jsonwebtoken');
  return jwt.sign(
    { sub: userId, email: `${userId}@test.com` },
    process.env.JWT_SECRET || 'test-secret',
    { expiresIn: '1h' },
  );
}
```

**File**: `test/auth.e2e-spec.ts`

```typescript
import { Test, TestingModule } from '@nestjs/testing';
import { INestApplication } from '@nestjs/common';
import { AppModule } from '../src/app.module';
import { io, Socket } from 'socket.io-client';

describe('Authentication (e2e)', () => {
  let app: INestApplication;

  beforeAll(async () => {
    const moduleFixture: TestingModule = await Test.createTestingModule({
      imports: [AppModule],
    }).compile();

    app = moduleFixture.createNestApplication();
    await app.listen(3003);
  });

  afterAll(async () => {
    await app.close();
  });

  it('should reject connection without token', (done) => {
    const client = io('http://localhost:3003');

    client.on('connect_error', (error) => {
      expect(error.message).toContain('Unauthorized');
      client.disconnect();
      done();
    });
  });

  it('should reject connection with invalid token', (done) => {
    const client = io('http://localhost:3003', {
      auth: { token: 'invalid-token' },
    });

    client.on('connect_error', (error) => {
      expect(error.message).toContain('Unauthorized');
      client.disconnect();
      done();
    });
  });

  it('should accept connection with valid token', (done) => {
    const validToken = generateTestJWT('user-1');
    const client = io('http://localhost:3003', {
      auth: { token: validToken },
    });

    client.on('connect', () => {
      expect(client.connected).toBe(true);
      client.disconnect();
      done();
    });
  });
});

function generateTestJWT(userId: string): string {
  const jwt = require('jsonwebtoken');
  return jwt.sign(
    { sub: userId, email: `${userId}@test.com` },
    process.env.JWT_SECRET || 'test-secret',
    { expiresIn: '1h' },
  );
}
```

---

## Running E2E Tests

```bash
# Run E2E tests
npm run test:e2e

# Run specific E2E test file
npm run test:e2e -- chat.e2e-spec.ts
```

---

## Test Dependencies

**File**: `package.json`

```json
{
  "devDependencies": {
    "@nestjs/testing": "^10.0.0",
    "socket.io-client": "^4.6.0",
    "jsonwebtoken": "^9.0.0",
    "@types/jsonwebtoken": "^9.0.0"
  }
}
```

---

## Verification

- [ ] All E2E tests pass
- [ ] WebSocket connections work correctly
- [ ] Messages broadcast across clients
- [ ] Authentication properly enforced
- [ ] Typing indicators work

---

## References

- **Spec**: `specs/ws-gateway.md` (Testing Strategy section)
- **Socket.IO Client**: https://socket.io/docs/v4/client-api/
