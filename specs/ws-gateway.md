# WebSocket Gateway Specification

**Service Type**: NestJS WebSocket Gateway  
**Language**: TypeScript  
**Framework**: NestJS with @nestjs/websockets  
**Real-Time**: Socket.IO  
**Scaling**: Redis Pub/Sub  
**Messaging**: AWS SQS FIFO (Publisher)

---

## Overview

The WebSocket Gateway is a stateless NestJS service that manages real-time WebSocket connections for the Turaf platform. It handles client connections, authenticates users via JWT, broadcasts messages and typing indicators across multiple gateway instances using Redis Pub/Sub, and publishes messages to SQS FIFO queues for persistence by the Communications Service.

---

## Responsibilities

- Accept and manage WebSocket connections from clients
- Authenticate connections using JWT tokens
- Handle real-time events (send_message, typing indicators, join/leave conversations)
- Publish messages to SQS FIFO queues for persistence
- Broadcast events across multiple gateway instances via Redis Pub/Sub
- Maintain conversation room subscriptions per connection
- Provide horizontal scalability through stateless design

---

## Architecture

### Stateless Design

- No local state storage
- All persistent data in Communications Service
- Redis used only for cross-instance event broadcasting
- Connections can be distributed across multiple instances
- Load balancer can route WebSocket connections to any instance

### Horizontal Scaling

```
Client 1 → ALB → Gateway Instance 1 ─┐
Client 2 → ALB → Gateway Instance 2 ─┼─→ Redis Pub/Sub ←─→ All Instances
Client 3 → ALB → Gateway Instance 3 ─┘
                        ↓
                   SQS FIFO Queues
                        ↓
            Communications Service (Persistence)
```

---

## NestJS Module Structure

```
src/
├── main.ts
├── app.module.ts
├── config/
│   ├── redis.config.ts
│   ├── sqs.config.ts
│   └── jwt.config.ts
├── auth/
│   ├── auth.guard.ts
│   ├── jwt.strategy.ts
│   └── ws-auth.guard.ts
├── gateways/
│   ├── chat.gateway.ts
│   └── typing.gateway.ts
├── services/
│   ├── redis-pub-sub.service.ts
│   ├── sqs-publisher.service.ts
│   └── identity-client.service.ts
├── dto/
│   ├── message.dto.ts
│   ├── typing-indicator.dto.ts
│   └── join-conversation.dto.ts
└── interfaces/
    └── authenticated-socket.interface.ts
```

---

## Configuration-Based Redis Adapter

### Environment-Based Switching

```typescript
// config/redis.config.ts
import { ConfigService } from '@nestjs/config';
import { IoAdapter } from '@nestjs/platform-socket.io';
import { createAdapter } from '@socket.io/redis-adapter';
import { createClient } from 'redis';

export class RedisIoAdapter extends IoAdapter {
  private adapterConstructor: ReturnType<typeof createAdapter>;

  async connectToRedis(configService: ConfigService): Promise<void> {
    const redisUrl = configService.get<string>('REDIS_URL');
    
    // Local development: redis://localhost:6379
    // AWS: redis://elasticache-endpoint:6379
    
    const pubClient = createClient({ url: redisUrl });
    const subClient = pubClient.duplicate();

    await Promise.all([pubClient.connect(), subClient.connect()]);

    this.adapterConstructor = createAdapter(pubClient, subClient);
  }

  createIOServer(port: number, options?: any): any {
    const server = super.createIOServer(port, options);
    server.adapter(this.adapterConstructor);
    return server;
  }
}
```

**Configuration**:
- **Local**: `REDIS_URL=redis://redis:6379` (Docker Compose)
- **AWS**: `REDIS_URL=redis://turaf-elasticache.abc123.0001.use1.cache.amazonaws.com:6379`

---

## Authentication

### JWT Validation

```typescript
// auth/ws-auth.guard.ts
import { CanActivate, ExecutionContext, Injectable } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { WsException } from '@nestjs/websockets';
import { Socket } from 'socket.io';

@Injectable()
export class WsAuthGuard implements CanActivate {
  constructor(private jwtService: JwtService) {}

  async canActivate(context: ExecutionContext): Promise<boolean> {
    const client: Socket = context.switchToWs().getClient();
    const token = this.extractTokenFromHandshake(client);

    if (!token) {
      throw new WsException('Unauthorized: No token provided');
    }

    try {
      const payload = await this.jwtService.verifyAsync(token, {
        secret: process.env.JWT_SECRET,
      });

      // Attach user info to socket
      client.data.userId = payload.sub;
      client.data.email = payload.email;
      
      return true;
    } catch (error) {
      throw new WsException('Unauthorized: Invalid token');
    }
  }

  private extractTokenFromHandshake(client: Socket): string | undefined {
    const token = client.handshake.auth?.token || 
                  client.handshake.headers?.authorization?.split(' ')[1];
    return token;
  }
}
```

### Connection Handshake

**Client-side** (Angular):
```typescript
const socket = io('ws://localhost:3000', {
  auth: {
    token: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...'
  }
});
```

---

## WebSocket Gateways

### ChatGateway

```typescript
// gateways/chat.gateway.ts
import {
  WebSocketGateway,
  SubscribeMessage,
  MessageBody,
  ConnectedSocket,
  OnGatewayConnection,
  OnGatewayDisconnect,
  WebSocketServer,
} from '@nestjs/websockets';
import { Server, Socket } from 'socket.io';
import { UseGuards } from '@nestjs/common';
import { WsAuthGuard } from '../auth/ws-auth.guard';

@WebSocketGateway({
  cors: {
    origin: process.env.ALLOWED_ORIGINS?.split(',') || ['http://localhost:4200'],
    credentials: true,
  },
})
@UseGuards(WsAuthGuard)
export class ChatGateway implements OnGatewayConnection, OnGatewayDisconnect {
  @WebSocketServer()
  server: Server;

  constructor(
    private sqsPublisher: SqsPublisherService,
    private redisPubSub: RedisPubSubService,
  ) {}

  async handleConnection(client: Socket) {
    console.log(`Client connected: ${client.id}, User: ${client.data.userId}`);
  }

  async handleDisconnect(client: Socket) {
    console.log(`Client disconnected: ${client.id}`);
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

    // Validate user is participant (could call Communications Service API)
    // For now, trust the client
    
    await client.join(conversationId);
    
    return {
      event: 'joined_conversation',
      data: { conversationId },
    };
  }

  @SubscribeMessage('leave_conversation')
  async handleLeaveConversation(
    @MessageBody() data: { conversationId: string },
    @ConnectedSocket() client: Socket,
  ) {
    await client.leave(data.conversationId);
    
    return {
      event: 'left_conversation',
      data: { conversationId: data.conversationId },
    };
  }

  @SubscribeMessage('send_message')
  async handleSendMessage(
    @MessageBody() data: SendMessageDto,
    @ConnectedSocket() client: Socket,
  ) {
    const userId = client.data.userId;
    const { conversationId, content, conversationType } = data;

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

    // Publish to SQS FIFO for persistence
    await this.sqsPublisher.publishMessage(queueUrl, messageDto, conversationId);

    // Broadcast to all clients in the conversation room (via Redis)
    await this.redisPubSub.publish('message_received', {
      conversationId,
      message: messageDto,
    });

    return {
      event: 'message_sent',
      data: { messageId: messageDto.id },
    };
  }

  // Subscribe to Redis Pub/Sub for cross-instance broadcasting
  async onModuleInit() {
    await this.redisPubSub.subscribe('message_received', (data) => {
      this.server.to(data.conversationId).emit('message_received', data.message);
    });
  }
}
```

### TypingGateway

```typescript
// gateways/typing.gateway.ts
@WebSocketGateway()
@UseGuards(WsAuthGuard)
export class TypingGateway {
  @WebSocketServer()
  server: Server;

  constructor(private redisPubSub: RedisPubSubService) {}

  @SubscribeMessage('start_typing')
  async handleStartTyping(
    @MessageBody() data: { conversationId: string },
    @ConnectedSocket() client: Socket,
  ) {
    const userId = client.data.userId;
    
    // Broadcast via Redis to all instances
    await this.redisPubSub.publish('typing_started', {
      conversationId: data.conversationId,
      userId,
    });
  }

  @SubscribeMessage('stop_typing')
  async handleStopTyping(
    @MessageBody() data: { conversationId: string },
    @ConnectedSocket() client: Socket,
  ) {
    const userId = client.data.userId;
    
    await this.redisPubSub.publish('typing_stopped', {
      conversationId: data.conversationId,
      userId,
    });
  }

  async onModuleInit() {
    await this.redisPubSub.subscribe('typing_started', (data) => {
      this.server.to(data.conversationId).emit('typing_started', {
        userId: data.userId,
      });
    });

    await this.redisPubSub.subscribe('typing_stopped', (data) => {
      this.server.to(data.conversationId).emit('typing_stopped', {
        userId: data.userId,
      });
    });
  }
}
```

---

## Services

### RedisPubSubService

```typescript
// services/redis-pub-sub.service.ts
import { Injectable, OnModuleDestroy } from '@nestjs/common';
import { createClient, RedisClientType } from 'redis';
import { ConfigService } from '@nestjs/config';

@Injectable()
export class RedisPubSubService implements OnModuleDestroy {
  private pubClient: RedisClientType;
  private subClient: RedisClientType;
  private subscribers: Map<string, (data: any) => void> = new Map();

  constructor(private configService: ConfigService) {
    this.initializeClients();
  }

  private async initializeClients() {
    const redisUrl = this.configService.get<string>('REDIS_URL');
    
    this.pubClient = createClient({ url: redisUrl });
    this.subClient = createClient({ url: redisUrl });

    await Promise.all([
      this.pubClient.connect(),
      this.subClient.connect(),
    ]);
  }

  async publish(channel: string, data: any): Promise<void> {
    await this.pubClient.publish(channel, JSON.stringify(data));
  }

  async subscribe(channel: string, callback: (data: any) => void): Promise<void> {
    this.subscribers.set(channel, callback);
    
    await this.subClient.subscribe(channel, (message) => {
      const data = JSON.parse(message);
      callback(data);
    });
  }

  async onModuleDestroy() {
    await Promise.all([
      this.pubClient.quit(),
      this.subClient.quit(),
    ]);
  }
}
```

### SqsPublisherService

```typescript
// services/sqs-publisher.service.ts
import { Injectable } from '@nestjs/common';
import { SQSClient, SendMessageCommand } from '@aws-sdk/client-sqs';
import { ConfigService } from '@nestjs/config';

@Injectable()
export class SqsPublisherService {
  private sqsClient: SQSClient;

  constructor(private configService: ConfigService) {
    this.sqsClient = new SQSClient({
      region: this.configService.get<string>('AWS_REGION', 'us-east-1'),
    });
  }

  async publishMessage(
    queueUrl: string,
    message: any,
    messageGroupId: string,  // conversation_id for ordering
  ): Promise<void> {
    const command = new SendMessageCommand({
      QueueUrl: queueUrl,
      MessageBody: JSON.stringify(message),
      MessageGroupId: messageGroupId,
      MessageDeduplicationId: message.id,  // Use message ID for deduplication
    });

    await this.sqsClient.send(command);
  }
}
```

---

## DTOs

```typescript
// dto/send-message.dto.ts
export class SendMessageDto {
  conversationId: string;
  content: string;
  conversationType: 'DIRECT' | 'GROUP';
}

// dto/join-conversation.dto.ts
export class JoinConversationDto {
  conversationId: string;
}

// dto/typing-indicator.dto.ts
export class TypingIndicatorDto {
  conversationId: string;
}
```

---

## Configuration

**Environment Variables**:
```env
# Server
PORT=3000
NODE_ENV=production

# JWT
JWT_SECRET=your-256-bit-secret

# Redis (Configuration-based)
REDIS_URL=redis://localhost:6379  # Local
# REDIS_URL=redis://elasticache-endpoint:6379  # AWS

# SQS
AWS_REGION=us-east-1
SQS_DIRECT_QUEUE_URL=https://sqs.us-east-1.amazonaws.com/123456789/communications-direct-messages.fifo
SQS_GROUP_QUEUE_URL=https://sqs.us-east-1.amazonaws.com/123456789/communications-group-messages.fifo

# CORS
ALLOWED_ORIGINS=http://localhost:4200,https://app.turafapp.com
```

---

## Dependencies (package.json)

```json
{
  "dependencies": {
    "@nestjs/common": "^10.0.0",
    "@nestjs/core": "^10.0.0",
    "@nestjs/platform-socket.io": "^10.0.0",
    "@nestjs/websockets": "^10.0.0",
    "@nestjs/config": "^3.0.0",
    "@nestjs/jwt": "^10.0.0",
    "@nestjs/passport": "^10.0.0",
    "passport": "^0.6.0",
    "passport-jwt": "^4.0.1",
    "socket.io": "^4.6.0",
    "@socket.io/redis-adapter": "^8.2.0",
    "redis": "^4.6.0",
    "@aws-sdk/client-sqs": "^3.400.0",
    "uuid": "^9.0.0",
    "rxjs": "^7.8.0"
  },
  "devDependencies": {
    "@nestjs/cli": "^10.0.0",
    "@nestjs/testing": "^10.0.0",
    "@types/node": "^20.0.0",
    "@types/passport-jwt": "^3.0.9",
    "typescript": "^5.0.0"
  }
}
```

---

## WebSocket Events

### Client → Server

| Event | Payload | Description |
|-------|---------|-------------|
| `join_conversation` | `{ conversationId: string }` | Subscribe to conversation updates |
| `leave_conversation` | `{ conversationId: string }` | Unsubscribe from conversation |
| `send_message` | `{ conversationId, content, conversationType }` | Send a message |
| `start_typing` | `{ conversationId: string }` | Indicate user is typing |
| `stop_typing` | `{ conversationId: string }` | Indicate user stopped typing |

### Server → Client

| Event | Payload | Description |
|-------|---------|-------------|
| `message_received` | `{ id, conversationId, senderId, content, createdAt }` | New message in conversation |
| `typing_started` | `{ userId: string }` | User started typing |
| `typing_stopped` | `{ userId: string }` | User stopped typing |
| `joined_conversation` | `{ conversationId: string }` | Confirmation of join |
| `left_conversation` | `{ conversationId: string }` | Confirmation of leave |
| `message_sent` | `{ messageId: string }` | Confirmation of send |

---

## Health Check

```typescript
// main.ts
import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);
  
  // Health check endpoint
  app.getHttpAdapter().get('/health', (req, res) => {
    res.status(200).json({ status: 'ok' });
  });
  
  await app.listen(3000);
}
bootstrap();
```

---

## Deployment Considerations

### Docker

```dockerfile
FROM node:20-alpine

WORKDIR /app

COPY package*.json ./
RUN npm ci --only=production

COPY . .
RUN npm run build

EXPOSE 3000

CMD ["node", "dist/main"]
```

### ECS Task Definition

- **CPU**: 512 (0.5 vCPU)
- **Memory**: 1024 MB
- **Port Mappings**: 3000 (WebSocket)
- **Health Check**: `/health` endpoint
- **Environment Variables**: Injected from Secrets Manager

### Scaling

- **Auto Scaling**: Based on active connections metric
- **Min Instances**: 2 (for high availability)
- **Max Instances**: 10
- **Target Metric**: 1000 connections per instance

---

## Testing Strategy

**Unit Tests**:
- Gateway event handlers
- Service methods
- DTO validation

**Integration Tests**:
- WebSocket connection flow
- Redis Pub/Sub broadcasting
- SQS message publishing

**E2E Tests**:
```typescript
describe('ChatGateway E2E', () => {
  let client1: Socket;
  let client2: Socket;

  beforeAll(() => {
    client1 = io('http://localhost:3000', { auth: { token: 'jwt1' } });
    client2 = io('http://localhost:3000', { auth: { token: 'jwt2' } });
  });

  it('should broadcast message to all clients in conversation', (done) => {
    const conversationId = 'conv-123';
    
    client1.emit('join_conversation', { conversationId });
    client2.emit('join_conversation', { conversationId });
    
    client2.on('message_received', (data) => {
      expect(data.content).toBe('Hello');
      done();
    });
    
    client1.emit('send_message', {
      conversationId,
      content: 'Hello',
      conversationType: 'DIRECT',
    });
  });
});
```

---

## References

- **PROJECT.md**: Sections 5, 11
- **Communications Service**: `specs/communications-service.md`
- **Event Schemas**: `specs/communications-event-schemas.md`
- **NestJS WebSockets**: https://docs.nestjs.com/websockets/gateways
- **Socket.IO Redis Adapter**: https://socket.io/docs/v4/redis-adapter/
