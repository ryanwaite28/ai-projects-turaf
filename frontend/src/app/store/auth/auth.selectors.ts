import { createFeatureSelector, createSelector } from '@ngrx/store';
import { AuthState } from './auth.state';

/**
 * Authentication Selectors
 * 
 * Memoized selectors for efficiently accessing auth state.
 * 
 * Following NgRx best practices:
 * - Use createFeatureSelector for top-level state
 * - Use createSelector for derived state
 * - Selectors are memoized for performance
 */

// Feature selector for auth state
export const selectAuthState = createFeatureSelector<AuthState>('auth');

// User selectors
export const selectUser = createSelector(
  selectAuthState,
  (state: AuthState) => state.user
);

export const selectUserId = createSelector(
  selectUser,
  (user) => user?.id
);

export const selectUserEmail = createSelector(
  selectUser,
  (user) => user?.email
);

export const selectUserFullName = createSelector(
  selectUser,
  (user) => user ? `${user.firstName} ${user.lastName}` : null
);

export const selectUserRole = createSelector(
  selectUser,
  (user) => user?.role
);

export const selectUserOrganizationId = createSelector(
  selectUser,
  (user) => user?.organizationId
);

// Token selector
export const selectToken = createSelector(
  selectAuthState,
  (state: AuthState) => state.token
);

// Loading selector
export const selectAuthLoading = createSelector(
  selectAuthState,
  (state: AuthState) => state.loading
);

// Error selector
export const selectAuthError = createSelector(
  selectAuthState,
  (state: AuthState) => state.error
);

// Authentication status selectors
export const selectIsAuthenticated = createSelector(
  selectUser,
  selectToken,
  (user, token) => !!user && !!token
);

export const selectIsAdmin = createSelector(
  selectUserRole,
  (role) => role === 'ADMIN'
);

export const selectIsMember = createSelector(
  selectUserRole,
  (role) => role === 'MEMBER'
);

export const selectIsViewer = createSelector(
  selectUserRole,
  (role) => role === 'VIEWER'
);

// Combined selectors for UI
export const selectAuthViewModel = createSelector(
  selectUser,
  selectAuthLoading,
  selectAuthError,
  selectIsAuthenticated,
  (user, loading, error, isAuthenticated) => ({
    user,
    loading,
    error,
    isAuthenticated
  })
);
