import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

/**
 * Loading Service
 * 
 * Manages global loading state for the application.
 */
@Injectable({
  providedIn: 'root'
})
export class LoadingService {
  
  private loadingSubject = new BehaviorSubject<boolean>(false);
  private loadingMap = new Map<string, boolean>();
  
  /**
   * Observable for loading state
   */
  loading$: Observable<boolean> = this.loadingSubject.asObservable();
  
  /**
   * Sets loading state for a specific key
   * 
   * @param loading Loading state
   * @param key Optional key to track multiple loading states
   */
  setLoading(loading: boolean, key: string = 'default'): void {
    if (loading) {
      this.loadingMap.set(key, loading);
    } else {
      this.loadingMap.delete(key);
    }
    
    // Emit true if any loading state is active
    this.loadingSubject.next(this.loadingMap.size > 0);
  }
  
  /**
   * Gets current loading state
   */
  isLoading(): boolean {
    return this.loadingSubject.value;
  }
  
  /**
   * Gets loading state for a specific key
   */
  isLoadingKey(key: string): boolean {
    return this.loadingMap.has(key);
  }
  
  /**
   * Clears all loading states
   */
  clearAll(): void {
    this.loadingMap.clear();
    this.loadingSubject.next(false);
  }
}
