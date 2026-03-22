import {
  WebSocketGateway,
  WebSocketServer,
  SubscribeMessage,
  OnGatewayConnection,
  OnGatewayDisconnect,
  ConnectedSocket,
  MessageBody,
  UseGuards,
} from '@nestjs/websockets';
import { Server, Socket } from 'socket.io';
import { WsAuthGuard } from '../auth/ws-auth.guard';
import { SqsPublisherService } from '../services/sqs-publisher.service';
import { Logger } from '@nestjs/common';

/**
 * Chat Gateway for real-time messaging.
 * 
 * Handles WebSocket connections for chat functionality:
 * - Client connections and disconnections
 * - Joining/leaving conversation rooms
 * - Sending messages
 * - Broadcasting messages via Redis Pub/Sub
 * - Publishing messages to SQS for persistence
 * 
 * This gateway is stateless and horizontally scalable.
 */
@WebSocketGateway({
  cors: {
    origin: process.env.ALLOWED_ORIGINS?.split(',') || ['http://localhost:4200'],
    credentials: true,
  },
})
@UseGuards(WsAuthGuard)
export class ChatGateway implements OnGatewayConnection, OnGatewayDisconnect {
  @WebSocketServer()
  server: Server;

  private readonly logger = new Logger(ChatGateway.name);

  constructor(private sqsPublisher: SqsPublisherService) {}

  async handleConnection(client: Socket) {
    const user = client.data.user;
    this.logger.log(`Client connected: ${client.id}, User: ${user?.userId}`);
  }

  async handleDisconnect(client: Socket) {
    const user = client.data.user;
    this.logger.log(`Client disconnected: ${client.id}, User: ${user?.userId}`);
  }

  @SubscribeMessage('join_conversation')
  async handleJoinConversation(
    @ConnectedSocket() client: Socket,
    @MessageBody() data: { conversationId: string },
  ) {
    const { conversationId } = data;
    const user = client.data.user;

    this.logger.log(`User ${user.userId} joining conversation ${conversationId}`);
    
    await client.join(`conversation:${conversationId}`);
    
    return {
      event: 'joined_conversation',
      data: { conversationId },
    };
  }

  @SubscribeMessage('leave_conversation')
  async handleLeaveConversation(
    @ConnectedSocket() client: Socket,
    @MessageBody() data: { conversationId: string },
  ) {
    const { conversationId } = data;
    const user = client.data.user;

    this.logger.log(`User ${user.userId} leaving conversation ${conversationId}`);
    
    await client.leave(`conversation:${conversationId}`);
    
    return {
      event: 'left_conversation',
      data: { conversationId },
    };
  }

  @SubscribeMessage('send_message')
  async handleSendMessage(
    @ConnectedSocket() client: Socket,
    @MessageBody() data: {
      conversationId: string;
      content: string;
      isDirect: boolean;
    },
  ) {
    const user = client.data.user;
    const { conversationId, content, isDirect } = data;

    this.logger.log(`User ${user.userId} sending message to conversation ${conversationId}`);

    // Publish to SQS for persistence
    await this.sqsPublisher.publishMessage({
      conversationId,
      senderId: user.userId,
      content,
      isDirect,
      organizationId: user.organizationId,
    });

    // Broadcast to all clients in the conversation room
    // Redis adapter will handle cross-instance broadcasting
    this.server.to(`conversation:${conversationId}`).emit('new_message', {
      conversationId,
      senderId: user.userId,
      content,
      timestamp: new Date().toISOString(),
    });

    return {
      event: 'message_sent',
      data: { conversationId },
    };
  }
}
