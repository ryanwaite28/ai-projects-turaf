import { User } from '../../models/user.model';

/**
 * Authentication state interface
 * 
 * Manages user authentication state including:
 * - Current authenticated user
 * - JWT token for API requests
 * - Loading state for async operations
 * - Error messages for failed operations
 */
export interface AuthState {
  user: User | null;
  token: string | null;
  loading: boolean;
  error: string | null;
}

/**
 * Initial authentication state
 * 
 * User starts as unauthenticated with no token.
 * This state is also used when user logs out.
 */
export const initialAuthState: AuthState = {
  user: null,
  token: null,
  loading: false,
  error: null
};
