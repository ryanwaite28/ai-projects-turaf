"""
Unit tests for DataAggregationService.

Tests cover aggregation logic, trend calculation, and insight generation.
"""

import pytest
from datetime import timedelta
from src.services.aggregation import DataAggregationService
from src.models.report_data import ReportData
from src.models.aggregated_data import AggregatedReportData, MetricSummary


class TestDataAggregationService:
    """Test suite for DataAggregationService."""
    
    @pytest.fixture
    def service(self):
        """Create service instance for tests."""
        return DataAggregationService()
    
    @pytest.fixture
    def sample_report_data(self):
        """Create sample report data for testing."""
        return ReportData(
            experiment={
                'id': 'exp-123',
                'startedAt': '2024-01-01T00:00:00Z',
                'completedAt': '2024-01-01T02:30:00Z',
                'result': 'SUCCESS'
            },
            hypothesis={
                'id': 'hyp-456',
                'statement': 'Test hypothesis'
            },
            problem={
                'id': 'prob-789',
                'title': 'Performance issue'
            },
            metrics=[
                {'id': 'metric-1', 'name': 'latency', 'value': 100},
                {'id': 'metric-2', 'name': 'latency', 'value': 150}
            ],
            aggregated_metrics={
                'latency': {
                    'count': 100,
                    'average': 125.5,
                    'min': 50.0,
                    'max': 200.0
                },
                'throughput': {
                    'count': 100,
                    'average': 1000.0,
                    'min': 800.0,
                    'max': 1200.0
                }
            }
        )
    
    def test_service_initialization(self, service):
        """Test that service initializes correctly."""
        assert service is not None
    
    def test_aggregate_data_success(self, service, sample_report_data):
        """Test successful data aggregation."""
        # When
        result = service.aggregate_data(sample_report_data)
        
        # Then
        assert isinstance(result, AggregatedReportData)
        assert result.experiment == sample_report_data.experiment
        assert result.hypothesis == sample_report_data.hypothesis
        assert result.problem == sample_report_data.problem
        assert isinstance(result.duration, timedelta)
        assert len(result.metric_summaries) == 2
        assert len(result.insights) > 0
    
    def test_aggregate_data_calculates_duration(self, service, sample_report_data):
        """Test that duration is calculated correctly."""
        # When
        result = service.aggregate_data(sample_report_data)
        
        # Then
        # 2 hours 30 minutes = 9000 seconds
        assert result.duration.total_seconds() == 9000
        assert result.get_duration_string() == '2 hours, 30 minutes'
    
    def test_aggregate_data_creates_metric_summaries(self, service, sample_report_data):
        """Test that metric summaries are created correctly."""
        # When
        result = service.aggregate_data(sample_report_data)
        
        # Then
        assert 'latency' in result.metric_summaries
        assert 'throughput' in result.metric_summaries
        
        latency_summary = result.metric_summaries['latency']
        assert isinstance(latency_summary, MetricSummary)
        assert latency_summary.name == 'latency'
        assert latency_summary.count == 100
        assert latency_summary.average == 125.5
        assert latency_summary.min_value == 50.0
        assert latency_summary.max_value == 200.0
    
    def test_aggregate_data_generates_insights(self, service, sample_report_data):
        """Test that insights are generated."""
        # When
        result = service.aggregate_data(sample_report_data)
        
        # Then
        assert len(result.insights) > 0
        # Should have insights for each metric plus experiment result
        assert any('latency' in insight for insight in result.insights)
        assert any('throughput' in insight for insight in result.insights)
        assert any('SUCCESS' in insight for insight in result.insights)
    
    def test_calculate_trend_stable(self, service):
        """Test trend calculation for stable metrics."""
        # Given
        metrics = {
            'average': 100.0,
            'min': 90.0,
            'max': 110.0
        }
        
        # When
        trend = service._calculate_trend(metrics)
        
        # Then
        assert trend == 'STABLE'
    
    def test_calculate_trend_increasing(self, service):
        """Test trend calculation for increasing metrics."""
        # Given
        metrics = {
            'average': 100.0,
            'min': 90.0,
            'max': 200.0  # max > 1.5 * average
        }
        
        # When
        trend = service._calculate_trend(metrics)
        
        # Then
        assert trend == 'INCREASING'
    
    def test_calculate_trend_decreasing(self, service):
        """Test trend calculation for decreasing metrics."""
        # Given
        metrics = {
            'average': 100.0,
            'min': 40.0,  # min < 0.5 * average
            'max': 110.0
        }
        
        # When
        trend = service._calculate_trend(metrics)
        
        # Then
        assert trend == 'DECREASING'
    
    def test_calculate_trend_with_zero_average(self, service):
        """Test trend calculation when average is zero."""
        # Given
        metrics = {
            'average': 0.0,
            'min': 0.0,
            'max': 0.0
        }
        
        # When
        trend = service._calculate_trend(metrics)
        
        # Then
        assert trend == 'STABLE'
    
    def test_create_metric_summary(self, service):
        """Test creating metric summary from aggregated data."""
        # Given
        name = 'response_time'
        metrics = {
            'count': 50,
            'average': 75.5,
            'min': 25.0,
            'max': 125.0
        }
        
        # When
        summary = service._create_metric_summary(name, metrics)
        
        # Then
        assert summary.name == 'response_time'
        assert summary.count == 50
        assert summary.average == 75.5
        assert summary.min_value == 25.0
        assert summary.max_value == 125.0
        assert summary.trend in ('INCREASING', 'DECREASING', 'STABLE')
    
    def test_generate_insights_includes_metric_stats(self, service, sample_report_data):
        """Test that insights include metric statistics."""
        # When
        insights = service._generate_insights(sample_report_data)
        
        # Then
        latency_insight = next(
            (i for i in insights if 'latency' in i.lower()),
            None
        )
        assert latency_insight is not None
        assert '125.50' in latency_insight
        assert '50.00' in latency_insight
        assert '200.00' in latency_insight
    
    def test_generate_insights_includes_experiment_result(self, service, sample_report_data):
        """Test that insights include experiment result."""
        # When
        insights = service._generate_insights(sample_report_data)
        
        # Then
        result_insight = next(
            (i for i in insights if 'result' in i.lower()),
            None
        )
        assert result_insight is not None
        assert 'SUCCESS' in result_insight
    
    def test_generate_insights_includes_metrics_count(self, service, sample_report_data):
        """Test that insights include metrics count."""
        # When
        insights = service._generate_insights(sample_report_data)
        
        # Then
        count_insight = next(
            (i for i in insights if 'metric data points' in i),
            None
        )
        assert count_insight is not None
        assert '2' in count_insight  # 2 metrics in sample data
    
    def test_generate_insights_skips_zero_averages(self, service):
        """Test that insights skip metrics with zero average."""
        # Given
        data = ReportData(
            experiment={'id': 'exp-123'},
            hypothesis={'id': 'hyp-456'},
            problem={'id': 'prob-789'},
            metrics=[],
            aggregated_metrics={
                'metric1': {'average': 0.0, 'min': 0.0, 'max': 0.0},
                'metric2': {'average': 100.0, 'min': 50.0, 'max': 150.0}
            }
        )
        
        # When
        insights = service._generate_insights(data)
        
        # Then
        # Should only have insight for metric2, not metric1
        metric_insights = [i for i in insights if 'Average' in i]
        assert len(metric_insights) == 1
        assert 'metric2' in metric_insights[0]
    
    def test_calculate_duration_raises_error_for_missing_started_at(self, service):
        """Test that duration calculation raises error when startedAt is missing."""
        # Given
        experiment = {
            'id': 'exp-123',
            'completedAt': '2024-01-01T12:00:00Z'
        }
        
        # When/Then
        with pytest.raises(ValueError) as exc_info:
            service._calculate_duration(experiment)
        assert 'startedAt' in str(exc_info.value)
    
    def test_calculate_duration_raises_error_for_missing_completed_at(self, service):
        """Test that duration calculation raises error when completedAt is missing."""
        # Given
        experiment = {
            'id': 'exp-123',
            'startedAt': '2024-01-01T00:00:00Z'
        }
        
        # When/Then
        with pytest.raises(ValueError) as exc_info:
            service._calculate_duration(experiment)
        assert 'completedAt' in str(exc_info.value)
    
    def test_calculate_duration_raises_error_for_invalid_timestamp(self, service):
        """Test that duration calculation raises error for invalid timestamp."""
        # Given
        experiment = {
            'id': 'exp-123',
            'startedAt': 'invalid-timestamp',
            'completedAt': '2024-01-01T12:00:00Z'
        }
        
        # When/Then
        with pytest.raises(ValueError) as exc_info:
            service._calculate_duration(experiment)
        assert 'Invalid timestamp format' in str(exc_info.value)
    
    def test_calculate_duration_raises_error_for_negative_duration(self, service):
        """Test that duration calculation raises error when completedAt is before startedAt."""
        # Given
        experiment = {
            'id': 'exp-123',
            'startedAt': '2024-01-01T12:00:00Z',
            'completedAt': '2024-01-01T00:00:00Z'  # Before startedAt
        }
        
        # When/Then
        with pytest.raises(ValueError) as exc_info:
            service._calculate_duration(experiment)
        assert 'before' in str(exc_info.value)
    
    def test_aggregate_data_logs_activity(self, service, sample_report_data, caplog):
        """Test that service logs aggregation activity."""
        # When
        with caplog.at_level('INFO'):
            service.aggregate_data(sample_report_data)
        
        # Then
        assert 'Aggregating data for experiment exp-123' in caplog.text
        assert 'Successfully aggregated data' in caplog.text
