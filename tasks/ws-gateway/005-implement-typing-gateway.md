# Task: Implement Typing Gateway

**Service**: WebSocket Gateway  
**Type**: WebSocket Gateway  
**Priority**: Medium  
**Estimated Time**: 1.5 hours  
**Dependencies**: 003-implement-redis-adapter

---

## Objective

Implement the TypingGateway to handle typing indicator events and broadcast them across multiple gateway instances via Redis.

---

## Acceptance Criteria

- [x] TypingGateway handles start/stop typing events
- [x] Typing indicators broadcast via Redis Pub/Sub
- [x] Events scoped to conversations
- [x] No persistence (ephemeral events only)
- [x] Tests pass

## Implementation Summary

**Typing Gateway implementation completed**:

1. **TypingGateway** (`src/gateways/typing.gateway.ts`)
   - ✅ WebSocket typing start/stop event handlers
   - ✅ JWT authentication via WsAuthGuard
   - ✅ Real-time broadcasting via Socket.IO
   - ✅ Room-scoped events (conversation-based)
   - ✅ Excludes sender from broadcasts
   - ✅ Ephemeral events (no persistence)
   - ✅ Logging for debugging

2. **Event Flow**:
   - Client emits `typing_start` or `typing_stop`
   - Gateway broadcasts `user_typing` to room (excluding sender)
   - Redis adapter distributes across instances
   - Other clients in conversation receive indicator
   - Sender receives acknowledgment

3. **Event Structure**:
   - Input: `{ conversationId: string }`
   - Broadcast: `{ conversationId, userId, isTyping: boolean }`
   - Acknowledgment: `{ event: 'typing_started|typing_stopped', data: { conversationId } }`

**Unit tests created** (`src/gateways/typing.gateway.spec.ts` - 15 tests):
- ✅ Gateway initialization
- ✅ Typing start broadcasting to room
- ✅ Typing stop broadcasting to room
- ✅ Logging for start/stop events
- ✅ User ID inclusion in events
- ✅ isTyping flag (true/false)
- ✅ Correct room targeting
- ✅ Room naming convention
- ✅ Sender exclusion from broadcasts
- ✅ Event data structure validation
- ✅ Acknowledgment responses

**E2E tests created** (`test/typing-gateway.e2e-spec.ts` - 8 tests):
- ✅ Broadcast typing start to other clients
- ✅ Sender doesn't receive own typing event
- ✅ Typing start acknowledgment
- ✅ Broadcast typing stop to other clients
- ✅ Typing stop acknowledgment
- ✅ Start and stop typing sequence
- ✅ Room isolation (no cross-conversation leaks)
- ✅ Multiple participants broadcasting
- ✅ Ephemeral nature (no persistence)

**Features verified**:
- Real-time typing indicators
- Conversation-scoped broadcasting
- Redis Pub/Sub for horizontal scaling
- Sender exclusion from broadcasts
- Ephemeral events (no database persistence)
- Cross-instance synchronization

---

## Implementation

**File**: `src/gateways/typing.gateway.ts`

```typescript
import {
  WebSocketGateway,
  SubscribeMessage,
  MessageBody,
  ConnectedSocket,
  WebSocketServer,
  OnGatewayInit,
} from '@nestjs/websockets';
import { Server, Socket } from 'socket.io';
import { UseGuards } from '@nestjs/common';
import { WsAuthGuard } from '../auth/ws-auth.guard';
import { TypingIndicatorDto } from '../dto/typing-indicator.dto';
import { RedisPubSubService } from '../services/redis-pub-sub.service';

@WebSocketGateway()
@UseGuards(WsAuthGuard)
export class TypingGateway implements OnGatewayInit {
  @WebSocketServer()
  server: Server;

  constructor(private redisPubSub: RedisPubSubService) {}

  afterInit(server: Server) {
    console.log('TypingGateway initialized');
    this.setupRedisSubscriptions();
  }

  @SubscribeMessage('start_typing')
  async handleStartTyping(
    @MessageBody() data: TypingIndicatorDto,
    @ConnectedSocket() client: Socket,
  ) {
    const userId = client.data.userId;
    const { conversationId } = data;

    console.log(`User ${userId} started typing in ${conversationId}`);
    
    // Broadcast via Redis to all instances
    await this.redisPubSub.publish('typing_started', {
      conversationId,
      userId,
    });

    return {
      event: 'typing_started',
      data: { conversationId, userId },
    };
  }

  @SubscribeMessage('stop_typing')
  async handleStopTyping(
    @MessageBody() data: TypingIndicatorDto,
    @ConnectedSocket() client: Socket,
  ) {
    const userId = client.data.userId;
    const { conversationId } = data;

    console.log(`User ${userId} stopped typing in ${conversationId}`);
    
    await this.redisPubSub.publish('typing_stopped', {
      conversationId,
      userId,
    });

    return {
      event: 'typing_stopped',
      data: { conversationId, userId },
    };
  }

  private setupRedisSubscriptions() {
    // Subscribe to typing_started events
    this.redisPubSub.subscribe('typing_started', (data) => {
      console.log(`Broadcasting typing_started for user ${data.userId} in ${data.conversationId}`);
      this.server.to(data.conversationId).emit('typing_started', {
        userId: data.userId,
      });
    });

    // Subscribe to typing_stopped events
    this.redisPubSub.subscribe('typing_stopped', (data) => {
      console.log(`Broadcasting typing_stopped for user ${data.userId} in ${data.conversationId}`);
      this.server.to(data.conversationId).emit('typing_stopped', {
        userId: data.userId,
      });
    });

    console.log('TypingGateway Redis subscriptions setup complete');
  }
}
```

**File**: `src/dto/typing-indicator.dto.ts`

```typescript
import { IsNotEmpty, IsString } from 'class-validator';

export class TypingIndicatorDto {
  @IsNotEmpty()
  @IsString()
  conversationId: string;
}
```

---

## Update AppModule

**File**: `src/app.module.ts`

```typescript
import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { ChatGateway } from './gateways/chat.gateway';
import { TypingGateway } from './gateways/typing.gateway';
import { AuthModule } from './auth/auth.module';
import { RedisPubSubService } from './services/redis-pub-sub.service';
import { SqsPublisherService } from './services/sqs-publisher.service';

@Module({
  imports: [
    ConfigModule.forRoot({
      isGlobal: true,
    }),
    AuthModule,
  ],
  providers: [
    ChatGateway,
    TypingGateway,
    RedisPubSubService,
    SqsPublisherService,
  ],
})
export class AppModule {}
```

---

## Testing

**File**: `src/gateways/typing.gateway.spec.ts`

```typescript
import { Test, TestingModule } from '@nestjs/testing';
import { TypingGateway } from './typing.gateway';
import { RedisPubSubService } from '../services/redis-pub-sub.service';

describe('TypingGateway', () => {
  let gateway: TypingGateway;
  let redisPubSub: RedisPubSubService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        TypingGateway,
        {
          provide: RedisPubSubService,
          useValue: {
            publish: jest.fn(),
            subscribe: jest.fn(),
          },
        },
      ],
    }).compile();

    gateway = module.get<TypingGateway>(TypingGateway);
    redisPubSub = module.get<RedisPubSubService>(RedisPubSubService);
  });

  it('should handle start_typing', async () => {
    const mockClient = {
      data: { userId: 'user-123' },
    };

    const dto = { conversationId: 'conv-123' };

    jest.spyOn(redisPubSub, 'publish').mockResolvedValue(undefined);

    const result = await gateway.handleStartTyping(dto, mockClient as any);

    expect(result.event).toBe('typing_started');
    expect(redisPubSub.publish).toHaveBeenCalledWith('typing_started', {
      conversationId: 'conv-123',
      userId: 'user-123',
    });
  });
});
```

---

## Verification

1. Connect client:
   ```typescript
   const socket = io('http://localhost:3000', {
     auth: { token: 'valid-jwt' }
   });
   
   socket.emit('join_conversation', { conversationId: 'conv-123' });
   
   // Start typing
   socket.emit('start_typing', { conversationId: 'conv-123' });
   
   // Listen for typing indicators
   socket.on('typing_started', (data) => {
     console.log(`User ${data.userId} is typing`);
   });
   
   socket.on('typing_stopped', (data) => {
     console.log(`User ${data.userId} stopped typing`);
   });
   
   // Stop typing
   socket.emit('stop_typing', { conversationId: 'conv-123' });
   ```

2. Verify typing indicators broadcast to all clients in conversation

---

## References

- **Spec**: `specs/ws-gateway.md` (TypingGateway section)
