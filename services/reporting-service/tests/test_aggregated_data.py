"""
Unit tests for aggregated data models.

Tests cover model creation, validation, and convenience methods.
"""

import pytest
from datetime import timedelta
from src.models.aggregated_data import MetricSummary, AggregatedReportData


class TestMetricSummary:
    """Test suite for MetricSummary model."""
    
    def test_metric_summary_creation_with_all_fields(self):
        """Test creating MetricSummary with all fields."""
        # Given/When
        summary = MetricSummary(
            name='latency',
            count=100,
            average=125.5,
            min_value=50.0,
            max_value=200.0,
            trend='STABLE'
        )
        
        # Then
        assert summary.name == 'latency'
        assert summary.count == 100
        assert summary.average == 125.5
        assert summary.min_value == 50.0
        assert summary.max_value == 200.0
        assert summary.trend == 'STABLE'
    
    def test_metric_summary_raises_error_for_empty_name(self):
        """Test that MetricSummary raises error when name is empty."""
        # When/Then
        with pytest.raises(ValueError) as exc_info:
            MetricSummary(
                name='',
                count=100,
                average=125.5,
                min_value=50.0,
                max_value=200.0,
                trend='STABLE'
            )
        assert 'name is required' in str(exc_info.value)
    
    def test_metric_summary_raises_error_for_negative_count(self):
        """Test that MetricSummary raises error when count is negative."""
        # When/Then
        with pytest.raises(ValueError) as exc_info:
            MetricSummary(
                name='latency',
                count=-1,
                average=125.5,
                min_value=50.0,
                max_value=200.0,
                trend='STABLE'
            )
        assert 'count must be non-negative' in str(exc_info.value)
    
    def test_metric_summary_raises_error_for_invalid_trend(self):
        """Test that MetricSummary raises error for invalid trend."""
        # When/Then
        with pytest.raises(ValueError) as exc_info:
            MetricSummary(
                name='latency',
                count=100,
                average=125.5,
                min_value=50.0,
                max_value=200.0,
                trend='INVALID'
            )
        assert 'trend must be INCREASING, DECREASING, or STABLE' in str(exc_info.value)
    
    def test_metric_summary_range_property(self):
        """Test range property calculation."""
        # Given
        summary = MetricSummary(
            name='latency',
            count=100,
            average=125.5,
            min_value=50.0,
            max_value=200.0,
            trend='STABLE'
        )
        
        # When/Then
        assert summary.range == 150.0  # 200.0 - 50.0
    
    def test_metric_summary_is_stable_property(self):
        """Test is_stable property."""
        # Given
        summary = MetricSummary(
            name='latency',
            count=100,
            average=125.5,
            min_value=50.0,
            max_value=200.0,
            trend='STABLE'
        )
        
        # When/Then
        assert summary.is_stable is True
        assert summary.is_increasing is False
        assert summary.is_decreasing is False
    
    def test_metric_summary_is_increasing_property(self):
        """Test is_increasing property."""
        # Given
        summary = MetricSummary(
            name='latency',
            count=100,
            average=125.5,
            min_value=50.0,
            max_value=200.0,
            trend='INCREASING'
        )
        
        # When/Then
        assert summary.is_stable is False
        assert summary.is_increasing is True
        assert summary.is_decreasing is False
    
    def test_metric_summary_is_decreasing_property(self):
        """Test is_decreasing property."""
        # Given
        summary = MetricSummary(
            name='latency',
            count=100,
            average=125.5,
            min_value=50.0,
            max_value=200.0,
            trend='DECREASING'
        )
        
        # When/Then
        assert summary.is_stable is False
        assert summary.is_increasing is False
        assert summary.is_decreasing is True


class TestAggregatedReportData:
    """Test suite for AggregatedReportData model."""
    
    @pytest.fixture
    def sample_metric_summary(self):
        """Create sample metric summary."""
        return MetricSummary(
            name='latency',
            count=100,
            average=125.5,
            min_value=50.0,
            max_value=200.0,
            trend='STABLE'
        )
    
    def test_aggregated_report_data_creation(self, sample_metric_summary):
        """Test creating AggregatedReportData with all fields."""
        # Given/When
        data = AggregatedReportData(
            experiment={'id': 'exp-123'},
            hypothesis={'id': 'hyp-456'},
            problem={'id': 'prob-789'},
            duration=timedelta(hours=2, minutes=30),
            metric_summaries={'latency': sample_metric_summary},
            insights=['Insight 1', 'Insight 2']
        )
        
        # Then
        assert data.experiment['id'] == 'exp-123'
        assert data.hypothesis['id'] == 'hyp-456'
        assert data.problem['id'] == 'prob-789'
        assert data.duration == timedelta(hours=2, minutes=30)
        assert len(data.metric_summaries) == 1
        assert len(data.insights) == 2
    
    def test_aggregated_report_data_raises_error_for_missing_experiment(self):
        """Test that AggregatedReportData raises error when experiment is missing."""
        # When/Then
        with pytest.raises(ValueError) as exc_info:
            AggregatedReportData(
                experiment=None,
                hypothesis={'id': 'hyp-456'},
                problem={'id': 'prob-789'},
                duration=timedelta(hours=1),
                metric_summaries={},
                insights=[]
            )
        assert 'experiment is required' in str(exc_info.value)
    
    def test_aggregated_report_data_raises_error_for_missing_hypothesis(self):
        """Test that AggregatedReportData raises error when hypothesis is missing."""
        # When/Then
        with pytest.raises(ValueError) as exc_info:
            AggregatedReportData(
                experiment={'id': 'exp-123'},
                hypothesis=None,
                problem={'id': 'prob-789'},
                duration=timedelta(hours=1),
                metric_summaries={},
                insights=[]
            )
        assert 'hypothesis is required' in str(exc_info.value)
    
    def test_aggregated_report_data_raises_error_for_missing_problem(self):
        """Test that AggregatedReportData raises error when problem is missing."""
        # When/Then
        with pytest.raises(ValueError) as exc_info:
            AggregatedReportData(
                experiment={'id': 'exp-123'},
                hypothesis={'id': 'hyp-456'},
                problem=None,
                duration=timedelta(hours=1),
                metric_summaries={},
                insights=[]
            )
        assert 'problem is required' in str(exc_info.value)
    
    def test_aggregated_report_data_raises_error_for_missing_duration(self):
        """Test that AggregatedReportData raises error when duration is missing."""
        # When/Then
        with pytest.raises(ValueError) as exc_info:
            AggregatedReportData(
                experiment={'id': 'exp-123'},
                hypothesis={'id': 'hyp-456'},
                problem={'id': 'prob-789'},
                duration=None,
                metric_summaries={},
                insights=[]
            )
        assert 'duration is required' in str(exc_info.value)
    
    def test_get_duration_string_with_hours_and_minutes(self):
        """Test duration string formatting with hours and minutes."""
        # Given
        data = AggregatedReportData(
            experiment={'id': 'exp-123'},
            hypothesis={'id': 'hyp-456'},
            problem={'id': 'prob-789'},
            duration=timedelta(hours=2, minutes=30),
            metric_summaries={},
            insights=[]
        )
        
        # When/Then
        assert data.get_duration_string() == '2 hours, 30 minutes'
    
    def test_get_duration_string_with_only_minutes(self):
        """Test duration string formatting with only minutes."""
        # Given
        data = AggregatedReportData(
            experiment={'id': 'exp-123'},
            hypothesis={'id': 'hyp-456'},
            problem={'id': 'prob-789'},
            duration=timedelta(minutes=45),
            metric_summaries={},
            insights=[]
        )
        
        # When/Then
        assert data.get_duration_string() == '45 minutes'
    
    def test_experiment_id_property(self):
        """Test experiment_id convenience property."""
        # Given
        data = AggregatedReportData(
            experiment={'id': 'exp-special-123'},
            hypothesis={'id': 'hyp-456'},
            problem={'id': 'prob-789'},
            duration=timedelta(hours=1),
            metric_summaries={},
            insights=[]
        )
        
        # When/Then
        assert data.experiment_id == 'exp-special-123'
    
    def test_metrics_count_property(self, sample_metric_summary):
        """Test metrics_count property."""
        # Given
        data = AggregatedReportData(
            experiment={'id': 'exp-123'},
            hypothesis={'id': 'hyp-456'},
            problem={'id': 'prob-789'},
            duration=timedelta(hours=1),
            metric_summaries={
                'latency': sample_metric_summary,
                'throughput': sample_metric_summary
            },
            insights=[]
        )
        
        # When/Then
        assert data.metrics_count == 2
    
    def test_insights_count_property(self):
        """Test insights_count property."""
        # Given
        data = AggregatedReportData(
            experiment={'id': 'exp-123'},
            hypothesis={'id': 'hyp-456'},
            problem={'id': 'prob-789'},
            duration=timedelta(hours=1),
            metric_summaries={},
            insights=['Insight 1', 'Insight 2', 'Insight 3']
        )
        
        # When/Then
        assert data.insights_count == 3
    
    def test_has_metrics_returns_true_when_metrics_exist(self, sample_metric_summary):
        """Test has_metrics returns True when metrics are present."""
        # Given
        data = AggregatedReportData(
            experiment={'id': 'exp-123'},
            hypothesis={'id': 'hyp-456'},
            problem={'id': 'prob-789'},
            duration=timedelta(hours=1),
            metric_summaries={'latency': sample_metric_summary},
            insights=[]
        )
        
        # When/Then
        assert data.has_metrics() is True
    
    def test_has_metrics_returns_false_when_no_metrics(self):
        """Test has_metrics returns False when no metrics."""
        # Given
        data = AggregatedReportData(
            experiment={'id': 'exp-123'},
            hypothesis={'id': 'hyp-456'},
            problem={'id': 'prob-789'},
            duration=timedelta(hours=1),
            metric_summaries={},
            insights=[]
        )
        
        # When/Then
        assert data.has_metrics() is False
    
    def test_has_insights_returns_true_when_insights_exist(self):
        """Test has_insights returns True when insights are present."""
        # Given
        data = AggregatedReportData(
            experiment={'id': 'exp-123'},
            hypothesis={'id': 'hyp-456'},
            problem={'id': 'prob-789'},
            duration=timedelta(hours=1),
            metric_summaries={},
            insights=['Insight 1']
        )
        
        # When/Then
        assert data.has_insights() is True
    
    def test_has_insights_returns_false_when_no_insights(self):
        """Test has_insights returns False when no insights."""
        # Given
        data = AggregatedReportData(
            experiment={'id': 'exp-123'},
            hypothesis={'id': 'hyp-456'},
            problem={'id': 'prob-789'},
            duration=timedelta(hours=1),
            metric_summaries={},
            insights=[]
        )
        
        # When/Then
        assert data.has_insights() is False
    
    def test_get_metric_summary(self, sample_metric_summary):
        """Test getting metric summary by name."""
        # Given
        data = AggregatedReportData(
            experiment={'id': 'exp-123'},
            hypothesis={'id': 'hyp-456'},
            problem={'id': 'prob-789'},
            duration=timedelta(hours=1),
            metric_summaries={'latency': sample_metric_summary},
            insights=[]
        )
        
        # When
        summary = data.get_metric_summary('latency')
        
        # Then
        assert summary == sample_metric_summary
    
    def test_get_metric_summary_raises_error_for_missing_metric(self):
        """Test that get_metric_summary raises error for missing metric."""
        # Given
        data = AggregatedReportData(
            experiment={'id': 'exp-123'},
            hypothesis={'id': 'hyp-456'},
            problem={'id': 'prob-789'},
            duration=timedelta(hours=1),
            metric_summaries={},
            insights=[]
        )
        
        # When/Then
        with pytest.raises(KeyError) as exc_info:
            data.get_metric_summary('nonexistent')
        assert 'nonexistent' in str(exc_info.value)
