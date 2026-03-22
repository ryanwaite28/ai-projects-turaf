import { Test, TestingModule } from '@nestjs/testing';
import { ChatGateway } from './chat.gateway';
import { SqsPublisherService } from '../services/sqs-publisher.service';
import { Server, Socket } from 'socket.io';

describe('ChatGateway', () => {
  let gateway: ChatGateway;
  let sqsPublisher: SqsPublisherService;
  let mockServer: Partial<Server>;

  beforeEach(async () => {
    mockServer = {
      to: jest.fn().mockReturnThis(),
      emit: jest.fn(),
    };

    const module: TestingModule = await Test.createTestingModule({
      providers: [
        ChatGateway,
        {
          provide: SqsPublisherService,
          useValue: {
            publishMessage: jest.fn().mockResolvedValue(undefined),
          },
        },
      ],
    }).compile();

    gateway = module.get<ChatGateway>(ChatGateway);
    sqsPublisher = module.get<SqsPublisherService>(SqsPublisherService);
    gateway.server = mockServer as Server;
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should be defined', () => {
    expect(gateway).toBeDefined();
  });

  describe('handleConnection', () => {
    it('should log client connection', async () => {
      const mockClient = {
        id: 'client-123',
        data: {
          user: {
            userId: 'user-123',
            email: 'test@example.com',
            organizationId: 'org-456',
          },
        },
      } as Socket;

      const loggerSpy = jest.spyOn(gateway['logger'], 'log');

      await gateway.handleConnection(mockClient);

      expect(loggerSpy).toHaveBeenCalledWith(
        expect.stringContaining('Client connected'),
      );
      expect(loggerSpy).toHaveBeenCalledWith(
        expect.stringContaining('user-123'),
      );
    });

    it('should handle connection with undefined user', async () => {
      const mockClient = {
        id: 'client-456',
        data: {},
      } as Socket;

      const loggerSpy = jest.spyOn(gateway['logger'], 'log');

      await gateway.handleConnection(mockClient);

      expect(loggerSpy).toHaveBeenCalled();
    });
  });

  describe('handleDisconnect', () => {
    it('should log client disconnection', async () => {
      const mockClient = {
        id: 'client-123',
        data: {
          user: {
            userId: 'user-123',
            email: 'test@example.com',
            organizationId: 'org-456',
          },
        },
      } as Socket;

      const loggerSpy = jest.spyOn(gateway['logger'], 'log');

      await gateway.handleDisconnect(mockClient);

      expect(loggerSpy).toHaveBeenCalledWith(
        expect.stringContaining('Client disconnected'),
      );
    });
  });

  describe('handleJoinConversation', () => {
    it('should join client to conversation room', async () => {
      const mockClient = {
        id: 'client-123',
        data: {
          user: {
            userId: 'user-123',
            email: 'test@example.com',
            organizationId: 'org-456',
          },
        },
        join: jest.fn().mockResolvedValue(undefined),
      } as unknown as Socket;

      const data = { conversationId: 'conv-789' };

      const result = await gateway.handleJoinConversation(mockClient, data);

      expect(mockClient.join).toHaveBeenCalledWith('conversation:conv-789');
      expect(result).toEqual({
        event: 'joined_conversation',
        data: { conversationId: 'conv-789' },
      });
    });

    it('should log join conversation event', async () => {
      const mockClient = {
        id: 'client-123',
        data: {
          user: {
            userId: 'user-123',
            email: 'test@example.com',
            organizationId: 'org-456',
          },
        },
        join: jest.fn().mockResolvedValue(undefined),
      } as unknown as Socket;

      const loggerSpy = jest.spyOn(gateway['logger'], 'log');
      const data = { conversationId: 'conv-789' };

      await gateway.handleJoinConversation(mockClient, data);

      expect(loggerSpy).toHaveBeenCalledWith(
        expect.stringContaining('joining conversation'),
      );
    });
  });

  describe('handleLeaveConversation', () => {
    it('should remove client from conversation room', async () => {
      const mockClient = {
        id: 'client-123',
        data: {
          user: {
            userId: 'user-123',
            email: 'test@example.com',
            organizationId: 'org-456',
          },
        },
        leave: jest.fn().mockResolvedValue(undefined),
      } as unknown as Socket;

      const data = { conversationId: 'conv-789' };

      const result = await gateway.handleLeaveConversation(mockClient, data);

      expect(mockClient.leave).toHaveBeenCalledWith('conversation:conv-789');
      expect(result).toEqual({
        event: 'left_conversation',
        data: { conversationId: 'conv-789' },
      });
    });

    it('should log leave conversation event', async () => {
      const mockClient = {
        id: 'client-123',
        data: {
          user: {
            userId: 'user-123',
            email: 'test@example.com',
            organizationId: 'org-456',
          },
        },
        leave: jest.fn().mockResolvedValue(undefined),
      } as unknown as Socket;

      const loggerSpy = jest.spyOn(gateway['logger'], 'log');
      const data = { conversationId: 'conv-789' };

      await gateway.handleLeaveConversation(mockClient, data);

      expect(loggerSpy).toHaveBeenCalledWith(
        expect.stringContaining('leaving conversation'),
      );
    });
  });

  describe('handleSendMessage', () => {
    it('should publish message to SQS and broadcast to room', async () => {
      const mockClient = {
        id: 'client-123',
        data: {
          user: {
            userId: 'user-123',
            email: 'test@example.com',
            organizationId: 'org-456',
          },
        },
      } as Socket;

      const data = {
        conversationId: 'conv-789',
        content: 'Hello World',
        isDirect: true,
      };

      const result = await gateway.handleSendMessage(mockClient, data);

      expect(sqsPublisher.publishMessage).toHaveBeenCalledWith({
        conversationId: 'conv-789',
        senderId: 'user-123',
        content: 'Hello World',
        isDirect: true,
        organizationId: 'org-456',
      });

      expect(mockServer.to).toHaveBeenCalledWith('conversation:conv-789');
      expect(mockServer.emit).toHaveBeenCalledWith(
        'new_message',
        expect.objectContaining({
          conversationId: 'conv-789',
          senderId: 'user-123',
          content: 'Hello World',
        }),
      );

      expect(result).toEqual({
        event: 'message_sent',
        data: { conversationId: 'conv-789' },
      });
    });

    it('should include timestamp in broadcast message', async () => {
      const mockClient = {
        id: 'client-123',
        data: {
          user: {
            userId: 'user-123',
            email: 'test@example.com',
            organizationId: 'org-456',
          },
        },
      } as Socket;

      const data = {
        conversationId: 'conv-789',
        content: 'Test message',
        isDirect: false,
      };

      await gateway.handleSendMessage(mockClient, data);

      expect(mockServer.emit).toHaveBeenCalledWith(
        'new_message',
        expect.objectContaining({
          timestamp: expect.any(String),
        }),
      );
    });

    it('should handle direct messages', async () => {
      const mockClient = {
        id: 'client-123',
        data: {
          user: {
            userId: 'user-123',
            email: 'test@example.com',
            organizationId: 'org-456',
          },
        },
      } as Socket;

      const data = {
        conversationId: 'conv-direct-123',
        content: 'Direct message',
        isDirect: true,
      };

      await gateway.handleSendMessage(mockClient, data);

      expect(sqsPublisher.publishMessage).toHaveBeenCalledWith(
        expect.objectContaining({
          isDirect: true,
        }),
      );
    });

    it('should handle group messages', async () => {
      const mockClient = {
        id: 'client-123',
        data: {
          user: {
            userId: 'user-123',
            email: 'test@example.com',
            organizationId: 'org-456',
          },
        },
      } as Socket;

      const data = {
        conversationId: 'conv-group-456',
        content: 'Group message',
        isDirect: false,
      };

      await gateway.handleSendMessage(mockClient, data);

      expect(sqsPublisher.publishMessage).toHaveBeenCalledWith(
        expect.objectContaining({
          isDirect: false,
        }),
      );
    });

    it('should log send message event', async () => {
      const mockClient = {
        id: 'client-123',
        data: {
          user: {
            userId: 'user-123',
            email: 'test@example.com',
            organizationId: 'org-456',
          },
        },
      } as Socket;

      const loggerSpy = jest.spyOn(gateway['logger'], 'log');
      const data = {
        conversationId: 'conv-789',
        content: 'Test',
        isDirect: true,
      };

      await gateway.handleSendMessage(mockClient, data);

      expect(loggerSpy).toHaveBeenCalledWith(
        expect.stringContaining('sending message'),
      );
    });

    it('should handle SQS publish errors gracefully', async () => {
      const mockClient = {
        id: 'client-123',
        data: {
          user: {
            userId: 'user-123',
            email: 'test@example.com',
            organizationId: 'org-456',
          },
        },
      } as Socket;

      const data = {
        conversationId: 'conv-789',
        content: 'Test',
        isDirect: true,
      };

      jest
        .spyOn(sqsPublisher, 'publishMessage')
        .mockRejectedValue(new Error('SQS error'));

      await expect(
        gateway.handleSendMessage(mockClient, data),
      ).rejects.toThrow('SQS error');
    });
  });

  describe('room management', () => {
    it('should use correct room naming convention', async () => {
      const mockClient = {
        id: 'client-123',
        data: {
          user: {
            userId: 'user-123',
            email: 'test@example.com',
            organizationId: 'org-456',
          },
        },
        join: jest.fn().mockResolvedValue(undefined),
      } as unknown as Socket;

      await gateway.handleJoinConversation(mockClient, {
        conversationId: 'test-conv',
      });

      expect(mockClient.join).toHaveBeenCalledWith('conversation:test-conv');
    });

    it('should broadcast to correct room', async () => {
      const mockClient = {
        id: 'client-123',
        data: {
          user: {
            userId: 'user-123',
            email: 'test@example.com',
            organizationId: 'org-456',
          },
        },
      } as Socket;

      await gateway.handleSendMessage(mockClient, {
        conversationId: 'room-test',
        content: 'Test',
        isDirect: true,
      });

      expect(mockServer.to).toHaveBeenCalledWith('conversation:room-test');
    });
  });
});
