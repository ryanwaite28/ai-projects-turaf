import { Test, TestingModule } from '@nestjs/testing';
import { ConfigService } from '@nestjs/config';
import { JwtStrategy } from './jwt.strategy';

describe('JwtStrategy', () => {
  let strategy: JwtStrategy;
  let configService: ConfigService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        JwtStrategy,
        {
          provide: ConfigService,
          useValue: {
            get: jest.fn().mockReturnValue('test-secret'),
          },
        },
      ],
    }).compile();

    strategy = module.get<JwtStrategy>(JwtStrategy);
    configService = module.get<ConfigService>(ConfigService);
  });

  it('should be defined', () => {
    expect(strategy).toBeDefined();
  });

  describe('validate', () => {
    it('should return user object from JWT payload', async () => {
      const payload = {
        sub: 'user-123',
        email: 'test@example.com',
        organizationId: 'org-456',
        iat: 1234567890,
        exp: 1234571490,
      };

      const result = await strategy.validate(payload);

      expect(result).toEqual({
        userId: 'user-123',
        email: 'test@example.com',
        organizationId: 'org-456',
      });
    });

    it('should extract userId from sub claim', async () => {
      const payload = {
        sub: 'user-789',
        email: 'user@example.com',
        organizationId: 'org-101',
      };

      const result = await strategy.validate(payload);

      expect(result.userId).toBe('user-789');
    });

    it('should handle payload with missing optional fields', async () => {
      const payload = {
        sub: 'user-minimal',
        email: 'minimal@example.com',
      };

      const result = await strategy.validate(payload);

      expect(result).toEqual({
        userId: 'user-minimal',
        email: 'minimal@example.com',
        organizationId: undefined,
      });
    });

    it('should preserve email from payload', async () => {
      const payload = {
        sub: 'user-email-test',
        email: 'preserve@example.com',
        organizationId: 'org-preserve',
      };

      const result = await strategy.validate(payload);

      expect(result.email).toBe('preserve@example.com');
    });

    it('should preserve organizationId from payload', async () => {
      const payload = {
        sub: 'user-org-test',
        email: 'org@example.com',
        organizationId: 'org-specific-123',
      };

      const result = await strategy.validate(payload);

      expect(result.organizationId).toBe('org-specific-123');
    });
  });

  describe('configuration', () => {
    it('should use JWT_SECRET from config service', () => {
      expect(configService.get).toHaveBeenCalledWith('JWT_SECRET');
    });
  });
});
