import { Test, TestingModule } from '@nestjs/testing';
import { INestApplication } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { io, Socket } from 'socket.io-client';
import { AppModule } from '../src/app.module';
import { RedisIoAdapter } from '../src/config/redis.config';

/**
 * E2E tests for Redis adapter horizontal scaling.
 * 
 * These tests verify that multiple WebSocket Gateway instances
 * can communicate with each other via Redis Pub/Sub.
 * 
 * Prerequisites:
 * - Redis server running on localhost:6379
 * 
 * To run these tests:
 * 1. Start Redis: docker run -p 6379:6379 redis:alpine
 * 2. Run tests: npm run test:e2e -- redis-adapter
 */
describe('Redis Adapter E2E', () => {
  let app1: INestApplication;
  let app2: INestApplication;
  let client1: Socket;
  let client2: Socket;

  beforeAll(async () => {
    // Create first application instance
    const moduleFixture1: TestingModule = await Test.createTestingModule({
      imports: [
        ConfigModule.forRoot({
          isGlobal: true,
          envFilePath: '.env.test',
        }),
        AppModule,
      ],
    }).compile();

    app1 = moduleFixture1.createNestApplication();
    
    const redisAdapter1 = new RedisIoAdapter(app1);
    await redisAdapter1.connectToRedis(app1.get('ConfigService'));
    app1.useWebSocketAdapter(redisAdapter1);
    
    await app1.listen(3001);

    // Create second application instance
    const moduleFixture2: TestingModule = await Test.createTestingModule({
      imports: [
        ConfigModule.forRoot({
          isGlobal: true,
          envFilePath: '.env.test',
        }),
        AppModule,
      ],
    }).compile();

    app2 = moduleFixture2.createNestApplication();
    
    const redisAdapter2 = new RedisIoAdapter(app2);
    await redisAdapter2.connectToRedis(app2.get('ConfigService'));
    app2.useWebSocketAdapter(redisAdapter2);
    
    await app2.listen(3002);
  });

  afterAll(async () => {
    if (client1) client1.disconnect();
    if (client2) client2.disconnect();
    if (app1) await app1.close();
    if (app2) await app2.close();
  });

  describe('Cross-instance communication', () => {
    it('should broadcast messages across instances', (done) => {
      const conversationId = 'test-conversation-123';
      const testMessage = 'Hello from instance 1';

      // Connect client1 to instance 1
      client1 = io('http://localhost:3001', {
        auth: { token: 'test-token' },
        transports: ['websocket'],
      });

      // Connect client2 to instance 2
      client2 = io('http://localhost:3002', {
        auth: { token: 'test-token' },
        transports: ['websocket'],
      });

      let client1Connected = false;
      let client2Connected = false;

      client1.on('connect', () => {
        client1Connected = true;
        if (client2Connected) {
          // Both connected, join conversation
          client1.emit('join_conversation', { conversationId });
          client2.emit('join_conversation', { conversationId });
        }
      });

      client2.on('connect', () => {
        client2Connected = true;
        if (client1Connected) {
          // Both connected, join conversation
          client1.emit('join_conversation', { conversationId });
          client2.emit('join_conversation', { conversationId });
        }
      });

      // Client2 should receive message sent by client1
      client2.on('new_message', (data) => {
        expect(data.conversationId).toBe(conversationId);
        expect(data.content).toBe(testMessage);
        done();
      });

      // After both joined, send message from client1
      let joinCount = 0;
      const onJoined = () => {
        joinCount++;
        if (joinCount === 2) {
          // Both joined, send message
          client1.emit('send_message', {
            conversationId,
            content: testMessage,
            isDirect: true,
          });
        }
      };

      client1.on('joined_conversation', onJoined);
      client2.on('joined_conversation', onJoined);
    }, 10000);

    it('should broadcast typing indicators across instances', (done) => {
      const conversationId = 'test-typing-456';

      client1 = io('http://localhost:3001', {
        auth: { token: 'test-token' },
        transports: ['websocket'],
      });

      client2 = io('http://localhost:3002', {
        auth: { token: 'test-token' },
        transports: ['websocket'],
      });

      let client1Connected = false;
      let client2Connected = false;

      client1.on('connect', () => {
        client1Connected = true;
        if (client2Connected) {
          client1.emit('join_conversation', { conversationId });
          client2.emit('join_conversation', { conversationId });
        }
      });

      client2.on('connect', () => {
        client2Connected = true;
        if (client1Connected) {
          client1.emit('join_conversation', { conversationId });
          client2.emit('join_conversation', { conversationId });
        }
      });

      // Client2 should receive typing indicator from client1
      client2.on('user_typing', (data) => {
        expect(data.conversationId).toBe(conversationId);
        expect(data.isTyping).toBe(true);
        done();
      });

      let joinCount = 0;
      const onJoined = () => {
        joinCount++;
        if (joinCount === 2) {
          // Both joined, send typing indicator
          client1.emit('typing_start', { conversationId });
        }
      };

      client1.on('joined_conversation', onJoined);
      client2.on('joined_conversation', onJoined);
    }, 10000);
  });

  describe('Room isolation', () => {
    it('should not broadcast to clients in different rooms', (done) => {
      const conversation1 = 'conversation-1';
      const conversation2 = 'conversation-2';

      client1 = io('http://localhost:3001', {
        auth: { token: 'test-token' },
        transports: ['websocket'],
      });

      client2 = io('http://localhost:3002', {
        auth: { token: 'test-token' },
        transports: ['websocket'],
      });

      let messagesReceived = 0;

      client1.on('connect', () => {
        client1.emit('join_conversation', { conversationId: conversation1 });
      });

      client2.on('connect', () => {
        client2.emit('join_conversation', { conversationId: conversation2 });
      });

      // Client2 should NOT receive message from conversation1
      client2.on('new_message', () => {
        messagesReceived++;
      });

      let joinCount = 0;
      const onJoined = () => {
        joinCount++;
        if (joinCount === 2) {
          // Send message to conversation1
          client1.emit('send_message', {
            conversationId: conversation1,
            content: 'Message for conversation 1',
            isDirect: true,
          });

          // Wait and verify client2 didn't receive it
          setTimeout(() => {
            expect(messagesReceived).toBe(0);
            done();
          }, 1000);
        }
      };

      client1.on('joined_conversation', onJoined);
      client2.on('joined_conversation', onJoined);
    }, 10000);
  });

  describe('Connection handling', () => {
    it('should handle client disconnection gracefully', (done) => {
      client1 = io('http://localhost:3001', {
        auth: { token: 'test-token' },
        transports: ['websocket'],
      });

      client1.on('connect', () => {
        client1.disconnect();
      });

      client1.on('disconnect', () => {
        expect(client1.connected).toBe(false);
        done();
      });
    }, 5000);

    it('should allow reconnection after disconnect', (done) => {
      client1 = io('http://localhost:3001', {
        auth: { token: 'test-token' },
        transports: ['websocket'],
      });

      let connectCount = 0;

      client1.on('connect', () => {
        connectCount++;
        if (connectCount === 1) {
          client1.disconnect();
        } else if (connectCount === 2) {
          expect(client1.connected).toBe(true);
          done();
        }
      });

      client1.on('disconnect', () => {
        // Reconnect
        client1.connect();
      });
    }, 10000);
  });
});
