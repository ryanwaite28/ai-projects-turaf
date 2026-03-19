# Task: Setup Angular Project

**Service**: Frontend  
**Phase**: 9  
**Estimated Time**: 2-3 hours  

## Objective

Setup Angular 17 project with Angular Material, routing, and environment configuration.

## Prerequisites

- [ ] Node.js 18+ installed
- [ ] Angular CLI installed

## Scope

**Files to Create**:
- Angular project structure
- `frontend/angular.json`
- `frontend/src/environments/environment.ts`
- `frontend/src/environments/environment.prod.ts`

## Implementation Details

### Create Angular Project

```bash
ng new turaf-frontend --routing --style=scss
cd turaf-frontend
ng add @angular/material
```

### Environment Configuration

```typescript
// environment.ts
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api/v1',
  identityServiceUrl: 'http://localhost:8081/api/v1',
  organizationServiceUrl: 'http://localhost:8082/api/v1',
  experimentServiceUrl: 'http://localhost:8083/api/v1',
  metricsServiceUrl: 'http://localhost:8084/api/v1'
};

// environment.prod.ts
export const environment = {
  production: true,
  apiUrl: 'https://api.turaf.com/api/v1',
  identityServiceUrl: 'https://api.turaf.com/identity/api/v1',
  organizationServiceUrl: 'https://api.turaf.com/organization/api/v1',
  experimentServiceUrl: 'https://api.turaf.com/experiment/api/v1',
  metricsServiceUrl: 'https://api.turaf.com/metrics/api/v1'
};
```

## Acceptance Criteria

- [x] Angular project created
- [x] Angular Material installed
- [x] Routing configured
- [x] Environment files created
- [x] Project builds successfully
- [x] Development server runs

## Testing Requirements

**Validation**:
- Run `ng serve`
- Run `ng build`
- Verify no errors

## References

- Specification: `specs/angular-frontend.md` (Project Setup section)
- Related Tasks: 002-setup-ngrx-store
