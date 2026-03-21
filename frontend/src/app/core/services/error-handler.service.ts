import { Injectable, ErrorHandler } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { NotificationService } from './notification.service';

/**
 * Global Error Handler Service
 * 
 * Handles all uncaught errors in the application.
 */
@Injectable({
  providedIn: 'root'
})
export class ErrorHandlerService implements ErrorHandler {
  
  constructor(private notificationService: NotificationService) {}
  
  /**
   * Handles errors globally
   */
  handleError(error: Error | HttpErrorResponse): void {
    let errorMessage = 'An unexpected error occurred';
    
    if (error instanceof HttpErrorResponse) {
      // Server-side error
      errorMessage = this.getServerErrorMessage(error);
    } else {
      // Client-side error
      errorMessage = this.getClientErrorMessage(error);
    }
    
    // Log to console in development
    if (!this.isProduction()) {
      console.error('Error:', error);
    }
    
    // Show notification to user
    this.notificationService.error(errorMessage);
    
    // TODO: Send to logging service in production
    // this.loggingService.logError(error);
  }
  
  /**
   * Gets error message from HTTP error response
   */
  private getServerErrorMessage(error: HttpErrorResponse): string {
    if (!navigator.onLine) {
      return 'No internet connection';
    }
    
    if (error.error?.message) {
      return error.error.message;
    }
    
    switch (error.status) {
      case 400:
        return 'Bad request. Please check your input.';
      case 401:
        return 'Unauthorized. Please log in.';
      case 403:
        return 'Forbidden. You do not have permission.';
      case 404:
        return 'Resource not found.';
      case 500:
        return 'Internal server error. Please try again later.';
      case 503:
        return 'Service unavailable. Please try again later.';
      default:
        return `Server error: ${error.status}`;
    }
  }
  
  /**
   * Gets error message from client-side error
   */
  private getClientErrorMessage(error: Error): string {
    return error.message || 'An unexpected error occurred';
  }
  
  /**
   * Checks if running in production
   */
  private isProduction(): boolean {
    // This would check environment.production in a real app
    return false;
  }
}
