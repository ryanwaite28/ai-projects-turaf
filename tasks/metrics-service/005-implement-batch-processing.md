# Task: Implement Batch Processing

**Service**: Metrics Service  
**Phase**: 5  
**Estimated Time**: 2 hours  

## Objective

Implement optimized batch processing for recording large volumes of metrics efficiently.

## Prerequisites

- [x] Task 003: Metric service implemented

## Scope

**Files to Create**:
- `services/metrics-service/src/main/java/com/turaf/metrics/application/BatchMetricService.java`
- `services/metrics-service/src/main/java/com/turaf/metrics/application/dto/BatchRecordRequest.java`

## Implementation Details

### Batch Metric Service

```java
@Service
public class BatchMetricService {
    private final MetricRepository metricRepository;
    private final EventPublisher eventPublisher;
    
    private static final int BATCH_SIZE = 1000;
    
    @Transactional
    public void recordBatch(BatchRecordRequest request) {
        String organizationId = TenantContextHolder.getOrganizationId();
        
        List<Metric> metrics = request.getMetrics().stream()
            .map(req -> createMetric(req, organizationId))
            .collect(Collectors.toList());
        
        // Process in batches
        Lists.partition(metrics, BATCH_SIZE).forEach(batch -> {
            metricRepository.saveAll(batch);
        });
        
        // Publish batch event
        MetricBatchRecorded event = new MetricBatchRecorded(
            UUID.randomUUID().toString(),
            organizationId,
            request.getExperimentId(),
            metrics.size(),
            Instant.now()
        );
        eventPublisher.publish(event);
    }
    
    private Metric createMetric(RecordMetricRequest req, String organizationId) {
        MetricId id = MetricId.generate();
        Metric metric = new Metric(
            id,
            organizationId,
            req.getExperimentId(),
            req.getName(),
            req.getValue(),
            MetricType.valueOf(req.getType()),
            req.getTimestamp() != null ? req.getTimestamp() : Instant.now()
        );
        
        if (req.getTags() != null) {
            req.getTags().forEach(metric::addTag);
        }
        
        return metric;
    }
}
```

## Acceptance Criteria

- [ ] Batch processing works efficiently
- [ ] Large batches partitioned correctly
- [ ] Batch events published
- [ ] Performance optimized
- [ ] Unit tests pass

## Testing Requirements

**Unit Tests**:
- Test batch processing
- Test large batches
- Test event publishing

**Test Files to Create**:
- `BatchMetricServiceTest.java`

## References

- Specification: `specs/metrics-service.md` (Batch Processing section)
- Related Tasks: 006-implement-rest-controllers
