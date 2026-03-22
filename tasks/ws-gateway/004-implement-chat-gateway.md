# Task: Implement Chat Gateway

**Service**: WebSocket Gateway  
**Type**: WebSocket Gateway  
**Priority**: High  
**Estimated Time**: 3 hours  
**Dependencies**: 002-implement-jwt-authentication, 003-implement-redis-adapter

---

## Objective

Implement the ChatGateway to handle real-time message sending, conversation joining/leaving, and message broadcasting across multiple gateway instances.

---

## Acceptance Criteria

- [ ] ChatGateway handles WebSocket connections
- [ ] Join/leave conversation events working
- [ ] Send message event publishes to SQS
- [ ] Messages broadcast via Redis Pub/Sub
- [ ] Connection/disconnection handling
- [ ] Tests pass

---

## Implementation

**File**: `src/gateways/chat.gateway.ts`

```typescript
import {
  WebSocketGateway,
  SubscribeMessage,
  MessageBody,
  ConnectedSocket,
  OnGatewayConnection,
  OnGatewayDisconnect,
  WebSocketServer,
  OnGatewayInit,
} from '@nestjs/websockets';
import { Server, Socket } from 'socket.io';
import { UseGuards } from '@nestjs/common';
import { WsAuthGuard } from '../auth/ws-auth.guard';
import { SendMessageDto } from '../dto/send-message.dto';
import { JoinConversationDto } from '../dto/join-conversation.dto';
import { SqsPublisherService } from '../services/sqs-publisher.service';
import { RedisPubSubService } from '../services/redis-pub-sub.service';
import { v4 as uuidv4 } from 'uuid';

@WebSocketGateway({
  cors: {
    origin: process.env.ALLOWED_ORIGINS?.split(',') || ['http://localhost:4200'],
    credentials: true,
  },
})
@UseGuards(WsAuthGuard)
export class ChatGateway
  implements OnGatewayInit, OnGatewayConnection, OnGatewayDisconnect
{
  @WebSocketServer()
  server: Server;

  constructor(
    private sqsPublisher: SqsPublisherService,
    private redisPubSub: RedisPubSubService,
  ) {}

  afterInit(server: Server) {
    console.log('ChatGateway initialized');
    this.setupRedisSubscriptions();
  }

  async handleConnection(client: Socket) {
    const userId = client.data.userId;
    console.log(`Client connected: ${client.id}, User: ${userId}`);
    
    // Join user to their personal room for direct notifications
    await client.join(`user:${userId}`);
  }

  async handleDisconnect(client: Socket) {
    const userId = client.data.userId;
    console.log(`Client disconnected: ${client.id}, User: ${userId}`);
    
    // Leave all rooms
    const rooms = Array.from(client.rooms).filter(room => room !== client.id);
    rooms.forEach(room => client.leave(room));
  }

  @SubscribeMessage('join_conversation')
  async handleJoinConversation(
    @MessageBody() data: JoinConversationDto,
    @ConnectedSocket() client: Socket,
  ) {
    const { conversationId } = data;
    const userId = client.data.userId;

    console.log(`User ${userId} joining conversation ${conversationId}`);

    // TODO: Validate user is participant (call Communications Service API)
    // For now, trust the client
    
    await client.join(conversationId);
    
    return {
      event: 'joined_conversation',
      data: { conversationId, userId },
    };
  }

  @SubscribeMessage('leave_conversation')
  async handleLeaveConversation(
    @MessageBody() data: { conversationId: string },
    @ConnectedSocket() client: Socket,
  ) {
    const { conversationId } = data;
    const userId = client.data.userId;

    console.log(`User ${userId} leaving conversation ${conversationId}`);
    
    await client.leave(conversationId);
    
    return {
      event: 'left_conversation',
      data: { conversationId, userId },
    };
  }

  @SubscribeMessage('send_message')
  async handleSendMessage(
    @MessageBody() data: SendMessageDto,
    @ConnectedSocket() client: Socket,
  ) {
    const userId = client.data.userId;
    const { conversationId, content, conversationType } = data;

    console.log(`User ${userId} sending message to conversation ${conversationId}`);

    // Create message DTO
    const messageDto = {
      id: uuidv4(),
      conversationId,
      senderId: userId,
      content,
      createdAt: new Date().toISOString(),
    };

    // Determine which SQS queue to use
    const queueUrl = conversationType === 'DIRECT' 
      ? process.env.SQS_DIRECT_QUEUE_URL
      : process.env.SQS_GROUP_QUEUE_URL;

    try {
      // Publish to SQS FIFO for persistence
      await this.sqsPublisher.publishMessage(
        queueUrl,
        messageDto,
        conversationId, // MessageGroupId for ordering
      );

      // Broadcast to all clients in the conversation room (via Redis)
      await this.redisPubSub.publish('message_received', {
        conversationId,
        message: messageDto,
      });

      return {
        event: 'message_sent',
        data: { messageId: messageDto.id, status: 'sent' },
      };
    } catch (error) {
      console.error('Error sending message:', error);
      return {
        event: 'message_error',
        data: { error: 'Failed to send message' },
      };
    }
  }

  private setupRedisSubscriptions() {
    // Subscribe to message_received events from Redis
    this.redisPubSub.subscribe('message_received', (data) => {
      console.log(`Broadcasting message to conversation ${data.conversationId}`);
      this.server.to(data.conversationId).emit('message_received', data.message);
    });

    console.log('Redis subscriptions setup complete');
  }
}
```

**File**: `src/dto/send-message.dto.ts`

```typescript
import { IsNotEmpty, IsString, IsEnum, MaxLength } from 'class-validator';

export class SendMessageDto {
  @IsNotEmpty()
  @IsString()
  conversationId: string;

  @IsNotEmpty()
  @IsString()
  @MaxLength(10000)
  content: string;

  @IsNotEmpty()
  @IsEnum(['DIRECT', 'GROUP'])
  conversationType: 'DIRECT' | 'GROUP';
}
```

**File**: `src/dto/join-conversation.dto.ts`

```typescript
import { IsNotEmpty, IsString } from 'class-validator';

export class JoinConversationDto {
  @IsNotEmpty()
  @IsString()
  conversationId: string;
}
```

---

## Testing

**File**: `src/gateways/chat.gateway.spec.ts`

```typescript
import { Test, TestingModule } from '@nestjs/testing';
import { ChatGateway } from './chat.gateway';
import { SqsPublisherService } from '../services/sqs-publisher.service';
import { RedisPubSubService } from '../services/redis-pub-sub.service';

describe('ChatGateway', () => {
  let gateway: ChatGateway;
  let sqsPublisher: SqsPublisherService;
  let redisPubSub: RedisPubSubService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        ChatGateway,
        {
          provide: SqsPublisherService,
          useValue: {
            publishMessage: jest.fn(),
          },
        },
        {
          provide: RedisPubSubService,
          useValue: {
            publish: jest.fn(),
            subscribe: jest.fn(),
          },
        },
      ],
    }).compile();

    gateway = module.get<ChatGateway>(ChatGateway);
    sqsPublisher = module.get<SqsPublisherService>(SqsPublisherService);
    redisPubSub = module.get<RedisPubSubService>(RedisPubSubService);
  });

  it('should be defined', () => {
    expect(gateway).toBeDefined();
  });

  it('should handle send_message', async () => {
    const mockClient = {
      data: { userId: 'user-123' },
    };

    const messageDto = {
      conversationId: 'conv-123',
      content: 'Hello',
      conversationType: 'DIRECT' as const,
    };

    jest.spyOn(sqsPublisher, 'publishMessage').mockResolvedValue(undefined);
    jest.spyOn(redisPubSub, 'publish').mockResolvedValue(undefined);

    const result = await gateway.handleSendMessage(messageDto, mockClient as any);

    expect(result.event).toBe('message_sent');
    expect(sqsPublisher.publishMessage).toHaveBeenCalled();
    expect(redisPubSub.publish).toHaveBeenCalled();
  });
});
```

---

## Verification

1. Start services:
   ```bash
   npm run start:dev
   ```

2. Connect client:
   ```typescript
   const socket = io('http://localhost:3000', {
     auth: { token: 'valid-jwt' }
   });
   
   socket.emit('join_conversation', { conversationId: 'conv-123' });
   
   socket.emit('send_message', {
     conversationId: 'conv-123',
     content: 'Hello World',
     conversationType: 'DIRECT'
   });
   
   socket.on('message_received', (message) => {
     console.log('Received:', message);
   });
   ```

3. Verify message sent to SQS
4. Verify message broadcast via Redis

---

## References

- **Spec**: `specs/ws-gateway.md` (ChatGateway section)
- **Socket.IO Docs**: https://socket.io/docs/v4/server-api/
