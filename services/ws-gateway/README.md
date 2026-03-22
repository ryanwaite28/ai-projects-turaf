# WebSocket Gateway

Real-time WebSocket gateway for Turaf Communications.

## Features

- WebSocket connections with Socket.IO
- JWT authentication
- Redis Pub/Sub for horizontal scaling
- SQS FIFO queue publishing
- Typing indicators
- Real-time message broadcasting
- Stateless design for horizontal scalability

## Tech Stack

- **Framework**: NestJS
- **WebSocket**: Socket.IO
- **Scaling**: Redis Pub/Sub
- **Messaging**: AWS SQS FIFO
- **Authentication**: JWT

## Architecture

The WebSocket Gateway is stateless and horizontally scalable:

```
Client 1 → ALB → Gateway Instance 1 ─┐
Client 2 → ALB → Gateway Instance 2 ─┼─→ Redis Pub/Sub ←─→ All Instances
Client 3 → ALB → Gateway Instance 3 ─┘
                        ↓
                   SQS FIFO Queues
                        ↓
            Communications Service (Persistence)
```

## Development

### Prerequisites

- Node.js 20+
- Redis (for local development)
- LocalStack (for SQS emulation)

### Setup

```bash
# Install dependencies
npm install

# Copy environment file
cp .env.example .env

# Start development server
npm run start:dev
```

### Environment Variables

See `.env.example` for required configuration.

## Build

```bash
npm run build
```

## Docker

```bash
# Build image
docker build -t turaf/ws-gateway:latest .

# Run container
docker run -p 3000:3000 --env-file .env turaf/ws-gateway:latest
```

## Testing

```bash
# Unit tests
npm test

# E2E tests
npm run test:e2e

# Test coverage
npm run test:cov
```

## WebSocket Events

### Client → Server

- `join_conversation` - Join a conversation room
- `leave_conversation` - Leave a conversation room
- `send_message` - Send a message
- `typing_start` - Start typing indicator
- `typing_stop` - Stop typing indicator

### Server → Client

- `new_message` - New message in conversation
- `user_typing` - User typing status changed
- `joined_conversation` - Confirmation of room join
- `left_conversation` - Confirmation of room leave

## Authentication

WebSocket connections must include a JWT token:

```javascript
// Option 1: Authorization header
const socket = io('ws://localhost:3000', {
  extraHeaders: {
    Authorization: 'Bearer <token>'
  }
});

// Option 2: Query parameter
const socket = io('ws://localhost:3000?token=<token>');

// Option 3: Auth object
const socket = io('ws://localhost:3000', {
  auth: {
    token: '<token>'
  }
});
```

## Scaling

The gateway uses Redis Pub/Sub to broadcast events across multiple instances:

1. Client sends message to Gateway Instance 1
2. Instance 1 publishes to SQS for persistence
3. Instance 1 broadcasts via Redis Pub/Sub
4. All instances receive broadcast and emit to their connected clients
5. Clients connected to any instance receive the message

## References

- **Spec**: `specs/ws-gateway.md`
- **NestJS Docs**: https://docs.nestjs.com/websockets/gateways
- **Socket.IO Docs**: https://socket.io/docs/v4/
- **Redis Adapter**: https://socket.io/docs/v4/redis-adapter/
