# Task: Implement SQS Publisher

**Service**: WebSocket Gateway  
**Type**: Infrastructure Service  
**Priority**: High  
**Estimated Time**: 2 hours  
**Dependencies**: 001-setup-nestjs-project

---

## Objective

Implement the SQS publisher service to publish messages to AWS SQS FIFO queues with proper message grouping and deduplication.

---

## Acceptance Criteria

- [ ] SqsPublisherService implemented
- [ ] FIFO queue support with MessageGroupId
- [ ] Message deduplication using message ID
- [ ] Error handling and logging
- [ ] LocalStack support for local development
- [ ] Tests pass

---

## Implementation

**File**: `src/services/sqs-publisher.service.ts`

```typescript
import { Injectable, OnModuleInit } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { SQSClient, SendMessageCommand } from '@aws-sdk/client-sqs';

@Injectable()
export class SqsPublisherService implements OnModuleInit {
  private sqsClient: SQSClient;

  constructor(private configService: ConfigService) {}

  onModuleInit() {
    const region = this.configService.get<string>('AWS_REGION', 'us-east-1');
    const endpoint = this.configService.get<string>('AWS_SQS_ENDPOINT');

    this.sqsClient = new SQSClient({
      region,
      ...(endpoint && { endpoint }), // LocalStack support
    });

    console.log('SQS Publisher initialized');
  }

  async publishMessage(
    queueUrl: string,
    message: any,
    messageGroupId: string,
  ): Promise<void> {
    try {
      const command = new SendMessageCommand({
        QueueUrl: queueUrl,
        MessageBody: JSON.stringify(message),
        MessageGroupId: messageGroupId, // Ensures ordering per conversation
        MessageDeduplicationId: message.id, // Prevents duplicates
      });

      const response = await this.sqsClient.send(command);

      console.log(`Message published to SQS: ${message.id}`, {
        messageId: response.MessageId,
        queueUrl,
        messageGroupId,
      });
    } catch (error) {
      console.error('Error publishing message to SQS:', error);
      throw new Error(`Failed to publish message to SQS: ${error.message}`);
    }
  }
}
```

**File**: `src/config/sqs.config.ts`

```typescript
import { ConfigService } from '@nestjs/config';

export class SqsConfig {
  static getDirectQueueUrl(configService: ConfigService): string {
    return configService.get<string>('SQS_DIRECT_QUEUE_URL');
  }

  static getGroupQueueUrl(configService: ConfigService): string {
    return configService.get<string>('SQS_GROUP_QUEUE_URL');
  }
}
```

---

## Environment Configuration

**File**: `.env`

```env
# AWS Configuration
AWS_REGION=us-east-1
AWS_SQS_ENDPOINT=http://localhost:4566  # LocalStack (remove for production)

# SQS Queue URLs
SQS_DIRECT_QUEUE_URL=http://localhost:4566/000000000000/communications-direct-messages.fifo
SQS_GROUP_QUEUE_URL=http://localhost:4566/000000000000/communications-group-messages.fifo
```

**Production** (AWS):
```env
AWS_REGION=us-east-1
# AWS_SQS_ENDPOINT not set (uses default AWS endpoints)

SQS_DIRECT_QUEUE_URL=https://sqs.us-east-1.amazonaws.com/123456789/communications-direct-messages.fifo
SQS_GROUP_QUEUE_URL=https://sqs.us-east-1.amazonaws.com/123456789/communications-group-messages.fifo
```

---

## Testing

**File**: `src/services/sqs-publisher.service.spec.ts`

```typescript
import { Test, TestingModule } from '@nestjs/testing';
import { ConfigService } from '@nestjs/config';
import { SqsPublisherService } from './sqs-publisher.service';
import { SQSClient, SendMessageCommand } from '@aws-sdk/client-sqs';

jest.mock('@aws-sdk/client-sqs');

describe('SqsPublisherService', () => {
  let service: SqsPublisherService;
  let sqsClient: jest.Mocked<SQSClient>;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        SqsPublisherService,
        {
          provide: ConfigService,
          useValue: {
            get: jest.fn((key: string, defaultValue?: any) => {
              const config = {
                AWS_REGION: 'us-east-1',
                AWS_SQS_ENDPOINT: 'http://localhost:4566',
              };
              return config[key] || defaultValue;
            }),
          },
        },
      ],
    }).compile();

    service = module.get<SqsPublisherService>(SqsPublisherService);
    service.onModuleInit();
  });

  it('should publish message to SQS', async () => {
    const mockSend = jest.fn().mockResolvedValue({ MessageId: 'msg-123' });
    (SQSClient as jest.Mock).mockImplementation(() => ({
      send: mockSend,
    }));

    const message = {
      id: 'test-msg-1',
      conversationId: 'conv-123',
      senderId: 'user-123',
      content: 'Hello',
    };

    await service.publishMessage(
      'http://localhost:4566/queue-url',
      message,
      'conv-123',
    );

    expect(mockSend).toHaveBeenCalled();
  });

  it('should throw error on SQS failure', async () => {
    const mockSend = jest.fn().mockRejectedValue(new Error('SQS Error'));
    (SQSClient as jest.Mock).mockImplementation(() => ({
      send: mockSend,
    }));

    const message = { id: 'test-msg-1', content: 'Hello' };

    await expect(
      service.publishMessage('queue-url', message, 'conv-123'),
    ).rejects.toThrow('Failed to publish message to SQS');
  });
});
```

---

## Integration Testing with LocalStack

**Setup LocalStack SQS Queues**:

```bash
# Create FIFO queues in LocalStack
aws --endpoint-url=http://localhost:4566 sqs create-queue \
  --queue-name communications-direct-messages.fifo \
  --attributes FifoQueue=true,ContentBasedDeduplication=false

aws --endpoint-url=http://localhost:4566 sqs create-queue \
  --queue-name communications-group-messages.fifo \
  --attributes FifoQueue=true,ContentBasedDeduplication=false
```

**Verify Message in Queue**:

```bash
# Receive messages from queue
aws --endpoint-url=http://localhost:4566 sqs receive-message \
  --queue-url http://localhost:4566/000000000000/communications-direct-messages.fifo
```

---

## Verification

1. Start LocalStack:
   ```bash
   docker-compose up -d localstack
   ```

2. Create SQS queues (see above)

3. Start gateway:
   ```bash
   npm run start:dev
   ```

4. Send test message via WebSocket

5. Verify message appears in SQS queue

---

## References

- **Spec**: `specs/ws-gateway.md` (SQS Publisher section)
- **AWS SDK**: https://docs.aws.amazon.com/AWSJavaScriptSDK/v3/latest/clients/client-sqs/
