import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent } from '@angular/common/http';
import { Store } from '@ngrx/store';
import { Observable } from 'rxjs';
import { take, switchMap } from 'rxjs/operators';
import { AppState } from '../../store/app.state';
import { selectToken } from '../../store/auth/auth.selectors';

/**
 * Auth Interceptor
 * 
 * Automatically adds JWT token to outgoing HTTP requests.
 * 
 * This interceptor:
 * - Retrieves the JWT token from the store
 * - Adds Authorization header to requests
 * - Skips token for public endpoints (login, register)
 * 
 * Following Angular best practices:
 * - Uses RxJS operators for async token retrieval
 * - Clones requests to maintain immutability
 * - Only adds token when present
 */
@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  
  /**
   * URLs that should not have the Authorization header
   */
  private readonly publicUrls = [
    '/api/auth/login',
    '/api/auth/register',
    '/api/auth/refresh'
  ];
  
  constructor(private store: Store<AppState>) {}
  
  /**
   * Intercepts HTTP requests and adds authentication token.
   * 
   * @param req The outgoing request
   * @param next The next handler in the chain
   * @returns Observable<HttpEvent<any>> The HTTP event stream
   */
  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    // Check if this is a public URL that doesn't need auth
    if (this.isPublicUrl(req.url)) {
      return next.handle(req);
    }
    
    // Get token from store and add to request
    return this.store.select(selectToken).pipe(
      take(1),
      switchMap(token => {
        if (token) {
          // Clone request and add Authorization header
          const authReq = req.clone({
            setHeaders: {
              Authorization: `Bearer ${token}`
            }
          });
          return next.handle(authReq);
        }
        
        // No token, proceed without Authorization header
        return next.handle(req);
      })
    );
  }
  
  /**
   * Checks if the URL is a public endpoint.
   * 
   * @param url The request URL
   * @returns boolean True if URL is public
   */
  private isPublicUrl(url: string): boolean {
    return this.publicUrls.some(publicUrl => url.includes(publicUrl));
  }
}
