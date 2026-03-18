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

- [x] All domain model tests pass
- [x] All application service tests pass
- [x] State machine tests pass
- [x] Code coverage > 80%
- [x] All edge cases covered

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
