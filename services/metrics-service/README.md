# Metrics Service

Metrics collection and storage service for the Turaf platform.

## Clean Architecture Layers

This service follows Clean Architecture principles with clear separation of concerns across four layers:

### Domain Layer (`com.turaf.metrics.domain`)

**Purpose**: Contains core business logic and domain models.

**Responsibilities**:
- Define domain entities (Metric, MetricValue, Aggregation)
- Define value objects (MetricId, MetricName, MetricValue)
- Define domain events (MetricRecorded, AggregationCompleted)
- Define repository interfaces
- Implement domain services for metric validation
- Enforce business invariants

**Dependencies**: None

### Application Layer (`com.turaf.metrics.application`)

**Purpose**: Orchestrates domain objects to implement use cases.

**Responsibilities**:
- Implement use cases (RecordMetric, QueryMetrics, AggregateMetrics)
- Define application services
- Coordinate domain objects
- Handle transaction boundaries

**Dependencies**: Domain layer only

### Infrastructure Layer (`com.turaf.metrics.infrastructure`)

**Purpose**: Implements technical concerns and external integrations.

**Responsibilities**:
- Implement repository interfaces using JPA
- Define JPA entities
- Implement event publishers
- External service integrations
- Database migrations

**Dependencies**: Domain and Application layers

### Interfaces Layer (`com.turaf.metrics.interfaces`)

**Purpose**: Exposes application functionality via REST API.

**Responsibilities**:
- REST controllers
- Request/Response DTOs
- Input validation
- Exception handlers
- API documentation

**Dependencies**: Application layer only

## Dependency Rules

```
Interfaces → Application → Domain
Infrastructure → Application → Domain
Infrastructure → Domain
```

## Package Structure

```
com.turaf.metrics/
├── domain/
│   ├── model/
│   ├── event/
│   ├── repository/
│   └── service/
├── application/
│   ├── service/
│   ├── dto/
│   └── usecase/
├── infrastructure/
│   ├── persistence/
│   ├── event/
│   ├── config/
│   └── migration/
└── interfaces/
    ├── rest/
    ├── dto/
    └── exception/
```

## Building

```bash
mvn clean install
mvn test
mvn spring-boot:run
```
