# NgRx Store

This directory contains the NgRx state management implementation for the Turaf platform frontend.

## Architecture

The store follows the NgRx pattern with:
- **State**: Immutable state interfaces
- **Actions**: Type-safe action creators
- **Reducers**: Pure functions for state transitions
- **Effects**: Side effect handlers (API calls, navigation, storage)
- **Selectors**: Memoized state queries

## Structure

```
store/
├── app.state.ts           # Root application state
├── auth/                  # Authentication feature state
│   ├── auth.state.ts      # Auth state interface
│   ├── auth.actions.ts    # Auth actions
│   ├── auth.reducer.ts    # Auth reducer
│   ├── auth.effects.ts    # Auth effects
│   ├── auth.selectors.ts  # Auth selectors
│   ├── index.ts           # Barrel export
│   └── *.spec.ts          # Unit tests
└── README.md              # This file
```

## Feature States

### Auth State

Manages user authentication:
- User profile data
- JWT token
- Loading states
- Error messages

**Actions**:
- `login` - Initiate login
- `loginSuccess` - Login succeeded
- `loginFailure` - Login failed
- `logout` - Initiate logout
- `logoutSuccess` - Logout succeeded
- `refreshToken` - Refresh JWT token
- `loadUserFromStorage` - Restore session from localStorage

**Selectors**:
- `selectUser` - Current user
- `selectToken` - JWT token
- `selectIsAuthenticated` - Authentication status
- `selectAuthLoading` - Loading state
- `selectAuthError` - Error message
- `selectUserRole` - User role
- `selectAuthViewModel` - Combined view model

## Usage Examples

### Dispatching Actions

```typescript
import { Store } from '@ngrx/store';
import { login } from './store/auth/auth.actions';

constructor(private store: Store) {}

onLogin(email: string, password: string) {
  this.store.dispatch(login({ email, password }));
}
```

### Selecting State

```typescript
import { Store } from '@ngrx/store';
import { selectUser, selectAuthLoading } from './store/auth/auth.selectors';

constructor(private store: Store) {}

ngOnInit() {
  this.user$ = this.store.select(selectUser);
  this.loading$ = this.store.select(selectAuthLoading);
}
```

### Using in Templates

```html
<div *ngIf="user$ | async as user">
  Welcome, {{ user.firstName }}!
</div>

<div *ngIf="loading$ | async">
  Loading...
</div>
```

## Best Practices

1. **Immutability**: Always return new state objects, never mutate existing state
2. **Pure Functions**: Reducers must be pure functions with no side effects
3. **Single Responsibility**: Each action should represent a single event
4. **Naming Convention**: Use `[Source] Event` format for action names
5. **Selectors**: Use selectors for all state access to enable memoization
6. **Effects**: Handle all side effects (API calls, navigation, storage) in effects
7. **Testing**: Write unit tests for reducers, selectors, and effects

## DevTools

NgRx DevTools is enabled in development mode. Install the Redux DevTools extension for your browser:
- [Chrome Extension](https://chrome.google.com/webstore/detail/redux-devtools/)
- [Firefox Extension](https://addons.mozilla.org/en-US/firefox/addon/reduxdevtools/)

## Future Feature States

The following feature states will be added:

- **Organizations State**: Organization management
- **Experiments State**: A/B test experiments
- **Metrics State**: Experiment metrics and analytics
- **Notifications State**: User notifications
- **UI State**: Global UI state (modals, sidebars, etc.)

## References

- [NgRx Documentation](https://ngrx.io/)
- [NgRx Best Practices](https://ngrx.io/guide/eslint-plugin/rules)
- [Angular State Management](https://angular.io/guide/state-management)
