import {
  WebSocketGateway,
  WebSocketServer,
  SubscribeMessage,
  ConnectedSocket,
  MessageBody,
  UseGuards,
} from '@nestjs/websockets';
import { Server, Socket } from 'socket.io';
import { WsAuthGuard } from '../auth/ws-auth.guard';
import { Logger } from '@nestjs/common';

/**
 * Typing Indicator Gateway.
 * 
 * Handles real-time typing indicators for conversations:
 * - User started typing
 * - User stopped typing
 * - Broadcasting typing status via Redis Pub/Sub
 * 
 * Typing indicators are ephemeral and not persisted.
 */
@WebSocketGateway({
  cors: {
    origin: process.env.ALLOWED_ORIGINS?.split(',') || ['http://localhost:4200'],
    credentials: true,
  },
})
@UseGuards(WsAuthGuard)
export class TypingGateway {
  @WebSocketServer()
  server: Server;

  private readonly logger = new Logger(TypingGateway.name);

  @SubscribeMessage('typing_start')
  async handleTypingStart(
    @ConnectedSocket() client: Socket,
    @MessageBody() data: { conversationId: string },
  ) {
    const user = client.data.user;
    const { conversationId } = data;

    this.logger.debug(`User ${user.userId} started typing in conversation ${conversationId}`);

    // Broadcast to all clients in the conversation except sender
    client.to(`conversation:${conversationId}`).emit('user_typing', {
      conversationId,
      userId: user.userId,
      isTyping: true,
    });

    return {
      event: 'typing_started',
      data: { conversationId },
    };
  }

  @SubscribeMessage('typing_stop')
  async handleTypingStop(
    @ConnectedSocket() client: Socket,
    @MessageBody() data: { conversationId: string },
  ) {
    const user = client.data.user;
    const { conversationId } = data;

    this.logger.debug(`User ${user.userId} stopped typing in conversation ${conversationId}`);

    // Broadcast to all clients in the conversation except sender
    client.to(`conversation:${conversationId}`).emit('user_typing', {
      conversationId,
      userId: user.userId,
      isTyping: false,
    });

    return {
      event: 'typing_stopped',
      data: { conversationId },
    };
  }
}
