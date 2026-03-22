/**
 * DTO for sending a message via WebSocket.
 */
export class SendMessageDto {
  conversationId: string;
  content: string;
  isDirect: boolean;
}
