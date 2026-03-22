import { Test, TestingModule } from '@nestjs/testing';
import { JwtService } from '@nestjs/jwt';
import { WsAuthGuard } from './ws-auth.guard';
import { WsException } from '@nestjs/websockets';
import { ExecutionContext } from '@nestjs/common';

describe('WsAuthGuard', () => {
  let guard: WsAuthGuard;
  let jwtService: JwtService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        WsAuthGuard,
        {
          provide: JwtService,
          useValue: {
            verifyAsync: jest.fn(),
          },
        },
      ],
    }).compile();

    guard = module.get<WsAuthGuard>(WsAuthGuard);
    jwtService = module.get<JwtService>(JwtService);
  });

  it('should be defined', () => {
    expect(guard).toBeDefined();
  });

  describe('canActivate', () => {
    it('should allow valid token from auth object', async () => {
      const mockClient = {
        handshake: {
          auth: { token: 'valid-token' },
          headers: {},
          query: {},
        },
        data: {},
      };

      const mockContext = {
        switchToWs: () => ({
          getClient: () => mockClient,
        }),
      } as ExecutionContext;

      jest.spyOn(jwtService, 'verifyAsync').mockResolvedValue({
        sub: 'user-123',
        email: 'test@example.com',
        organizationId: 'org-456',
      });

      const result = await guard.canActivate(mockContext);

      expect(result).toBe(true);
      expect(mockClient.data.user).toEqual({
        userId: 'user-123',
        email: 'test@example.com',
        organizationId: 'org-456',
      });
      expect(jwtService.verifyAsync).toHaveBeenCalledWith('valid-token');
    });

    it('should allow valid token from Authorization header', async () => {
      const mockClient = {
        handshake: {
          headers: { authorization: 'Bearer valid-token' },
          auth: {},
          query: {},
        },
        data: {},
      };

      const mockContext = {
        switchToWs: () => ({
          getClient: () => mockClient,
        }),
      } as ExecutionContext;

      jest.spyOn(jwtService, 'verifyAsync').mockResolvedValue({
        sub: 'user-789',
        email: 'user@example.com',
        organizationId: 'org-101',
      });

      const result = await guard.canActivate(mockContext);

      expect(result).toBe(true);
      expect(mockClient.data.user.userId).toBe('user-789');
    });

    it('should allow valid token from query parameter', async () => {
      const mockClient = {
        handshake: {
          query: { token: 'valid-query-token' },
          headers: {},
          auth: {},
        },
        data: {},
      };

      const mockContext = {
        switchToWs: () => ({
          getClient: () => mockClient,
        }),
      } as ExecutionContext;

      jest.spyOn(jwtService, 'verifyAsync').mockResolvedValue({
        sub: 'user-query',
        email: 'query@example.com',
        organizationId: 'org-query',
      });

      const result = await guard.canActivate(mockContext);

      expect(result).toBe(true);
      expect(mockClient.data.user.userId).toBe('user-query');
    });

    it('should reject missing token', async () => {
      const mockClient = {
        handshake: {
          auth: {},
          headers: {},
          query: {},
        },
        data: {},
      };

      const mockContext = {
        switchToWs: () => ({
          getClient: () => mockClient,
        }),
      } as ExecutionContext;

      await expect(guard.canActivate(mockContext)).rejects.toThrow(
        new WsException('Unauthorized: No token provided'),
      );
    });

    it('should reject invalid token', async () => {
      const mockClient = {
        handshake: {
          auth: { token: 'invalid-token' },
          headers: {},
          query: {},
        },
        data: {},
      };

      const mockContext = {
        switchToWs: () => ({
          getClient: () => mockClient,
        }),
      } as ExecutionContext;

      jest
        .spyOn(jwtService, 'verifyAsync')
        .mockRejectedValue(new Error('Invalid token'));

      await expect(guard.canActivate(mockContext)).rejects.toThrow(
        new WsException('Unauthorized: Invalid token'),
      );
    });

    it('should reject expired token', async () => {
      const mockClient = {
        handshake: {
          auth: { token: 'expired-token' },
          headers: {},
          query: {},
        },
        data: {},
      };

      const mockContext = {
        switchToWs: () => ({
          getClient: () => mockClient,
        }),
      } as ExecutionContext;

      jest
        .spyOn(jwtService, 'verifyAsync')
        .mockRejectedValue(new Error('Token expired'));

      await expect(guard.canActivate(mockContext)).rejects.toThrow(
        new WsException('Unauthorized: Invalid token'),
      );
    });

    it('should prioritize auth object over header', async () => {
      const mockClient = {
        handshake: {
          auth: { token: 'auth-token' },
          headers: { authorization: 'Bearer header-token' },
          query: {},
        },
        data: {},
      };

      const mockContext = {
        switchToWs: () => ({
          getClient: () => mockClient,
        }),
      } as ExecutionContext;

      jest.spyOn(jwtService, 'verifyAsync').mockResolvedValue({
        sub: 'user-123',
        email: 'test@example.com',
        organizationId: 'org-456',
      });

      await guard.canActivate(mockContext);

      // Should use auth-token, not header-token
      expect(jwtService.verifyAsync).toHaveBeenCalledWith('auth-token');
    });

    it('should handle malformed Authorization header', async () => {
      const mockClient = {
        handshake: {
          headers: { authorization: 'InvalidFormat token' },
          auth: {},
          query: {},
        },
        data: {},
      };

      const mockContext = {
        switchToWs: () => ({
          getClient: () => mockClient,
        }),
      } as ExecutionContext;

      await expect(guard.canActivate(mockContext)).rejects.toThrow(
        new WsException('Unauthorized: No token provided'),
      );
    });
  });

  describe('extractTokenFromHandshake', () => {
    it('should extract token from auth object', () => {
      const mockClient = {
        handshake: {
          auth: { token: 'test-token' },
          headers: {},
          query: {},
        },
      } as any;

      const token = (guard as any).extractTokenFromHandshake(mockClient);
      expect(token).toBe('test-token');
    });

    it('should extract token from Authorization header', () => {
      const mockClient = {
        handshake: {
          headers: { authorization: 'Bearer header-token' },
          auth: {},
          query: {},
        },
      } as any;

      const token = (guard as any).extractTokenFromHandshake(mockClient);
      expect(token).toBe('header-token');
    });

    it('should extract token from query parameter', () => {
      const mockClient = {
        handshake: {
          query: { token: 'query-token' },
          headers: {},
          auth: {},
        },
      } as any;

      const token = (guard as any).extractTokenFromHandshake(mockClient);
      expect(token).toBe('query-token');
    });

    it('should return null when no token present', () => {
      const mockClient = {
        handshake: {
          auth: {},
          headers: {},
          query: {},
        },
      } as any;

      const token = (guard as any).extractTokenFromHandshake(mockClient);
      expect(token).toBeNull();
    });
  });
});
