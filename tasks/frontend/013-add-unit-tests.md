# Task: Add Unit Tests

**Service**: Frontend  
**Phase**: 9  
**Estimated Time**: 4 hours  

## Objective

Create comprehensive unit tests for all components, services, and store logic.

## Prerequisites

- [x] All frontend implementation tasks completed

## Scope

**Test Files to Create**:
- Component tests for all components
- Service tests for all services
- Store tests (reducers, effects, selectors)
- Guard and interceptor tests

## Acceptance Criteria

- [ ] All components tested
- [x] All services tested (core services completed)
- [ ] Store logic tested
- [ ] Code coverage > 80%
- [ ] All tests pass

## Progress

### Completed
- ✅ Core services tests (6 files):
  - error-handler.service.spec.ts
  - loading.service.spec.ts
  - storage.service.spec.ts
  - identity.service.spec.ts
  - organization.service.spec.ts
  - websocket.service.spec.ts

### Remaining
- Auth store tests (actions, reducer, effects, selectors)
- Feature store tests (problems, hypotheses, experiments, metrics, reports)
- Feature service tests
- Guard and interceptor tests
- Component tests

## References

- Specification: `specs/angular-frontend.md` (Testing section)
- Related Tasks: 014-add-e2e-tests
