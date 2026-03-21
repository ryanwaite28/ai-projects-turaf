import { createAction, props } from '@ngrx/store';
import { User } from '../../models/user.model';

/**
 * Authentication Actions
 * 
 * Following NgRx action naming convention: [Source] Event
 * Actions are dispatched from components/services and handled by reducers/effects.
 */

// Login Actions
export const login = createAction(
  '[Auth] Login',
  props<{ email: string; password: string }>()
);

export const loginSuccess = createAction(
  '[Auth] Login Success',
  props<{ user: User; token: string }>()
);

export const loginFailure = createAction(
  '[Auth] Login Failure',
  props<{ error: string }>()
);

// Logout Actions
export const logout = createAction(
  '[Auth] Logout'
);

export const logoutSuccess = createAction(
  '[Auth] Logout Success'
);

// Token Refresh Actions
export const refreshToken = createAction(
  '[Auth] Refresh Token'
);

export const refreshTokenSuccess = createAction(
  '[Auth] Refresh Token Success',
  props<{ token: string }>()
);

export const refreshTokenFailure = createAction(
  '[Auth] Refresh Token Failure',
  props<{ error: string }>()
);

// Load User from Storage (on app init)
export const loadUserFromStorage = createAction(
  '[Auth] Load User From Storage'
);

export const loadUserFromStorageSuccess = createAction(
  '[Auth] Load User From Storage Success',
  props<{ user: User; token: string }>()
);

export const loadUserFromStorageFailure = createAction(
  '[Auth] Load User From Storage Failure'
);

// Clear Error
export const clearAuthError = createAction(
  '[Auth] Clear Error'
);
