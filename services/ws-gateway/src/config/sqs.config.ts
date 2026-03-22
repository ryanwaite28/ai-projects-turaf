import { registerAs } from '@nestjs/config';

/**
 * SQS configuration for publishing messages to FIFO queues.
 * 
 * The WebSocket Gateway publishes messages to SQS FIFO queues
 * for persistence by the Communications Service.
 * 
 * Configuration:
 * - Local: LocalStack endpoints (http://localhost:4566)
 * - AWS: Actual SQS queue URLs
 */
export default registerAs('sqs', () => ({
  region: process.env.AWS_REGION || 'us-east-1',
  directQueueUrl: process.env.SQS_DIRECT_QUEUE_URL,
  groupQueueUrl: process.env.SQS_GROUP_QUEUE_URL,
}));
