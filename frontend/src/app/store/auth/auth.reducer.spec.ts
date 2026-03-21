import { authReducer } from './auth.reducer';
import { initialAuthState, AuthState } from './auth.state';
import * as AuthActions from './auth.actions';
import { User, UserRole } from '../../models/user.model';

describe('AuthReducer', () => {
  const mockUser: User = {
    id: '1',
    email: 'test@example.com',
    firstName: 'Test',
    lastName: 'User',
    organizationId: 'org-1',
    role: UserRole.ADMIN,
    createdAt: '2024-03-20T12:00:00Z',
    updatedAt: '2024-03-20T12:00:00Z'
  };
  
  const mockToken = 'mock-jwt-token';
  
  describe('initial state', () => {
    it('should return the initial state', () => {
      const action = { type: 'Unknown' };
      const state = authReducer(undefined, action);
      
      expect(state).toEqual(initialAuthState);
    });
  });
  
  describe('login actions', () => {
    it('should set loading to true on login', () => {
      const action = AuthActions.login({
        email: 'test@example.com',
        password: 'password'
      });
      const state = authReducer(initialAuthState, action);
      
      expect(state.loading).toBe(true);
      expect(state.error).toBeNull();
    });
    
    it('should set user and token on loginSuccess', () => {
      const action = AuthActions.loginSuccess({
        user: mockUser,
        token: mockToken
      });
      const state = authReducer(initialAuthState, action);
      
      expect(state.user).toEqual(mockUser);
      expect(state.token).toEqual(mockToken);
      expect(state.loading).toBe(false);
      expect(state.error).toBeNull();
    });
    
    it('should set error on loginFailure', () => {
      const error = 'Invalid credentials';
      const action = AuthActions.loginFailure({ error });
      const state = authReducer(initialAuthState, action);
      
      expect(state.loading).toBe(false);
      expect(state.error).toEqual(error);
      expect(state.user).toBeNull();
      expect(state.token).toBeNull();
    });
  });
  
  describe('logout actions', () => {
    it('should set loading to true on logout', () => {
      const currentState: AuthState = {
        user: mockUser,
        token: mockToken,
        loading: false,
        error: null
      };
      const action = AuthActions.logout();
      const state = authReducer(currentState, action);
      
      expect(state.loading).toBe(true);
    });
    
    it('should reset to initial state on logoutSuccess', () => {
      const currentState: AuthState = {
        user: mockUser,
        token: mockToken,
        loading: true,
        error: null
      };
      const action = AuthActions.logoutSuccess();
      const state = authReducer(currentState, action);
      
      expect(state).toEqual(initialAuthState);
    });
  });
  
  describe('token refresh actions', () => {
    it('should set loading to true on refreshToken', () => {
      const currentState: AuthState = {
        user: mockUser,
        token: mockToken,
        loading: false,
        error: null
      };
      const action = AuthActions.refreshToken();
      const state = authReducer(currentState, action);
      
      expect(state.loading).toBe(true);
      expect(state.error).toBeNull();
    });
    
    it('should update token on refreshTokenSuccess', () => {
      const newToken = 'new-jwt-token';
      const currentState: AuthState = {
        user: mockUser,
        token: mockToken,
        loading: true,
        error: null
      };
      const action = AuthActions.refreshTokenSuccess({ token: newToken });
      const state = authReducer(currentState, action);
      
      expect(state.token).toEqual(newToken);
      expect(state.loading).toBe(false);
      expect(state.error).toBeNull();
      expect(state.user).toEqual(mockUser);
    });
    
    it('should set error on refreshTokenFailure', () => {
      const error = 'Token refresh failed';
      const currentState: AuthState = {
        user: mockUser,
        token: mockToken,
        loading: true,
        error: null
      };
      const action = AuthActions.refreshTokenFailure({ error });
      const state = authReducer(currentState, action);
      
      expect(state.loading).toBe(false);
      expect(state.error).toEqual(error);
    });
  });
  
  describe('load from storage actions', () => {
    it('should set loading to true on loadUserFromStorage', () => {
      const action = AuthActions.loadUserFromStorage();
      const state = authReducer(initialAuthState, action);
      
      expect(state.loading).toBe(true);
    });
    
    it('should set user and token on loadUserFromStorageSuccess', () => {
      const action = AuthActions.loadUserFromStorageSuccess({
        user: mockUser,
        token: mockToken
      });
      const state = authReducer(initialAuthState, action);
      
      expect(state.user).toEqual(mockUser);
      expect(state.token).toEqual(mockToken);
      expect(state.loading).toBe(false);
      expect(state.error).toBeNull();
    });
    
    it('should set loading to false on loadUserFromStorageFailure', () => {
      const currentState: AuthState = {
        ...initialAuthState,
        loading: true
      };
      const action = AuthActions.loadUserFromStorageFailure();
      const state = authReducer(currentState, action);
      
      expect(state.loading).toBe(false);
    });
  });
  
  describe('clearAuthError action', () => {
    it('should clear error', () => {
      const currentState: AuthState = {
        ...initialAuthState,
        error: 'Some error'
      };
      const action = AuthActions.clearAuthError();
      const state = authReducer(currentState, action);
      
      expect(state.error).toBeNull();
    });
  });
});
