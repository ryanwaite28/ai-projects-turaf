# Task: Add Unit Tests

**Service**: Experiment Service  
**Phase**: 4  
**Estimated Time**: 3 hours  

## Objective

Create comprehensive unit tests for domain model, application services, and infrastructure components.

## Prerequisites

- [x] All experiment-service implementation tasks completed

## Scope

**Test Files to Create**:
- Domain tests (from task 001)
- `ProblemServiceTest.java`
- `HypothesisServiceTest.java`
- `ExperimentServiceTest.java`
- `ExperimentStateMachineTest.java`

## Acceptance Criteria

- [ ] All domain model tests pass
- [ ] All application service tests pass
- [ ] State machine tests pass
- [ ] Code coverage > 80%
- [ ] All edge cases covered

## Testing Requirements

**Unit Test Coverage**:
- Domain entities and state machine
- Application services
- Event publisher
- Repository implementations
- Exception scenarios

## References

- Specification: `specs/experiment-service.md`
- Related Tasks: 010-add-integration-tests
