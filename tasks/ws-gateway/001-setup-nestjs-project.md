# Task: Setup NestJS WebSocket Gateway Project

**Service**: WebSocket Gateway  
**Type**: NestJS Application  
**Priority**: High  
**Estimated Time**: 2 hours  
**Dependencies**: None

---

## Objective

Initialize the NestJS project for the WebSocket Gateway with all necessary dependencies, configuration, and directory structure.

---

## Acceptance Criteria

- [ ] NestJS project created with WebSocket support
- [ ] All dependencies installed
- [ ] Project structure follows NestJS best practices
- [ ] Configuration files created
- [ ] Docker support added
- [ ] Project builds and starts successfully

---

## Implementation

### 1. Create NestJS Project

```bash
cd services
npx @nestjs/cli new ws-gateway
cd ws-gateway
```

### 2. Install Dependencies

```bash
npm install --save \
  @nestjs/websockets \
  @nestjs/platform-socket.io \
  @nestjs/config \
  @nestjs/jwt \
  @nestjs/passport \
  passport \
  passport-jwt \
  socket.io \
  @socket.io/redis-adapter \
  redis \
  @aws-sdk/client-sqs \
  uuid

npm install --save-dev \
  @types/passport-jwt \
  @types/socket.io
```

### 3. Update package.json

**File**: `package.json`

```json
{
  "name": "ws-gateway",
  "version": "1.0.0",
  "description": "WebSocket Gateway for Turaf Communications",
  "scripts": {
    "build": "nest build",
    "start": "nest start",
    "start:dev": "nest start --watch",
    "start:prod": "node dist/main",
    "test": "jest",
    "test:watch": "jest --watch",
    "test:cov": "jest --coverage"
  },
  "dependencies": {
    "@nestjs/common": "^10.0.0",
    "@nestjs/core": "^10.0.0",
    "@nestjs/platform-express": "^10.0.0",
    "@nestjs/websockets": "^10.0.0",
    "@nestjs/platform-socket.io": "^10.0.0",
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
  }
}
```

### 4. Create Directory Structure

```
services/ws-gateway/
├── src/
│   ├── main.ts
│   ├── app.module.ts
│   ├── config/
│   │   ├── redis.config.ts
│   │   ├── sqs.config.ts
│   │   └── jwt.config.ts
│   ├── auth/
│   │   ├── auth.module.ts
│   │   ├── ws-auth.guard.ts
│   │   └── jwt.strategy.ts
│   ├── gateways/
│   │   ├── chat.gateway.ts
│   │   └── typing.gateway.ts
│   ├── services/
│   │   ├── redis-pub-sub.service.ts
│   │   └── sqs-publisher.service.ts
│   └── dto/
│       ├── send-message.dto.ts
│       └── join-conversation.dto.ts
├── test/
├── .env.example
├── Dockerfile
├── .dockerignore
└── README.md
```

### 5. Create Main Application

**File**: `src/main.ts`

```typescript
import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';
import { ConfigService } from '@nestjs/config';
import { RedisIoAdapter } from './config/redis.config';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);
  
  const configService = app.get(ConfigService);
  
  // Setup Redis adapter for WebSocket scaling
  const redisIoAdapter = new RedisIoAdapter(app);
  await redisIoAdapter.connectToRedis(configService);
  app.useWebSocketAdapter(redisIoAdapter);
  
  // Enable CORS
  app.enableCors({
    origin: configService.get<string>('ALLOWED_ORIGINS')?.split(',') || ['http://localhost:4200'],
    credentials: true,
  });
  
  // Health check endpoint
  app.getHttpAdapter().get('/health', (req, res) => {
    res.status(200).json({ status: 'ok' });
  });
  
  const port = configService.get<number>('PORT', 3000);
  await app.listen(port);
  console.log(`WebSocket Gateway running on port ${port}`);
}
bootstrap();
```

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

### 6. Create Configuration Files

**File**: `.env.example`

```env
# Server
PORT=3000
NODE_ENV=development

# JWT
JWT_SECRET=your-256-bit-secret

# Redis
REDIS_URL=redis://localhost:6379

# AWS
AWS_REGION=us-east-1
SQS_DIRECT_QUEUE_URL=http://localhost:4566/000000000000/communications-direct-messages.fifo
SQS_GROUP_QUEUE_URL=http://localhost:4566/000000000000/communications-group-messages.fifo

# CORS
ALLOWED_ORIGINS=http://localhost:4200
```

### 7. Create Dockerfile

**File**: `Dockerfile`

```dockerfile
FROM node:20-alpine AS builder

WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

FROM node:20-alpine

WORKDIR /app

COPY package*.json ./
RUN npm ci --only=production

COPY --from=builder /app/dist ./dist

EXPOSE 3000

CMD ["node", "dist/main"]
```

**File**: `.dockerignore`

```
node_modules/
dist/
.git/
.env
*.md
test/
```

### 8. Create README

**File**: `README.md`

```markdown
# WebSocket Gateway

Real-time WebSocket gateway for Turaf Communications.

## Features

- WebSocket connections with Socket.IO
- JWT authentication
- Redis Pub/Sub for horizontal scaling
- SQS FIFO queue publishing
- Typing indicators
- Real-time message broadcasting

## Tech Stack

- NestJS
- Socket.IO
- Redis
- AWS SQS

## Development

```bash
npm install
npm run start:dev
```

## Build

```bash
npm run build
```

## Docker

```bash
docker build -t turaf/ws-gateway:latest .
docker run -p 3000:3000 --env-file .env turaf/ws-gateway:latest
```

## Environment Variables

See `.env.example` for required configuration.
```

---

## Verification

1. Install dependencies:
   ```bash
   cd services/ws-gateway
   npm install
   ```

2. Build project:
   ```bash
   npm run build
   ```

3. Start development server:
   ```bash
   npm run start:dev
   ```

4. Verify health endpoint:
   ```bash
   curl http://localhost:3000/health
   ```

---

## References

- **Spec**: `specs/ws-gateway.md`
- **NestJS Docs**: https://docs.nestjs.com/websockets/gateways
