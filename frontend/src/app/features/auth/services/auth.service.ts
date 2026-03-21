import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { LoginRequest, LoginResponse } from '../../../models/user.model';
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
   * @returns Observable<{ token: string }> New token
   */
  refreshToken(): Observable<{ token: string }> {
    return this.http.post<{ token: string }>(`${this.apiUrl}/refresh`, {});
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
   * @param email User's email address
   * @returns Observable<void> Request confirmation
   */
  requestPasswordReset(email: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/password-reset/request`, { email });
  }
  
  /**
   * Resets password with token.
   * 
   * @param token Reset token from email
   * @param newPassword New password
   * @returns Observable<void> Reset confirmation
   */
  resetPassword(token: string, newPassword: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/password-reset/confirm`, {
      token,
      newPassword
    });
  }
}

/**
 * User registration request interface
 */
export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  organizationName?: string;
}
