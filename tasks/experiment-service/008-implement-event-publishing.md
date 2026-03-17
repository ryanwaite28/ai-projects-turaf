# Task: Implement Event Publishing

**Service**: Experiment Service  
**Phase**: 4  
**Estimated Time**: 2 hours  

## Objective

Implement EventBridge event publishing infrastructure for experiment domain events.

## Prerequisites

- [x] Task 001: Domain model with events created
- [x] Task 006: Experiment service implemented

## Scope

**Files to Create**:
- `services/experiment-service/src/main/java/com/turaf/experiment/infrastructure/events/EventPublisher.java`
- `services/experiment-service/src/main/java/com/turaf/experiment/infrastructure/events/EventBridgePublisher.java`
- `services/experiment-service/src/main/java/com/turaf/experiment/infrastructure/events/EventMapper.java`
- `services/experiment-service/src/main/java/com/turaf/experiment/infrastructure/config/EventBridgeConfig.java`

## Implementation Details

Similar to organization-service event publishing implementation. Key events to publish:
- ProblemCreated
- HypothesisCreated
- ExperimentStarted
- ExperimentCompleted

## Acceptance Criteria

- [ ] EventPublisher interface defined
- [ ] EventBridge publisher implementation works
- [ ] Events published with correct envelope
- [ ] All domain events supported
- [ ] Integration tests pass

## Testing Requirements

**Integration Tests**:
- Test event publishing for all event types
- Test event envelope structure

**Test Files to Create**:
- `EventBridgePublisherTest.java`

## References

- Specification: `specs/experiment-service.md` (Event Publishing section)
- Specification: `specs/event-schemas.md`
- Related Tasks: 009-add-unit-tests
