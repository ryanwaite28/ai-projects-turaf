# Task: Implement Aggregation Service

**Service**: Metrics Service  
**Phase**: 5  
**Estimated Time**: 3 hours  

## Objective

Implement metric aggregation service for calculating statistics (avg, min, max, sum, count) over time ranges.

## Prerequisites

- [x] Task 002: Repositories implemented
- [x] Task 003: Metric service implemented

## Scope

**Files to Create**:
- `services/metrics-service/src/main/java/com/turaf/metrics/application/AggregationService.java`
- `services/metrics-service/src/main/java/com/turaf/metrics/application/dto/AggregatedMetricsDto.java`
- `services/metrics-service/src/main/java/com/turaf/metrics/domain/Aggregation.java`

## Implementation Details

### Aggregation Service

```java
@Service
public class AggregationService {
    private final MetricRepository metricRepository;
    
    public AggregatedMetricsDto aggregateMetrics(String experimentId, String metricName, 
                                                 Instant start, Instant end) {
        List<Metric> metrics = metricRepository.findByExperimentIdAndName(
            experimentId, metricName, start, end
        );
        
        if (metrics.isEmpty()) {
            return AggregatedMetricsDto.empty();
        }
        
        DoubleSummaryStatistics stats = metrics.stream()
            .mapToDouble(Metric::getValue)
            .summaryStatistics();
        
        return new AggregatedMetricsDto(
            metricName,
            stats.getCount(),
            stats.getSum(),
            stats.getAverage(),
            stats.getMin(),
            stats.getMax(),
            start,
            end
        );
    }
    
    public Map<String, AggregatedMetricsDto> aggregateAllMetrics(String experimentId, 
                                                                  Instant start, Instant end) {
        List<Metric> metrics = metricRepository.findByExperimentId(experimentId, start, end);
        
        return metrics.stream()
            .collect(Collectors.groupingBy(Metric::getName))
            .entrySet()
            .stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> calculateStats(entry.getKey(), entry.getValue(), start, end)
            ));
    }
    
    private AggregatedMetricsDto calculateStats(String name, List<Metric> metrics, 
                                                Instant start, Instant end) {
        DoubleSummaryStatistics stats = metrics.stream()
            .mapToDouble(Metric::getValue)
            .summaryStatistics();
        
        return new AggregatedMetricsDto(
            name,
            stats.getCount(),
            stats.getSum(),
            stats.getAverage(),
            stats.getMin(),
            stats.getMax(),
            start,
            end
        );
    }
}
```

## Acceptance Criteria

- [x] Aggregate single metric works
- [x] Aggregate all metrics works
- [x] Statistics calculated correctly
- [x] Empty results handled
- [x] Unit tests pass

## Testing Requirements

**Unit Tests**:
- Test aggregation calculations
- Test empty metric sets
- Test multiple metrics

**Test Files to Create**:
- `AggregationServiceTest.java`

## References

- Specification: `specs/metrics-service.md` (Aggregation section)
- Related Tasks: 005-implement-batch-processing
