import { Test, TestingModule } from '@nestjs/testing';
import { INestApplication } from '@nestjs/common';
import { AppModule } from '../src/app.module';
import { io, Socket } from 'socket.io-client';
import * as jwt from 'jsonwebtoken';

function generateTestJWT(userId: string, organizationId: string = 'org-123'): string {
  return jwt.sign(
    {
      sub: userId,
      email: `${userId}@test.com`,
      organizationId,
    },
    process.env.JWT_SECRET || 'test-secret-key',
    { expiresIn: '1h' },
  );
}

describe('AppModule (e2e)', () => {
  let app: INestApplication;
  let serverUrl: string;

  beforeAll(async () => {
    const moduleFixture: TestingModule = await Test.createTestingModule({
      imports: [AppModule],
    }).compile();

    app = moduleFixture.createNestApplication();
    await app.listen(0);

    const address = app.getHttpServer().address();
    const port = address.port;
    serverUrl = `http://localhost:${port}`;
  });

  afterAll(async () => {
    await app.close();
  });

  describe('Application Initialization', () => {
    it('should start the application successfully', () => {
      expect(app).toBeDefined();
      expect(app.getHttpServer()).toBeDefined();
    });

    it('should accept WebSocket connections', (done) => {
      const validToken = generateTestJWT('user-app-test');
      const client = io(serverUrl, {
        auth: { token: validToken },
        transports: ['websocket'],
      });

      client.on('connect', () => {
        expect(client.connected).toBe(true);
        client.disconnect();
        done();
      });

      client.on('connect_error', (error) => {
        client.disconnect();
        done(error);
      });
    });
  });

  describe('Full Message Flow', () => {
    let client1: Socket;
    let client2: Socket;

    beforeEach((done) => {
      const token1 = generateTestJWT('user-flow-1');
      const token2 = generateTestJWT('user-flow-2');

      client1 = io(serverUrl, {
        auth: { token: token1 },
        transports: ['websocket'],
      });

      client2 = io(serverUrl, {
        auth: { token: token2 },
        transports: ['websocket'],
      });

      let connectedCount = 0;
      const checkDone = () => {
        connectedCount++;
        if (connectedCount === 2) done();
      };

      client1.on('connect', checkDone);
      client2.on('connect', checkDone);
    });

    afterEach(() => {
      client1.disconnect();
      client2.disconnect();
    });

    it('should handle complete conversation flow', (done) => {
      const conversationId = 'conv-full-flow';
      let step = 0;

      client1.emit('join_conversation', { conversationId });
      client2.emit('join_conversation', { conversationId });

      client1.on('joined_conversation', (data) => {
        expect(data.conversationId).toBe(conversationId);
        step++;
        if (step === 2) {
          client1.emit('send_message', {
            conversationId,
            content: 'Hello from client 1',
            isDirect: true,
          });
        }
      });

      client2.on('joined_conversation', (data) => {
        expect(data.conversationId).toBe(conversationId);
        step++;
      });

      client2.on('new_message', (message) => {
        expect(message.content).toBe('Hello from client 1');
        expect(message.conversationId).toBe(conversationId);
        done();
      });
    });

    it('should handle typing indicators in conversation', (done) => {
      const conversationId = 'conv-typing-flow';

      client1.emit('join_conversation', { conversationId });
      client2.emit('join_conversation', { conversationId });

      client2.on('user_typing', (data) => {
        expect(data.conversationId).toBe(conversationId);
        expect(data.userId).toBe('user-flow-1');
        expect(data.isTyping).toBe(true);
        done();
      });

      setTimeout(() => {
        client1.emit('typing_start', { conversationId });
      }, 200);
    });

    it('should handle leave conversation', (done) => {
      const conversationId = 'conv-leave-flow';

      client1.emit('join_conversation', { conversationId });

      client1.on('joined_conversation', () => {
        setTimeout(() => {
          client1.emit('leave_conversation', { conversationId });
        }, 100);
      });

      client1.on('left_conversation', (data) => {
        expect(data.conversationId).toBe(conversationId);
        done();
      });
    });
  });

  describe('Multiple Conversations', () => {
    let client1: Socket;
    let client2: Socket;
    let client3: Socket;

    beforeEach((done) => {
      const token1 = generateTestJWT('user-multi-conv-1');
      const token2 = generateTestJWT('user-multi-conv-2');
      const token3 = generateTestJWT('user-multi-conv-3');

      client1 = io(serverUrl, {
        auth: { token: token1 },
        transports: ['websocket'],
      });

      client2 = io(serverUrl, {
        auth: { token: token2 },
        transports: ['websocket'],
      });

      client3 = io(serverUrl, {
        auth: { token: token3 },
        transports: ['websocket'],
      });

      let connectedCount = 0;
      const checkDone = () => {
        connectedCount++;
        if (connectedCount === 3) done();
      };

      client1.on('connect', checkDone);
      client2.on('connect', checkDone);
      client3.on('connect', checkDone);
    });

    afterEach(() => {
      client1.disconnect();
      client2.disconnect();
      client3.disconnect();
    });

    it('should isolate messages between different conversations', (done) => {
      const conv1 = 'conv-isolation-1';
      const conv2 = 'conv-isolation-2';

      client1.emit('join_conversation', { conversationId: conv1 });
      client2.emit('join_conversation', { conversationId: conv1 });
      client3.emit('join_conversation', { conversationId: conv2 });

      let client2ReceivedMessage = false;
      let client3ReceivedMessage = false;

      client2.on('new_message', (message) => {
        expect(message.conversationId).toBe(conv1);
        client2ReceivedMessage = true;
        checkCompletion();
      });

      client3.on('new_message', () => {
        client3ReceivedMessage = true;
      });

      const checkCompletion = () => {
        setTimeout(() => {
          expect(client2ReceivedMessage).toBe(true);
          expect(client3ReceivedMessage).toBe(false);
          done();
        }, 500);
      };

      setTimeout(() => {
        client1.emit('send_message', {
          conversationId: conv1,
          content: 'Message for conv1 only',
          isDirect: true,
        });
      }, 200);
    });

    it('should allow user to join multiple conversations', (done) => {
      const conv1 = 'conv-multi-join-1';
      const conv2 = 'conv-multi-join-2';

      let joinedCount = 0;

      client1.on('joined_conversation', (data) => {
        joinedCount++;
        if (joinedCount === 2) {
          expect(joinedCount).toBe(2);
          done();
        }
      });

      client1.emit('join_conversation', { conversationId: conv1 });
      setTimeout(() => {
        client1.emit('join_conversation', { conversationId: conv2 });
      }, 100);
    });
  });

  describe('Error Handling', () => {
    let client: Socket;

    beforeEach((done) => {
      const token = generateTestJWT('user-error-test');
      client = io(serverUrl, {
        auth: { token },
        transports: ['websocket'],
      });

      client.on('connect', done);
    });

    afterEach(() => {
      client.disconnect();
    });

    it('should handle invalid event data gracefully', (done) => {
      client.emit('join_conversation', { invalidField: 'test' });

      setTimeout(() => {
        expect(client.connected).toBe(true);
        done();
      }, 500);
    });

    it('should maintain connection after error', (done) => {
      client.emit('join_conversation', null);

      setTimeout(() => {
        expect(client.connected).toBe(true);
        client.emit('join_conversation', { conversationId: 'conv-valid' });

        client.on('joined_conversation', (data) => {
          expect(data.conversationId).toBe('conv-valid');
          done();
        });
      }, 200);
    });
  });

  describe('Concurrent Operations', () => {
    let clients: Socket[];

    beforeEach((done) => {
      clients = [];
      let connectedCount = 0;
      const totalClients = 5;

      for (let i = 0; i < totalClients; i++) {
        const token = generateTestJWT(`user-concurrent-${i}`);
        const client = io(serverUrl, {
          auth: { token },
          transports: ['websocket'],
        });

        client.on('connect', () => {
          connectedCount++;
          if (connectedCount === totalClients) done();
        });

        clients.push(client);
      }
    });

    afterEach(() => {
      clients.forEach((client) => client.disconnect());
    });

    it('should handle multiple clients joining same conversation', (done) => {
      const conversationId = 'conv-concurrent';
      let joinedCount = 0;

      clients.forEach((client) => {
        client.on('joined_conversation', (data) => {
          expect(data.conversationId).toBe(conversationId);
          joinedCount++;
          if (joinedCount === clients.length) {
            done();
          }
        });

        client.emit('join_conversation', { conversationId });
      });
    });

    it('should broadcast message to all clients in conversation', (done) => {
      const conversationId = 'conv-broadcast';
      let receivedCount = 0;

      clients.forEach((client, index) => {
        client.emit('join_conversation', { conversationId });

        if (index > 0) {
          client.on('new_message', (message) => {
            expect(message.content).toBe('Broadcast test');
            receivedCount++;
            if (receivedCount === clients.length - 1) {
              done();
            }
          });
        }
      });

      setTimeout(() => {
        clients[0].emit('send_message', {
          conversationId,
          content: 'Broadcast test',
          isDirect: true,
        });
      }, 300);
    });
  });

  describe('Performance', () => {
    it('should handle rapid successive events', (done) => {
      const token = generateTestJWT('user-rapid');
      const client = io(serverUrl, {
        auth: { token },
        transports: ['websocket'],
      });

      client.on('connect', () => {
        const conversationId = 'conv-rapid';
        let joinedCount = 0;

        client.on('joined_conversation', () => {
          joinedCount++;
          if (joinedCount === 10) {
            expect(joinedCount).toBe(10);
            client.disconnect();
            done();
          }
        });

        for (let i = 0; i < 10; i++) {
          client.emit('join_conversation', { conversationId: `${conversationId}-${i}` });
        }
      });
    });
  });
});
