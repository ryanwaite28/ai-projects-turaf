import { Injectable, OnModuleInit, OnModuleDestroy } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { createClient, RedisClientType } from 'redis';
import { Logger } from '@nestjs/common';

/**
 * Redis Pub/Sub service for cross-instance event broadcasting.
 * 
 * This service is used by the Socket.IO Redis adapter to broadcast
 * events across multiple WebSocket Gateway instances.
 * 
 * Note: The actual Pub/Sub is handled by the Redis adapter in
 * redis.config.ts. This service is for any additional Redis
 * operations that might be needed.
 */
@Injectable()
export class RedisPubSubService implements OnModuleInit, OnModuleDestroy {
  private readonly logger = new Logger(RedisPubSubService.name);
  private redisClient: RedisClientType;

  constructor(private configService: ConfigService) {}

  async onModuleInit() {
    const redisUrl = this.configService.get<string>('REDIS_URL', 'redis://localhost:6379');
    
    this.redisClient = createClient({ url: redisUrl });
    
    this.redisClient.on('error', (err) => {
      this.logger.error('Redis Client Error', err);
    });

    await this.redisClient.connect();
    this.logger.log('Redis Pub/Sub service initialized');
  }

  async onModuleDestroy() {
    if (this.redisClient) {
      await this.redisClient.quit();
      this.logger.log('Redis Pub/Sub service destroyed');
    }
  }

  async publish(channel: string, message: string): Promise<void> {
    await this.redisClient.publish(channel, message);
  }

  async subscribe(channel: string, callback: (message: string) => void): Promise<void> {
    await this.redisClient.subscribe(channel, callback);
  }
}
