import { Injectable } from '@angular/core';
import { Router, CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree } from '@angular/router';
import { Store } from '@ngrx/store';
import { Observable } from 'rxjs';
import { map, take } from 'rxjs/operators';
import { AppState } from '../../store/app.state';
import { selectIsAuthenticated } from '../../store/auth/auth.selectors';

/**
 * Auth Guard
 * 
 * Protects routes that require authentication.
 * Redirects unauthenticated users to the login page.
 * 
 * Usage:
 * ```typescript
 * {
 *   path: 'dashboard',
 *   component: DashboardComponent,
 *   canActivate: [AuthGuard]
 * }
 * ```
 */
@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {
  
  constructor(
    private store: Store<AppState>,
    private router: Router
  ) {}
  
  /**
   * Determines if a route can be activated based on authentication status.
   * 
   * @param route The activated route snapshot
   * @param state The router state snapshot
   * @returns Observable<boolean | UrlTree> - true if authenticated, UrlTree to login if not
   */
  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<boolean | UrlTree> {
    return this.store.select(selectIsAuthenticated).pipe(
      take(1),
      map(isAuthenticated => {
        if (isAuthenticated) {
          return true;
        }
        
        // Store the attempted URL for redirecting after login
        const returnUrl = state.url;
        
        // Redirect to login page with return URL
        return this.router.createUrlTree(['/login'], {
          queryParams: { returnUrl }
        });
      })
    );
  }
}
