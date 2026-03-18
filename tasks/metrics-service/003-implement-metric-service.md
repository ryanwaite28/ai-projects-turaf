# Task: Implement Metric Service

**Service**: Metrics Service  
**Phase**: 5  
**Estimated Time**: 2-3 hours  

## Objective

Implement the application layer service for metric recording and retrieval.

## Prerequisites

- [x] Task 001: Domain model created
- [x] Task 002: Repositories implemented

## Scope

**Files to Create**:
- `services/metrics-service/src/main/java/com/turaf/metrics/application/MetricService.java`
- `services/metrics-service/src/main/java/com/turaf/metrics/application/dto/RecordMetricRequest.java`
- `services/metrics-service/src/main/java/com/turaf/metrics/application/dto/MetricDto.java`

## Implementation Details

### Metric Service

```java
@Service
@Transactional
public class MetricService {
    private final MetricRepository metricRepository;
    private final EventPublisher eventPublisher;
    
    public MetricDto recordMetric(RecordMetricRequest request) {
        String organizationId = TenantContextHolder.getOrganizationId();
        MetricId id = MetricId.generate();
        
        Metric metric = new Metric(
            id,
            organizationId,
            request.getExperimentId(),
            request.getName(),
            request.getValue(),
            MetricType.valueOf(request.getType()),
            request.getTimestamp() != null ? request.getTimestamp() : Instant.now()
        );
        
        if (request.getTags() != null) {
            request.getTags().forEach(metric::addTag);
        }
        
        metricRepository.save(metric);
        
        MetricRecorded event = new MetricRecorded(
            UUID.randomUUID().toString(),
            organizationId,
            request.getExperimentId(),
            request.getName(),
            request.getValue(),
            metric.getTimestamp()
        );
        eventPublisher.publish(event);
        
        return MetricDto.fromDomain(metric);
    }
    
    public List<MetricDto> recordBatch(List<RecordMetricRequest> requests) {
        return requests.stream()
            .map(this::recordMetric)
            .collect(Collectors.toList());
    }
    
    public List<MetricDto> getMetrics(String experimentId, String name, Instant start, Instant end) {
        List<Metric> metrics;
        if (name != null) {
            metrics = metricRepository.findByExperimentIdAndName(experimentId, name, start, end);
        } else {
            metrics = metricRepository.findByExperimentId(experimentId, start, end);
        }
        return metrics.stream()
            .map(MetricDto::fromDomain)
            .collect(Collectors.toList());
    }
}
```

## Acceptance Criteria

- [x] Record single metric works
- [x] Record batch metrics works
- [x] Get metrics by experiment works
- [x] Time-range filtering works
- [x] Events published
- [x] Unit tests pass

## Testing Requirements

**Unit Tests**:
- Test record metric
- Test record batch
- Test get metrics with filters

**Test Files to Create**:
- `MetricServiceTest.java`

## References

- Specification: `specs/metrics-service.md` (Application Services section)
- Related Tasks: 004-implement-aggregation-service
