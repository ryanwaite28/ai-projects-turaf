import { Injectable } from '@angular/core';
import { Router, CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree } from '@angular/router';
import { Store } from '@ngrx/store';
import { Observable } from 'rxjs';
import { map, take } from 'rxjs/operators';
import { AppState } from '../../store/app.state';
import { selectUserRole, selectIsAuthenticated } from '../../store/auth/auth.selectors';
import { UserRole } from '../../models/user.model';

/**
 * Role Guard
 * 
 * Protects routes that require specific user roles.
 * Redirects unauthorized users to the dashboard or login page.
 * 
 * Usage:
 * ```typescript
 * {
 *   path: 'admin',
 *   component: AdminComponent,
 *   canActivate: [RoleGuard],
 *   data: { roles: [UserRole.ADMIN] }
 * }
 * ```
 */
@Injectable({
  providedIn: 'root'
})
export class RoleGuard implements CanActivate {
  
  constructor(
    private store: Store<AppState>,
    private router: Router
  ) {}
  
  /**
   * Determines if a route can be activated based on user role.
   * 
   * @param route The activated route snapshot
   * @param state The router state snapshot
   * @returns Observable<boolean | UrlTree> - true if authorized, UrlTree to redirect if not
   */
  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<boolean | UrlTree> {
    const requiredRoles = route.data['roles'] as UserRole[];
    
    if (!requiredRoles || requiredRoles.length === 0) {
      // No roles required, allow access
      return this.store.select(selectIsAuthenticated).pipe(
        take(1),
        map(isAuthenticated => {
          if (isAuthenticated) {
            return true;
          }
          return this.router.createUrlTree(['/login']);
        })
      );
    }
    
    return this.store.select(selectUserRole).pipe(
      take(1),
      map(userRole => {
        if (!userRole) {
          // Not authenticated
          return this.router.createUrlTree(['/login']);
        }
        
        if (requiredRoles.includes(userRole)) {
          // User has required role
          return true;
        }
        
        // User doesn't have required role, redirect to dashboard
        return this.router.createUrlTree(['/dashboard']);
      })
    );
  }
}
