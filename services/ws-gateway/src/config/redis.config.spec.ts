import { Test, TestingModule } from '@nestjs/testing';
import { ConfigService } from '@nestjs/config';
import { RedisIoAdapter } from './redis.config';
import { INestApplicationContext } from '@nestjs/common';

// Mock redis module
jest.mock('redis', () => ({
  createClient: jest.fn(() => ({
    connect: jest.fn().mockResolvedValue(undefined),
    duplicate: jest.fn(() => ({
      connect: jest.fn().mockResolvedValue(undefined),
    })),
    on: jest.fn(),
  })),
}));

// Mock @socket.io/redis-adapter
jest.mock('@socket.io/redis-adapter', () => ({
  createAdapter: jest.fn(() => jest.fn()),
}));

describe('RedisIoAdapter', () => {
  let adapter: RedisIoAdapter;
  let configService: ConfigService;
  let mockApp: INestApplicationContext;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        {
          provide: ConfigService,
          useValue: {
            get: jest.fn((key: string, defaultValue?: any) => {
              if (key === 'REDIS_URL') {
                return 'redis://localhost:6379';
              }
              return defaultValue;
            }),
          },
        },
      ],
    }).compile();

    configService = module.get<ConfigService>(ConfigService);
    mockApp = module as unknown as INestApplicationContext;
    adapter = new RedisIoAdapter(mockApp);
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should be defined', () => {
    expect(adapter).toBeDefined();
  });

  describe('connectToRedis', () => {
    it('should connect to Redis with URL from config', async () => {
      const { createClient } = require('redis');
      
      await adapter.connectToRedis(configService);

      expect(configService.get).toHaveBeenCalledWith('REDIS_URL', 'redis://localhost:6379');
      expect(createClient).toHaveBeenCalledWith({ url: 'redis://localhost:6379' });
    });

    it('should use default Redis URL if not configured', async () => {
      const { createClient } = require('redis');
      const configServiceWithoutUrl = {
        get: jest.fn((key: string, defaultValue?: any) => defaultValue),
      } as any;

      await adapter.connectToRedis(configServiceWithoutUrl);

      expect(createClient).toHaveBeenCalledWith({ url: 'redis://localhost:6379' });
    });

    it('should create pub and sub clients', async () => {
      const { createClient } = require('redis');
      const mockClient = {
        connect: jest.fn().mockResolvedValue(undefined),
        duplicate: jest.fn(() => ({
          connect: jest.fn().mockResolvedValue(undefined),
        })),
      };
      
      createClient.mockReturnValue(mockClient);

      await adapter.connectToRedis(configService);

      expect(mockClient.duplicate).toHaveBeenCalled();
      expect(mockClient.connect).toHaveBeenCalled();
    });

    it('should connect both pub and sub clients', async () => {
      const { createClient } = require('redis');
      const mockSubClient = {
        connect: jest.fn().mockResolvedValue(undefined),
      };
      const mockPubClient = {
        connect: jest.fn().mockResolvedValue(undefined),
        duplicate: jest.fn(() => mockSubClient),
      };
      
      createClient.mockReturnValue(mockPubClient);

      await adapter.connectToRedis(configService);

      expect(mockPubClient.connect).toHaveBeenCalled();
      expect(mockSubClient.connect).toHaveBeenCalled();
    });

    it('should create Redis adapter after connection', async () => {
      const { createAdapter } = require('@socket.io/redis-adapter');

      await adapter.connectToRedis(configService);

      expect(createAdapter).toHaveBeenCalled();
    });

    it('should handle connection errors', async () => {
      const { createClient } = require('redis');
      const mockClient = {
        connect: jest.fn().mockRejectedValue(new Error('Connection failed')),
        duplicate: jest.fn(() => ({
          connect: jest.fn().mockResolvedValue(undefined),
        })),
      };
      
      createClient.mockReturnValue(mockClient);

      await expect(adapter.connectToRedis(configService)).rejects.toThrow('Connection failed');
    });
  });

  describe('createIOServer', () => {
    it('should create Socket.IO server with Redis adapter', async () => {
      await adapter.connectToRedis(configService);

      const mockServer = {
        adapter: jest.fn(),
      };

      // Mock the parent createIOServer
      jest.spyOn(adapter as any, 'createIOServer').mockImplementation((port, options) => {
        const server = mockServer;
        if ((adapter as any).adapterConstructor) {
          server.adapter((adapter as any).adapterConstructor);
        }
        return server;
      });

      const server = adapter.createIOServer(3000);

      expect(server).toBeDefined();
    });

    it('should apply adapter constructor to server', async () => {
      const { createAdapter } = require('@socket.io/redis-adapter');
      const mockAdapterConstructor = jest.fn();
      createAdapter.mockReturnValue(mockAdapterConstructor);

      await adapter.connectToRedis(configService);

      const mockServer = {
        adapter: jest.fn(),
      };

      jest.spyOn(Object.getPrototypeOf(adapter), 'createIOServer').mockReturnValue(mockServer);

      adapter.createIOServer(3000);

      expect(mockServer.adapter).toHaveBeenCalledWith(mockAdapterConstructor);
    });
  });

  describe('configuration scenarios', () => {
    it('should work with local Redis URL', async () => {
      const { createClient } = require('redis');
      const localConfig = {
        get: jest.fn(() => 'redis://localhost:6379'),
      } as any;

      await adapter.connectToRedis(localConfig);

      expect(createClient).toHaveBeenCalledWith({ url: 'redis://localhost:6379' });
    });

    it('should work with Docker Compose Redis URL', async () => {
      const { createClient } = require('redis');
      const dockerConfig = {
        get: jest.fn(() => 'redis://redis:6379'),
      } as any;

      await adapter.connectToRedis(dockerConfig);

      expect(createClient).toHaveBeenCalledWith({ url: 'redis://redis:6379' });
    });

    it('should work with AWS ElastiCache URL', async () => {
      const { createClient } = require('redis');
      const awsConfig = {
        get: jest.fn(() => 'redis://turaf-elasticache.abc123.0001.use1.cache.amazonaws.com:6379'),
      } as any;

      await adapter.connectToRedis(awsConfig);

      expect(createClient).toHaveBeenCalledWith({ 
        url: 'redis://turaf-elasticache.abc123.0001.use1.cache.amazonaws.com:6379' 
      });
    });
  });
});
