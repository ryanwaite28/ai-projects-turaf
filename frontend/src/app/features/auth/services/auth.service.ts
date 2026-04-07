import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { LoginRequest, LoginResponse, RegisterRequest, RefreshTokenRequest, RefreshTokenResponse, PasswordResetRequest, PasswordResetConfirmRequest, User } from '../../../models/user.model';
import { environment } from '../../../../environments/environment';

/**
 * Auth Service
 * 
 * Handles authentication-related API calls.
 * 
 * This service provides methods for:
 * - User login
 * - User registration
 * - Token refresh
 * - Password reset
 * 
 * All methods return Observables for reactive programming.
 */
@Injectable({
  providedIn: 'root'
})
export class AuthService {
  
  private readonly apiUrl = `${environment.apiUrl}/auth`;
  
  constructor(private http: HttpClient) {}
  
  /**
   * Authenticates a user with email and password.
   * 
   * @param credentials User login credentials
   * @returns Observable<LoginResponse> Login response with user and token
   */
  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, credentials);
  }
  
  /**
   * Registers a new user.
   * 
   * @param userData User registration data
   * @returns Observable<LoginResponse> Registration response with user and token
   */
  register(userData: RegisterRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/register`, userData);
  }
  
  /**
   * Refreshes the JWT token.
   * 
   * @param refreshToken The refresh token
   * @returns Observable<RefreshTokenResponse> New tokens
   */
  refreshToken(refreshToken: string): Observable<RefreshTokenResponse> {
    return this.http.post<RefreshTokenResponse>(`${this.apiUrl}/refresh`, { refreshToken });
  }
  
  /**
   * Gets the current authenticated user.
   * 
   * @returns Observable<User> Current user data
   */
  getCurrentUser(): Observable<User> {
    return this.http.get<User>(`${this.apiUrl}/me`);
  }
  
  /**
   * Logs out the current user.
   * 
   * @returns Observable<void> Logout confirmation
   */
  logout(): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/logout`, {});
  }
  
  /**
   * Requests a password reset email.
   * 
   * @param request Password reset request with email
   * @returns Observable<void> Request confirmation
   */
  requestPasswordReset(request: PasswordResetRequest): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/password-reset/request`, request);
  }
  
  /**
   * Resets password with token.
   * 
   * @param request Password reset confirmation with token and new password
   * @returns Observable<void> Reset confirmation
   */
  resetPassword(request: PasswordResetConfirmRequest): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/password-reset/confirm`, request);
  }
}
