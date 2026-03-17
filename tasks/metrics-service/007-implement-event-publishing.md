# Task: Implement Event Publishing

**Service**: Metrics Service  
**Phase**: 5  
**Estimated Time**: 2 hours  

## Objective

Implement EventBridge event publishing infrastructure for metric events.

## Prerequisites

- [x] Task 001: Domain model with events created
- [x] Task 003: Metric service implemented

## Scope

**Files to Create**:
- `services/metrics-service/src/main/java/com/turaf/metrics/infrastructure/events/EventPublisher.java`
- `services/metrics-service/src/main/java/com/turaf/metrics/infrastructure/events/EventBridgePublisher.java`
- `services/metrics-service/src/main/java/com/turaf/metrics/infrastructure/events/EventMapper.java`
- `services/metrics-service/src/main/java/com/turaf/metrics/infrastructure/config/EventBridgeConfig.java`

## Implementation Details

Similar to previous services. Key events:
- MetricRecorded
- MetricBatchRecorded

## Acceptance Criteria

- [ ] EventPublisher implemented
- [ ] Events published correctly
- [ ] Integration tests pass

## Testing Requirements

**Integration Tests**:
- Test event publishing

**Test Files to Create**:
- `EventBridgePublisherTest.java`

## References

- Specification: `specs/metrics-service.md` (Event Publishing section)
- Related Tasks: 008-add-unit-tests
