# Task: Add Routing

**Service**: Frontend  
**Phase**: 9  
**Estimated Time**: 2 hours  

## Objective

Configure application routing with lazy loading and route guards.

## Prerequisites

- [x] All feature modules implemented

## Scope

**Files to Create**:
- `frontend/src/app/app-routing.module.ts`

## Implementation Details

### App Routing

```typescript
const routes: Routes = [
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  {
    path: 'auth',
    loadChildren: () => import('./features/auth/auth.module').then(m => m.AuthModule)
  },
  {
    path: 'dashboard',
    loadChildren: () => import('./features/dashboard/dashboard.module').then(m => m.DashboardModule),
    canActivate: [AuthGuard]
  },
  {
    path: 'problems',
    loadChildren: () => import('./features/problems/problems.module').then(m => m.ProblemsModule),
    canActivate: [AuthGuard]
  },
  {
    path: 'experiments',
    loadChildren: () => import('./features/experiments/experiments.module').then(m => m.ExperimentsModule),
    canActivate: [AuthGuard]
  },
  { path: '**', redirectTo: '/dashboard' }
];
```

## Acceptance Criteria

- [ ] Routing configured
- [ ] Lazy loading works
- [ ] Guards protect routes
- [ ] Navigation works
- [ ] Deep linking works

## References

- Specification: `specs/angular-frontend.md` (Routing section)
- Related Tasks: 013-add-unit-tests
