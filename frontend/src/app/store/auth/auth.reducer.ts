import { createReducer, on } from '@ngrx/store';
import { AuthState, initialAuthState } from './auth.state';
import * as AuthActions from './auth.actions';

/**
 * Authentication Reducer
 * 
 * Pure function that handles state transitions based on dispatched actions.
 * 
 * Following NgRx best practices:
 * - Immutable state updates
 * - No side effects
 * - Predictable state transitions
 */
export const authReducer = createReducer(
  initialAuthState,
  
  // Login
  on(AuthActions.login, (state): AuthState => ({
    ...state,
    loading: true,
    error: null
  })),
  
  on(AuthActions.loginSuccess, (state, { user, token }): AuthState => ({
    ...state,
    user,
    token,
    loading: false,
    error: null
  })),
  
  on(AuthActions.loginFailure, (state, { error }): AuthState => ({
    ...state,
    loading: false,
    error
  })),
  
  // Logout
  on(AuthActions.logout, (state): AuthState => ({
    ...state,
    loading: true
  })),
  
  on(AuthActions.logoutSuccess, (): AuthState => ({
    ...initialAuthState
  })),
  
  // Token Refresh
  on(AuthActions.refreshToken, (state): AuthState => ({
    ...state,
    loading: true,
    error: null
  })),
  
  on(AuthActions.refreshTokenSuccess, (state, { token }): AuthState => ({
    ...state,
    token,
    loading: false,
    error: null
  })),
  
  on(AuthActions.refreshTokenFailure, (state, { error }): AuthState => ({
    ...state,
    loading: false,
    error
  })),
  
  // Load from Storage
  on(AuthActions.loadUserFromStorage, (state): AuthState => ({
    ...state,
    loading: true
  })),
  
  on(AuthActions.loadUserFromStorageSuccess, (state, { user, token }): AuthState => ({
    ...state,
    user,
    token,
    loading: false,
    error: null
  })),
  
  on(AuthActions.loadUserFromStorageFailure, (state): AuthState => ({
    ...state,
    loading: false
  })),
  
  // Clear Error
  on(AuthActions.clearAuthError, (state): AuthState => ({
    ...state,
    error: null
  }))
);
