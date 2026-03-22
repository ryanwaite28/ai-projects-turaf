import { Test, TestingModule } from '@nestjs/testing';
import { TypingGateway } from './typing.gateway';
import { Server, Socket } from 'socket.io';

describe('TypingGateway', () => {
  let gateway: TypingGateway;
  let mockServer: Partial<Server>;

  beforeEach(async () => {
    mockServer = {
      to: jest.fn().mockReturnThis(),
      emit: jest.fn(),
    };

    const module: TestingModule = await Test.createTestingModule({
      providers: [TypingGateway],
    }).compile();

    gateway = module.get<TypingGateway>(TypingGateway);
    gateway.server = mockServer as Server;
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should be defined', () => {
    expect(gateway).toBeDefined();
  });

  describe('handleTypingStart', () => {
    it('should broadcast typing start to conversation room', async () => {
      const mockClient = {
        id: 'client-123',
        data: {
          user: {
            userId: 'user-123',
            email: 'test@example.com',
            organizationId: 'org-456',
          },
        },
        to: jest.fn().mockReturnThis(),
        emit: jest.fn(),
      } as unknown as Socket;

      const data = { conversationId: 'conv-789' };

      const result = await gateway.handleTypingStart(mockClient, data);

      expect(mockClient.to).toHaveBeenCalledWith('conversation:conv-789');
      expect(mockClient.emit).toHaveBeenCalledWith('user_typing', {
        conversationId: 'conv-789',
        userId: 'user-123',
        isTyping: true,
      });

      expect(result).toEqual({
        event: 'typing_started',
        data: { conversationId: 'conv-789' },
      });
    });

    it('should log typing start event', async () => {
      const mockClient = {
        id: 'client-123',
        data: {
          user: {
            userId: 'user-123',
            email: 'test@example.com',
            organizationId: 'org-456',
          },
        },
        to: jest.fn().mockReturnThis(),
        emit: jest.fn(),
      } as unknown as Socket;

      const loggerSpy = jest.spyOn(gateway['logger'], 'debug');
      const data = { conversationId: 'conv-789' };

      await gateway.handleTypingStart(mockClient, data);

      expect(loggerSpy).toHaveBeenCalledWith(
        expect.stringContaining('started typing'),
      );
    });

    it('should include user ID in typing event', async () => {
      const mockClient = {
        id: 'client-456',
        data: {
          user: {
            userId: 'user-specific-id',
            email: 'user@example.com',
            organizationId: 'org-123',
          },
        },
        to: jest.fn().mockReturnThis(),
        emit: jest.fn(),
      } as unknown as Socket;

      const data = { conversationId: 'conv-test' };

      await gateway.handleTypingStart(mockClient, data);

      expect(mockClient.emit).toHaveBeenCalledWith(
        'user_typing',
        expect.objectContaining({
          userId: 'user-specific-id',
        }),
      );
    });

    it('should set isTyping to true', async () => {
      const mockClient = {
        id: 'client-123',
        data: {
          user: {
            userId: 'user-123',
            email: 'test@example.com',
            organizationId: 'org-456',
          },
        },
        to: jest.fn().mockReturnThis(),
        emit: jest.fn(),
      } as unknown as Socket;

      const data = { conversationId: 'conv-789' };

      await gateway.handleTypingStart(mockClient, data);

      expect(mockClient.emit).toHaveBeenCalledWith(
        'user_typing',
        expect.objectContaining({
          isTyping: true,
        }),
      );
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
        to: jest.fn().mockReturnThis(),
        emit: jest.fn(),
      } as unknown as Socket;

      const data = { conversationId: 'specific-conv-id' };

      await gateway.handleTypingStart(mockClient, data);

      expect(mockClient.to).toHaveBeenCalledWith('conversation:specific-conv-id');
    });
  });

  describe('handleTypingStop', () => {
    it('should broadcast typing stop to conversation room', async () => {
      const mockClient = {
        id: 'client-123',
        data: {
          user: {
            userId: 'user-123',
            email: 'test@example.com',
            organizationId: 'org-456',
          },
        },
        to: jest.fn().mockReturnThis(),
        emit: jest.fn(),
      } as unknown as Socket;

      const data = { conversationId: 'conv-789' };

      const result = await gateway.handleTypingStop(mockClient, data);

      expect(mockClient.to).toHaveBeenCalledWith('conversation:conv-789');
      expect(mockClient.emit).toHaveBeenCalledWith('user_typing', {
        conversationId: 'conv-789',
        userId: 'user-123',
        isTyping: false,
      });

      expect(result).toEqual({
        event: 'typing_stopped',
        data: { conversationId: 'conv-789' },
      });
    });

    it('should log typing stop event', async () => {
      const mockClient = {
        id: 'client-123',
        data: {
          user: {
            userId: 'user-123',
            email: 'test@example.com',
            organizationId: 'org-456',
          },
        },
        to: jest.fn().mockReturnThis(),
        emit: jest.fn(),
      } as unknown as Socket;

      const loggerSpy = jest.spyOn(gateway['logger'], 'debug');
      const data = { conversationId: 'conv-789' };

      await gateway.handleTypingStop(mockClient, data);

      expect(loggerSpy).toHaveBeenCalledWith(
        expect.stringContaining('stopped typing'),
      );
    });

    it('should set isTyping to false', async () => {
      const mockClient = {
        id: 'client-123',
        data: {
          user: {
            userId: 'user-123',
            email: 'test@example.com',
            organizationId: 'org-456',
          },
        },
        to: jest.fn().mockReturnThis(),
        emit: jest.fn(),
      } as unknown as Socket;

      const data = { conversationId: 'conv-789' };

      await gateway.handleTypingStop(mockClient, data);

      expect(mockClient.emit).toHaveBeenCalledWith(
        'user_typing',
        expect.objectContaining({
          isTyping: false,
        }),
      );
    });

    it('should include user ID in stop typing event', async () => {
      const mockClient = {
        id: 'client-456',
        data: {
          user: {
            userId: 'user-stop-id',
            email: 'user@example.com',
            organizationId: 'org-123',
          },
        },
        to: jest.fn().mockReturnThis(),
        emit: jest.fn(),
      } as unknown as Socket;

      const data = { conversationId: 'conv-test' };

      await gateway.handleTypingStop(mockClient, data);

      expect(mockClient.emit).toHaveBeenCalledWith(
        'user_typing',
        expect.objectContaining({
          userId: 'user-stop-id',
        }),
      );
    });
  });

  describe('room targeting', () => {
    it('should use conversation room naming convention', async () => {
      const mockClient = {
        id: 'client-123',
        data: {
          user: {
            userId: 'user-123',
            email: 'test@example.com',
            organizationId: 'org-456',
          },
        },
        to: jest.fn().mockReturnThis(),
        emit: jest.fn(),
      } as unknown as Socket;

      await gateway.handleTypingStart(mockClient, {
        conversationId: 'test-room',
      });

      expect(mockClient.to).toHaveBeenCalledWith('conversation:test-room');

      await gateway.handleTypingStop(mockClient, {
        conversationId: 'test-room',
      });

      expect(mockClient.to).toHaveBeenCalledWith('conversation:test-room');
    });

    it('should broadcast to room excluding sender', async () => {
      const mockClient = {
        id: 'client-sender',
        data: {
          user: {
            userId: 'user-sender',
            email: 'sender@example.com',
            organizationId: 'org-456',
          },
        },
        to: jest.fn().mockReturnThis(),
        emit: jest.fn(),
      } as unknown as Socket;

      await gateway.handleTypingStart(mockClient, {
        conversationId: 'conv-123',
      });

      // Verify client.to() is called (which excludes sender)
      expect(mockClient.to).toHaveBeenCalled();
    });
  });

  describe('event data structure', () => {
    it('should return correct event structure for typing start', async () => {
      const mockClient = {
        id: 'client-123',
        data: {
          user: {
            userId: 'user-123',
            email: 'test@example.com',
            organizationId: 'org-456',
          },
        },
        to: jest.fn().mockReturnThis(),
        emit: jest.fn(),
      } as unknown as Socket;

      const result = await gateway.handleTypingStart(mockClient, {
        conversationId: 'conv-test',
      });

      expect(result).toHaveProperty('event', 'typing_started');
      expect(result).toHaveProperty('data');
      expect(result.data).toHaveProperty('conversationId', 'conv-test');
    });

    it('should return correct event structure for typing stop', async () => {
      const mockClient = {
        id: 'client-123',
        data: {
          user: {
            userId: 'user-123',
            email: 'test@example.com',
            organizationId: 'org-456',
          },
        },
        to: jest.fn().mockReturnThis(),
        emit: jest.fn(),
      } as unknown as Socket;

      const result = await gateway.handleTypingStop(mockClient, {
        conversationId: 'conv-test',
      });

      expect(result).toHaveProperty('event', 'typing_stopped');
      expect(result).toHaveProperty('data');
      expect(result.data).toHaveProperty('conversationId', 'conv-test');
    });

    it('should emit user_typing event with correct structure', async () => {
      const mockClient = {
        id: 'client-123',
        data: {
          user: {
            userId: 'user-123',
            email: 'test@example.com',
            organizationId: 'org-456',
          },
        },
        to: jest.fn().mockReturnThis(),
        emit: jest.fn(),
      } as unknown as Socket;

      await gateway.handleTypingStart(mockClient, {
        conversationId: 'conv-test',
      });

      expect(mockClient.emit).toHaveBeenCalledWith('user_typing', {
        conversationId: 'conv-test',
        userId: 'user-123',
        isTyping: true,
      });
    });
  });
});
