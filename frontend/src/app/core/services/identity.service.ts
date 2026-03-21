import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

/**
 * User profile
 */
export interface UserProfile {
  id: string;
  email: string;
  firstName?: string;
  lastName?: string;
  avatar?: string;
  role?: string;
  organizationId?: string;
  createdAt: string;
  updatedAt: string;
}

/**
 * Update profile request
 */
export interface UpdateProfileRequest {
  firstName?: string;
  lastName?: string;
  avatar?: string;
}

/**
 * Change password request
 */
export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

/**
 * Identity Service
 * 
 * Manages user identity and profile information.
 */
@Injectable({
  providedIn: 'root'
})
export class IdentityService {
  
  private readonly apiUrl = `${environment.apiUrl}/users`;
  private currentUserSubject = new BehaviorSubject<UserProfile | null>(null);
  
  /**
   * Observable for current user
   */
  currentUser$ = this.currentUserSubject.asObservable();
  
  constructor(private http: HttpClient) {}
  
  /**
   * Gets the current user's profile
   */
  getCurrentUser(): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.apiUrl}/me`).pipe(
      tap(user => this.currentUserSubject.next(user))
    );
  }
  
  /**
   * Gets a user by ID
   */
  getUser(id: string): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.apiUrl}/${id}`);
  }
  
  /**
   * Updates the current user's profile
   */
  updateProfile(request: UpdateProfileRequest): Observable<UserProfile> {
    return this.http.patch<UserProfile>(`${this.apiUrl}/me`, request).pipe(
      tap(user => this.currentUserSubject.next(user))
    );
  }
  
  /**
   * Changes the current user's password
   */
  changePassword(request: ChangePasswordRequest): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/me/password`, request);
  }
  
  /**
   * Uploads a profile avatar
   */
  uploadAvatar(file: File): Observable<UserProfile> {
    const formData = new FormData();
    formData.append('avatar', file);
    
    return this.http.post<UserProfile>(`${this.apiUrl}/me/avatar`, formData).pipe(
      tap(user => this.currentUserSubject.next(user))
    );
  }
  
  /**
   * Deletes the current user's account
   */
  deleteAccount(): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/me`).pipe(
      tap(() => this.currentUserSubject.next(null))
    );
  }
  
  /**
   * Gets the current user value
   */
  getCurrentUserValue(): UserProfile | null {
    return this.currentUserSubject.value;
  }
  
  /**
   * Clears the current user
   */
  clearCurrentUser(): void {
    this.currentUserSubject.next(null);
  }
}
