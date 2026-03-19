"""
Unit tests for DataFetchingService.

Tests cover data orchestration, error handling, and validation.
"""

import pytest
from unittest.mock import Mock, MagicMock
from requests.exceptions import HTTPError, Timeout
from src.services.data_fetching import DataFetchingService
from src.models.report_data import ReportData


class TestDataFetchingService:
    """Test suite for DataFetchingService."""
    
    @pytest.fixture
    def mock_experiment_client(self):
        """Create mock experiment client."""
        return Mock()
    
    @pytest.fixture
    def mock_metrics_client(self):
        """Create mock metrics client."""
        return Mock()
    
    @pytest.fixture
    def service(self, mock_experiment_client, mock_metrics_client):
        """Create service with mocked clients."""
        return DataFetchingService(
            experiment_client=mock_experiment_client,
            metrics_client=mock_metrics_client
        )
    
    def test_fetch_report_data_success(self, service, mock_experiment_client, mock_metrics_client):
        """Test successful data fetching orchestration."""
        # Given
        experiment_id = 'exp-123'
        organization_id = 'org-456'
        
        experiment_data = {
            'id': experiment_id,
            'hypothesisId': 'hyp-789',
            'startedAt': '2024-01-01T00:00:00Z',
            'completedAt': '2024-01-01T12:00:00Z',
            'status': 'COMPLETED'
        }
        hypothesis_data = {
            'id': 'hyp-789',
            'problemId': 'prob-101',
            'statement': 'Test hypothesis'
        }
        problem_data = {
            'id': 'prob-101',
            'title': 'Performance issue'
        }
        metrics_data = [
            {'id': 'metric-1', 'value': 100},
            {'id': 'metric-2', 'value': 150}
        ]
        aggregated_data = {
            'latency': {'average': 125.0, 'min': 100.0, 'max': 150.0}
        }
        
        mock_experiment_client.get_experiment.return_value = experiment_data
        mock_experiment_client.get_hypothesis.return_value = hypothesis_data
        mock_experiment_client.get_problem.return_value = problem_data
        mock_metrics_client.get_metrics.return_value = metrics_data
        mock_metrics_client.get_aggregated_metrics.return_value = aggregated_data
        
        # When
        result = service.fetch_report_data(experiment_id, organization_id)
        
        # Then
        assert isinstance(result, ReportData)
        assert result.experiment == experiment_data
        assert result.hypothesis == hypothesis_data
        assert result.problem == problem_data
        assert result.metrics == metrics_data
        assert result.aggregated_metrics == aggregated_data
    
    def test_fetch_report_data_calls_clients_in_sequence(
        self,
        service,
        mock_experiment_client,
        mock_metrics_client
    ):
        """Test that service calls clients in correct sequence."""
        # Given
        experiment_id = 'exp-123'
        organization_id = 'org-456'
        
        mock_experiment_client.get_experiment.return_value = {
            'id': experiment_id,
            'hypothesisId': 'hyp-789',
            'startedAt': '2024-01-01T00:00:00Z',
            'completedAt': '2024-01-01T12:00:00Z'
        }
        mock_experiment_client.get_hypothesis.return_value = {
            'id': 'hyp-789',
            'problemId': 'prob-101'
        }
        mock_experiment_client.get_problem.return_value = {
            'id': 'prob-101'
        }
        mock_metrics_client.get_metrics.return_value = []
        mock_metrics_client.get_aggregated_metrics.return_value = {}
        
        # When
        service.fetch_report_data(experiment_id, organization_id)
        
        # Then
        mock_experiment_client.get_experiment.assert_called_once_with(
            experiment_id,
            organization_id
        )
        mock_experiment_client.get_hypothesis.assert_called_once_with(
            'hyp-789',
            organization_id
        )
        mock_experiment_client.get_problem.assert_called_once_with(
            'prob-101',
            organization_id
        )
        mock_metrics_client.get_metrics.assert_called_once_with(
            experiment_id,
            '2024-01-01T00:00:00Z',
            '2024-01-01T12:00:00Z',
            organization_id
        )
        mock_metrics_client.get_aggregated_metrics.assert_called_once_with(
            experiment_id,
            '2024-01-01T00:00:00Z',
            '2024-01-01T12:00:00Z',
            organization_id
        )
    
    def test_fetch_report_data_raises_error_for_missing_hypothesis_id(
        self,
        service,
        mock_experiment_client
    ):
        """Test that service raises error when hypothesisId is missing."""
        # Given
        experiment_id = 'exp-123'
        organization_id = 'org-456'
        
        mock_experiment_client.get_experiment.return_value = {
            'id': experiment_id,
            # Missing hypothesisId
            'startedAt': '2024-01-01T00:00:00Z',
            'completedAt': '2024-01-01T12:00:00Z'
        }
        
        # When/Then
        with pytest.raises(ValueError) as exc_info:
            service.fetch_report_data(experiment_id, organization_id)
        assert 'hypothesisId' in str(exc_info.value)
    
    def test_fetch_report_data_raises_error_for_missing_started_at(
        self,
        service,
        mock_experiment_client
    ):
        """Test that service raises error when startedAt is missing."""
        # Given
        experiment_id = 'exp-123'
        organization_id = 'org-456'
        
        mock_experiment_client.get_experiment.return_value = {
            'id': experiment_id,
            'hypothesisId': 'hyp-789',
            # Missing startedAt
            'completedAt': '2024-01-01T12:00:00Z'
        }
        
        # When/Then
        with pytest.raises(ValueError) as exc_info:
            service.fetch_report_data(experiment_id, organization_id)
        assert 'startedAt' in str(exc_info.value)
    
    def test_fetch_report_data_raises_error_for_missing_completed_at(
        self,
        service,
        mock_experiment_client
    ):
        """Test that service raises error when completedAt is missing."""
        # Given
        experiment_id = 'exp-123'
        organization_id = 'org-456'
        
        mock_experiment_client.get_experiment.return_value = {
            'id': experiment_id,
            'hypothesisId': 'hyp-789',
            'startedAt': '2024-01-01T00:00:00Z'
            # Missing completedAt
        }
        
        # When/Then
        with pytest.raises(ValueError) as exc_info:
            service.fetch_report_data(experiment_id, organization_id)
        assert 'completedAt' in str(exc_info.value)
    
    def test_fetch_report_data_raises_error_for_missing_problem_id(
        self,
        service,
        mock_experiment_client
    ):
        """Test that service raises error when problemId is missing."""
        # Given
        experiment_id = 'exp-123'
        organization_id = 'org-456'
        
        mock_experiment_client.get_experiment.return_value = {
            'id': experiment_id,
            'hypothesisId': 'hyp-789',
            'startedAt': '2024-01-01T00:00:00Z',
            'completedAt': '2024-01-01T12:00:00Z'
        }
        mock_experiment_client.get_hypothesis.return_value = {
            'id': 'hyp-789'
            # Missing problemId
        }
        
        # When/Then
        with pytest.raises(ValueError) as exc_info:
            service.fetch_report_data(experiment_id, organization_id)
        assert 'problemId' in str(exc_info.value)
    
    def test_fetch_report_data_propagates_http_errors(
        self,
        service,
        mock_experiment_client
    ):
        """Test that service propagates HTTP errors from clients."""
        # Given
        experiment_id = 'exp-123'
        organization_id = 'org-456'
        
        mock_experiment_client.get_experiment.side_effect = HTTPError('404 Not Found')
        
        # When/Then
        with pytest.raises(HTTPError):
            service.fetch_report_data(experiment_id, organization_id)
    
    def test_fetch_report_data_propagates_timeout_errors(
        self,
        service,
        mock_experiment_client
    ):
        """Test that service propagates timeout errors from clients."""
        # Given
        experiment_id = 'exp-123'
        organization_id = 'org-456'
        
        mock_experiment_client.get_experiment.side_effect = Timeout('Request timed out')
        
        # When/Then
        with pytest.raises(Timeout):
            service.fetch_report_data(experiment_id, organization_id)
    
    def test_service_initialization_with_default_clients(self):
        """Test service initialization creates default clients."""
        # When
        service = DataFetchingService()
        
        # Then
        assert service.experiment_client is not None
        assert service.metrics_client is not None
    
    def test_fetch_report_data_returns_correct_metrics_count(
        self,
        service,
        mock_experiment_client,
        mock_metrics_client
    ):
        """Test that ReportData has correct metrics count."""
        # Given
        experiment_id = 'exp-123'
        organization_id = 'org-456'
        
        mock_experiment_client.get_experiment.return_value = {
            'id': experiment_id,
            'hypothesisId': 'hyp-789',
            'startedAt': '2024-01-01T00:00:00Z',
            'completedAt': '2024-01-01T12:00:00Z'
        }
        mock_experiment_client.get_hypothesis.return_value = {
            'id': 'hyp-789',
            'problemId': 'prob-101'
        }
        mock_experiment_client.get_problem.return_value = {'id': 'prob-101'}
        mock_metrics_client.get_metrics.return_value = [
            {'id': '1'}, {'id': '2'}, {'id': '3'}
        ]
        mock_metrics_client.get_aggregated_metrics.return_value = {}
        
        # When
        result = service.fetch_report_data(experiment_id, organization_id)
        
        # Then
        assert result.metrics_count == 3
        assert result.has_metrics() is True
