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
- `services/reporting-service/src/services/aggregation.py`
- `services/reporting-service/src/models/aggregated_data.py`

## Implementation Details

### Data Aggregation Service

```python
import logging
import statistics
from typing import Dict, Any, List
from datetime import datetime
from collections import defaultdict
from models.report_data import ReportData
from models.aggregated_data import AggregatedReportData, MetricSummary

logger = logging.getLogger(__name__)

class DataAggregationService:
    """Service for aggregating and analyzing report data"""
    
    def aggregate_data(self, data: ReportData) -> AggregatedReportData:
        """
        Aggregate and analyze report data.
        
        Args:
            data: Raw report data from services
            
        Returns:
            Aggregated report data with summaries and insights
        """
        experiment = data.experiment
        
        # Calculate experiment duration
        started_at = datetime.fromisoformat(experiment['startedAt'].replace('Z', '+00:00'))
        completed_at = datetime.fromisoformat(experiment['completedAt'].replace('Z', '+00:00'))
        duration = completed_at - started_at
        
        # Aggregate metrics
        metric_summaries = {
            name: self._create_metric_summary(name, metrics)
            for name, metrics in data.aggregated_metrics.items()
        }
        
        # Calculate key insights
        insights = self._generate_insights(data)
        
        return AggregatedReportData(
            experiment=experiment,
            hypothesis=data.hypothesis,
            problem=data.problem,
            duration=duration,
            metric_summaries=metric_summaries,
            insights=insights
        )
    
    def _create_metric_summary(self, name: str, metrics: Dict[str, Any]) -> MetricSummary:
        """
        Create summary for a single metric.
        
        Args:
            name: Metric name
            metrics: Aggregated metric data
            
        Returns:
            MetricSummary object
        """
        return MetricSummary(
            name=name,
            count=metrics.get('count', 0),
            average=metrics.get('average', 0.0),
            min_value=metrics.get('min', 0.0),
            max_value=metrics.get('max', 0.0),
            trend=self._calculate_trend(metrics)
        )
    
    def _generate_insights(self, data: ReportData) -> List[str]:
        """
        Generate insights from metrics data.
        
        Args:
            data: Report data
            
        Returns:
            List of insight strings
        """
        insights = []
        
        # Add insights based on aggregated metrics
        for name, metrics in data.aggregated_metrics.items():
            avg = metrics.get('average', 0)
            if avg > 0:
                insights.append(
                    f"Average {name}: {avg:.2f} "
                    f"(min: {metrics.get('min', 0):.2f}, "
                    f"max: {metrics.get('max', 0):.2f})"
                )
        
        # Add experiment-specific insights
        if data.experiment.get('result'):
            insights.append(f"Experiment result: {data.experiment['result']}")
        
        return insights
    
    def _calculate_trend(self, metrics: Dict[str, Any]) -> str:
        """
        Calculate trend for metric.
        
        Args:
            metrics: Metric data with average, min, max
            
        Returns:
            Trend string: INCREASING, DECREASING, or STABLE
        """
        avg = metrics.get('average', 0)
        max_val = metrics.get('max', 0)
        min_val = metrics.get('min', 0)
        
        if avg == 0:
            return 'STABLE'
        
        # Simple trend calculation
        if max_val > avg * 1.5:
            return 'INCREASING'
        elif min_val < avg * 0.5:
            return 'DECREASING'
        
        return 'STABLE'
```

### Aggregated Data Models

```python
from dataclasses import dataclass
from typing import Dict, List, Any
from datetime import timedelta

@dataclass
class MetricSummary:
    """Summary statistics for a metric"""
    name: str
    count: int
    average: float
    min_value: float
    max_value: float
    trend: str

@dataclass
class AggregatedReportData:
    """Aggregated data for report generation"""
    experiment: Dict[str, Any]
    hypothesis: Dict[str, Any]
    problem: Dict[str, Any]
    duration: timedelta
    metric_summaries: Dict[str, MetricSummary]
    insights: List[str]
    
    def get_duration_string(self) -> str:
        """Format duration as human-readable string"""
        total_seconds = int(self.duration.total_seconds())
        hours = total_seconds // 3600
        minutes = (total_seconds % 3600) // 60
        
        if hours > 0:
            return f"{hours} hours, {minutes} minutes"
        else:
            return f"{minutes} minutes"
```

## Acceptance Criteria

- [x] Data aggregation works correctly
- [x] Metrics summarized properly
- [x] Insights generated
- [x] Trends calculated (INCREASING, DECREASING, STABLE)
- [x] Duration formatted correctly
- [x] Unit tests pass

## Testing Requirements

**Unit Tests**:
- Test data aggregation
- Test metric summaries
- Test insight generation
- Test trend calculation
- Test duration formatting

**Test Files to Create**:
- `tests/test_aggregation.py`

## References

- Specification: `specs/reporting-service.md` (Aggregation section)
- Related Tasks: 005-create-report-templates
