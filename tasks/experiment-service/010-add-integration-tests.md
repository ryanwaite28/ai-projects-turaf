# Task: Add Integration Tests

**Service**: Experiment Service  
**Phase**: 4  
**Estimated Time**: 3 hours  

## Objective

Create integration tests that verify the complete experiment management flow from API endpoints through to database.

## Prerequisites

- [x] All experiment-service implementation tasks completed
- [x] Task 009: Unit tests added

## Scope

**Test Files to Create**:
- `ExperimentControllerIntegrationTest.java`
- `ExperimentFlowIntegrationTest.java`

## Implementation Details

Test complete flow:
1. Create problem
2. Create hypothesis for problem
3. Create experiment for hypothesis
4. Start experiment
5. Complete experiment
6. Verify state transitions
7. Verify events published

## Acceptance Criteria

- [ ] All API endpoints tested end-to-end
- [ ] Complete experiment flow tested
- [ ] State transitions verified
- [ ] Event publishing verified
- [ ] All integration tests pass

## Testing Requirements

**Integration Test Coverage**:
- Problem, Hypothesis, Experiment CRUD
- State transitions
- Event publishing
- Tenant isolation
- Error scenarios

## References

- Specification: `specs/experiment-service.md`
- Related Tasks: All experiment-service tasks
