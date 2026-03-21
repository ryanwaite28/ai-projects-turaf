import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { of } from 'rxjs';
import { map, catchError, exhaustMap, tap } from 'rxjs/operators';
import * as AuthActions from './auth.actions';

/**
 * Authentication Effects
 * 
 * Handles side effects for authentication actions:
 * - API calls to authentication service
 * - Local storage operations
 * - Navigation after login/logout
 * 
 * Following NgRx best practices:
 * - Use exhaustMap for login to prevent multiple simultaneous requests
 * - Handle errors gracefully
 * - Dispatch success/failure actions
 */
@Injectable()
export class AuthEffects {
  
  /**
   * Login Effect
   * 
   * Triggers on login action, calls authentication service,
   * stores token in localStorage, and navigates to dashboard on success.
   */
  login$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AuthActions.login),
      exhaustMap(({ email, password }) =>
        // TODO: Replace with actual AuthService call
        // this.authService.login({ email, password }).pipe(
        of({
          user: {
            id: '1',
            email,
            firstName: 'Test',
            lastName: 'User',
            organizationId: 'org-1',
            role: 'ADMIN' as any,
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString()
          },
          token: 'mock-jwt-token'
        }).pipe(
          map(response => {
            // Store token in localStorage
            localStorage.setItem('auth_token', response.token);
            localStorage.setItem('user', JSON.stringify(response.user));
            
            return AuthActions.loginSuccess({
              user: response.user,
              token: response.token
            });
          }),
          catchError(error =>
            of(AuthActions.loginFailure({
              error: error.error?.message || 'Login failed'
            }))
          )
        )
      )
    )
  );
  
  /**
   * Login Success Effect
   * 
   * Navigates to dashboard after successful login.
   */
  loginSuccess$ = createEffect(
    () =>
      this.actions$.pipe(
        ofType(AuthActions.loginSuccess),
        tap(() => {
          this.router.navigate(['/dashboard']);
        })
      ),
    { dispatch: false }
  );
  
  /**
   * Logout Effect
   * 
   * Clears localStorage and dispatches logout success.
   */
  logout$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AuthActions.logout),
      map(() => {
        // Clear localStorage
        localStorage.removeItem('auth_token');
        localStorage.removeItem('user');
        
        return AuthActions.logoutSuccess();
      })
    )
  );
  
  /**
   * Logout Success Effect
   * 
   * Navigates to login page after logout.
   */
  logoutSuccess$ = createEffect(
    () =>
      this.actions$.pipe(
        ofType(AuthActions.logoutSuccess),
        tap(() => {
          this.router.navigate(['/login']);
        })
      ),
    { dispatch: false }
  );
  
  /**
   * Load User from Storage Effect
   * 
   * Attempts to restore user session from localStorage on app init.
   */
  loadUserFromStorage$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AuthActions.loadUserFromStorage),
      map(() => {
        const token = localStorage.getItem('auth_token');
        const userJson = localStorage.getItem('user');
        
        if (token && userJson) {
          try {
            const user = JSON.parse(userJson);
            return AuthActions.loadUserFromStorageSuccess({ user, token });
          } catch (error) {
            return AuthActions.loadUserFromStorageFailure();
          }
        }
        
        return AuthActions.loadUserFromStorageFailure();
      })
    )
  );
  
  /**
   * Refresh Token Effect
   * 
   * Refreshes JWT token before expiration.
   */
  refreshToken$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AuthActions.refreshToken),
      exhaustMap(() =>
        // TODO: Replace with actual AuthService call
        // this.authService.refreshToken().pipe(
        of({ token: 'new-mock-jwt-token' }).pipe(
          map(response => {
            // Store new token
            localStorage.setItem('auth_token', response.token);
            
            return AuthActions.refreshTokenSuccess({ token: response.token });
          }),
          catchError(error =>
            of(AuthActions.refreshTokenFailure({
              error: error.error?.message || 'Token refresh failed'
            }))
          )
        )
      )
    )
  );
  
  constructor(
    private actions$: Actions,
    private router: Router
    // private authService: AuthService  // TODO: Inject when service is created
  ) {}
}
