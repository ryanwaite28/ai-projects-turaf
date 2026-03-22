# Task: Implement JWT Authentication

**Service**: WebSocket Gateway  
**Type**: Authentication  
**Priority**: High  
**Estimated Time**: 2 hours  
**Dependencies**: 001-setup-nestjs-project

---

## Objective

Implement JWT authentication for WebSocket connections using NestJS guards and strategies.

---

## Acceptance Criteria

- [ ] JWT strategy configured
- [ ] WebSocket authentication guard implemented
- [ ] Token validation working
- [ ] User info attached to socket
- [ ] Unauthorized connections rejected
- [ ] Tests pass

---

## Implementation

**File**: `src/auth/auth.module.ts`

```typescript
import { Module } from '@nestjs/common';
import { JwtModule } from '@nestjs/jwt';
import { PassportModule } from '@nestjs/passport';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { JwtStrategy } from './jwt.strategy';
import { WsAuthGuard } from './ws-auth.guard';

@Module({
  imports: [
    PassportModule,
    JwtModule.registerAsync({
      imports: [ConfigModule],
      useFactory: async (configService: ConfigService) => ({
        secret: configService.get<string>('JWT_SECRET'),
        signOptions: { expiresIn: '24h' },
      }),
      inject: [ConfigService],
    }),
  ],
  providers: [JwtStrategy, WsAuthGuard],
  exports: [WsAuthGuard],
})
export class AuthModule {}
```

**File**: `src/auth/jwt.strategy.ts`

```typescript
import { Injectable } from '@nestjs/common';
import { PassportStrategy } from '@nestjs/passport';
import { ExtractJwt, Strategy } from 'passport-jwt';
import { ConfigService } from '@nestjs/config';

@Injectable()
export class JwtStrategy extends PassportStrategy(Strategy) {
  constructor(private configService: ConfigService) {
    super({
      jwtFromRequest: ExtractJwt.fromAuthHeaderAsBearerToken(),
      ignoreExpiration: false,
      secretOrKey: configService.get<string>('JWT_SECRET'),
    });
  }

  async validate(payload: any) {
    return {
      userId: payload.sub,
      email: payload.email,
      organizationId: payload.organizationId,
    };
  }
}
```

**File**: `src/auth/ws-auth.guard.ts`

```typescript
import { CanActivate, ExecutionContext, Injectable } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { WsException } from '@nestjs/websockets';
import { Socket } from 'socket.io';

@Injectable()
export class WsAuthGuard implements CanActivate {
  constructor(private jwtService: JwtService) {}

  async canActivate(context: ExecutionContext): Promise<boolean> {
    const client: Socket = context.switchToWs().getClient();
    const token = this.extractTokenFromHandshake(client);

    if (!token) {
      throw new WsException('Unauthorized: No token provided');
    }

    try {
      const payload = await this.jwtService.verifyAsync(token, {
        secret: process.env.JWT_SECRET,
      });

      // Attach user info to socket for later use
      client.data.userId = payload.sub;
      client.data.email = payload.email;
      client.data.organizationId = payload.organizationId;
      
      return true;
    } catch (error) {
      throw new WsException('Unauthorized: Invalid token');
    }
  }

  private extractTokenFromHandshake(client: Socket): string | undefined {
    // Try auth object first (preferred)
    if (client.handshake.auth?.token) {
      return client.handshake.auth.token;
    }
    
    // Fallback to Authorization header
    const authHeader = client.handshake.headers?.authorization;
    if (authHeader && authHeader.startsWith('Bearer ')) {
      return authHeader.substring(7);
    }
    
    return undefined;
  }
}
```

---

## Testing

**File**: `src/auth/ws-auth.guard.spec.ts`

```typescript
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

  it('should allow valid token', async () => {
    const mockClient = {
      handshake: {
        auth: { token: 'valid-token' },
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
    });

    const result = await guard.canActivate(mockContext);

    expect(result).toBe(true);
    expect(mockClient.data.userId).toBe('user-123');
  });

  it('should reject missing token', async () => {
    const mockClient = {
      handshake: { auth: {} },
      data: {},
    };

    const mockContext = {
      switchToWs: () => ({
        getClient: () => mockClient,
      }),
    } as ExecutionContext;

    await expect(guard.canActivate(mockContext)).rejects.toThrow(WsException);
  });
});
```

---

## Verification

1. Start gateway:
   ```bash
   npm run start:dev
   ```

2. Test with valid JWT:
   ```typescript
   const socket = io('http://localhost:3000', {
     auth: { token: 'valid-jwt-token' }
   });
   ```

3. Verify connection succeeds with valid token
4. Verify connection rejected with invalid/missing token

---

## References

- **Spec**: `specs/ws-gateway.md` (Authentication section)
- **NestJS Docs**: https://docs.nestjs.com/security/authentication
