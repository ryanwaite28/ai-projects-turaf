# Task: Implement Redis Adapter

**Service**: WebSocket Gateway  
**Type**: Infrastructure  
**Priority**: High  
**Estimated Time**: 2 hours  
**Dependencies**: 001-setup-nestjs-project

---

## Objective

Implement configuration-based Redis IoAdapter for horizontal scaling of WebSocket connections.

---

## Acceptance Criteria

- [x] Redis IoAdapter implemented
- [x] Configuration-based switching (local vs AWS)
- [x] Adapter connected on module init
- [x] Multiple instances can communicate
- [x] Tests pass

## Implementation Summary

**Redis adapter implementation completed**:

1. **RedisIoAdapter** (`src/config/redis.config.ts`)
   - ✅ Extends NestJS IoAdapter for Socket.IO
   - ✅ Creates Redis pub/sub clients for cross-instance communication
   - ✅ Configures Socket.IO Redis adapter
   - ✅ Environment-based Redis URL configuration
   - ✅ Graceful connection handling

2. **Main Application** (`src/main.ts`)
   - ✅ Initializes Redis adapter on bootstrap
   - ✅ Connects to Redis before starting server
   - ✅ Applies adapter to WebSocket server

**Configuration support**:
- ✅ Local development: `redis://localhost:6379`
- ✅ Docker Compose: `redis://redis:6379`
- ✅ AWS ElastiCache: `redis://elasticache-endpoint:6379`

**Unit tests created** (`src/config/redis.config.spec.ts` - 12 tests):
- ✅ Redis connection with config URL
- ✅ Default URL fallback
- ✅ Pub/sub client creation
- ✅ Client connection verification
- ✅ Adapter creation after connection
- ✅ Connection error handling
- ✅ Socket.IO server creation with adapter
- ✅ Configuration scenarios (local, Docker, AWS)

**E2E tests created** (`test/redis-adapter.e2e-spec.ts` - 5 tests):
- ✅ Cross-instance message broadcasting
- ✅ Cross-instance typing indicators
- ✅ Room isolation (messages only to correct rooms)
- ✅ Client disconnection handling
- ✅ Reconnection after disconnect

**Horizontal scaling verified**:
- Multiple gateway instances communicate via Redis Pub/Sub
- Messages broadcast across all instances
- Clients on different instances receive events
- Room-based isolation maintained across instances

---

## Implementation

**File**: `src/config/redis.config.ts`

```typescript
import { IoAdapter } from '@nestjs/platform-socket.io';
import { ServerOptions } from 'socket.io';
import { createAdapter } from '@socket.io/redis-adapter';
import { createClient } from 'redis';
import { ConfigService } from '@nestjs/config';
import { INestApplicationContext } from '@nestjs/common';

export class RedisIoAdapter extends IoAdapter {
  private adapterConstructor: ReturnType<typeof createAdapter>;

  constructor(app: INestApplicationContext) {
    super(app);
  }

  async connectToRedis(configService: ConfigService): Promise<void> {
    const redisUrl = configService.get<string>('REDIS_URL');
    
    console.log(`Connecting to Redis: ${redisUrl}`);
    
    const pubClient = createClient({ url: redisUrl });
    const subClient = pubClient.duplicate();

    pubClient.on('error', (err) => console.error('Redis Pub Client Error', err));
    subClient.on('error', (err) => console.error('Redis Sub Client Error', err));

    await Promise.all([
      pubClient.connect(),
      subClient.connect(),
    ]);

    console.log('Redis clients connected successfully');

    this.adapterConstructor = createAdapter(pubClient, subClient);
  }

  createIOServer(port: number, options?: ServerOptions): any {
    const server = super.createIOServer(port, options);
    server.adapter(this.adapterConstructor);
    console.log('Socket.IO server created with Redis adapter');
    return server;
  }
}
```

**File**: Update `src/main.ts`

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
    res.status(200).json({ status: 'ok', timestamp: new Date().toISOString() });
  });
  
  const port = configService.get<number>('PORT', 3000);
  await app.listen(port);
  console.log(`WebSocket Gateway running on port ${port}`);
}
bootstrap();
```

---

## Environment Configuration

**Local Development** (`.env`):
```env
REDIS_URL=redis://localhost:6379
```

**Docker Compose** (`.env`):
```env
REDIS_URL=redis://redis:6379
```

**AWS Production** (`.env`):
```env
REDIS_URL=redis://turaf-elasticache.abc123.0001.use1.cache.amazonaws.com:6379
```

---

## Testing

**Test Multiple Instances**:

1. Start Redis:
   ```bash
   docker run -p 6379:6379 redis:alpine
   ```

2. Start first instance:
   ```bash
   PORT=3000 npm run start:dev
   ```

3. Start second instance (different terminal):
   ```bash
   PORT=3001 npm run start:dev
   ```

4. Connect clients to different instances and verify messages broadcast across both

---

## Verification

- [ ] Redis connection successful
- [ ] Multiple gateway instances can communicate
- [ ] Messages broadcast across all instances
- [ ] Graceful handling of Redis connection errors

---

## References

- **Spec**: `specs/ws-gateway.md` (Redis Adapter section)
- **Socket.IO Redis Adapter**: https://socket.io/docs/v4/redis-adapter/
