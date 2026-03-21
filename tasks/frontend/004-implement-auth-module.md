# Task: Implement Auth Module

**Service**: Frontend  
**Phase**: 9  
**Estimated Time**: 3 hours  

## Objective

Implement authentication module with login, register, and logout components.

## Prerequisites

- [x] Task 002: NgRx store setup
- [x] Task 003: Core module created

## Scope

**Files to Create**:
- `frontend/src/app/features/auth/auth.module.ts`
- `frontend/src/app/features/auth/login/login.component.ts`
- `frontend/src/app/features/auth/register/register.component.ts`
- `frontend/src/app/features/auth/auth-routing.module.ts`

## Implementation Details

### Login Component

```typescript
@Component({
  selector: 'app-login',
  template: `
    <mat-card>
      <mat-card-header>
        <mat-card-title>Login</mat-card-title>
      </mat-card-header>
      <mat-card-content>
        <form [formGroup]="loginForm" (ngSubmit)="onSubmit()">
          <mat-form-field>
            <input matInput placeholder="Email" formControlName="email">
          </mat-form-field>
          <mat-form-field>
            <input matInput type="password" placeholder="Password" formControlName="password">
          </mat-form-field>
          <button mat-raised-button color="primary" type="submit">Login</button>
        </form>
      </mat-card-content>
    </mat-card>
  `
})
export class LoginComponent {
  loginForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required]
  });
  
  constructor(
    private fb: FormBuilder,
    private store: Store<AppState>
  ) {}
  
  onSubmit() {
    if (this.loginForm.valid) {
      this.store.dispatch(login(this.loginForm.value));
    }
  }
}
```

## Acceptance Criteria

- [x] Auth module created
- [x] Login component implemented
- [x] Register component implemented
- [x] Forms validated
- [x] NgRx integration working
- [x] Routing configured

## Testing Requirements

**Unit Tests**:
- Test login component
- Test register component
- Test form validation

## References

- Specification: `specs/angular-frontend.md` (Auth Module section)
- Related Tasks: 005-implement-dashboard-module
