import { Injectable, Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { SQSClient, SendMessageCommand } from '@aws-sdk/client-sqs';
import { v4 as uuidv4 } from 'uuid';

/**
 * SQS Publisher service for sending messages to FIFO queues.
 * 
 * Publishes messages to SQS FIFO queues for persistence by the
 * Communications Service. Uses message group IDs to maintain
 * ordering within conversations.
 */
@Injectable()
export class SqsPublisherService {
  private readonly logger = new Logger(SqsPublisherService.name);
  private sqsClient: SQSClient;
  private directQueueUrl: string;
  private groupQueueUrl: string;

  constructor(private configService: ConfigService) {
    const region = this.configService.get<string>('AWS_REGION', 'us-east-1');
    
    this.sqsClient = new SQSClient({
      region,
      endpoint: this.configService.get<string>('AWS_ENDPOINT'), // For LocalStack
    });

    this.directQueueUrl = this.configService.get<string>('SQS_DIRECT_QUEUE_URL');
    this.groupQueueUrl = this.configService.get<string>('SQS_GROUP_QUEUE_URL');
  }

  async publishMessage(data: {
    conversationId: string;
    senderId: string;
    content: string;
    isDirect: boolean;
    organizationId: string;
  }): Promise<void> {
    const { conversationId, senderId, content, isDirect, organizationId } = data;
    
    const queueUrl = isDirect ? this.directQueueUrl : this.groupQueueUrl;
    
    if (!queueUrl) {
      this.logger.error('SQS queue URL not configured');
      throw new Error('SQS queue URL not configured');
    }

    const messageBody = JSON.stringify({
      conversationId,
      senderId,
      content,
      organizationId,
      timestamp: new Date().toISOString(),
    });

    const command = new SendMessageCommand({
      QueueUrl: queueUrl,
      MessageBody: messageBody,
      MessageGroupId: conversationId, // Ensures ordering within conversation
      MessageDeduplicationId: uuidv4(), // Prevents duplicates
    });

    try {
      await this.sqsClient.send(command);
      this.logger.log(`Message published to SQS for conversation ${conversationId}`);
    } catch (error) {
      this.logger.error(`Failed to publish message to SQS: ${error.message}`, error.stack);
      throw error;
    }
  }
}
