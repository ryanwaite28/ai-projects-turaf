import { IoAdapter } from '@nestjs/platform-socket.io';
import { ServerOptions } from 'socket.io';
import { createAdapter } from '@socket.io/redis-adapter';
import { createClient } from 'redis';
import { ConfigService } from '@nestjs/config';

/**
 * Redis adapter for Socket.IO to enable horizontal scaling.
 * 
 * This adapter uses Redis Pub/Sub to broadcast events across multiple
 * WebSocket Gateway instances, allowing clients connected to different
 * instances to communicate with each other.
 * 
 * Configuration:
 * - Local: REDIS_URL=redis://localhost:6379
 * - AWS: REDIS_URL=redis://elasticache-endpoint:6379
 */
export class RedisIoAdapter extends IoAdapter {
  private adapterConstructor: ReturnType<typeof createAdapter>;

  async connectToRedis(configService: ConfigService): Promise<void> {
    const redisUrl = configService.get<string>('REDIS_URL', 'redis://localhost:6379');
    
    console.log(`Connecting to Redis at ${redisUrl}`);
    
    const pubClient = createClient({ url: redisUrl });
    const subClient = pubClient.duplicate();

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
    return server;
  }
}
