import { Module } from '@nestjs/common';
import { JwtModule } from '@nestjs/jwt';
import { PassportModule } from '@nestjs/passport';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { JwtStrategy } from './jwt.strategy';
import { WsAuthGuard } from './ws-auth.guard';

/**
 * Authentication module for WebSocket connections.
 * 
 * Provides JWT-based authentication for WebSocket connections,
 * validating tokens issued by the Identity Service.
 */
@Module({
  imports: [
    PassportModule,
    JwtModule.registerAsync({
      imports: [ConfigModule],
      useFactory: async (configService: ConfigService) => ({
        secret: configService.get<string>('JWT_SECRET'),
        signOptions: {
          expiresIn: '1h',
        },
      }),
      inject: [ConfigService],
    }),
  ],
  providers: [JwtStrategy, WsAuthGuard],
  exports: [WsAuthGuard],
})
export class AuthModule {}
