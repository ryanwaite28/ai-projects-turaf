import * as fromSelectors from './auth.selectors';
import { AuthState } from './auth.state';
import { User, UserRole } from '../../models/user.model';

describe('Auth Selectors', () => {
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
  
  const mockAuthState: AuthState = {
    user: mockUser,
    token: mockToken,
    loading: false,
    error: null
  };
  
  const mockAppState = {
    auth: mockAuthState
  };
  
  describe('selectAuthState', () => {
    it('should select the auth state', () => {
      const result = fromSelectors.selectAuthState(mockAppState as any);
      expect(result).toEqual(mockAuthState);
    });
  });
  
  describe('selectUser', () => {
    it('should select the user', () => {
      const result = fromSelectors.selectUser(mockAppState as any);
      expect(result).toEqual(mockUser);
    });
    
    it('should return null when user is not set', () => {
      const state = { auth: { ...mockAuthState, user: null } };
      const result = fromSelectors.selectUser(state as any);
      expect(result).toBeNull();
    });
  });
  
  describe('selectUserId', () => {
    it('should select the user ID', () => {
      const result = fromSelectors.selectUserId(mockAppState as any);
      expect(result).toEqual('1');
    });
  });
  
  describe('selectUserEmail', () => {
    it('should select the user email', () => {
      const result = fromSelectors.selectUserEmail(mockAppState as any);
      expect(result).toEqual('test@example.com');
    });
  });
  
  describe('selectUserFullName', () => {
    it('should select the user full name', () => {
      const result = fromSelectors.selectUserFullName(mockAppState as any);
      expect(result).toEqual('Test User');
    });
    
    it('should return null when user is not set', () => {
      const state = { auth: { ...mockAuthState, user: null } };
      const result = fromSelectors.selectUserFullName(state as any);
      expect(result).toBeNull();
    });
  });
  
  describe('selectUserRole', () => {
    it('should select the user role', () => {
      const result = fromSelectors.selectUserRole(mockAppState as any);
      expect(result).toEqual(UserRole.ADMIN);
    });
  });
  
  describe('selectUserOrganizationId', () => {
    it('should select the user organization ID', () => {
      const result = fromSelectors.selectUserOrganizationId(mockAppState as any);
      expect(result).toEqual('org-1');
    });
  });
  
  describe('selectToken', () => {
    it('should select the token', () => {
      const result = fromSelectors.selectToken(mockAppState as any);
      expect(result).toEqual(mockToken);
    });
  });
  
  describe('selectAuthLoading', () => {
    it('should select the loading state', () => {
      const result = fromSelectors.selectAuthLoading(mockAppState as any);
      expect(result).toBe(false);
    });
  });
  
  describe('selectAuthError', () => {
    it('should select the error', () => {
      const result = fromSelectors.selectAuthError(mockAppState as any);
      expect(result).toBeNull();
    });
    
    it('should select error when present', () => {
      const state = { auth: { ...mockAuthState, error: 'Login failed' } };
      const result = fromSelectors.selectAuthError(state as any);
      expect(result).toEqual('Login failed');
    });
  });
  
  describe('selectIsAuthenticated', () => {
    it('should return true when user and token are present', () => {
      const result = fromSelectors.selectIsAuthenticated(mockAppState as any);
      expect(result).toBe(true);
    });
    
    it('should return false when user is null', () => {
      const state = { auth: { ...mockAuthState, user: null } };
      const result = fromSelectors.selectIsAuthenticated(state as any);
      expect(result).toBe(false);
    });
    
    it('should return false when token is null', () => {
      const state = { auth: { ...mockAuthState, token: null } };
      const result = fromSelectors.selectIsAuthenticated(state as any);
      expect(result).toBe(false);
    });
  });
  
  describe('role selectors', () => {
    it('selectIsAdmin should return true for ADMIN role', () => {
      const result = fromSelectors.selectIsAdmin(mockAppState as any);
      expect(result).toBe(true);
    });
    
    it('selectIsMember should return true for MEMBER role', () => {
      const memberUser = { ...mockUser, role: UserRole.MEMBER };
      const state = { auth: { ...mockAuthState, user: memberUser } };
      const result = fromSelectors.selectIsMember(state as any);
      expect(result).toBe(true);
    });
    
    it('selectIsViewer should return true for VIEWER role', () => {
      const viewerUser = { ...mockUser, role: UserRole.VIEWER };
      const state = { auth: { ...mockAuthState, user: viewerUser } };
      const result = fromSelectors.selectIsViewer(state as any);
      expect(result).toBe(true);
    });
  });
  
  describe('selectAuthViewModel', () => {
    it('should select combined auth view model', () => {
      const result = fromSelectors.selectAuthViewModel(mockAppState as any);
      
      expect(result).toEqual({
        user: mockUser,
        loading: false,
        error: null,
        isAuthenticated: true
      });
    });
  });
});
