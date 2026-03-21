# Task: Create Core Module

**Service**: Frontend  
**Phase**: 9  
**Estimated Time**: 2 hours  

## Objective

Create core module with shared services, guards, and interceptors.

## Prerequisites

- [x] Task 001: Angular project setup
- [x] Task 002: NgRx store setup

## Scope

**Files to Create**:
- `frontend/src/app/core/core.module.ts`
- `frontend/src/app/core/guards/auth.guard.ts`
- `frontend/src/app/core/interceptors/auth.interceptor.ts`
- `frontend/src/app/core/interceptors/error.interceptor.ts`

## Implementation Details

### Auth Guard

```typescript
@Injectable({ providedIn: 'root' })
export class AuthGuard implements CanActivate {
  constructor(
    private store: Store<AppState>,
    private router: Router
  ) {}
  
  canActivate(): Observable<boolean> {
    return this.store.select(selectIsAuthenticated).pipe(
      tap(isAuthenticated => {
        if (!isAuthenticated) {
          this.router.navigate(['/login']);
        }
      })
    );
  }
}
```

### Auth Interceptor

```typescript
@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private store: Store<AppState>) {}
  
  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return this.store.select(selectToken).pipe(
      take(1),
      switchMap(token => {
        if (token) {
          req = req.clone({
            setHeaders: {
              Authorization: `Bearer ${token}`
            }
          });
        }
        return next.handle(req);
      })
    );
  }
}
```

## Acceptance Criteria

- [x] Core module created
- [x] Auth guard implemented
- [x] Auth interceptor implemented
- [x] Error interceptor implemented
- [x] Guards protect routes
- [x] Interceptors work correctly

## Testing Requirements

**Unit Tests**:
- Test auth guard
- Test interceptors

## References

- Specification: `specs/angular-frontend.md` (Core Module section)
- Related Tasks: 004-implement-auth-module
