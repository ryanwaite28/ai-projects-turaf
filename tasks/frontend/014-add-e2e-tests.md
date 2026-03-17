# Task: Add E2E Tests

**Service**: Frontend  
**Phase**: 9  
**Estimated Time**: 3 hours  

## Objective

Create end-to-end tests for critical user flows using Playwright or Cypress.

## Prerequisites

- [x] All frontend implementation tasks completed

## Scope

**Test Files to Create**:
- Login flow test
- Create problem flow test
- Create experiment flow test
- Complete experiment flow test

## Implementation Details

### Example E2E Test

```typescript
describe('Experiment Flow', () => {
  it('should create and start experiment', () => {
    cy.login('user@example.com', 'password');
    cy.visit('/experiments');
    cy.contains('Create Experiment').click();
    cy.get('[name="name"]').type('Test Experiment');
    cy.get('[name="description"]').type('Test Description');
    cy.contains('Save').click();
    cy.contains('Test Experiment').should('be.visible');
    cy.contains('Start').click();
    cy.contains('Running').should('be.visible');
  });
});
```

## Acceptance Criteria

- [ ] E2E framework configured
- [ ] Critical flows tested
- [ ] Tests run in CI/CD
- [ ] All E2E tests pass

## References

- Specification: `specs/angular-frontend.md` (Testing section)
- Related Tasks: All frontend tasks
