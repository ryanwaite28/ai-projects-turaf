import { Socket } from 'socket.io';

/**
 * Authenticated Socket interface.
 * 
 * Extends the Socket.IO Socket interface to include user information
 * attached during authentication.
 */
export interface AuthenticatedSocket extends Socket {
  data: {
    user: {
      userId: string;
      email: string;
      organizationId: string;
    };
  };
}
