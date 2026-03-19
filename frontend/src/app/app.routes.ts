import { Routes } from '@angular/router';
import { authGuard } from './core/auth/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    redirectTo: '/dashboard',
    pathMatch: 'full'
  },
  {
    path: 'auth',
    loadChildren: () => import('./features/auth/auth.routes').then(m => m.AUTH_ROUTES)
  },
  {
    path: 'dashboard',
    loadChildren: () => import('./features/dashboard/dashboard.routes').then(m => m.DASHBOARD_ROUTES),
    canActivate: [authGuard]
  },
  {
    path: 'problems',
    loadChildren: () => import('./features/problems/problems.routes').then(m => m.PROBLEMS_ROUTES),
    canActivate: [authGuard]
  },
  {
    path: 'hypotheses',
    loadChildren: () => import('./features/hypotheses/hypotheses.routes').then(m => m.HYPOTHESES_ROUTES),
    canActivate: [authGuard]
  },
  {
    path: 'experiments',
    loadChildren: () => import('./features/experiments/experiments.routes').then(m => m.EXPERIMENTS_ROUTES),
    canActivate: [authGuard]
  },
  {
    path: 'metrics',
    loadChildren: () => import('./features/metrics/metrics.routes').then(m => m.METRICS_ROUTES),
    canActivate: [authGuard]
  },
  {
    path: 'reports',
    loadChildren: () => import('./features/reports/reports.routes').then(m => m.REPORTS_ROUTES),
    canActivate: [authGuard]
  },
  {
    path: '**',
    redirectTo: '/dashboard'
  }
];
