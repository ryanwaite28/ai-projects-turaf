# Task: Implement Aggregation Logic

**Service**: Reporting Service  
**Phase**: 7  
**Estimated Time**: 2 hours  

## Objective

Implement logic to aggregate and analyze fetched data for report generation.

## Prerequisites

- [x] Task 003: Data fetching implemented

## Scope

**Files to Create**:
- `services/reporting-service/src/main/java/com/turaf/reporting/service/DataAggregationService.java`
- `services/reporting-service/src/main/java/com/turaf/reporting/model/AggregatedReportData.java`

## Implementation Details

### Data Aggregation Service

```java
public class DataAggregationService {
    
    public AggregatedReportData aggregateData(ReportData data) {
        ExperimentDto experiment = data.getExperiment();
        
        // Calculate experiment duration
        Duration duration = Duration.between(
            experiment.getStartedAt(),
            experiment.getCompletedAt()
        );
        
        // Aggregate metrics
        Map<String, MetricSummary> metricSummaries = data.getAggregatedMetrics()
            .entrySet()
            .stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> createMetricSummary(entry.getValue())
            ));
        
        // Calculate key insights
        List<String> insights = generateInsights(data);
        
        return new AggregatedReportData(
            experiment,
            data.getHypothesis(),
            data.getProblem(),
            duration,
            metricSummaries,
            insights
        );
    }
    
    private MetricSummary createMetricSummary(AggregatedMetricsDto metrics) {
        return new MetricSummary(
            metrics.getName(),
            metrics.getCount(),
            metrics.getAverage(),
            metrics.getMin(),
            metrics.getMax(),
            calculateTrend(metrics)
        );
    }
    
    private List<String> generateInsights(ReportData data) {
        List<String> insights = new ArrayList<>();
        
        // Add insights based on metrics
        data.getAggregatedMetrics().forEach((name, metrics) -> {
            if (metrics.getAverage() > 0) {
                insights.add(String.format(
                    "Average %s: %.2f (min: %.2f, max: %.2f)",
                    name, metrics.getAverage(), metrics.getMin(), metrics.getMax()
                ));
            }
        });
        
        return insights;
    }
    
    private String calculateTrend(AggregatedMetricsDto metrics) {
        // Simple trend calculation
        if (metrics.getMax() > metrics.getAverage() * 1.5) {
            return "INCREASING";
        } else if (metrics.getMin() < metrics.getAverage() * 0.5) {
            return "DECREASING";
        }
        return "STABLE";
    }
}
```

## Acceptance Criteria

- [ ] Data aggregation works correctly
- [ ] Metrics summarized properly
- [ ] Insights generated
- [ ] Trends calculated
- [ ] Unit tests pass

## Testing Requirements

**Unit Tests**:
- Test data aggregation
- Test metric summaries
- Test insight generation

**Test Files to Create**:
- `DataAggregationServiceTest.java`

## References

- Specification: `specs/reporting-service.md` (Aggregation section)
- Related Tasks: 005-create-report-templates
