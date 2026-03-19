"""
Unit tests for ReportData model.

Tests cover model creation, validation, and convenience methods.
"""

import pytest
from src.models.report_data import ReportData


class TestReportData:
    """Test suite for ReportData model."""
    
    def test_report_data_creation_with_all_fields(self):
        """Test creating ReportData with all required fields."""
        # Given/When
        data = ReportData(
            experiment={'id': 'exp-123', 'status': 'COMPLETED'},
            hypothesis={'id': 'hyp-456', 'statement': 'Test'},
            problem={'id': 'prob-789', 'title': 'Issue'},
            metrics=[{'id': 'metric-1', 'value': 100}],
            aggregated_metrics={'latency': {'average': 95.5}}
        )
        
        # Then
        assert data.experiment['id'] == 'exp-123'
        assert data.hypothesis['id'] == 'hyp-456'
        assert data.problem['id'] == 'prob-789'
        assert len(data.metrics) == 1
        assert data.aggregated_metrics['latency']['average'] == 95.5
    
    def test_report_data_raises_error_for_missing_experiment(self):
        """Test that ReportData raises error when experiment is missing."""
        # When/Then
        with pytest.raises(ValueError) as exc_info:
            ReportData(
                experiment=None,
                hypothesis={'id': 'hyp-456'},
                problem={'id': 'prob-789'},
                metrics=[],
                aggregated_metrics={}
            )
        assert 'experiment is required' in str(exc_info.value)
    
    def test_report_data_raises_error_for_empty_experiment(self):
        """Test that ReportData raises error when experiment is empty."""
        # When/Then
        with pytest.raises(ValueError) as exc_info:
            ReportData(
                experiment={},
                hypothesis={'id': 'hyp-456'},
                problem={'id': 'prob-789'},
                metrics=[],
                aggregated_metrics={}
            )
        assert 'experiment is required' in str(exc_info.value)
    
    def test_report_data_raises_error_for_missing_hypothesis(self):
        """Test that ReportData raises error when hypothesis is missing."""
        # When/Then
        with pytest.raises(ValueError) as exc_info:
            ReportData(
                experiment={'id': 'exp-123'},
                hypothesis=None,
                problem={'id': 'prob-789'},
                metrics=[],
                aggregated_metrics={}
            )
        assert 'hypothesis is required' in str(exc_info.value)
    
    def test_report_data_raises_error_for_missing_problem(self):
        """Test that ReportData raises error when problem is missing."""
        # When/Then
        with pytest.raises(ValueError) as exc_info:
            ReportData(
                experiment={'id': 'exp-123'},
                hypothesis={'id': 'hyp-456'},
                problem=None,
                metrics=[],
                aggregated_metrics={}
            )
        assert 'problem is required' in str(exc_info.value)
    
    def test_report_data_raises_error_for_missing_metrics(self):
        """Test that ReportData raises error when metrics is None."""
        # When/Then
        with pytest.raises(ValueError) as exc_info:
            ReportData(
                experiment={'id': 'exp-123'},
                hypothesis={'id': 'hyp-456'},
                problem={'id': 'prob-789'},
                metrics=None,
                aggregated_metrics={}
            )
        assert 'metrics is required' in str(exc_info.value)
    
    def test_report_data_allows_empty_metrics_list(self):
        """Test that ReportData allows empty metrics list."""
        # When
        data = ReportData(
            experiment={'id': 'exp-123'},
            hypothesis={'id': 'hyp-456'},
            problem={'id': 'prob-789'},
            metrics=[],  # Empty list is valid
            aggregated_metrics={}
        )
        
        # Then
        assert data.metrics == []
        assert data.metrics_count == 0
    
    def test_report_data_raises_error_for_missing_aggregated_metrics(self):
        """Test that ReportData raises error when aggregated_metrics is missing."""
        # When/Then
        with pytest.raises(ValueError) as exc_info:
            ReportData(
                experiment={'id': 'exp-123'},
                hypothesis={'id': 'hyp-456'},
                problem={'id': 'prob-789'},
                metrics=[],
                aggregated_metrics=None
            )
        assert 'aggregated_metrics is required' in str(exc_info.value)
    
    def test_experiment_id_property(self):
        """Test experiment_id convenience property."""
        # Given
        data = ReportData(
            experiment={'id': 'exp-special-123'},
            hypothesis={'id': 'hyp-456'},
            problem={'id': 'prob-789'},
            metrics=[],
            aggregated_metrics={}
        )
        
        # When/Then
        assert data.experiment_id == 'exp-special-123'
    
    def test_experiment_id_property_returns_empty_string_if_missing(self):
        """Test experiment_id returns empty string when id is missing."""
        # Given
        data = ReportData(
            experiment={'status': 'COMPLETED'},  # No id field
            hypothesis={'id': 'hyp-456'},
            problem={'id': 'prob-789'},
            metrics=[],
            aggregated_metrics={}
        )
        
        # When/Then
        assert data.experiment_id == ''
    
    def test_organization_id_property(self):
        """Test organization_id convenience property."""
        # Given
        data = ReportData(
            experiment={'id': 'exp-123', 'organizationId': 'org-special-456'},
            hypothesis={'id': 'hyp-456'},
            problem={'id': 'prob-789'},
            metrics=[],
            aggregated_metrics={}
        )
        
        # When/Then
        assert data.organization_id == 'org-special-456'
    
    def test_metrics_count_property(self):
        """Test metrics_count property."""
        # Given
        data = ReportData(
            experiment={'id': 'exp-123'},
            hypothesis={'id': 'hyp-456'},
            problem={'id': 'prob-789'},
            metrics=[{'id': '1'}, {'id': '2'}, {'id': '3'}],
            aggregated_metrics={}
        )
        
        # When/Then
        assert data.metrics_count == 3
    
    def test_has_metrics_returns_true_when_metrics_exist(self):
        """Test has_metrics returns True when metrics are present."""
        # Given
        data = ReportData(
            experiment={'id': 'exp-123'},
            hypothesis={'id': 'hyp-456'},
            problem={'id': 'prob-789'},
            metrics=[{'id': '1'}],
            aggregated_metrics={}
        )
        
        # When/Then
        assert data.has_metrics() is True
    
    def test_has_metrics_returns_false_when_no_metrics(self):
        """Test has_metrics returns False when metrics list is empty."""
        # Given
        data = ReportData(
            experiment={'id': 'exp-123'},
            hypothesis={'id': 'hyp-456'},
            problem={'id': 'prob-789'},
            metrics=[],
            aggregated_metrics={}
        )
        
        # When/Then
        assert data.has_metrics() is False
