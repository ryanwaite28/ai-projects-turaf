import { CanActivate, ExecutionContext, Injectable } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { WsException } from '@nestjs/websockets';
import { Socket } from 'socket.io';

/**
 * WebSocket authentication guard.
 * 
 * Validates JWT tokens from WebSocket handshake and attaches
 * user information to the socket for use in gateway handlers.
 * 
 * Token can be provided in:
 * - Authorization header: Bearer <token>
 * - Query parameter: ?token=<token>
 */
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
      const payload = await this.jwtService.verifyAsync(token);
      
      // Attach user info to socket for use in handlers
      client.data.user = {
        userId: payload.sub,
        email: payload.email,
        organizationId: payload.organizationId,
      };
      
      return true;
    } catch (error) {
      throw new WsException('Unauthorized: Invalid token');
    }
  }

  private extractTokenFromHandshake(client: Socket): string | null {
    // Try Authorization header first
    const authHeader = client.handshake.headers.authorization;
    if (authHeader && authHeader.startsWith('Bearer ')) {
      return authHeader.substring(7);
    }

    // Try query parameter
    const token = client.handshake.query.token;
    if (token && typeof token === 'string') {
      return token;
    }

    // Try auth object (some clients send it this way)
    const auth = client.handshake.auth;
    if (auth && auth.token) {
      return auth.token;
    }

    return null;
  }
}
