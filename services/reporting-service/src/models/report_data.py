"""
Domain models for report data.

This module defines value objects and data transfer objects for
report generation, following DDD principles.
"""

from dataclasses import dataclass
from typing import Dict, Any, List


@dataclass
class ReportData:
    """
    Value object containing all data needed for report generation.
    
    This immutable object aggregates data from multiple services,
    providing a complete dataset for report generation. It follows
    the Value Object pattern from DDD.
    
    Attributes:
        experiment: Experiment details including configuration and timestamps
        hypothesis: Hypothesis statement and validation criteria
        problem: Problem description and business context
        metrics: List of raw metric data points
        aggregated_metrics: Statistical aggregations of metrics
        
    Example:
        >>> data = ReportData(
        ...     experiment={'id': 'exp-123', 'status': 'COMPLETED'},
        ...     hypothesis={'id': 'hyp-456', 'statement': 'Test hypothesis'},
        ...     problem={'id': 'prob-789', 'title': 'Performance issue'},
        ...     metrics=[{'name': 'latency', 'value': 100}],
        ...     aggregated_metrics={'latency': {'average': 95.5}}
        ... )
        >>> data.experiment['id']
        'exp-123'
    """
    experiment: Dict[str, Any]
    hypothesis: Dict[str, Any]
    problem: Dict[str, Any]
    metrics: List[Dict[str, Any]]
    aggregated_metrics: Dict[str, Any]
    
    def __post_init__(self):
        """Validate required fields after initialization."""
        if not self.experiment:
            raise ValueError("experiment is required")
        if not self.hypothesis:
            raise ValueError("hypothesis is required")
        if not self.problem:
            raise ValueError("problem is required")
        if self.metrics is None:
            raise ValueError("metrics is required (can be empty list)")
        if not self.aggregated_metrics:
            raise ValueError("aggregated_metrics is required")
    
    @property
    def experiment_id(self) -> str:
        """Get experiment ID for convenience."""
        return self.experiment.get('id', '')
    
    @property
    def organization_id(self) -> str:
        """Get organization ID from experiment data."""
        return self.experiment.get('organizationId', '')
    
    @property
    def metrics_count(self) -> int:
        """Get total number of metrics collected."""
        return len(self.metrics)
    
    def has_metrics(self) -> bool:
        """Check if any metrics were collected."""
        return len(self.metrics) > 0
