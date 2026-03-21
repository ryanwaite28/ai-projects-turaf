import { Component, OnInit } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { Store } from '@ngrx/store';
import { Observable } from 'rxjs';
import { filter } from 'rxjs/operators';
import { AppState } from '../../../store/app.state';
import { selectIsAuthenticated, selectCurrentUser } from '../../../store/auth/auth.selectors';
import { logout } from '../../../store/auth/auth.actions';

/**
 * Navigation menu item
 */
interface NavItem {
  label: string;
  path: string;
  icon: string;
  requiresAuth: boolean;
}

/**
 * Navigation Component
 * 
 * Main navigation menu for the application.
 */
@Component({
  selector: 'app-navigation',
  templateUrl: './navigation.component.html',
  styleUrls: ['./navigation.component.scss']
})
export class NavigationComponent implements OnInit {
  
  isAuthenticated$ = this.store.select(selectIsAuthenticated);
  currentUser$ = this.store.select(selectCurrentUser);
  
  currentRoute: string = '';
  isMobileMenuOpen: boolean = false;
  
  /**
   * Navigation menu items
   */
  navItems: NavItem[] = [
    { label: 'Dashboard', path: '/dashboard', icon: 'dashboard', requiresAuth: true },
    { label: 'Problems', path: '/problems', icon: 'assignment', requiresAuth: true },
    { label: 'Hypotheses', path: '/hypotheses', icon: 'lightbulb', requiresAuth: true },
    { label: 'Experiments', path: '/experiments', icon: 'science', requiresAuth: true },
    { label: 'Metrics', path: '/metrics', icon: 'show_chart', requiresAuth: true },
    { label: 'Reports', path: '/reports', icon: 'description', requiresAuth: true }
  ];
  
  constructor(
    private store: Store<AppState>,
    private router: Router
  ) {}
  
  ngOnInit(): void {
    this.currentRoute = this.router.url;
    
    this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe((event: any) => {
        this.currentRoute = event.url;
        this.isMobileMenuOpen = false;
      });
  }
  
  /**
   * Checks if a route is active
   */
  isActive(path: string): boolean {
    return this.currentRoute.startsWith(path);
  }
  
  /**
   * Toggles mobile menu
   */
  toggleMobileMenu(): void {
    this.isMobileMenuOpen = !this.isMobileMenuOpen;
  }
  
  /**
   * Logs out the current user
   */
  onLogout(): void {
    this.store.dispatch(logout());
  }
}
