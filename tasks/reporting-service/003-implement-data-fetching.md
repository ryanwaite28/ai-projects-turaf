# Task: Implement Data Fetching

**Service**: Reporting Service  
**Phase**: 7  
**Estimated Time**: 3 hours  

## Objective

Implement data fetching logic to retrieve experiment, hypothesis, problem, and metrics data from respective services.

## Prerequisites

- [x] Task 002: Event handler implemented

## Scope

**Files to Create**:
- `services/reporting-service/src/main/java/com/turaf/reporting/client/ExperimentServiceClient.java`
- `services/reporting-service/src/main/java/com/turaf/reporting/client/MetricsServiceClient.java`
- `services/reporting-service/src/main/java/com/turaf/reporting/service/DataFetchingService.java`
- `services/reporting-service/src/main/java/com/turaf/reporting/model/ReportData.java`

## Implementation Details

### Data Fetching Service

```java
public class DataFetchingService {
    private final ExperimentServiceClient experimentClient;
    private final MetricsServiceClient metricsClient;
    
    public ReportData fetchReportData(String experimentId, String organizationId) {
        // Fetch experiment details
        ExperimentDto experiment = experimentClient.getExperiment(experimentId, organizationId);
        
        // Fetch hypothesis
        HypothesisDto hypothesis = experimentClient.getHypothesis(
            experiment.getHypothesisId(), 
            organizationId
        );
        
        // Fetch problem
        ProblemDto problem = experimentClient.getProblem(
            hypothesis.getProblemId(), 
            organizationId
        );
        
        // Fetch metrics
        List<MetricDto> metrics = metricsClient.getMetrics(
            experimentId,
            experiment.getStartedAt(),
            experiment.getCompletedAt(),
            organizationId
        );
        
        // Fetch aggregated metrics
        Map<String, AggregatedMetricsDto> aggregatedMetrics = metricsClient.getAggregatedMetrics(
            experimentId,
            experiment.getStartedAt(),
            experiment.getCompletedAt(),
            organizationId
        );
        
        return new ReportData(experiment, hypothesis, problem, metrics, aggregatedMetrics);
    }
}
```

### Service Clients

```java
public class ExperimentServiceClient {
    private final String baseUrl;
    private final HttpClient httpClient;
    
    public ExperimentDto getExperiment(String experimentId, String organizationId) {
        // HTTP GET to experiment-service
        String url = baseUrl + "/api/v1/experiments/" + experimentId;
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("X-Organization-Id", organizationId)
            .GET()
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(response.body(), ExperimentDto.class);
    }
}
```

## Acceptance Criteria

- [ ] Data fetching service retrieves all required data
- [ ] Service clients make HTTP requests correctly
- [ ] Error handling for failed requests
- [ ] Retry logic implemented
- [ ] Unit tests pass

## Testing Requirements

**Unit Tests**:
- Test data fetching
- Test HTTP client calls
- Test error handling

**Test Files to Create**:
- `DataFetchingServiceTest.java`
- `ExperimentServiceClientTest.java`

## References

- Specification: `specs/reporting-service.md` (Data Fetching section)
- Related Tasks: 004-implement-aggregation-logic
