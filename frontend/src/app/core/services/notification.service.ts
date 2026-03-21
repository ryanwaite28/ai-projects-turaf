import { Injectable } from '@angular/core';
import { Subject, Observable } from 'rxjs';

/**
 * Notification types
 */
export enum NotificationType {
  SUCCESS = 'success',
  ERROR = 'error',
  WARNING = 'warning',
  INFO = 'info'
}

/**
 * Notification interface
 */
export interface Notification {
  type: NotificationType;
  message: string;
  duration?: number;
}

/**
 * Notification Service
 * 
 * Provides a centralized service for displaying notifications/toasts.
 * Components can subscribe to notifications and display them in the UI.
 * 
 * Usage:
 * ```typescript
 * constructor(private notificationService: NotificationService) {}
 * 
 * showSuccess() {
 *   this.notificationService.success('Operation completed successfully!');
 * }
 * ```
 */
@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  
  private notificationSubject = new Subject<Notification>();
  
  /**
   * Observable stream of notifications
   */
  public notifications$: Observable<Notification> = this.notificationSubject.asObservable();
  
  /**
   * Shows a success notification
   * 
   * @param message The success message
   * @param duration Duration in milliseconds (default: 3000)
   */
  success(message: string, duration: number = 3000): void {
    this.show({
      type: NotificationType.SUCCESS,
      message,
      duration
    });
  }
  
  /**
   * Shows an error notification
   * 
   * @param message The error message
   * @param duration Duration in milliseconds (default: 5000)
   */
  error(message: string, duration: number = 5000): void {
    this.show({
      type: NotificationType.ERROR,
      message,
      duration
    });
  }
  
  /**
   * Shows a warning notification
   * 
   * @param message The warning message
   * @param duration Duration in milliseconds (default: 4000)
   */
  warning(message: string, duration: number = 4000): void {
    this.show({
      type: NotificationType.WARNING,
      message,
      duration
    });
  }
  
  /**
   * Shows an info notification
   * 
   * @param message The info message
   * @param duration Duration in milliseconds (default: 3000)
   */
  info(message: string, duration: number = 3000): void {
    this.show({
      type: NotificationType.INFO,
      message,
      duration
    });
  }
  
  /**
   * Shows a notification
   * 
   * @param notification The notification to show
   */
  private show(notification: Notification): void {
    this.notificationSubject.next(notification);
  }
}
