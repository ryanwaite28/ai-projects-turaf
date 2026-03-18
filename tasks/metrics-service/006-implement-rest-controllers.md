# Task: Implement REST Controllers

**Service**: Metrics Service  
**Phase**: 5  
**Estimated Time**: 2-3 hours  

## Objective

Implement REST API endpoints for metric recording, retrieval, and aggregation.

## Prerequisites

- [x] Task 003: Metric service implemented
- [x] Task 004: Aggregation service implemented
- [x] Task 005: Batch processing implemented

## Scope

**Files to Create**:
- `services/metrics-service/src/main/java/com/turaf/metrics/interfaces/rest/MetricController.java`
- `services/metrics-service/src/main/java/com/turaf/metrics/interfaces/rest/GlobalExceptionHandler.java`

## Implementation Details

### Metric Controller

```java
@RestController
@RequestMapping("/api/v1/metrics")
@PreAuthorize("isAuthenticated()")
public class MetricController {
    private final MetricService metricService;
    private final AggregationService aggregationService;
    private final BatchMetricService batchMetricService;
    
    @PostMapping
    public ResponseEntity<MetricDto> recordMetric(@Valid @RequestBody RecordMetricRequest request) {
        MetricDto metric = metricService.recordMetric(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(metric);
    }
    
    @PostMapping("/batch")
    public ResponseEntity<Void> recordBatch(@Valid @RequestBody BatchRecordRequest request) {
        batchMetricService.recordBatch(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
    
    @GetMapping
    public ResponseEntity<List<MetricDto>> getMetrics(
            @RequestParam String experimentId,
            @RequestParam(required = false) String name,
            @RequestParam Instant start,
            @RequestParam Instant end) {
        List<MetricDto> metrics = metricService.getMetrics(experimentId, name, start, end);
        return ResponseEntity.ok(metrics);
    }
    
    @GetMapping("/aggregate")
    public ResponseEntity<AggregatedMetricsDto> aggregateMetrics(
            @RequestParam String experimentId,
            @RequestParam String name,
            @RequestParam Instant start,
            @RequestParam Instant end) {
        AggregatedMetricsDto aggregated = aggregationService.aggregateMetrics(
            experimentId, name, start, end
        );
        return ResponseEntity.ok(aggregated);
    }
    
    @GetMapping("/aggregate/all")
    public ResponseEntity<Map<String, AggregatedMetricsDto>> aggregateAllMetrics(
            @RequestParam String experimentId,
            @RequestParam Instant start,
            @RequestParam Instant end) {
        Map<String, AggregatedMetricsDto> aggregated = aggregationService.aggregateAllMetrics(
            experimentId, start, end
        );
        return ResponseEntity.ok(aggregated);
    }
}
```

## Acceptance Criteria

- [x] POST /api/v1/metrics endpoint works
- [x] POST /api/v1/metrics/batch endpoint works
- [x] GET /api/v1/metrics endpoint works
- [x] GET /api/v1/metrics/aggregate endpoint works
- [x] GET /api/v1/metrics/aggregate/all endpoint works
- [x] All endpoints documented with OpenAPI

## Testing Requirements

**Integration Tests**:
- Test record metric
- Test batch recording
- Test metric retrieval
- Test aggregation

**Test Files to Create**:
- `MetricControllerTest.java`

## References

- Specification: `specs/metrics-service.md` (API Endpoints section)
- Related Tasks: 007-implement-event-publishing
