import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { ChatGateway } from './gateways/chat.gateway';
import { TypingGateway } from './gateways/typing.gateway';
import { AuthModule } from './auth/auth.module';
import { RedisPubSubService } from './services/redis-pub-sub.service';
import { SqsPublisherService } from './services/sqs-publisher.service';

@Module({
  imports: [
    ConfigModule.forRoot({
      isGlobal: true,
    }),
    AuthModule,
  ],
  providers: [
    ChatGateway,
    TypingGateway,
    RedisPubSubService,
    SqsPublisherService,
  ],
})
export class AppModule {}
