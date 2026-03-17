# Task: Setup NgRx Store

**Service**: Frontend  
**Phase**: 9  
**Estimated Time**: 3 hours  

## Objective

Setup NgRx for state management with store, effects, and selectors.

## Prerequisites

- [x] Task 001: Angular project setup

## Scope

**Files to Create**:
- `frontend/src/app/store/app.state.ts`
- `frontend/src/app/store/auth/auth.state.ts`
- `frontend/src/app/store/auth/auth.actions.ts`
- `frontend/src/app/store/auth/auth.reducer.ts`
- `frontend/src/app/store/auth/auth.effects.ts`
- `frontend/src/app/store/auth/auth.selectors.ts`

## Implementation Details

### Install NgRx

```bash
ng add @ngrx/store
ng add @ngrx/effects
ng add @ngrx/store-devtools
```

### App State

```typescript
export interface AppState {
  auth: AuthState;
  organizations: OrganizationState;
  experiments: ExperimentState;
  metrics: MetricsState;
}
```

### Auth State

```typescript
export interface AuthState {
  user: User | null;
  token: string | null;
  loading: boolean;
  error: string | null;
}

export const initialAuthState: AuthState = {
  user: null,
  token: null,
  loading: false,
  error: null
};
```

### Auth Actions

```typescript
export const login = createAction(
  '[Auth] Login',
  props<{ email: string; password: string }>()
);

export const loginSuccess = createAction(
  '[Auth] Login Success',
  props<{ user: User; token: string }>()
);

export const loginFailure = createAction(
  '[Auth] Login Failure',
  props<{ error: string }>()
);

export const logout = createAction('[Auth] Logout');
```

### Auth Reducer

```typescript
export const authReducer = createReducer(
  initialAuthState,
  on(login, state => ({ ...state, loading: true, error: null })),
  on(loginSuccess, (state, { user, token }) => ({
    ...state,
    user,
    token,
    loading: false,
    error: null
  })),
  on(loginFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error
  })),
  on(logout, () => initialAuthState)
);
```

## Acceptance Criteria

- [ ] NgRx installed and configured
- [ ] Store structure defined
- [ ] Auth state implemented
- [ ] Actions, reducers, effects created
- [ ] Selectors defined
- [ ] DevTools working

## Testing Requirements

**Unit Tests**:
- Test reducers
- Test selectors
- Test effects

## References

- Specification: `specs/angular-frontend.md` (State Management section)
- Related Tasks: 003-create-core-module
