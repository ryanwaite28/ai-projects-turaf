# Metrics Service Specification

**Source**: PROJECT.md (Section 40)

This specification defines the Metrics Service, responsible for metric ingestion, storage, aggregation, and querying.

---

## Service Overview

**Purpose**: Manage metric collection, storage, aggregation, and analysis for experiments

**Bounded Context**: Metrics and Analytics

**Service Type**: Core microservice (ECS Fargate)

---

## Responsibilities

- Metric ingestion and validation
- Metric storage (time-series data)
- Metric aggregation (avg, sum, count, min, max)
- Metric querying and filtering
- Batch metric processing
- Domain event publishing for metric events

---

## Technology Stack

**Framework**: Spring Boot 3.x  
**Persistence**: Spring Data JPA  
**Database**: PostgreSQL schema `metrics_schema` on shared RDS instance  
**Database User**: `metrics_user` (schema-scoped permissions)  
**Events**: Spring Cloud AWS (EventBridge)  
**Build Tool**: Maven  
**Java Version**: Java 17  

**Key Dependencies**:
- `spring-boot-starter-web`
- `spring-boot-starter-data-jpa`
- `spring-boot-starter-validation`
- `spring-boot-starter-actuator`
- `spring-cloud-aws-messaging`
- `postgresql` driver

---

## API Endpoints

### POST /api/v1/metrics

**Purpose**: Record a single metric for an experiment

**Headers**:
```
Authorization: Bearer {access-token}
X-Organization-Id: {organization-id}
```

**Request Body**:
```json
{
  "experimentId": "uuid",
  "name": "conversion_rate",
  "value": 0.25,
  "type": "GAUGE",
  "tags": {
    "variant": "A",
    "source": "web"
  }
}
```

**Response** (201 Created):
```json
{
  "metricId": "uuid",
  "experimentId": "uuid",
  "organizationId": "uuid",
  "name": "conversion_rate",
  "value": 0.25,
  "type": "GAUGE",
  "timestamp": "ISO-8601",
  "tags": {
    "variant": "A",
    "source": "web"
  }
}
```

**Validation**:
- experimentId must exist and belong to organization
- Experiment must be in RUNNING status
- name: 1-100 characters, required
- value: numeric, required
- type: COUNTER, GAUGE, or HISTOGRAM (required)
- tags: optional key-value pairs

**Business Rules**:
- Publishes MetricRecorded event

---

### POST /api/v1/metrics/batch

**Purpose**: Record multiple metrics at once

**Request Body**:
```json
{
  "experimentId": "uuid",
  "metrics": [
    {
      "name": "conversion_rate",
      "value": 0.25,
      "unit": "percentage"
    },
    {
      "name": "avg_session_time",
      "value": 145.5,
      "unit": "seconds"
    }
  ]
}
```

**Response** (201 Created):
```json
{
  "metricsCreated": 2,
  "metrics": [
    {
      "metricId": "uuid",
      "name": "conversion_rate",
      "value": 0.25
    },
    {
      "metricId": "uuid",
      "name": "avg_session_time",
      "value": 145.5
    }
  ]
}
```

**Business Rules**:
- All metrics recorded with same timestamp
- Atomic operation (all succeed or all fail)
- Publishes MetricBatchRecorded event

---

### GET /api/v1/experiments/{experimentId}/metrics

**Purpose**: Get all metrics for an experiment

**Query Parameters**:
- `name` (optional filter by metric name)
- `startTime` (optional filter by time range)
- `endTime` (optional filter by time range)
- `page`, `size`, `sort`

**Response** (200 OK):
```json
{
  "metrics": [
    {
      "metricId": "uuid",
      "name": "conversion_rate",
      "value": 0.25,
      "unit": "percentage",
      "recordedAt": "ISO-8601",
      "metadata": {}
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 150,
  "totalPages": 8
}
```

---

### GET /api/v1/metrics/{id}

**Purpose**: Get a specific metric by ID

**Response** (200 OK):
```json
{
  "metricId": "uuid",
  "experimentId": "uuid",
  "organizationId": "uuid",
  "name": "conversion_rate",
  "value": 0.25,
  "unit": "percentage",
  "recordedAt": "ISO-8601",
  "metadata": {
    "variant": "A",
    "source": "web"
  }
}
```

---

### GET /api/v1/metrics/aggregate

**Purpose**: Get aggregated metrics for an experiment

**Query Parameters**:
- `experimentId` (required)
- `metricName` (required)
- `aggregationType` (required: avg, sum, count, min, max)
- `startTime` (optional)
- `endTime` (optional)

**Response** (200 OK):
```json
{
  "experimentId": "uuid",
  "metricName": "conversion_rate",
  "aggregationType": "avg",
  "value": 0.247,
  "count": 150,
  "startTime": "ISO-8601",
  "endTime": "ISO-8601",
  "calculatedAt": "ISO-8601"
}
```

**Aggregation Types**:
- `avg`: Average value
- `sum`: Sum of all values
- `count`: Number of data points
- `min`: Minimum value
- `max`: Maximum value

---

## Database Schema

**Schema**: `metrics_schema`  
**Connection Configuration**:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:5432/turaf?currentSchema=metrics_schema
    username: metrics_user
    password: ${DB_PASSWORD}
  jpa:
    properties:
      hibernate:
        default_schema: metrics_schema
  flyway:
    schemas: metrics_schema
    default-schema: metrics_schema
```

### metrics

```sql
CREATE TABLE metrics (
    id UUID PRIMARY KEY,
    experiment_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    value NUMERIC NOT NULL,
    unit VARCHAR(50),
    recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_metrics_experiment_id ON metrics(experiment_id);
CREATE INDEX idx_metrics_org_id ON metrics(organization_id);
CREATE INDEX idx_metrics_name ON metrics(name);
CREATE INDEX idx_metrics_recorded_at ON metrics(recorded_at DESC);
CREATE INDEX idx_metrics_experiment_name ON metrics(experiment_id, name);
```

### metric_aggregations

```sql
CREATE TABLE metric_aggregations (
    id UUID PRIMARY KEY,
    experiment_id UUID NOT NULL,
    metric_name VARCHAR(100) NOT NULL,
    aggregation_type VARCHAR(20) NOT NULL,
    value NUMERIC NOT NULL,
    count INTEGER NOT NULL,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    calculated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_aggregation_type CHECK (aggregation_type IN ('avg', 'sum', 'count', 'min', 'max'))
);

CREATE INDEX idx_aggregations_experiment_id ON metric_aggregations(experiment_id);
CREATE INDEX idx_aggregations_metric_name ON metric_aggregations(metric_name);
CREATE INDEX idx_aggregations_type ON metric_aggregations(aggregation_type);
```

---

## Application Services

### MetricService

**Responsibilities**:
- Metric ingestion
- Metric validation
- Metric storage
- Event publishing

**Methods**:
```java
public interface MetricService {
    MetricDto recordMetric(RecordMetricRequest request, OrganizationId orgId);
    BatchMetricResponse recordMetricsBatch(BatchMetricRequest request, OrganizationId orgId);
    MetricDto getMetric(MetricId id, OrganizationId orgId);
    Page<MetricDto> getExperimentMetrics(ExperimentId experimentId, Optional<String> metricName, 
                                         Optional<Instant> startTime, Optional<Instant> endTime,
                                         Pageable pageable, OrganizationId orgId);
}
```

### MetricAggregationService

**Responsibilities**:
- Calculate aggregations
- Cache aggregation results
- Provide aggregation queries

**Methods**:
```java
public interface MetricAggregationService {
    AggregationResult calculateAggregation(ExperimentId experimentId, String metricName, 
                                          AggregationType type, Optional<Instant> startTime, 
                                          Optional<Instant> endTime, OrganizationId orgId);
    List<AggregationResult> calculateAllAggregations(ExperimentId experimentId, String metricName,
                                                     OrganizationId orgId);
    void precomputeAggregations(ExperimentId experimentId);
}
```

---

## Domain Logic

### Metric Entity

```java
@Entity
@Table(name = "metrics")
public class Metric {
    
    @Id
    private UUID id;
    
    @Column(nullable = false)
    private UUID experimentId;
    
    @Column(nullable = false)
    private UUID organizationId;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(nullable = false)
    private BigDecimal value;
    
    @Column(length = 50)
    private String unit;
    
    @Column(nullable = false)
    private Instant recordedAt;
    
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;
    
    public void validate() {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Metric name is required");
        }
        if (value == null) {
            throw new ValidationException("Metric value is required");
        }
        if (recordedAt == null) {
            recordedAt = Instant.now();
        }
    }
}
```

### MetricValue Value Object

```java
public class MetricValue {
    private final BigDecimal value;
    
    public MetricValue(BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }
        this.value = value;
    }
    
    public BigDecimal getValue() {
        return value;
    }
    
    public boolean isPositive() {
        return value.compareTo(BigDecimal.ZERO) > 0;
    }
    
    public boolean isNegative() {
        return value.compareTo(BigDecimal.ZERO) < 0;
    }
    
    public MetricValue add(MetricValue other) {
        return new MetricValue(this.value.add(other.value));
    }
}
```

---

## Metric Validation

### Validation Rules

**Experiment Validation**:
- Experiment must exist
- Experiment must belong to organization
- Experiment must be in RUNNING status
- recordedAt must be between experiment start and end times

**Metric Name Validation**:
- Required, 1-100 characters
- Alphanumeric, underscores, hyphens only
- Case-insensitive

**Value Validation**:
- Must be numeric
- Can be positive, negative, or zero
- Precision up to 10 decimal places

**Unit Validation**:
- Optional, max 50 characters
- Common units: percentage, seconds, count, bytes, etc.

---

## Aggregation Logic

### Aggregation Calculations

**Average (avg)**:
```java
public BigDecimal calculateAverage(List<Metric> metrics) {
    if (metrics.isEmpty()) {
        return BigDecimal.ZERO;
    }
    BigDecimal sum = metrics.stream()
        .map(Metric::getValue)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    return sum.divide(BigDecimal.valueOf(metrics.size()), 10, RoundingMode.HALF_UP);
}
```

**Sum**:
```java
public BigDecimal calculateSum(List<Metric> metrics) {
    return metrics.stream()
        .map(Metric::getValue)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
}
```

**Count**:
```java
public long calculateCount(List<Metric> metrics) {
    return metrics.size();
}
```

**Min/Max**:
```java
public BigDecimal calculateMin(List<Metric> metrics) {
    return metrics.stream()
        .map(Metric::getValue)
        .min(BigDecimal::compareTo)
        .orElse(BigDecimal.ZERO);
}

public BigDecimal calculateMax(List<Metric> metrics) {
    return metrics.stream()
        .map(Metric::getValue)
        .max(BigDecimal::compareTo)
        .orElse(BigDecimal.ZERO);
}
```

---

## Events Published

### MetricRecorded

**Triggered When**: Single metric is recorded

**Payload**:
```json
{
  "metricId": "uuid",
  "experimentId": "uuid",
  "metricName": "string",
  "metricValue": "number",
  "unit": "string",
  "recordedAt": "ISO-8601"
}
```

### MetricBatchRecorded

**Triggered When**: Batch of metrics is recorded

**Payload**:
```json
{
  "experimentId": "uuid",
  "metricsCount": 10,
  "metricNames": ["conversion_rate", "session_time"],
  "recordedAt": "ISO-8601"
}
```

---

## Batch Processing

### Batch Ingestion Strategy

**Benefits**:
- Reduced API calls
- Better performance
- Atomic operations
- Single timestamp for related metrics

**Implementation**:
```java
@Transactional
public BatchMetricResponse recordMetricsBatch(BatchMetricRequest request, OrganizationId orgId) {
    // Validate experiment
    Experiment experiment = validateExperiment(request.getExperimentId(), orgId);
    
    // Create all metrics with same timestamp
    Instant recordedAt = Instant.now();
    List<Metric> metrics = request.getMetrics().stream()
        .map(m -> createMetric(m, experiment.getId(), orgId, recordedAt))
        .collect(Collectors.toList());
    
    // Save all at once
    List<Metric> saved = metricRepository.saveAll(metrics);
    
    // Publish batch event
    publishBatchEvent(experiment.getId(), saved);
    
    return toBatchResponse(saved);
}
```

---

## Time-Series Queries

### Time Range Filtering

**Query by Time Range**:
```java
@Query("SELECT m FROM Metric m WHERE m.experimentId = :experimentId " +
       "AND m.recordedAt BETWEEN :startTime AND :endTime " +
       "ORDER BY m.recordedAt DESC")
List<Metric> findByExperimentAndTimeRange(
    @Param("experimentId") UUID experimentId,
    @Param("startTime") Instant startTime,
    @Param("endTime") Instant endTime
);
```

**Query by Metric Name and Time Range**:
```java
@Query("SELECT m FROM Metric m WHERE m.experimentId = :experimentId " +
       "AND m.name = :name " +
       "AND m.recordedAt BETWEEN :startTime AND :endTime " +
       "ORDER BY m.recordedAt DESC")
List<Metric> findByExperimentNameAndTimeRange(
    @Param("experimentId") UUID experimentId,
    @Param("name") String name,
    @Param("startTime") Instant startTime,
    @Param("endTime") Instant endTime
);
```

---

## Performance Optimization

### Database Indexes

**Composite Indexes**:
- `(experiment_id, name)` - Fast metric name filtering
- `(experiment_id, recorded_at)` - Fast time-series queries
- `(organization_id, experiment_id)` - Multi-tenant queries

### Query Optimization

**Pagination**:
- Always use pagination for metric lists
- Default page size: 20
- Max page size: 100

**Aggregation Caching**:
- Cache aggregation results
- Invalidate on new metrics
- TTL: 5 minutes

---

## Error Handling

**Error Codes**:
- `MET_001`: Metric not found
- `MET_002`: Experiment not in RUNNING status
- `MET_003`: Invalid metric value
- `MET_004`: Experiment not found
- `MET_005`: Metric recorded outside experiment time range

---

## Testing Strategy

### Unit Tests
- Test metric validation
- Test aggregation calculations
- Test value object behavior

### Integration Tests
- Test repository queries
- Test time-series queries
- Test batch operations
- Test event publishing

### API Tests
- Test all endpoints
- Test pagination
- Test filtering
- Test error scenarios

---

## Monitoring

### Metrics to Track
- Metrics recorded per second
- Batch size distribution
- Aggregation calculation time
- Query response time
- Failed validations

### Logging
- Log all metric ingestion
- Log aggregation calculations
- Log validation failures
- Include experiment context

---

## Future Enhancements

- Real-time metric streaming
- Advanced statistical analysis (percentiles, standard deviation)
- Metric visualization endpoints
- Metric alerts and thresholds
- Time-series database (TimescaleDB extension)
- Metric retention policies

---

## References

- PROJECT.md: Metrics Service specification
- domain-model.md: Metric entity definition
- event-flow.md: Event specifications
