# Task: Create Metrics Service Domain Model

**Service**: Metrics Service  
**Phase**: 5  
**Estimated Time**: 3 hours  

## Objective

Create the domain model for the Metrics Service including Metric entity and aggregation logic for time-series data.

## Prerequisites

- [x] Task 001: Clean Architecture layers established
- [x] Task 002: DDD patterns implemented
- [x] Task 003: Multi-tenant context setup

## Scope

**Files to Create**:
- `services/metrics-service/src/main/java/com/turaf/metrics/domain/Metric.java`
- `services/metrics-service/src/main/java/com/turaf/metrics/domain/MetricId.java`
- `services/metrics-service/src/main/java/com/turaf/metrics/domain/MetricType.java`
- `services/metrics-service/src/main/java/com/turaf/metrics/domain/MetricRepository.java`
- `services/metrics-service/src/main/java/com/turaf/metrics/domain/event/MetricRecorded.java`
- `services/metrics-service/src/main/java/com/turaf/metrics/domain/event/MetricBatchRecorded.java`

## Implementation Details

### Metric Entity

```java
public class Metric extends Entity<MetricId> implements TenantAware {
    private String organizationId;
    private String experimentId;
    private String name;
    private Double value;
    private MetricType type;
    private Instant timestamp;
    private Map<String, String> tags;
    
    public Metric(MetricId id, String organizationId, String experimentId, 
                 String name, Double value, MetricType type, Instant timestamp) {
        super(id);
        this.organizationId = Objects.requireNonNull(organizationId);
        this.experimentId = Objects.requireNonNull(experimentId);
        this.name = validateName(name);
        this.value = Objects.requireNonNull(value);
        this.type = Objects.requireNonNull(type);
        this.timestamp = Objects.requireNonNull(timestamp);
        this.tags = new HashMap<>();
    }
    
    public void addTag(String key, String value) {
        this.tags.put(key, value);
    }
    
    private String validateName(String name) {
        if (name == null || name.isBlank() || name.length() > 100) {
            throw new IllegalArgumentException("Name must be 1-100 characters");
        }
        return name;
    }
    
    // Getters
}
```

### MetricType Enum

```java
public enum MetricType {
    COUNTER,
    GAUGE,
    HISTOGRAM
}
```

### Repository Interface

```java
public interface MetricRepository {
    void save(Metric metric);
    void saveAll(List<Metric> metrics);
    List<Metric> findByExperimentId(String experimentId, Instant start, Instant end);
    List<Metric> findByExperimentIdAndName(String experimentId, String name, Instant start, Instant end);
    Map<String, Double> aggregateByExperiment(String experimentId, String metricName);
}
```

### Domain Events

```java
public class MetricRecorded implements DomainEvent {
    private final String eventId;
    private final String organizationId;
    private final String experimentId;
    private final String metricName;
    private final Double value;
    private final Instant timestamp;
    
    @Override
    public String getEventType() {
        return "MetricRecorded";
    }
    
    // Constructor, getters
}
```

## Acceptance Criteria

- [ ] Metric entity created with validation
- [ ] MetricType enum defined
- [ ] Repository interface defined
- [ ] Domain events created
- [ ] Tags support implemented
- [ ] Unit tests pass

## Testing Requirements

**Unit Tests**:
- Test Metric creation and validation
- Test tag management
- Test metric type validation

**Test Files to Create**:
- `MetricTest.java`

## References

- Specification: `specs/metrics-service.md` (Domain Model section)
- Related Tasks: 002-create-repositories
