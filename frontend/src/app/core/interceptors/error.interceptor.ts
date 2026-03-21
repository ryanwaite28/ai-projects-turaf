import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AppState } from '../../store/app.state';
import { logout } from '../../store/auth/auth.actions';

/**
 * Error Interceptor
 * 
 * Handles HTTP errors globally across the application.
 * 
 * This interceptor:
 * - Catches HTTP errors
 * - Handles 401 Unauthorized (logout user)
 * - Handles 403 Forbidden (redirect to dashboard)
 * - Provides user-friendly error messages
 * - Logs errors for debugging
 * 
 * Following Angular best practices:
 * - Centralized error handling
 * - Consistent error response format
 * - Automatic session management
 */
@Injectable()
export class ErrorInterceptor implements HttpInterceptor {
  
  constructor(
    private store: Store<AppState>,
    private router: Router
  ) {}
  
  /**
   * Intercepts HTTP requests and handles errors.
   * 
   * @param req The outgoing request
   * @param next The next handler in the chain
   * @returns Observable<HttpEvent<any>> The HTTP event stream
   */
  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(req).pipe(
      catchError((error: HttpErrorResponse) => {
        let errorMessage = 'An error occurred';
        
        if (error.error instanceof ErrorEvent) {
          // Client-side error
          errorMessage = `Client Error: ${error.error.message}`;
          console.error('Client-side error:', error.error.message);
        } else {
          // Server-side error
          errorMessage = this.getServerErrorMessage(error);
          console.error(`Server-side error: ${error.status} - ${error.message}`);
          
          // Handle specific HTTP status codes
          this.handleHttpError(error);
        }
        
        // Return error with user-friendly message
        return throwError(() => ({
          status: error.status,
          message: errorMessage,
          originalError: error
        }));
      })
    );
  }
  
  /**
   * Handles specific HTTP error status codes.
   * 
   * @param error The HTTP error response
   */
  private handleHttpError(error: HttpErrorResponse): void {
    switch (error.status) {
      case 401:
        // Unauthorized - logout user and redirect to login
        console.warn('Unauthorized access - logging out user');
        this.store.dispatch(logout());
        break;
        
      case 403:
        // Forbidden - redirect to dashboard
        console.warn('Forbidden access - redirecting to dashboard');
        this.router.navigate(['/dashboard']);
        break;
        
      case 404:
        // Not Found - could redirect to 404 page
        console.warn('Resource not found:', error.url);
        break;
        
      case 500:
      case 502:
      case 503:
        // Server errors
        console.error('Server error:', error.status);
        break;
        
      default:
        console.error('Unhandled HTTP error:', error.status);
    }
  }
  
  /**
   * Extracts user-friendly error message from server response.
   * 
   * @param error The HTTP error response
   * @returns string The error message
   */
  private getServerErrorMessage(error: HttpErrorResponse): string {
    // Try to extract message from error response
    if (error.error?.message) {
      return error.error.message;
    }
    
    if (error.error?.error) {
      return error.error.error;
    }
    
    // Default messages based on status code
    switch (error.status) {
      case 400:
        return 'Bad request. Please check your input.';
      case 401:
        return 'Unauthorized. Please log in again.';
      case 403:
        return 'You do not have permission to access this resource.';
      case 404:
        return 'The requested resource was not found.';
      case 500:
        return 'Internal server error. Please try again later.';
      case 502:
        return 'Bad gateway. The server is temporarily unavailable.';
      case 503:
        return 'Service unavailable. Please try again later.';
      default:
        return `Server error: ${error.status} - ${error.statusText}`;
    }
  }
}
