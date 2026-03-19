"""
Domain models for aggregated report data.

This module defines value objects for aggregated and analyzed data,
following DDD principles.
"""

from dataclasses import dataclass
from typing import Dict, List, Any
from datetime import timedelta


@dataclass
class MetricSummary:
    """
    Value object containing statistical summary for a metric.
    
    This immutable object represents aggregated statistics for a single
    metric, including central tendency, spread, and trend analysis.
    
    Attributes:
        name: Metric name (e.g., 'response_time', 'error_rate')
        count: Number of data points collected
        average: Mean value across all data points
        min_value: Minimum observed value
        max_value: Maximum observed value
        trend: Trend classification (INCREASING, DECREASING, STABLE)
        
    Example:
        >>> summary = MetricSummary(
        ...     name='latency',
        ...     count=100,
        ...     average=125.5,
        ...     min_value=50.0,
        ...     max_value=200.0,
        ...     trend='STABLE'
        ... )
        >>> summary.name
        'latency'
    """
    name: str
    count: int
    average: float
    min_value: float
    max_value: float
    trend: str
    
    def __post_init__(self):
        """Validate metric summary after initialization."""
        if not self.name:
            raise ValueError("name is required")
        if self.count < 0:
            raise ValueError("count must be non-negative")
        if self.trend not in ('INCREASING', 'DECREASING', 'STABLE'):
            raise ValueError(
                f"trend must be INCREASING, DECREASING, or STABLE, got: {self.trend}"
            )
    
    @property
    def range(self) -> float:
        """Calculate the range (max - min) of the metric."""
        return self.max_value - self.min_value
    
    @property
    def is_stable(self) -> bool:
        """Check if the metric trend is stable."""
        return self.trend == 'STABLE'
    
    @property
    def is_increasing(self) -> bool:
        """Check if the metric trend is increasing."""
        return self.trend == 'INCREASING'
    
    @property
    def is_decreasing(self) -> bool:
        """Check if the metric trend is decreasing."""
        return self.trend == 'DECREASING'


@dataclass
class AggregatedReportData:
    """
    Value object containing all aggregated data for report generation.
    
    This immutable object represents the final aggregated dataset ready
    for template rendering, including analyzed metrics, insights, and
    formatted duration.
    
    Attributes:
        experiment: Experiment details
        hypothesis: Hypothesis information
        problem: Problem description
        duration: Experiment duration as timedelta
        metric_summaries: Dictionary mapping metric names to summaries
        insights: List of generated insights
        
    Example:
        >>> from datetime import timedelta
        >>> data = AggregatedReportData(
        ...     experiment={'id': 'exp-123'},
        ...     hypothesis={'id': 'hyp-456'},
        ...     problem={'id': 'prob-789'},
        ...     duration=timedelta(hours=2, minutes=30),
        ...     metric_summaries={'latency': MetricSummary(...)},
        ...     insights=['Average latency: 125.5ms']
        ... )
        >>> data.get_duration_string()
        '2 hours, 30 minutes'
    """
    experiment: Dict[str, Any]
    hypothesis: Dict[str, Any]
    problem: Dict[str, Any]
    duration: timedelta
    metric_summaries: Dict[str, MetricSummary]
    insights: List[str]
    
    def __post_init__(self):
        """Validate aggregated data after initialization."""
        if not self.experiment:
            raise ValueError("experiment is required")
        if not self.hypothesis:
            raise ValueError("hypothesis is required")
        if not self.problem:
            raise ValueError("problem is required")
        if self.duration is None:
            raise ValueError("duration is required")
        if self.metric_summaries is None:
            raise ValueError("metric_summaries is required (can be empty dict)")
        if self.insights is None:
            raise ValueError("insights is required (can be empty list)")
    
    def get_duration_string(self) -> str:
        """
        Format duration as human-readable string.
        
        Returns:
            Formatted duration string (e.g., "2 hours, 30 minutes")
            
        Example:
            >>> data = AggregatedReportData(...)
            >>> data.duration = timedelta(hours=1, minutes=45)
            >>> data.get_duration_string()
            '1 hours, 45 minutes'
        """
        total_seconds = int(self.duration.total_seconds())
        hours = total_seconds // 3600
        minutes = (total_seconds % 3600) // 60
        
        if hours > 0:
            return f"{hours} hours, {minutes} minutes"
        else:
            return f"{minutes} minutes"
    
    @property
    def experiment_id(self) -> str:
        """Get experiment ID for convenience."""
        return self.experiment.get('id', '')
    
    @property
    def metrics_count(self) -> int:
        """Get total number of metric types analyzed."""
        return len(self.metric_summaries)
    
    @property
    def insights_count(self) -> int:
        """Get total number of insights generated."""
        return len(self.insights)
    
    def has_metrics(self) -> bool:
        """Check if any metrics were analyzed."""
        return len(self.metric_summaries) > 0
    
    def has_insights(self) -> bool:
        """Check if any insights were generated."""
        return len(self.insights) > 0
    
    def get_metric_summary(self, metric_name: str) -> MetricSummary:
        """
        Get summary for a specific metric.
        
        Args:
            metric_name: Name of the metric
            
        Returns:
            MetricSummary for the requested metric
            
        Raises:
            KeyError: If metric name not found
        """
        if metric_name not in self.metric_summaries:
            raise KeyError(f"Metric '{metric_name}' not found in summaries")
        return self.metric_summaries[metric_name]
