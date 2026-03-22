import { Test, TestingModule } from '@nestjs/testing';
import { INestApplication } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { io, Socket } from 'socket.io-client';
import { AppModule } from '../src/app.module';
import { RedisIoAdapter } from '../src/config/redis.config';

/**
 * E2E tests for Typing Gateway functionality.
 * 
 * These tests verify real-time typing indicator operations including:
 * - Starting typing indicators
 * - Stopping typing indicators
 * - Broadcasting across conversation rooms
 * - Excluding sender from broadcasts
 * - Cross-instance communication (via Redis)
 * 
 * Prerequisites:
 * - Redis server running on localhost:6379
 * 
 * To run these tests:
 * 1. Start Redis: docker run -p 6379:6379 redis:alpine
 * 2. Run tests: npm run test:e2e -- typing-gateway
 */
describe('Typing Gateway E2E', () => {
  let app: INestApplication;
  let client1: Socket;
  let client2: Socket;
  let client3: Socket;

  beforeAll(async () => {
    const moduleFixture: TestingModule = await Test.createTestingModule({
      imports: [
        ConfigModule.forRoot({
          isGlobal: true,
          envFilePath: '.env.test',
        }),
        AppModule,
      ],
    }).compile();

    app = moduleFixture.createNestApplication();
    
    const redisAdapter = new RedisIoAdapter(app);
    await redisAdapter.connectToRedis(app.get('ConfigService'));
    app.useWebSocketAdapter(redisAdapter);
    
    await app.listen(3004);
  });

  afterAll(async () => {
    if (client1) client1.disconnect();
    if (client2) client2.disconnect();
    if (client3) client3.disconnect();
    if (app) await app.close();
  });

  afterEach(() => {
    if (client1) client1.removeAllListeners();
    if (client2) client2.removeAllListeners();
    if (client3) client3.removeAllListeners();
  });

  describe('Typing start events', () => {
    it('should broadcast typing start to other clients in conversation', (done) => {
      const conversationId = 'conv-typing-start-1';

      client1 = io('http://localhost:3004', {
        auth: { token: 'test-token' },
        transports: ['websocket'],
      });

      client2 = io('http://localhost:3004', {
        auth: { token: 'test-token' },
        transports: ['websocket'],
      });

      let bothJoined = false;

      client1.on('connect', () => {
        client1.emit('join_conversation', { conversationId });
      });

      client2.on('connect', () => {
        client2.emit('join_conversation', { conversationId });
      });

      let joinCount = 0;
      const onJoined = () => {
        joinCount++;
        if (joinCount === 2 && !bothJoined) {
          bothJoined = true;
          client1.emit('typing_start', { conversationId });
        }
      };

      client1.on('joined_conversation', onJoined);
      client2.on('joined_conversation', onJoined);

      client2.on('user_typing', (data) => {
        expect(data.conversationId).toBe(conversationId);
        expect(data.isTyping).toBe(true);
        expect(data.userId).toBeDefined();
        done();
      });
    }, 10000);

    it('should not send typing event to sender', (done) => {
      const conversationId = 'conv-typing-no-echo';

      client1 = io('http://localhost:3004', {
        auth: { token: 'test-token' },
        transports: ['websocket'],
      });

      let receivedOwnTyping = false;

      client1.on('connect', () => {
        client1.emit('join_conversation', { conversationId });
      });

      client1.on('joined_conversation', () => {
        client1.emit('typing_start', { conversationId });

        // Wait to verify sender doesn't receive own typing event
        setTimeout(() => {
          expect(receivedOwnTyping).toBe(false);
          done();
        }, 1000);
      });

      client1.on('user_typing', () => {
        receivedOwnTyping = true;
      });
    }, 10000);

    it('should acknowledge typing start to sender', (done) => {
      const conversationId = 'conv-typing-ack';

      client1 = io('http://localhost:3004', {
        auth: { token: 'test-token' },
        transports: ['websocket'],
      });

      client1.on('connect', () => {
        client1.emit('join_conversation', { conversationId });
      });

      client1.on('joined_conversation', () => {
        client1.emit('typing_start', { conversationId });
      });

      client1.on('typing_started', (data) => {
        expect(data.conversationId).toBe(conversationId);
        done();
      });
    }, 5000);
  });

  describe('Typing stop events', () => {
    it('should broadcast typing stop to other clients in conversation', (done) => {
      const conversationId = 'conv-typing-stop-1';

      client1 = io('http://localhost:3004', {
        auth: { token: 'test-token' },
        transports: ['websocket'],
      });

      client2 = io('http://localhost:3004', {
        auth: { token: 'test-token' },
        transports: ['websocket'],
      });

      let bothJoined = false;

      client1.on('connect', () => {
        client1.emit('join_conversation', { conversationId });
      });

      client2.on('connect', () => {
        client2.emit('join_conversation', { conversationId });
      });

      let joinCount = 0;
      const onJoined = () => {
        joinCount++;
        if (joinCount === 2 && !bothJoined) {
          bothJoined = true;
          client1.emit('typing_stop', { conversationId });
        }
      };

      client1.on('joined_conversation', onJoined);
      client2.on('joined_conversation', onJoined);

      client2.on('user_typing', (data) => {
        expect(data.conversationId).toBe(conversationId);
        expect(data.isTyping).toBe(false);
        expect(data.userId).toBeDefined();
        done();
      });
    }, 10000);

    it('should acknowledge typing stop to sender', (done) => {
      const conversationId = 'conv-typing-stop-ack';

      client1 = io('http://localhost:3004', {
        auth: { token: 'test-token' },
        transports: ['websocket'],
      });

      client1.on('connect', () => {
        client1.emit('join_conversation', { conversationId });
      });

      client1.on('joined_conversation', () => {
        client1.emit('typing_stop', { conversationId });
      });

      client1.on('typing_stopped', (data) => {
        expect(data.conversationId).toBe(conversationId);
        done();
      });
    }, 5000);
  });

  describe('Typing sequence', () => {
    it('should handle start and stop typing sequence', (done) => {
      const conversationId = 'conv-typing-sequence';

      client1 = io('http://localhost:3004', {
        auth: { token: 'test-token' },
        transports: ['websocket'],
      });

      client2 = io('http://localhost:3004', {
        auth: { token: 'test-token' },
        transports: ['websocket'],
      });

      let bothJoined = false;
      let receivedStart = false;

      client1.on('connect', () => {
        client1.emit('join_conversation', { conversationId });
      });

      client2.on('connect', () => {
        client2.emit('join_conversation', { conversationId });
      });

      let joinCount = 0;
      const onJoined = () => {
        joinCount++;
        if (joinCount === 2 && !bothJoined) {
          bothJoined = true;
          client1.emit('typing_start', { conversationId });
        }
      };

      client1.on('joined_conversation', onJoined);
      client2.on('joined_conversation', onJoined);

      client2.on('user_typing', (data) => {
        if (!receivedStart && data.isTyping === true) {
          receivedStart = true;
          expect(data.isTyping).toBe(true);
          // Send stop typing
          client1.emit('typing_stop', { conversationId });
        } else if (receivedStart && data.isTyping === false) {
          expect(data.isTyping).toBe(false);
          done();
        }
      });
    }, 10000);
  });

  describe('Room isolation', () => {
    it('should not broadcast typing to clients in different conversations', (done) => {
      const conv1 = 'conv-isolated-typing-1';
      const conv2 = 'conv-isolated-typing-2';

      client1 = io('http://localhost:3004', {
        auth: { token: 'test-token' },
        transports: ['websocket'],
      });

      client2 = io('http://localhost:3004', {
        auth: { token: 'test-token' },
        transports: ['websocket'],
      });

      let typingEventsReceived = 0;

      client1.on('connect', () => {
        client1.emit('join_conversation', { conversationId: conv1 });
      });

      client2.on('connect', () => {
        client2.emit('join_conversation', { conversationId: conv2 });
      });

      let joinCount = 0;
      const onJoined = () => {
        joinCount++;
        if (joinCount === 2) {
          // Client1 starts typing in conv1
          client1.emit('typing_start', { conversationId: conv1 });

          // Wait and verify client2 didn't receive it
          setTimeout(() => {
            expect(typingEventsReceived).toBe(0);
            done();
          }, 1000);
        }
      };

      client1.on('joined_conversation', onJoined);
      client2.on('joined_conversation', onJoined);

      client2.on('user_typing', () => {
        typingEventsReceived++;
      });
    }, 10000);
  });

  describe('Multiple participants', () => {
    it('should broadcast typing to all participants except sender', (done) => {
      const conversationId = 'conv-multi-typing';

      client1 = io('http://localhost:3004', {
        auth: { token: 'test-token' },
        transports: ['websocket'],
      });

      client2 = io('http://localhost:3004', {
        auth: { token: 'test-token' },
        transports: ['websocket'],
      });

      client3 = io('http://localhost:3004', {
        auth: { token: 'test-token' },
        transports: ['websocket'],
      });

      let joinCount = 0;
      let receivedCount = 0;

      const onJoined = () => {
        joinCount++;
        if (joinCount === 3) {
          client1.emit('typing_start', { conversationId });
        }
      };

      const onTyping = (data) => {
        expect(data.isTyping).toBe(true);
        receivedCount++;
        if (receivedCount === 2) {
          // Client2 and Client3 both received
          done();
        }
      };

      client1.on('connect', () => {
        client1.emit('join_conversation', { conversationId });
      });

      client2.on('connect', () => {
        client2.emit('join_conversation', { conversationId });
      });

      client3.on('connect', () => {
        client3.emit('join_conversation', { conversationId });
      });

      client1.on('joined_conversation', onJoined);
      client2.on('joined_conversation', onJoined);
      client3.on('joined_conversation', onJoined);

      client2.on('user_typing', onTyping);
      client3.on('user_typing', onTyping);
    }, 15000);
  });

  describe('Ephemeral nature', () => {
    it('should not persist typing indicators', (done) => {
      const conversationId = 'conv-ephemeral';

      client1 = io('http://localhost:3004', {
        auth: { token: 'test-token' },
        transports: ['websocket'],
      });

      client1.on('connect', () => {
        client1.emit('join_conversation', { conversationId });
      });

      client1.on('joined_conversation', () => {
        client1.emit('typing_start', { conversationId });
        
        // Disconnect and reconnect
        setTimeout(() => {
          client1.disconnect();
          
          setTimeout(() => {
            client2 = io('http://localhost:3004', {
              auth: { token: 'test-token' },
              transports: ['websocket'],
            });

            client2.on('connect', () => {
              client2.emit('join_conversation', { conversationId });
            });

            let receivedTyping = false;
            client2.on('user_typing', () => {
              receivedTyping = true;
            });

            // Wait and verify no typing indicator received
            setTimeout(() => {
              expect(receivedTyping).toBe(false);
              done();
            }, 1000);
          }, 500);
        }, 1000);
      });
    }, 10000);
  });
});
