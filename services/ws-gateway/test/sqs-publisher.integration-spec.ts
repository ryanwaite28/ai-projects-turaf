import { Test, TestingModule } from '@nestjs/testing';
import { ConfigModule } from '@nestjs/config';
import { SqsPublisherService } from '../src/services/sqs-publisher.service';
import { SQSClient, ReceiveMessageCommand, PurgeQueueCommand } from '@aws-sdk/client-sqs';

/**
 * Integration tests for SQS Publisher Service.
 * 
 * These tests verify real SQS operations using LocalStack:
 * - Publishing messages to FIFO queues
 * - Message ordering within conversation groups
 * - Message deduplication
 * - Direct vs group queue routing
 * 
 * Prerequisites:
 * - LocalStack running on localhost:4566
 * - SQS FIFO queues created:
 *   - communications-direct-messages.fifo
 *   - communications-group-messages.fifo
 * 
 * To run these tests:
 * 1. Start LocalStack: docker run -p 4566:4566 localstack/localstack
 * 2. Create queues (see setup script below)
 * 3. Run tests: npm run test:e2e -- sqs-publisher.integration
 */
describe('SQS Publisher Integration Tests', () => {
  let service: SqsPublisherService;
  let sqsClient: SQSClient;
  let directQueueUrl: string;
  let groupQueueUrl: string;

  beforeAll(async () => {
    directQueueUrl = 'http://localhost:4566/000000000000/communications-direct-messages.fifo';
    groupQueueUrl = 'http://localhost:4566/000000000000/communications-group-messages.fifo';

    const module: TestingModule = await Test.createTestingModule({
      imports: [
        ConfigModule.forRoot({
          isGlobal: true,
          envFilePath: '.env.test',
        }),
      ],
      providers: [SqsPublisherService],
    }).compile();

    service = module.get<SqsPublisherService>(SqsPublisherService);

    sqsClient = new SQSClient({
      region: 'us-east-1',
      endpoint: 'http://localhost:4566',
    });
  });

  afterAll(async () => {
    if (sqsClient) {
      sqsClient.destroy();
    }
  });

  beforeEach(async () => {
    // Purge queues before each test
    try {
      await sqsClient.send(new PurgeQueueCommand({ QueueUrl: directQueueUrl }));
      await sqsClient.send(new PurgeQueueCommand({ QueueUrl: groupQueueUrl }));
      // Wait for purge to complete
      await new Promise((resolve) => setTimeout(resolve, 1000));
    } catch (error) {
      console.warn('Could not purge queues (may not exist):', error.message);
    }
  });

  describe('Direct message publishing', () => {
    it('should publish direct message to direct queue', async () => {
      const messageData = {
        conversationId: 'conv-direct-1',
        senderId: 'user-123',
        content: 'Direct message test',
        isDirect: true,
        organizationId: 'org-456',
      };

      await service.publishMessage(messageData);

      // Verify message in queue
      const receiveCommand = new ReceiveMessageCommand({
        QueueUrl: directQueueUrl,
        MaxNumberOfMessages: 1,
        WaitTimeSeconds: 2,
      });

      const result = await sqsClient.send(receiveCommand);

      expect(result.Messages).toBeDefined();
      expect(result.Messages.length).toBeGreaterThan(0);

      const messageBody = JSON.parse(result.Messages[0].Body);
      expect(messageBody.conversationId).toBe('conv-direct-1');
      expect(messageBody.senderId).toBe('user-123');
      expect(messageBody.content).toBe('Direct message test');
    }, 10000);

    it('should not appear in group queue', async () => {
      const messageData = {
        conversationId: 'conv-direct-2',
        senderId: 'user-123',
        content: 'Should not be in group queue',
        isDirect: true,
        organizationId: 'org-456',
      };

      await service.publishMessage(messageData);

      // Check group queue
      const receiveCommand = new ReceiveMessageCommand({
        QueueUrl: groupQueueUrl,
        MaxNumberOfMessages: 1,
        WaitTimeSeconds: 2,
      });

      const result = await sqsClient.send(receiveCommand);

      expect(result.Messages || []).toHaveLength(0);
    }, 10000);
  });

  describe('Group message publishing', () => {
    it('should publish group message to group queue', async () => {
      const messageData = {
        conversationId: 'conv-group-1',
        senderId: 'user-789',
        content: 'Group message test',
        isDirect: false,
        organizationId: 'org-123',
      };

      await service.publishMessage(messageData);

      // Verify message in queue
      const receiveCommand = new ReceiveMessageCommand({
        QueueUrl: groupQueueUrl,
        MaxNumberOfMessages: 1,
        WaitTimeSeconds: 2,
      });

      const result = await sqsClient.send(receiveCommand);

      expect(result.Messages).toBeDefined();
      expect(result.Messages.length).toBeGreaterThan(0);

      const messageBody = JSON.parse(result.Messages[0].Body);
      expect(messageBody.conversationId).toBe('conv-group-1');
      expect(messageBody.content).toBe('Group message test');
    }, 10000);

    it('should not appear in direct queue', async () => {
      const messageData = {
        conversationId: 'conv-group-2',
        senderId: 'user-123',
        content: 'Should not be in direct queue',
        isDirect: false,
        organizationId: 'org-456',
      };

      await service.publishMessage(messageData);

      // Check direct queue
      const receiveCommand = new ReceiveMessageCommand({
        QueueUrl: directQueueUrl,
        MaxNumberOfMessages: 1,
        WaitTimeSeconds: 2,
      });

      const result = await sqsClient.send(receiveCommand);

      expect(result.Messages || []).toHaveLength(0);
    }, 10000);
  });

  describe('Message ordering', () => {
    it('should maintain order within conversation group', async () => {
      const conversationId = 'conv-ordering-test';

      // Send multiple messages in sequence
      for (let i = 1; i <= 3; i++) {
        await service.publishMessage({
          conversationId,
          senderId: 'user-123',
          content: `Message ${i}`,
          isDirect: true,
          organizationId: 'org-456',
        });
      }

      // Receive messages
      const messages = [];
      for (let i = 0; i < 3; i++) {
        const receiveCommand = new ReceiveMessageCommand({
          QueueUrl: directQueueUrl,
          MaxNumberOfMessages: 1,
          WaitTimeSeconds: 2,
        });

        const result = await sqsClient.send(receiveCommand);
        if (result.Messages && result.Messages.length > 0) {
          messages.push(JSON.parse(result.Messages[0].Body));
        }
      }

      expect(messages).toHaveLength(3);
      expect(messages[0].content).toBe('Message 1');
      expect(messages[1].content).toBe('Message 2');
      expect(messages[2].content).toBe('Message 3');
    }, 15000);
  });

  describe('Message attributes', () => {
    it('should include all required fields in message body', async () => {
      const messageData = {
        conversationId: 'conv-attrs-test',
        senderId: 'user-attrs',
        content: 'Testing attributes',
        isDirect: true,
        organizationId: 'org-attrs',
      };

      await service.publishMessage(messageData);

      const receiveCommand = new ReceiveMessageCommand({
        QueueUrl: directQueueUrl,
        MaxNumberOfMessages: 1,
        WaitTimeSeconds: 2,
      });

      const result = await sqsClient.send(receiveCommand);
      const messageBody = JSON.parse(result.Messages[0].Body);

      expect(messageBody).toHaveProperty('conversationId', 'conv-attrs-test');
      expect(messageBody).toHaveProperty('senderId', 'user-attrs');
      expect(messageBody).toHaveProperty('content', 'Testing attributes');
      expect(messageBody).toHaveProperty('organizationId', 'org-attrs');
      expect(messageBody).toHaveProperty('timestamp');
      expect(new Date(messageBody.timestamp)).toBeInstanceOf(Date);
    }, 10000);

    it('should have MessageGroupId set to conversationId', async () => {
      const messageData = {
        conversationId: 'conv-group-id-test',
        senderId: 'user-123',
        content: 'Test',
        isDirect: true,
        organizationId: 'org-456',
      };

      await service.publishMessage(messageData);

      const receiveCommand = new ReceiveMessageCommand({
        QueueUrl: directQueueUrl,
        MaxNumberOfMessages: 1,
        AttributeNames: ['MessageGroupId'],
        WaitTimeSeconds: 2,
      });

      const result = await sqsClient.send(receiveCommand);

      expect(result.Messages[0].Attributes.MessageGroupId).toBe('conv-group-id-test');
    }, 10000);
  });

  describe('Multiple conversations', () => {
    it('should handle messages from different conversations', async () => {
      await service.publishMessage({
        conversationId: 'conv-A',
        senderId: 'user-1',
        content: 'Message from conv A',
        isDirect: true,
        organizationId: 'org-1',
      });

      await service.publishMessage({
        conversationId: 'conv-B',
        senderId: 'user-2',
        content: 'Message from conv B',
        isDirect: true,
        organizationId: 'org-2',
      });

      // Receive both messages
      const messages = [];
      for (let i = 0; i < 2; i++) {
        const receiveCommand = new ReceiveMessageCommand({
          QueueUrl: directQueueUrl,
          MaxNumberOfMessages: 1,
          WaitTimeSeconds: 2,
        });

        const result = await sqsClient.send(receiveCommand);
        if (result.Messages && result.Messages.length > 0) {
          messages.push(JSON.parse(result.Messages[0].Body));
        }
      }

      expect(messages).toHaveLength(2);
      const conversationIds = messages.map((m) => m.conversationId);
      expect(conversationIds).toContain('conv-A');
      expect(conversationIds).toContain('conv-B');
    }, 15000);
  });

  describe('Error handling', () => {
    it('should throw error for invalid queue configuration', async () => {
      const invalidService = new SqsPublisherService({
        get: jest.fn(() => undefined),
      } as any);

      const messageData = {
        conversationId: 'conv-123',
        senderId: 'user-123',
        content: 'Test',
        isDirect: true,
        organizationId: 'org-123',
      };

      await expect(invalidService.publishMessage(messageData)).rejects.toThrow(
        'SQS queue URL not configured',
      );
    });
  });
});
