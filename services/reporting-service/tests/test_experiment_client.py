"""
Unit tests for ExperimentServiceClient.

Tests cover HTTP requests, retry logic, error handling, and timeout behavior.
"""

import pytest
import responses
from requests.exceptions import Timeout, HTTPError, ConnectionError
from src.clients.experiment_client import ExperimentServiceClient


class TestExperimentServiceClient:
    """Test suite for ExperimentServiceClient."""
    
    @pytest.fixture
    def client(self):
        """Create client instance for tests."""
        return ExperimentServiceClient(
            base_url='https://test-api.turaf.com',
            timeout=5
        )
    
    @responses.activate
    def test_get_experiment_success(self, client):
        """Test successful experiment fetch."""
        # Given
        experiment_id = 'exp-123'
        organization_id = 'org-456'
        expected_data = {
            'id': experiment_id,
            'hypothesisId': 'hyp-789',
            'startedAt': '2024-01-01T00:00:00Z',
            'completedAt': '2024-01-01T12:00:00Z',
            'status': 'COMPLETED'
        }
        
        responses.add(
            responses.GET,
            f'https://test-api.turaf.com/api/v1/experiments/{experiment_id}',
            json=expected_data,
            status=200
        )
        
        # When
        result = client.get_experiment(experiment_id, organization_id)
        
        # Then
        assert result == expected_data
        assert len(responses.calls) == 1
        assert responses.calls[0].request.headers['X-Organization-Id'] == organization_id
    
    @responses.activate
    def test_get_experiment_with_retry_on_500(self, client):
        """Test that client retries on 500 errors."""
        # Given
        experiment_id = 'exp-123'
        organization_id = 'org-456'
        expected_data = {'id': experiment_id}
        
        # First two calls fail, third succeeds
        responses.add(
            responses.GET,
            f'https://test-api.turaf.com/api/v1/experiments/{experiment_id}',
            json={'error': 'Server error'},
            status=500
        )
        responses.add(
            responses.GET,
            f'https://test-api.turaf.com/api/v1/experiments/{experiment_id}',
            json={'error': 'Server error'},
            status=500
        )
        responses.add(
            responses.GET,
            f'https://test-api.turaf.com/api/v1/experiments/{experiment_id}',
            json=expected_data,
            status=200
        )
        
        # When
        result = client.get_experiment(experiment_id, organization_id)
        
        # Then
        assert result == expected_data
        assert len(responses.calls) == 3
    
    @responses.activate
    def test_get_experiment_fails_after_max_retries(self, client):
        """Test that client fails after 3 retry attempts."""
        # Given
        experiment_id = 'exp-123'
        organization_id = 'org-456'
        
        # All calls fail
        for _ in range(3):
            responses.add(
                responses.GET,
                f'https://test-api.turaf.com/api/v1/experiments/{experiment_id}',
                json={'error': 'Server error'},
                status=500
            )
        
        # When/Then
        with pytest.raises(HTTPError):
            client.get_experiment(experiment_id, organization_id)
        
        assert len(responses.calls) == 3
    
    @responses.activate
    def test_get_experiment_handles_404(self, client):
        """Test that client raises HTTPError for 404."""
        # Given
        experiment_id = 'exp-999'
        organization_id = 'org-456'
        
        responses.add(
            responses.GET,
            f'https://test-api.turaf.com/api/v1/experiments/{experiment_id}',
            json={'error': 'Not found'},
            status=404
        )
        
        # When/Then
        with pytest.raises(HTTPError):
            client.get_experiment(experiment_id, organization_id)
    
    @responses.activate
    def test_get_hypothesis_success(self, client):
        """Test successful hypothesis fetch."""
        # Given
        hypothesis_id = 'hyp-123'
        organization_id = 'org-456'
        expected_data = {
            'id': hypothesis_id,
            'problemId': 'prob-789',
            'statement': 'Test hypothesis',
            'criteria': 'Success criteria'
        }
        
        responses.add(
            responses.GET,
            f'https://test-api.turaf.com/api/v1/hypotheses/{hypothesis_id}',
            json=expected_data,
            status=200
        )
        
        # When
        result = client.get_hypothesis(hypothesis_id, organization_id)
        
        # Then
        assert result == expected_data
        assert len(responses.calls) == 1
    
    @responses.activate
    def test_get_problem_success(self, client):
        """Test successful problem fetch."""
        # Given
        problem_id = 'prob-123'
        organization_id = 'org-456'
        expected_data = {
            'id': problem_id,
            'title': 'Performance issue',
            'description': 'Detailed description',
            'context': 'Business context'
        }
        
        responses.add(
            responses.GET,
            f'https://test-api.turaf.com/api/v1/problems/{problem_id}',
            json=expected_data,
            status=200
        )
        
        # When
        result = client.get_problem(problem_id, organization_id)
        
        # Then
        assert result == expected_data
        assert len(responses.calls) == 1
    
    def test_client_initialization_with_defaults(self):
        """Test client initialization with default values."""
        # When
        client = ExperimentServiceClient()
        
        # Then
        assert client.base_url == 'https://api.turaf.com'
        assert client.timeout == 10
    
    def test_client_initialization_with_custom_values(self):
        """Test client initialization with custom values."""
        # When
        client = ExperimentServiceClient(
            base_url='https://custom.api.com',
            timeout=30
        )
        
        # Then
        assert client.base_url == 'https://custom.api.com'
        assert client.timeout == 30
    
    @responses.activate
    def test_get_experiment_includes_organization_header(self, client):
        """Test that organization ID is sent in header."""
        # Given
        experiment_id = 'exp-123'
        organization_id = 'org-special'
        
        responses.add(
            responses.GET,
            f'https://test-api.turaf.com/api/v1/experiments/{experiment_id}',
            json={'id': experiment_id},
            status=200
        )
        
        # When
        client.get_experiment(experiment_id, organization_id)
        
        # Then
        request_headers = responses.calls[0].request.headers
        assert request_headers['X-Organization-Id'] == 'org-special'
