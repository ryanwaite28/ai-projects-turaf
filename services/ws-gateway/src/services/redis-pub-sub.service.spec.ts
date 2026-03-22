import { Test, TestingModule } from '@nestjs/testing';
import { ConfigService } from '@nestjs/config';
import { RedisPubSubService } from './redis-pub-sub.service';
import { createClient } from 'redis';

jest.mock('redis');

describe('RedisPubSubService', () => {
  let service: RedisPubSubService;
  let mockRedisClient: any;

  beforeEach(async () => {
    mockRedisClient = {
      connect: jest.fn().mockResolvedValue(undefined),
      quit: jest.fn().mockResolvedValue(undefined),
      on: jest.fn(),
      publish: jest.fn().mockResolvedValue(1),
      subscribe: jest.fn().mockResolvedValue(undefined),
    };

    (createClient as jest.Mock).mockReturnValue(mockRedisClient);

    const module: TestingModule = await Test.createTestingModule({
      providers: [
        RedisPubSubService,
        {
          provide: ConfigService,
          useValue: {
            get: jest.fn((key: string, defaultValue?: any) => {
              if (key === 'REDIS_URL') return 'redis://localhost:6379';
              return defaultValue;
            }),
          },
        },
      ],
    }).compile();

    service = module.get<RedisPubSubService>(RedisPubSubService);
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should be defined', () => {
    expect(service).toBeDefined();
  });

  describe('onModuleInit', () => {
    it('should create Redis client with URL from config', async () => {
      await service.onModuleInit();

      expect(createClient).toHaveBeenCalledWith({
        url: 'redis://localhost:6379',
      });
    });

    it('should use default Redis URL if not configured', async () => {
      const serviceWithDefault = new RedisPubSubService({
        get: jest.fn(() => undefined),
      } as any);

      (createClient as jest.Mock).mockClear();
      await serviceWithDefault.onModuleInit();

      expect(createClient).toHaveBeenCalledWith({
        url: 'redis://localhost:6379',
      });
    });

    it('should register error handler', async () => {
      await service.onModuleInit();

      expect(mockRedisClient.on).toHaveBeenCalledWith('error', expect.any(Function));
    });

    it('should connect to Redis', async () => {
      await service.onModuleInit();

      expect(mockRedisClient.connect).toHaveBeenCalled();
    });

    it('should log initialization', async () => {
      const loggerSpy = jest.spyOn(service['logger'], 'log');

      await service.onModuleInit();

      expect(loggerSpy).toHaveBeenCalledWith('Redis Pub/Sub service initialized');
    });

    it('should handle connection errors', async () => {
      const connectionError = new Error('Connection failed');
      mockRedisClient.connect.mockRejectedValue(connectionError);

      await expect(service.onModuleInit()).rejects.toThrow('Connection failed');
    });

    it('should log Redis client errors', async () => {
      const loggerSpy = jest.spyOn(service['logger'], 'error');
      await service.onModuleInit();

      const errorHandler = mockRedisClient.on.mock.calls.find(
        (call) => call[0] === 'error',
      )[1];

      const testError = new Error('Redis error');
      errorHandler(testError);

      expect(loggerSpy).toHaveBeenCalledWith('Redis Client Error', testError);
    });
  });

  describe('onModuleDestroy', () => {
    it('should quit Redis client', async () => {
      await service.onModuleInit();
      await service.onModuleDestroy();

      expect(mockRedisClient.quit).toHaveBeenCalled();
    });

    it('should log destruction', async () => {
      await service.onModuleInit();

      const loggerSpy = jest.spyOn(service['logger'], 'log');
      await service.onModuleDestroy();

      expect(loggerSpy).toHaveBeenCalledWith('Redis Pub/Sub service destroyed');
    });

    it('should handle missing client gracefully', async () => {
      await expect(service.onModuleDestroy()).resolves.not.toThrow();
    });

    it('should handle quit errors gracefully', async () => {
      await service.onModuleInit();
      mockRedisClient.quit.mockRejectedValue(new Error('Quit failed'));

      await expect(service.onModuleDestroy()).rejects.toThrow('Quit failed');
    });
  });

  describe('publish', () => {
    beforeEach(async () => {
      await service.onModuleInit();
    });

    it('should publish message to channel', async () => {
      const channel = 'test-channel';
      const message = 'test-message';

      await service.publish(channel, message);

      expect(mockRedisClient.publish).toHaveBeenCalledWith(channel, message);
    });

    it('should publish JSON message', async () => {
      const channel = 'events';
      const message = JSON.stringify({ type: 'test', data: 'value' });

      await service.publish(channel, message);

      expect(mockRedisClient.publish).toHaveBeenCalledWith(channel, message);
    });

    it('should handle publish errors', async () => {
      mockRedisClient.publish.mockRejectedValue(new Error('Publish failed'));

      await expect(service.publish('channel', 'message')).rejects.toThrow(
        'Publish failed',
      );
    });

    it('should publish to multiple channels', async () => {
      await service.publish('channel1', 'message1');
      await service.publish('channel2', 'message2');

      expect(mockRedisClient.publish).toHaveBeenCalledTimes(2);
      expect(mockRedisClient.publish).toHaveBeenNthCalledWith(1, 'channel1', 'message1');
      expect(mockRedisClient.publish).toHaveBeenNthCalledWith(2, 'channel2', 'message2');
    });

    it('should handle empty message', async () => {
      await service.publish('channel', '');

      expect(mockRedisClient.publish).toHaveBeenCalledWith('channel', '');
    });
  });

  describe('subscribe', () => {
    beforeEach(async () => {
      await service.onModuleInit();
    });

    it('should subscribe to channel with callback', async () => {
      const channel = 'test-channel';
      const callback = jest.fn();

      await service.subscribe(channel, callback);

      expect(mockRedisClient.subscribe).toHaveBeenCalledWith(channel, callback);
    });

    it('should handle subscription errors', async () => {
      mockRedisClient.subscribe.mockRejectedValue(new Error('Subscribe failed'));

      const callback = jest.fn();
      await expect(service.subscribe('channel', callback)).rejects.toThrow(
        'Subscribe failed',
      );
    });

    it('should subscribe to multiple channels', async () => {
      const callback1 = jest.fn();
      const callback2 = jest.fn();

      await service.subscribe('channel1', callback1);
      await service.subscribe('channel2', callback2);

      expect(mockRedisClient.subscribe).toHaveBeenCalledTimes(2);
      expect(mockRedisClient.subscribe).toHaveBeenNthCalledWith(1, 'channel1', callback1);
      expect(mockRedisClient.subscribe).toHaveBeenNthCalledWith(2, 'channel2', callback2);
    });

    it('should invoke callback when message received', async () => {
      const callback = jest.fn();
      mockRedisClient.subscribe.mockImplementation((channel, cb) => {
        cb('test-message');
        return Promise.resolve();
      });

      await service.subscribe('channel', callback);

      expect(callback).toHaveBeenCalledWith('test-message');
    });
  });

  describe('integration scenarios', () => {
    beforeEach(async () => {
      await service.onModuleInit();
    });

    it('should support pub/sub pattern', async () => {
      const channel = 'events';
      const message = 'event-data';
      const callback = jest.fn();

      await service.subscribe(channel, callback);
      await service.publish(channel, message);

      expect(mockRedisClient.subscribe).toHaveBeenCalledWith(channel, callback);
      expect(mockRedisClient.publish).toHaveBeenCalledWith(channel, message);
    });

    it('should handle lifecycle correctly', async () => {
      expect(mockRedisClient.connect).toHaveBeenCalled();

      await service.onModuleDestroy();

      expect(mockRedisClient.quit).toHaveBeenCalled();
    });
  });

  describe('configuration', () => {
    it('should support custom Redis URL', async () => {
      const customService = new RedisPubSubService({
        get: jest.fn(() => 'redis://custom-host:6380'),
      } as any);

      (createClient as jest.Mock).mockClear();
      await customService.onModuleInit();

      expect(createClient).toHaveBeenCalledWith({
        url: 'redis://custom-host:6380',
      });
    });

    it('should support Redis with authentication', async () => {
      const authService = new RedisPubSubService({
        get: jest.fn(() => 'redis://user:password@host:6379'),
      } as any);

      (createClient as jest.Mock).mockClear();
      await authService.onModuleInit();

      expect(createClient).toHaveBeenCalledWith({
        url: 'redis://user:password@host:6379',
      });
    });
  });
});
