# Task: Add Integration Tests

**Service**: Metrics Service  
**Phase**: 5  
**Estimated Time**: 3 hours  

## Objective

Create integration tests that verify the complete metrics management flow from API endpoints through to database.

## Prerequisites

- [x] All metrics-service implementation tasks completed
- [x] Task 008: Unit tests added

## Scope

**Test Files to Create**:
- `MetricControllerIntegrationTest.java`
- `MetricsFlowIntegrationTest.java`

## Implementation Details

Test complete flow:
1. Record single metric
2. Record batch metrics
3. Retrieve metrics with filters
4. Aggregate metrics
5. Verify time-series queries
6. Verify events published

## Acceptance Criteria

- [ ] All API endpoints tested end-to-end
- [ ] Complete metrics flow tested
- [ ] Aggregation verified
- [ ] Batch processing verified
- [ ] All integration tests pass

## Testing Requirements

**Integration Test Coverage**:
- Metric recording
- Batch processing
- Time-range queries
- Aggregation
- Event publishing
- Tenant isolation

## References

- Specification: `specs/metrics-service.md`
- Related Tasks: All metrics-service tasks
