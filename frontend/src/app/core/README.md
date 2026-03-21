# Core Module

This module contains singleton services, guards, and interceptors that are used throughout the application.

## Structure

```
core/
├── guards/
│   ├── auth.guard.ts          # Authentication guard
│   ├── role.guard.ts          # Role-based authorization guard
│   └── *.spec.ts              # Unit tests
├── interceptors/
│   ├── auth.interceptor.ts    # JWT token interceptor
│   ├── error.interceptor.ts   # Global error handler
│   └── *.spec.ts              # Unit tests
├── services/
│   ├── notification.service.ts # Notification/toast service
│   └── *.spec.ts              # Unit tests
├── core.module.ts             # Core module definition
└── README.md                  # This file
```

## Guards

### AuthGuard

Protects routes that require authentication. Redirects unauthenticated users to the login page.

**Usage:**
```typescript
{
  path: 'dashboard',
  component: DashboardComponent,
  canActivate: [AuthGuard]
}
```

### RoleGuard

Protects routes that require specific user roles. Redirects unauthorized users to the dashboard.

**Usage:**
```typescript
{
  path: 'admin',
  component: AdminComponent,
  canActivate: [RoleGuard],
  data: { roles: [UserRole.ADMIN] }
}
```

## Interceptors

### AuthInterceptor

Automatically adds JWT token to outgoing HTTP requests.

**Features:**
- Retrieves token from NgRx store
- Adds `Authorization: Bearer <token>` header
- Skips public endpoints (login, register, refresh)

### ErrorInterceptor

Handles HTTP errors globally across the application.

**Features:**
- Catches all HTTP errors
- Handles 401 Unauthorized (logout user)
- Handles 403 Forbidden (redirect to dashboard)
- Provides user-friendly error messages
- Logs errors for debugging

## Services

### NotificationService

Centralized service for displaying notifications/toasts.

**Usage:**
```typescript
constructor(private notificationService: NotificationService) {}

showSuccess() {
  this.notificationService.success('Operation completed!');
}

showError() {
  this.notificationService.error('Something went wrong!');
}
```

**Methods:**
- `success(message, duration?)` - Show success notification
- `error(message, duration?)` - Show error notification
- `warning(message, duration?)` - Show warning notification
- `info(message, duration?)` - Show info notification

**Subscribing to notifications:**
```typescript
this.notificationService.notifications$.subscribe(notification => {
  // Display notification in UI
  console.log(notification.type, notification.message);
});
```

## Import Guard

The CoreModule includes an import guard to prevent it from being imported more than once:

```typescript
constructor(@Optional() @SkipSelf() parentModule: CoreModule) {
  if (parentModule) {
    throw new Error(
      'CoreModule is already loaded. Import it in the AppModule only.'
    );
  }
}
```

## Best Practices

1. **Single Import**: Only import CoreModule in AppModule
2. **Singleton Services**: All services in this module are singletons (providedIn: 'root')
3. **HTTP Interceptors**: Configured in CoreModule providers
4. **Guards**: Provided in root for dependency injection
5. **Error Handling**: Centralized in ErrorInterceptor
6. **Authentication**: Managed through guards and interceptors

## Testing

All guards, interceptors, and services have comprehensive unit tests:
- Guards: Test authentication and authorization logic
- Interceptors: Test HTTP request/response handling
- Services: Test business logic and observables

Run tests:
```bash
ng test
```

## Related Modules

- **Store Module**: Provides state management (auth state)
- **Shared Module**: Provides reusable components and pipes
- **Feature Modules**: Use guards and services from Core Module
