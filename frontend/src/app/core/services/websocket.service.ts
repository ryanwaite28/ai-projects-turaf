import { Injectable } from '@angular/core';
import { Observable, Subject, timer } from 'rxjs';
import { webSocket, WebSocketSubject } from 'rxjs/webSocket';
import { retryWhen, tap, delayWhen } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

/**
 * WebSocket message
 */
export interface WebSocketMessage {
  type: string;
  payload: any;
  timestamp?: string;
}

/**
 * WebSocket Service
 * 
 * Manages WebSocket connections for real-time updates.
 */
@Injectable({
  providedIn: 'root'
})
export class WebSocketService {
  
  private socket$: WebSocketSubject<WebSocketMessage> | null = null;
  private messagesSubject$ = new Subject<WebSocketMessage>();
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  
  /**
   * Observable for incoming messages
   */
  messages$ = this.messagesSubject$.asObservable();
  
  /**
   * Connects to WebSocket server
   */
  connect(token?: string): void {
    if (this.socket$) {
      return;
    }
    
    const wsUrl = this.getWebSocketUrl(token);
    
    this.socket$ = webSocket({
      url: wsUrl,
      openObserver: {
        next: () => {
          console.log('WebSocket connected');
          this.reconnectAttempts = 0;
        }
      },
      closeObserver: {
        next: () => {
          console.log('WebSocket disconnected');
          this.socket$ = null;
          this.reconnect(token);
        }
      }
    });
    
    this.socket$
      .pipe(
        retryWhen(errors =>
          errors.pipe(
            tap(err => console.error('WebSocket error:', err)),
            delayWhen(() => timer(this.getReconnectDelay()))
          )
        )
      )
      .subscribe(
        message => this.messagesSubject$.next(message),
        error => console.error('WebSocket error:', error)
      );
  }
  
  /**
   * Sends a message through WebSocket
   */
  send(message: WebSocketMessage): void {
    if (this.socket$) {
      this.socket$.next(message);
    } else {
      console.warn('WebSocket not connected');
    }
  }
  
  /**
   * Subscribes to a specific message type
   */
  on(type: string): Observable<WebSocketMessage> {
    return new Observable(observer => {
      const subscription = this.messages$.subscribe(message => {
        if (message.type === type) {
          observer.next(message);
        }
      });
      
      return () => subscription.unsubscribe();
    });
  }
  
  /**
   * Disconnects from WebSocket server
   */
  disconnect(): void {
    if (this.socket$) {
      this.socket$.complete();
      this.socket$ = null;
    }
  }
  
  /**
   * Gets WebSocket URL with optional token
   */
  private getWebSocketUrl(token?: string): string {
    const baseUrl = environment.wsUrl || environment.apiUrl.replace('http', 'ws');
    return token ? `${baseUrl}?token=${token}` : baseUrl;
  }
  
  /**
   * Attempts to reconnect to WebSocket
   */
  private reconnect(token?: string): void {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      console.log(`Reconnecting... Attempt ${this.reconnectAttempts}`);
      
      setTimeout(() => {
        this.connect(token);
      }, this.getReconnectDelay());
    } else {
      console.error('Max reconnect attempts reached');
    }
  }
  
  /**
   * Gets reconnect delay with exponential backoff
   */
  private getReconnectDelay(): number {
    return Math.min(1000 * Math.pow(2, this.reconnectAttempts), 30000);
  }
}
