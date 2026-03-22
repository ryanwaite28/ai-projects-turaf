import { registerAs } from '@nestjs/config';

/**
 * JWT configuration for WebSocket authentication.
 * 
 * The WebSocket Gateway validates JWT tokens issued by the Identity Service
 * to authenticate WebSocket connections.
 * 
 * Configuration:
 * - JWT_SECRET: Secret key for validating tokens (must match Identity Service)
 */
export default registerAs('jwt', () => ({
  secret: process.env.JWT_SECRET || 'your-256-bit-secret',
}));
