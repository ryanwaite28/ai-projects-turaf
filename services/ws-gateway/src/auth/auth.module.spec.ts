import { Test, TestingModule } from '@nestjs/testing';
import { ConfigModule } from '@nestjs/config';
import { AuthModule } from './auth.module';
import { WsAuthGuard } from './ws-auth.guard';
import { JwtStrategy } from './jwt.strategy';

describe('AuthModule', () => {
  let module: TestingModule;

  beforeEach(async () => {
    module = await Test.createTestingModule({
      imports: [
        ConfigModule.forRoot({
          isGlobal: true,
          envFilePath: '.env.test',
        }),
        AuthModule,
      ],
    }).compile();
  });

  it('should be defined', () => {
    expect(module).toBeDefined();
  });

  it('should provide WsAuthGuard', () => {
    const guard = module.get<WsAuthGuard>(WsAuthGuard);
    expect(guard).toBeDefined();
    expect(guard).toBeInstanceOf(WsAuthGuard);
  });

  it('should provide JwtStrategy', () => {
    const strategy = module.get<JwtStrategy>(JwtStrategy);
    expect(strategy).toBeDefined();
    expect(strategy).toBeInstanceOf(JwtStrategy);
  });

  it('should export WsAuthGuard', () => {
    const guard = module.get<WsAuthGuard>(WsAuthGuard);
    expect(guard).toBeDefined();
  });
});
