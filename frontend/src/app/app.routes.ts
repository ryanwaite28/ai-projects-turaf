import { Routes } from '@angular/router';
import { authGuard } from './core/auth/guards/auth.guard';

/**
 * Application Routes
 * 
 * Configures lazy-loaded routes with authentication guards.
 */
export const routes: Routes = [
  {
    path: '',
    redirectTo: '/dashboard',
    pathMatch: 'full'
  },
  {
    path: 'auth',
    loadChildren: () => import('./features/auth/auth.module').then(m => m.AuthModule)
  },
  {
    path: 'dashboard',
    loadChildren: () => import('./features/dashboard/dashboard.module').then(m => m.DashboardModule),
    canActivate: [authGuard]
  },
  {
    path: 'problems',
    loadChildren: () => import('./features/problems/problems.module').then(m => m.ProblemsModule),
    canActivate: [authGuard]
  },
  {
    path: 'hypotheses',
    loadChildren: () => import('./features/hypotheses/hypotheses.module').then(m => m.HypothesesModule),
    canActivate: [authGuard]
  },
  {
    path: 'experiments',
    loadChildren: () => import('./features/experiments/experiments.module').then(m => m.ExperimentsModule),
    canActivate: [authGuard]
  },
  {
    path: 'metrics',
    loadChildren: () => import('./features/metrics/metrics.module').then(m => m.MetricsModule),
    canActivate: [authGuard]
  },
  {
    path: 'reports',
    loadChildren: () => import('./features/reports/reports.module').then(m => m.ReportsModule),
    canActivate: [authGuard]
  },
  {
    path: '**',
    redirectTo: '/dashboard'
  }
];
