import { Test, TestingModule } from '@nestjs/testing';
import { INestApplication } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { io, Socket } from 'socket.io-client';
import { AppModule } from '../src/app.module';
import { RedisIoAdapter } from '../src/config/redis.config';

/**
 * E2E tests for Chat Gateway functionality.
 * 
 * These tests verify real-time chat operations including:
 * - Joining and leaving conversations
 * - Sending and receiving messages
 * - Room-based message isolation
 * - Cross-instance communication (via Redis)
 * 
 * Prerequisites:
 * - Redis server running on localhost:6379
 * - LocalStack or AWS SQS available (for message persistence)
 * 
 * To run these tests:
 * 1. Start Redis: docker run -p 6379:6379 redis:alpine
 * 2. Start LocalStack (optional): docker run -p 4566:4566 localstack/localstack
 * 3. Run tests: npm run test:e2e -- chat-gateway
 */
describe('Chat Gateway E2E', () => {
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
    
    await app.listen(3003);
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

  describe('Connection handling', () => {
    it('should connect with valid JWT token', (done) => {
      client1 = io('http://localhost:3003', {
        auth: { token: 'test-token' },
        transports: ['websocket'],
      });

      client1.on('connect', () => {
        expect(client1.connected).toBe(true);
        done();
      });
    }, 5000);

    it('should disconnect gracefully', (done) => {
      client1 = io('http://localhost:3003', {
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
  });

  describe('Conversation joining', () => {
    it('should join conversation successfully', (done) => {
      client1 = io('http://localhost:3003', {
        auth: { token: 'test-token' },
        transports: ['websocket'],
      });

      client1.on('connect', () => {
        client1.emit('join_conversation', { conversationId: 'conv-test-1' });
      });

      client1.on('joined_conversation', (data) => {
        expect(data.conversationId).toBe('conv-test-1');
        done();
      });
    }, 5000);

    it('should leave conversation successfully', (done) => {
      client1 = io('http://localhost:3003', {
        auth: { token: 'test-token' },
        transports: ['websocket'],
      });

      let joined = false;

      client1.on('connect', () => {
        client1.emit('join_conversation', { conversationId: 'conv-test-2' });
      });

      client1.on('joined_conversation', () => {
        joined = true;
        client1.emit('leave_conversation', { conversationId: 'conv-test-2' });
      });

      client1.on('left_conversation', (data) => {
        expect(joined).toBe(true);
        expect(data.conversationId).toBe('conv-test-2');
        done();
      });
    }, 5000);
  });

  describe('Message sending and receiving', () => {
    it('should send and receive messages in same conversation', (done) => {
      const conversationId = 'conv-messaging-1';
      const testMessage = 'Hello from E2E test';

      client1 = io('http://localhost:3003', {
        auth: { token: 'test-token' },
        transports: ['websocket'],
      });

      client2 = io('http://localhost:3003', {
        auth: { token: 'test-token' },
        transports: ['websocket'],
      });

      let client1Joined = false;
      let client2Joined = false;

      const checkBothJoined = () => {
        if (client1Joined && client2Joined) {
          client1.emit('send_message', {
            conversationId,
            content: testMessage,
            isDirect: true,
          });
        }
      };

      client1.on('connect', () => {
        client1.emit('join_conversation', { conversationId });
      });

      client2.on('connect', () => {
        client2.emit('join_conversation', { conversationId });
      });

      client1.on('joined_conversation', () => {
        client1Joined = true;
        checkBothJoined();
      });

      client2.on('joined_conversation', () => {
        client2Joined = true;
        checkBothJoined();
      });

      client2.on('new_message', (message) => {
        expect(message.conversationId).toBe(conversationId);
        expect(message.content).toBe(testMessage);
        expect(message.timestamp).toBeDefined();
        done();
      });
    }, 10000);

    it('should not receive messages from different conversation', (done) => {
      const conv1 = 'conv-isolated-1';
      const conv2 = 'conv-isolated-2';

      client1 = io('http://localhost:3003', {
        auth: { token: 'test-token' },
        transports: ['websocket'],
      });

      client2 = io('http://localhost:3003', {
        auth: { token: 'test-token' },
        transports: ['websocket'],
      });

      let messagesReceived = 0;

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
          // Send message to conv1
          client1.emit('send_message', {
            conversationId: conv1,
            content: 'Message for conv1',
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

      client2.on('new_message', () => {
        messagesReceived++;
      });
    }, 10000);

    it('should handle direct messages', (done) => {
      const conversationId = 'conv-direct-test';

      client1 = io('http://localhost:3003', {
        auth: { token: 'test-token' },
        transports: ['websocket'],
      });

      client2 = io('http://localhost:3003', {
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
          client1.emit('send_message', {
            conversationId,
            content: 'Direct message test',
            isDirect: true,
          });
        }
      };

      client1.on('joined_conversation', onJoined);
      client2.on('joined_conversation', onJoined);

      client2.on('new_message', (message) => {
        expect(message.content).toBe('Direct message test');
        done();
      });
    }, 10000);

    it('should handle group messages', (done) => {
      const conversationId = 'conv-group-test';

      client1 = io('http://localhost:3003', {
        auth: { token: 'test-token' },
        transports: ['websocket'],
      });

      client2 = io('http://localhost:3003', {
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
          client1.emit('send_message', {
            conversationId,
            content: 'Group message test',
            isDirect: false,
          });
        }
      };

      client1.on('joined_conversation', onJoined);
      client2.on('joined_conversation', onJoined);

      client2.on('new_message', (message) => {
        expect(message.content).toBe('Group message test');
        done();
      });
    }, 10000);
  });

  describe('Multiple participants', () => {
    it('should broadcast to all participants in conversation', (done) => {
      const conversationId = 'conv-multi-participant';
      const testMessage = 'Message to all';

      client1 = io('http://localhost:3003', {
        auth: { token: 'test-token' },
        transports: ['websocket'],
      });

      client2 = io('http://localhost:3003', {
        auth: { token: 'test-token' },
        transports: ['websocket'],
      });

      client3 = io('http://localhost:3003', {
        auth: { token: 'test-token' },
        transports: ['websocket'],
      });

      let joinCount = 0;
      let receivedCount = 0;

      const onJoined = () => {
        joinCount++;
        if (joinCount === 3) {
          client1.emit('send_message', {
            conversationId,
            content: testMessage,
            isDirect: false,
          });
        }
      };

      const onMessage = (message) => {
        expect(message.content).toBe(testMessage);
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

      client2.on('new_message', onMessage);
      client3.on('new_message', onMessage);
    }, 15000);
  });

  describe('Message acknowledgment', () => {
    it('should acknowledge message sent', (done) => {
      const conversationId = 'conv-ack-test';

      client1 = io('http://localhost:3003', {
        auth: { token: 'test-token' },
        transports: ['websocket'],
      });

      client1.on('connect', () => {
        client1.emit('join_conversation', { conversationId });
      });

      client1.on('joined_conversation', () => {
        client1.emit('send_message', {
          conversationId,
          content: 'Test acknowledgment',
          isDirect: true,
        });
      });

      client1.on('message_sent', (data) => {
        expect(data.conversationId).toBe(conversationId);
        done();
      });
    }, 5000);
  });
});
