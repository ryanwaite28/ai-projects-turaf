# Angular Frontend Specification

**Source**: PROJECT.md (Section 38)  
**Updated**: 2026-04-01 - Frontend-BFF API Alignment

This specification defines the Angular frontend application for the Turaf platform.

> **Note**: The frontend services have been updated to align with the BFF API implementation. Key changes include:
> - Authentication: Updated `LoginResponse` model, added token refresh and password reset support
> - Experiments: Added `organizationId` parameters, aligned state transitions to `/start`, `/complete`, `/cancel`
> - Problems/Hypotheses: Removed unsupported pagination params, return arrays instead of paginated responses
> - Metrics: Replaced general listing with `getExperimentMetrics()`, removed advanced endpoints not yet in BFF
> - Dashboard: Updated to use `/overview` endpoint, added `getExperimentFull()` and `getOrganizationSummary()`
> - Reports: Replaced REST CRUD with stub (reports are Lambda-generated, stored in S3)
> 
> See `/docs/api/api-discrepancy-report.md` for full details.

---

## Frontend Overview

**Framework**: Angular 17.x  
**UI Library**: Angular Material or PrimeNG  
**State Management**: NgRx  
**Build Tool**: Angular CLI  
**Package Manager**: npm  

---

## Application Architecture

### Directory Structure

```
frontend/
├── src/
│   ├── app/
│   │   ├── core/
│   │   │   ├── auth/
│   │   │   │   ├── guards/
│   │   │   │   ├── interceptors/
│   │   │   │   └── services/
│   │   │   ├── services/
│   │   │   │   ├── api.service.ts
│   │   │   │   ├── error-handler.service.ts
│   │   │   │   ├── loading.service.ts
│   │   │   │   └── notification.service.ts
│   │   │   └── core.module.ts
│   │   ├── shared/
│   │   │   ├── components/
│   │   │   │   ├── navigation/
│   │   │   │   ├── header/
│   │   │   │   ├── footer/
│   │   │   │   ├── loading-spinner/
│   │   │   │   └── error-display/
│   │   │   ├── pipes/
│   │   │   ├── directives/
│   │   │   └── shared.module.ts
│   │   ├── features/
│   │   │   ├── auth/
│   │   │   │   ├── login/
│   │   │   │   ├── register/
│   │   │   │   ├── password-reset/
│   │   │   │   └── auth.module.ts
│   │   │   ├── dashboard/
│   │   │   │   ├── dashboard.component.ts
│   │   │   │   └── dashboard.module.ts
│   │   │   ├── problems/
│   │   │   │   ├── problem-list/
│   │   │   │   ├── problem-form/
│   │   │   │   ├── problem-detail/
│   │   │   │   └── problems.module.ts
│   │   │   ├── hypotheses/
│   │   │   │   ├── hypothesis-list/
│   │   │   │   ├── hypothesis-form/
│   │   │   │   ├── hypothesis-detail/
│   │   │   │   └── hypotheses.module.ts
│   │   │   ├── experiments/
│   │   │   │   ├── experiment-list/
│   │   │   │   ├── experiment-wizard/
│   │   │   │   ├── experiment-dashboard/
│   │   │   │   └── experiments.module.ts
│   │   │   ├── metrics/
│   │   │   │   ├── metric-entry/
│   │   │   │   ├── metric-charts/
│   │   │   │   └── metrics.module.ts
│   │   │   └── reports/
│   │   │       ├── report-list/
│   │   │       ├── report-viewer/
│   │   │       └── reports.module.ts
│   │   ├── models/
│   │   │   ├── user.model.ts
│   │   │   ├── organization.model.ts
│   │   │   ├── problem.model.ts
│   │   │   ├── hypothesis.model.ts
│   │   │   ├── experiment.model.ts
│   │   │   ├── metric.model.ts
│   │   │   └── report.model.ts
│   │   ├── store/
│   │   │   ├── auth/
│   │   │   ├── experiments/
│   │   │   ├── problems/
│   │   │   └── app.state.ts
│   │   ├── app.component.ts
│   │   ├── app.routes.ts
│   │   └── app.config.ts
│   ├── assets/
│   │   ├── images/
│   │   ├── icons/
│   │   └── styles/
│   ├── environments/
│   │   ├── environment.ts
│   │   ├── environment.dev.ts
│   │   ├── environment.qa.ts
│   │   └── environment.prod.ts
│   └── styles/
│       ├── _variables.scss
│       ├── _mixins.scss
│       └── styles.scss
├── angular.json
├── package.json
├── tsconfig.json
└── README.md
```

---

## Core Module

### Authentication Service

**File**: `src/app/core/auth/services/auth.service.ts`

**Responsibilities**:
- User login and registration
- Token management (access and refresh)
- User profile retrieval
- Logout

**Implementation**:
```typescript
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly apiUrl = environment.apiUrl;
  private currentUserSubject = new BehaviorSubject<User | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient) {
    this.loadUserFromStorage();
  }

  login(email: string, password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/auth/login`, { email, password })
      .pipe(
        tap(response => this.handleAuthResponse(response)),
        catchError(this.handleError)
      );
  }

  register(data: RegisterRequest): Observable<User> {
    return this.http.post<User>(`${this.apiUrl}/auth/register`, data);
  }

  refreshToken(): Observable<TokenResponse> {
    const refreshToken = this.getRefreshToken();
    return this.http.post<TokenResponse>(`${this.apiUrl}/auth/refresh`, { refreshToken })
      .pipe(
        tap(response => this.storeTokens(response.accessToken, refreshToken))
      );
  }

  logout(): void {
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
    localStorage.removeItem('current_user');
    this.currentUserSubject.next(null);
  }

  private handleAuthResponse(response: LoginResponse): void {
    this.storeTokens(response.accessToken, response.refreshToken);
    this.currentUserSubject.next(response.user);
    localStorage.setItem('current_user', JSON.stringify(response.user));
  }

  private storeTokens(accessToken: string, refreshToken: string): void {
    localStorage.setItem('access_token', accessToken);
    localStorage.setItem('refresh_token', refreshToken);
  }

  getAccessToken(): string | null {
    return localStorage.getItem('access_token');
  }

  private getRefreshToken(): string | null {
    return localStorage.getItem('refresh_token');
  }
}
```

---

### Auth Guard

**File**: `src/app/core/auth/guards/auth.guard.ts`

**Purpose**: Protect routes that require authentication

**Implementation**:
```typescript
export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.getAccessToken()) {
    return true;
  }

  router.navigate(['/auth/login'], { queryParams: { returnUrl: state.url } });
  return false;
};
```

---

### HTTP Interceptors

#### Auth Interceptor

**File**: `src/app/core/auth/interceptors/auth.interceptor.ts`

**Purpose**: Add JWT token to all API requests

**Implementation**:
```typescript
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.getAccessToken();

  if (token && req.url.includes(environment.apiUrl)) {
    req = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  return next(req);
};
```

#### Error Interceptor

**File**: `src/app/core/interceptors/error.interceptor.ts`

**Purpose**: Handle HTTP errors globally

**Implementation**:
```typescript
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const notificationService = inject(NotificationService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        authService.logout();
        router.navigate(['/auth/login']);
        notificationService.error('Session expired. Please login again.');
      } else if (error.status === 403) {
        notificationService.error('You do not have permission to perform this action.');
      } else if (error.status >= 500) {
        notificationService.error('Server error. Please try again later.');
      }

      return throwError(() => error);
    })
  );
};
```

---

## Feature Modules

### Authentication Module

**Components**:

1. **LoginComponent**
   - Email/password form
   - Form validation
   - Remember me checkbox
   - Forgot password link

2. **RegisterComponent**
   - Registration form (email, password, name)
   - Password strength indicator
   - Terms acceptance checkbox

3. **PasswordResetComponent**
   - Email input for reset link
   - Success message

---

### Dashboard Module

**Component**: `DashboardComponent`

**Widgets**:
- Organization overview (member count, experiment count)
- Recent experiments (last 5)
- Key metrics summary
- Quick actions (Create Problem, Create Experiment)

**Implementation**:
```typescript
@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {
  organizationStats$: Observable<OrganizationStats>;
  recentExperiments$: Observable<Experiment[]>;
  
  constructor(
    private organizationService: OrganizationService,
    private experimentService: ExperimentService
  ) {}

  ngOnInit(): void {
    this.organizationStats$ = this.organizationService.getStats();
    this.recentExperiments$ = this.experimentService.getRecent(5);
  }
}
```

---

### Problems Module

**Components**:

1. **ProblemListComponent**
   - Data table with pagination
   - Filtering by title
   - Sorting by date
   - Create button

2. **ProblemFormComponent**
   - Reactive form for create/edit
   - Fields: title, description, affectedUsers, context
   - Form validation

3. **ProblemDetailComponent**
   - Display problem details
   - List of hypotheses
   - Edit/delete actions

---

### Experiments Module

**Components**:

1. **ExperimentListComponent**
   - Data table with status badges
   - Filter by status (DRAFT, RUNNING, COMPLETED)
   - Sort by date

2. **ExperimentWizardComponent**
   - Multi-step form (stepper)
   - Step 1: Select hypothesis
   - Step 2: Experiment details
   - Step 3: Review and create

3. **ExperimentDashboardComponent**
   - Experiment details
   - Metrics chart
   - Timeline
   - Start/Complete buttons
   - Add metric button

**Implementation**:
```typescript
@Component({
  selector: 'app-experiment-dashboard',
  templateUrl: './experiment-dashboard.component.html'
})
export class ExperimentDashboardComponent implements OnInit {
  experiment$: Observable<Experiment>;
  metrics$: Observable<Metric[]>;
  
  constructor(
    private route: ActivatedRoute,
    private experimentService: ExperimentService,
    private metricsService: MetricsService
  ) {}

  ngOnInit(): void {
    const experimentId = this.route.snapshot.params['id'];
    this.experiment$ = this.experimentService.getById(experimentId);
    this.metrics$ = this.metricsService.getByExperiment(experimentId);
  }

  startExperiment(experimentId: string): void {
    this.experimentService.start(experimentId).subscribe({
      next: () => this.notificationService.success('Experiment started'),
      error: (err) => this.notificationService.error('Failed to start experiment')
    });
  }

  completeExperiment(experimentId: string, data: CompleteExperimentRequest): void {
    this.experimentService.complete(experimentId, data).subscribe({
      next: () => this.notificationService.success('Experiment completed'),
      error: (err) => this.notificationService.error('Failed to complete experiment')
    });
  }
}
```

---

### Metrics Module

**Components**:

1. **MetricEntryComponent**
   - Form to record metric
   - Fields: name, value, unit
   - Batch entry option

2. **MetricChartsComponent**
   - Line chart for time-series metrics
   - Bar chart for aggregations
   - Chart.js or D3.js integration

---

### Reports Module

**Components**:

1. **ReportListComponent**
   - List of generated reports
   - Download button
   - View button

2. **ReportViewerComponent**
   - Display report (PDF or HTML)
   - Download option
   - Share option

---

## State Management (NgRx)

### Auth State

**File**: `src/app/store/auth/auth.state.ts`

```typescript
export interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  loading: boolean;
  error: string | null;
}

export const initialAuthState: AuthState = {
  user: null,
  isAuthenticated: false,
  loading: false,
  error: null
};
```

**Actions**:
```typescript
export const AuthActions = createActionGroup({
  source: 'Auth',
  events: {
    'Login': props<{ email: string; password: string }>(),
    'Login Success': props<{ user: User; accessToken: string }>(),
    'Login Failure': props<{ error: string }>(),
    'Logout': emptyProps(),
    'Load User': emptyProps(),
    'Load User Success': props<{ user: User }>()
  }
});
```

**Reducer**:
```typescript
export const authReducer = createReducer(
  initialAuthState,
  on(AuthActions.login, (state) => ({ ...state, loading: true, error: null })),
  on(AuthActions.loginSuccess, (state, { user }) => ({
    ...state,
    user,
    isAuthenticated: true,
    loading: false
  })),
  on(AuthActions.loginFailure, (state, { error }) => ({
    ...state,
    error,
    loading: false
  })),
  on(AuthActions.logout, () => initialAuthState)
);
```

**Selectors**:
```typescript
export const selectAuthState = (state: AppState) => state.auth;
export const selectCurrentUser = createSelector(selectAuthState, (state) => state.user);
export const selectIsAuthenticated = createSelector(selectAuthState, (state) => state.isAuthenticated);
```

---

## API Services

### Experiment Service

**File**: `src/app/core/services/experiment.service.ts`

```typescript
@Injectable({ providedIn: 'root' })
export class ExperimentService {
  private readonly apiUrl = `${environment.apiUrl}/experiments`;

  constructor(private http: HttpClient) {}

  getAll(params?: ExperimentQueryParams): Observable<PaginatedResponse<Experiment>> {
    return this.http.get<PaginatedResponse<Experiment>>(this.apiUrl, { params });
  }

  getById(id: string): Observable<Experiment> {
    return this.http.get<Experiment>(`${this.apiUrl}/${id}`);
  }

  create(data: CreateExperimentRequest): Observable<Experiment> {
    return this.http.post<Experiment>(this.apiUrl, data);
  }

  update(id: string, data: UpdateExperimentRequest): Observable<Experiment> {
    return this.http.put<Experiment>(`${this.apiUrl}/${id}`, data);
  }

  start(id: string): Observable<Experiment> {
    return this.http.post<Experiment>(`${this.apiUrl}/${id}/start`, {});
  }

  complete(id: string, data: CompleteExperimentRequest): Observable<Experiment> {
    return this.http.post<Experiment>(`${this.apiUrl}/${id}/complete`, data);
  }
}
```

---

## Routing Configuration

**File**: `src/app/app.routes.ts`

```typescript
export const routes: Routes = [
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  {
    path: 'auth',
    loadChildren: () => import('./features/auth/auth.routes').then(m => m.AUTH_ROUTES)
  },
  {
    path: 'dashboard',
    component: DashboardComponent,
    canActivate: [authGuard]
  },
  {
    path: 'problems',
    loadChildren: () => import('./features/problems/problems.routes').then(m => m.PROBLEM_ROUTES),
    canActivate: [authGuard]
  },
  {
    path: 'experiments',
    loadChildren: () => import('./features/experiments/experiments.routes').then(m => m.EXPERIMENT_ROUTES),
    canActivate: [authGuard]
  },
  {
    path: 'metrics',
    loadChildren: () => import('./features/metrics/metrics.routes').then(m => m.METRIC_ROUTES),
    canActivate: [authGuard]
  },
  {
    path: 'reports',
    loadChildren: () => import('./features/reports/reports.routes').then(m => m.REPORT_ROUTES),
    canActivate: [authGuard]
  },
  { path: '**', redirectTo: '/dashboard' }
];
```

---

## Environment Configuration

**File**: `src/environments/environment.prod.ts`

```typescript
export const environment = {
  production: true,
  apiUrl: 'https://api.turaf.com',
  apiGatewayUrl: 'https://gateway.turaf.com'
};
```

**File**: `src/environments/environment.dev.ts`

```typescript
export const environment = {
  production: false,
  apiUrl: 'https://api.dev.turaf.com',
  apiGatewayUrl: 'https://gateway.dev.turaf.com'
};
```

---

## UI/UX Design

### Design System

**Colors**:
- Primary: #1976D2 (Blue)
- Accent: #FF4081 (Pink)
- Success: #4CAF50 (Green)
- Warning: #FF9800 (Orange)
- Error: #F44336 (Red)

**Typography**:
- Font Family: Roboto, sans-serif
- Headings: 24px, 20px, 18px, 16px
- Body: 14px
- Small: 12px

**Spacing**:
- Base unit: 8px
- Small: 8px
- Medium: 16px
- Large: 24px
- XLarge: 32px

---

## Testing

### Unit Tests

**Example**: `experiment.service.spec.ts`

```typescript
describe('ExperimentService', () => {
  let service: ExperimentService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ExperimentService]
    });
    service = TestBed.inject(ExperimentService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  it('should fetch experiments', () => {
    const mockExperiments = [{ id: '1', name: 'Test' }];

    service.getAll().subscribe(response => {
      expect(response.data).toEqual(mockExperiments);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/experiments`);
    expect(req.request.method).toBe('GET');
    req.flush({ data: mockExperiments });
  });
});
```

---

## Build and Deployment

### Build Commands

**Development**:
```bash
ng serve
```

**Production Build**:
```bash
ng build --configuration=production
```

**Output**: `dist/frontend/` directory

### Deployment to S3

```bash
aws s3 sync dist/frontend/ s3://turaf-frontend-prod/ --delete
aws cloudfront create-invalidation --distribution-id DISTRIBUTION_ID --paths "/*"
```

---

## References

- PROJECT.md: Frontend specifications
- Angular Documentation
- NgRx Documentation
- Angular Material Documentation
