"""
Data aggregation service for report generation.

This module provides services for aggregating, analyzing, and generating
insights from raw report data, following DDD principles.
"""

import logging
from typing import Dict, Any, List
from datetime import datetime, timedelta
from models.report_data import ReportData
from models.aggregated_data import AggregatedReportData, MetricSummary

logger = logging.getLogger(__name__)


class DataAggregationService:
    """
    Application service for aggregating and analyzing report data.
    
    This service transforms raw data into analyzed, aggregated data
    ready for report generation. It follows the Application Service
    pattern from DDD, coordinating domain logic without containing it.
    
    Responsibilities:
    - Calculate experiment duration
    - Create metric summaries from aggregated data
    - Generate insights based on metrics and experiment results
    - Calculate trends for metrics
    - Format data for template rendering
    
    The service follows the Single Responsibility Principle (SOLID),
    focusing solely on data aggregation and analysis.
    """
    
    def __init__(self):
        """Initialize the data aggregation service."""
        logger.info("DataAggregationService initialized")
    
    def aggregate_data(self, data: ReportData) -> AggregatedReportData:
        """
        Aggregate and analyze report data.
        
        This method transforms raw ReportData into AggregatedReportData
        by performing statistical analysis, trend detection, and insight
        generation.
        
        Args:
            data: Raw report data from services
            
        Returns:
            AggregatedReportData with analyzed metrics and insights
            
        Raises:
            ValueError: If required fields are missing from data
            
        Example:
            >>> service = DataAggregationService()
            >>> raw_data = ReportData(...)
            >>> aggregated = service.aggregate_data(raw_data)
            >>> aggregated.get_duration_string()
            '2 hours, 30 minutes'
        """
        logger.info(f"Aggregating data for experiment {data.experiment_id}")
        
        experiment = data.experiment
        
        # Calculate experiment duration
        duration = self._calculate_duration(experiment)
        logger.debug(f"Experiment duration: {duration}")
        
        # Create metric summaries from aggregated metrics
        metric_summaries = {
            name: self._create_metric_summary(name, metrics)
            for name, metrics in data.aggregated_metrics.items()
        }
        logger.debug(f"Created {len(metric_summaries)} metric summaries")
        
        # Generate insights from data
        insights = self._generate_insights(data)
        logger.debug(f"Generated {len(insights)} insights")
        
        # Create and return aggregated data
        aggregated_data = AggregatedReportData(
            experiment=experiment,
            hypothesis=data.hypothesis,
            problem=data.problem,
            duration=duration,
            metric_summaries=metric_summaries,
            insights=insights
        )
        
        logger.info(
            f"Successfully aggregated data for experiment {data.experiment_id}: "
            f"{aggregated_data.metrics_count} metrics, "
            f"{aggregated_data.insights_count} insights"
        )
        
        return aggregated_data
    
    def _calculate_duration(self, experiment: Dict[str, Any]) -> timedelta:
        """
        Calculate experiment duration from timestamps.
        
        Args:
            experiment: Experiment data with startedAt and completedAt
            
        Returns:
            Duration as timedelta
            
        Raises:
            ValueError: If timestamps are missing or invalid
        """
        if 'startedAt' not in experiment:
            raise ValueError("experiment missing required field 'startedAt'")
        if 'completedAt' not in experiment:
            raise ValueError("experiment missing required field 'completedAt'")
        
        try:
            # Parse ISO-8601 timestamps, handling 'Z' timezone
            started_at = datetime.fromisoformat(
                experiment['startedAt'].replace('Z', '+00:00')
            )
            completed_at = datetime.fromisoformat(
                experiment['completedAt'].replace('Z', '+00:00')
            )
            
            duration = completed_at - started_at
            
            if duration.total_seconds() < 0:
                raise ValueError(
                    f"completedAt ({completed_at}) is before startedAt ({started_at})"
                )
            
            return duration
            
        except (ValueError, AttributeError) as e:
            raise ValueError(f"Invalid timestamp format: {str(e)}")
    
    def _create_metric_summary(self, name: str, metrics: Dict[str, Any]) -> MetricSummary:
        """
        Create summary statistics for a single metric.
        
        Args:
            name: Metric name
            metrics: Aggregated metric data with count, average, min, max
            
        Returns:
            MetricSummary object with statistics and trend
            
        Example:
            >>> service = DataAggregationService()
            >>> metrics = {
            ...     'count': 100,
            ...     'average': 125.5,
            ...     'min': 50.0,
            ...     'max': 200.0
            ... }
            >>> summary = service._create_metric_summary('latency', metrics)
            >>> summary.trend
            'STABLE'
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
        Generate insights from metrics and experiment data.
        
        Insights are human-readable observations about the experiment
        results, including metric statistics and experiment outcomes.
        
        Args:
            data: Report data with metrics and experiment info
            
        Returns:
            List of insight strings
            
        Example:
            >>> service = DataAggregationService()
            >>> data = ReportData(...)
            >>> insights = service._generate_insights(data)
            >>> insights[0]
            'Average latency: 125.50 (min: 50.00, max: 200.00)'
        """
        insights = []
        
        # Generate insights from aggregated metrics
        for name, metrics in data.aggregated_metrics.items():
            avg = metrics.get('average', 0)
            if avg > 0:
                insight = (
                    f"Average {name}: {avg:.2f} "
                    f"(min: {metrics.get('min', 0):.2f}, "
                    f"max: {metrics.get('max', 0):.2f})"
                )
                insights.append(insight)
        
        # Add experiment result insight if available
        if data.experiment.get('result'):
            insights.append(f"Experiment result: {data.experiment['result']}")
        
        # Add metrics count insight
        if data.has_metrics():
            insights.append(
                f"Collected {data.metrics_count} metric data points during experiment"
            )
        
        return insights
    
    def _calculate_trend(self, metrics: Dict[str, Any]) -> str:
        """
        Calculate trend classification for a metric.
        
        Trend is determined by comparing max and min values to the average:
        - INCREASING: max > 1.5 * average (high variance upward)
        - DECREASING: min < 0.5 * average (high variance downward)
        - STABLE: otherwise (low variance)
        
        Args:
            metrics: Metric data with average, min, max
            
        Returns:
            Trend string: 'INCREASING', 'DECREASING', or 'STABLE'
            
        Example:
            >>> service = DataAggregationService()
            >>> metrics = {'average': 100, 'min': 90, 'max': 110}
            >>> service._calculate_trend(metrics)
            'STABLE'
            >>> metrics = {'average': 100, 'min': 50, 'max': 110}
            >>> service._calculate_trend(metrics)
            'DECREASING'
        """
        avg = metrics.get('average', 0)
        max_val = metrics.get('max', 0)
        min_val = metrics.get('min', 0)
        
        # Handle edge case: no data
        if avg == 0:
            return 'STABLE'
        
        # Check for increasing trend (high max relative to average)
        if max_val > avg * 1.5:
            return 'INCREASING'
        
        # Check for decreasing trend (low min relative to average)
        if min_val < avg * 0.5:
            return 'DECREASING'
        
        # Default to stable
        return 'STABLE'
