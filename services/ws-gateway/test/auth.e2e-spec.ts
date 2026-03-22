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

describe('Authentication (e2e)', () => {
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

  describe('Connection Authentication', () => {
    it('should reject connection without token', (done) => {
      const client = io(serverUrl, {
        transports: ['websocket'],
      });

      client.on('connect_error', (error) => {
        expect(error.message).toContain('Unauthorized');
        client.disconnect();
        done();
      });

      setTimeout(() => {
        if (client.connected) {
          client.disconnect();
          done(new Error('Client should not have connected'));
        }
      }, 1000);
    });

    it('should reject connection with invalid token', (done) => {
      const client = io(serverUrl, {
        auth: { token: 'invalid-token-12345' },
        transports: ['websocket'],
      });

      client.on('connect_error', (error) => {
        expect(error.message).toContain('Unauthorized');
        client.disconnect();
        done();
      });

      setTimeout(() => {
        if (client.connected) {
          client.disconnect();
          done(new Error('Client should not have connected'));
        }
      }, 1000);
    });

    it('should reject connection with expired token', (done) => {
      const expiredToken = jwt.sign(
        { sub: 'user-123', email: 'user@test.com', organizationId: 'org-123' },
        process.env.JWT_SECRET || 'test-secret-key',
        { expiresIn: '-1h' },
      );

      const client = io(serverUrl, {
        auth: { token: expiredToken },
        transports: ['websocket'],
      });

      client.on('connect_error', (error) => {
        expect(error.message).toContain('Unauthorized');
        client.disconnect();
        done();
      });

      setTimeout(() => {
        if (client.connected) {
          client.disconnect();
          done(new Error('Client should not have connected'));
        }
      }, 1000);
    });

    it('should accept connection with valid token in auth object', (done) => {
      const validToken = generateTestJWT('user-valid-1');
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

    it('should accept connection with valid token in query params', (done) => {
      const validToken = generateTestJWT('user-valid-2');
      const client = io(serverUrl, {
        query: { token: validToken },
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

    it('should accept connection with valid token in extraHeaders', (done) => {
      const validToken = generateTestJWT('user-valid-3');
      const client = io(serverUrl, {
        extraHeaders: {
          authorization: `Bearer ${validToken}`,
        },
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

    it('should reject connection with malformed Bearer token', (done) => {
      const client = io(serverUrl, {
        extraHeaders: {
          authorization: 'Bearer',
        },
        transports: ['websocket'],
      });

      client.on('connect_error', (error) => {
        expect(error.message).toContain('Unauthorized');
        client.disconnect();
        done();
      });

      setTimeout(() => {
        if (client.connected) {
          client.disconnect();
          done(new Error('Client should not have connected'));
        }
      }, 1000);
    });
  });

  describe('User Data Attachment', () => {
    it('should attach user data to socket after authentication', (done) => {
      const userId = 'user-data-test';
      const organizationId = 'org-data-test';
      const validToken = generateTestJWT(userId, organizationId);

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

  describe('Multiple Clients', () => {
    it('should allow multiple clients with different tokens', (done) => {
      const token1 = generateTestJWT('user-multi-1');
      const token2 = generateTestJWT('user-multi-2');

      const client1 = io(serverUrl, {
        auth: { token: token1 },
        transports: ['websocket'],
      });

      const client2 = io(serverUrl, {
        auth: { token: token2 },
        transports: ['websocket'],
      });

      let connectedCount = 0;
      const checkDone = () => {
        connectedCount++;
        if (connectedCount === 2) {
          expect(client1.connected).toBe(true);
          expect(client2.connected).toBe(true);
          client1.disconnect();
          client2.disconnect();
          done();
        }
      };

      client1.on('connect', checkDone);
      client2.on('connect', checkDone);

      client1.on('connect_error', (error) => {
        client1.disconnect();
        client2.disconnect();
        done(error);
      });

      client2.on('connect_error', (error) => {
        client1.disconnect();
        client2.disconnect();
        done(error);
      });
    });

    it('should allow same user to connect multiple times', (done) => {
      const token = generateTestJWT('user-same');

      const client1 = io(serverUrl, {
        auth: { token },
        transports: ['websocket'],
      });

      const client2 = io(serverUrl, {
        auth: { token },
        transports: ['websocket'],
      });

      let connectedCount = 0;
      const checkDone = () => {
        connectedCount++;
        if (connectedCount === 2) {
          expect(client1.connected).toBe(true);
          expect(client2.connected).toBe(true);
          client1.disconnect();
          client2.disconnect();
          done();
        }
      };

      client1.on('connect', checkDone);
      client2.on('connect', checkDone);

      client1.on('connect_error', (error) => {
        client1.disconnect();
        client2.disconnect();
        done(error);
      });

      client2.on('connect_error', (error) => {
        client1.disconnect();
        client2.disconnect();
        done(error);
      });
    });
  });

  describe('Disconnection', () => {
    it('should handle client disconnection gracefully', (done) => {
      const validToken = generateTestJWT('user-disconnect');
      const client = io(serverUrl, {
        auth: { token: validToken },
        transports: ['websocket'],
      });

      client.on('connect', () => {
        expect(client.connected).toBe(true);
        client.disconnect();
      });

      client.on('disconnect', () => {
        expect(client.connected).toBe(false);
        done();
      });

      client.on('connect_error', (error) => {
        client.disconnect();
        done(error);
      });
    });

    it('should allow reconnection after disconnection', (done) => {
      const validToken = generateTestJWT('user-reconnect');
      const client = io(serverUrl, {
        auth: { token: validToken },
        transports: ['websocket'],
        reconnection: true,
      });

      let connectCount = 0;

      client.on('connect', () => {
        connectCount++;
        if (connectCount === 1) {
          client.disconnect();
          setTimeout(() => {
            client.connect();
          }, 100);
        } else if (connectCount === 2) {
          expect(client.connected).toBe(true);
          client.disconnect();
          done();
        }
      });

      client.on('connect_error', (error) => {
        client.disconnect();
        done(error);
      });
    });
  });

  describe('Token Validation', () => {
    it('should reject token with missing required fields', (done) => {
      const invalidToken = jwt.sign(
        { email: 'user@test.com' },
        process.env.JWT_SECRET || 'test-secret-key',
        { expiresIn: '1h' },
      );

      const client = io(serverUrl, {
        auth: { token: invalidToken },
        transports: ['websocket'],
      });

      client.on('connect_error', (error) => {
        expect(error.message).toContain('Unauthorized');
        client.disconnect();
        done();
      });

      setTimeout(() => {
        if (client.connected) {
          client.disconnect();
          done(new Error('Client should not have connected'));
        }
      }, 1000);
    });

    it('should reject token signed with wrong secret', (done) => {
      const wrongSecretToken = jwt.sign(
        { sub: 'user-123', email: 'user@test.com', organizationId: 'org-123' },
        'wrong-secret-key',
        { expiresIn: '1h' },
      );

      const client = io(serverUrl, {
        auth: { token: wrongSecretToken },
        transports: ['websocket'],
      });

      client.on('connect_error', (error) => {
        expect(error.message).toContain('Unauthorized');
        client.disconnect();
        done();
      });

      setTimeout(() => {
        if (client.connected) {
          client.disconnect();
          done(new Error('Client should not have connected'));
        }
      }, 1000);
    });
  });
});
