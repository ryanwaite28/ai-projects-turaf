import { Test, TestingModule } from '@nestjs/testing';
import { ConfigService } from '@nestjs/config';
import { SqsPublisherService } from './sqs-publisher.service';
import { SQSClient, SendMessageCommand } from '@aws-sdk/client-sqs';

jest.mock('@aws-sdk/client-sqs');
jest.mock('uuid', () => ({
  v4: jest.fn(() => 'mock-uuid-1234'),
}));

describe('SqsPublisherService', () => {
  let service: SqsPublisherService;
  let configService: ConfigService;
  let mockSend: jest.Mock;

  beforeEach(async () => {
    mockSend = jest.fn().mockResolvedValue({ MessageId: 'msg-123' });

    (SQSClient as jest.MockedClass<typeof SQSClient>).mockImplementation(
      () =>
        ({
          send: mockSend,
        } as any),
    );

    const module: TestingModule = await Test.createTestingModule({
      providers: [
        SqsPublisherService,
        {
          provide: ConfigService,
          useValue: {
            get: jest.fn((key: string, defaultValue?: any) => {
              const config = {
                AWS_REGION: 'us-east-1',
                AWS_ENDPOINT: 'http://localhost:4566',
                SQS_DIRECT_QUEUE_URL: 'http://localhost:4566/000000000000/direct.fifo',
                SQS_GROUP_QUEUE_URL: 'http://localhost:4566/000000000000/group.fifo',
              };
              return config[key] || defaultValue;
            }),
          },
        },
      ],
    }).compile();

    service = module.get<SqsPublisherService>(SqsPublisherService);
    configService = module.get<ConfigService>(ConfigService);
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should be defined', () => {
    expect(service).toBeDefined();
  });

  describe('constructor', () => {
    it('should initialize SQS client with region', () => {
      expect(SQSClient).toHaveBeenCalledWith(
        expect.objectContaining({
          region: 'us-east-1',
        }),
      );
    });

    it('should initialize SQS client with endpoint for LocalStack', () => {
      expect(SQSClient).toHaveBeenCalledWith(
        expect.objectContaining({
          endpoint: 'http://localhost:4566',
        }),
      );
    });

    it('should load queue URLs from config', () => {
      expect(configService.get).toHaveBeenCalledWith('SQS_DIRECT_QUEUE_URL');
      expect(configService.get).toHaveBeenCalledWith('SQS_GROUP_QUEUE_URL');
    });
  });

  describe('publishMessage', () => {
    it('should publish direct message to direct queue', async () => {
      const messageData = {
        conversationId: 'conv-123',
        senderId: 'user-456',
        content: 'Hello World',
        isDirect: true,
        organizationId: 'org-789',
      };

      await service.publishMessage(messageData);

      expect(mockSend).toHaveBeenCalledWith(
        expect.objectContaining({
          input: expect.objectContaining({
            QueueUrl: 'http://localhost:4566/000000000000/direct.fifo',
            MessageGroupId: 'conv-123',
            MessageDeduplicationId: 'mock-uuid-1234',
          }),
        }),
      );
    });

    it('should publish group message to group queue', async () => {
      const messageData = {
        conversationId: 'conv-456',
        senderId: 'user-789',
        content: 'Group message',
        isDirect: false,
        organizationId: 'org-123',
      };

      await service.publishMessage(messageData);

      expect(mockSend).toHaveBeenCalledWith(
        expect.objectContaining({
          input: expect.objectContaining({
            QueueUrl: 'http://localhost:4566/000000000000/group.fifo',
            MessageGroupId: 'conv-456',
          }),
        }),
      );
    });

    it('should include all message data in body', async () => {
      const messageData = {
        conversationId: 'conv-test',
        senderId: 'user-test',
        content: 'Test content',
        isDirect: true,
        organizationId: 'org-test',
      };

      await service.publishMessage(messageData);

      const sentCommand = mockSend.mock.calls[0][0];
      const messageBody = JSON.parse(sentCommand.input.MessageBody);

      expect(messageBody).toMatchObject({
        conversationId: 'conv-test',
        senderId: 'user-test',
        content: 'Test content',
        organizationId: 'org-test',
      });
      expect(messageBody.timestamp).toBeDefined();
    });

    it('should use conversationId as MessageGroupId for ordering', async () => {
      const messageData = {
        conversationId: 'conv-ordering-test',
        senderId: 'user-123',
        content: 'Message 1',
        isDirect: true,
        organizationId: 'org-123',
      };

      await service.publishMessage(messageData);

      expect(mockSend).toHaveBeenCalledWith(
        expect.objectContaining({
          input: expect.objectContaining({
            MessageGroupId: 'conv-ordering-test',
          }),
        }),
      );
    });

    it('should use UUID for MessageDeduplicationId', async () => {
      const messageData = {
        conversationId: 'conv-123',
        senderId: 'user-123',
        content: 'Test',
        isDirect: true,
        organizationId: 'org-123',
      };

      await service.publishMessage(messageData);

      expect(mockSend).toHaveBeenCalledWith(
        expect.objectContaining({
          input: expect.objectContaining({
            MessageDeduplicationId: 'mock-uuid-1234',
          }),
        }),
      );
    });

    it('should include timestamp in message body', async () => {
      const beforeTime = new Date().toISOString();

      const messageData = {
        conversationId: 'conv-123',
        senderId: 'user-123',
        content: 'Test',
        isDirect: true,
        organizationId: 'org-123',
      };

      await service.publishMessage(messageData);

      const sentCommand = mockSend.mock.calls[0][0];
      const messageBody = JSON.parse(sentCommand.input.MessageBody);

      expect(messageBody.timestamp).toBeDefined();
      expect(new Date(messageBody.timestamp).getTime()).toBeGreaterThanOrEqual(
        new Date(beforeTime).getTime(),
      );
    });

    it('should log successful message publication', async () => {
      const loggerSpy = jest.spyOn(service['logger'], 'log');

      const messageData = {
        conversationId: 'conv-log-test',
        senderId: 'user-123',
        content: 'Test',
        isDirect: true,
        organizationId: 'org-123',
      };

      await service.publishMessage(messageData);

      expect(loggerSpy).toHaveBeenCalledWith(
        expect.stringContaining('Message published to SQS'),
      );
    });

    it('should throw error if direct queue URL not configured', async () => {
      const serviceWithoutQueue = new SqsPublisherService({
        get: jest.fn((key: string) => {
          if (key === 'SQS_DIRECT_QUEUE_URL') return undefined;
          if (key === 'AWS_REGION') return 'us-east-1';
          return undefined;
        }),
      } as any);

      const messageData = {
        conversationId: 'conv-123',
        senderId: 'user-123',
        content: 'Test',
        isDirect: true,
        organizationId: 'org-123',
      };

      await expect(serviceWithoutQueue.publishMessage(messageData)).rejects.toThrow(
        'SQS queue URL not configured',
      );
    });

    it('should throw error if group queue URL not configured', async () => {
      const serviceWithoutQueue = new SqsPublisherService({
        get: jest.fn((key: string) => {
          if (key === 'SQS_GROUP_QUEUE_URL') return undefined;
          if (key === 'AWS_REGION') return 'us-east-1';
          return undefined;
        }),
      } as any);

      const messageData = {
        conversationId: 'conv-123',
        senderId: 'user-123',
        content: 'Test',
        isDirect: false,
        organizationId: 'org-123',
      };

      await expect(serviceWithoutQueue.publishMessage(messageData)).rejects.toThrow(
        'SQS queue URL not configured',
      );
    });

    it('should log error and rethrow on SQS failure', async () => {
      const sqsError = new Error('SQS service unavailable');
      mockSend.mockRejectedValue(sqsError);

      const loggerSpy = jest.spyOn(service['logger'], 'error');

      const messageData = {
        conversationId: 'conv-123',
        senderId: 'user-123',
        content: 'Test',
        isDirect: true,
        organizationId: 'org-123',
      };

      await expect(service.publishMessage(messageData)).rejects.toThrow(
        'SQS service unavailable',
      );

      expect(loggerSpy).toHaveBeenCalledWith(
        expect.stringContaining('Failed to publish message to SQS'),
        expect.any(String),
      );
    });

    it('should handle network errors gracefully', async () => {
      mockSend.mockRejectedValue(new Error('Network timeout'));

      const messageData = {
        conversationId: 'conv-123',
        senderId: 'user-123',
        content: 'Test',
        isDirect: true,
        organizationId: 'org-123',
      };

      await expect(service.publishMessage(messageData)).rejects.toThrow('Network timeout');
    });

    it('should serialize complex message content', async () => {
      const messageData = {
        conversationId: 'conv-123',
        senderId: 'user-123',
        content: 'Message with special chars: <>&"\'',
        isDirect: true,
        organizationId: 'org-123',
      };

      await service.publishMessage(messageData);

      const sentCommand = mockSend.mock.calls[0][0];
      const messageBody = JSON.parse(sentCommand.input.MessageBody);

      expect(messageBody.content).toBe('Message with special chars: <>&"\'');
    });
  });

  describe('queue selection', () => {
    it('should select direct queue when isDirect is true', async () => {
      const messageData = {
        conversationId: 'conv-123',
        senderId: 'user-123',
        content: 'Test',
        isDirect: true,
        organizationId: 'org-123',
      };

      await service.publishMessage(messageData);

      expect(mockSend).toHaveBeenCalledWith(
        expect.objectContaining({
          input: expect.objectContaining({
            QueueUrl: expect.stringContaining('direct.fifo'),
          }),
        }),
      );
    });

    it('should select group queue when isDirect is false', async () => {
      const messageData = {
        conversationId: 'conv-123',
        senderId: 'user-123',
        content: 'Test',
        isDirect: false,
        organizationId: 'org-123',
      };

      await service.publishMessage(messageData);

      expect(mockSend).toHaveBeenCalledWith(
        expect.objectContaining({
          input: expect.objectContaining({
            QueueUrl: expect.stringContaining('group.fifo'),
          }),
        }),
      );
    });
  });
});
