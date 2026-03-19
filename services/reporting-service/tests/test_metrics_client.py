"""
Unit tests for MetricsServiceClient.

Tests cover HTTP requests, retry logic, error handling, and query parameters.
"""

import pytest
import responses
from requests.exceptions import HTTPError
from src.clients.metrics_client import MetricsServiceClient


class TestMetricsServiceClient:
    """Test suite for MetricsServiceClient."""
    
    @pytest.fixture
    def client(self):
        """Create client instance for tests."""
        return MetricsServiceClient(
            base_url='https://test-metrics.turaf.com',
            timeout=5
        )
    
    @responses.activate
    def test_get_metrics_success(self, client):
        """Test successful metrics fetch."""
        # Given
        experiment_id = 'exp-123'
        organization_id = 'org-456'
        start_time = '2024-01-01T00:00:00Z'
        end_time = '2024-01-01T12:00:00Z'
        expected_data = [
            {'id': 'metric-1', 'name': 'latency', 'value': 100},
            {'id': 'metric-2', 'name': 'latency', 'value': 150}
        ]
        
        responses.add(
            responses.GET,
            'https://test-metrics.turaf.com/api/v1/metrics',
            json=expected_data,
            status=200
        )
        
        # When
        result = client.get_metrics(
            experiment_id,
            start_time,
            end_time,
            organization_id
        )
        
        # Then
        assert result == expected_data
        assert len(responses.calls) == 1
        
        # Verify query parameters
        request = responses.calls[0].request
        assert 'experimentId=exp-123' in request.url
        assert 'startTime=2024-01-01T00%3A00%3A00Z' in request.url
        assert 'endTime=2024-01-01T12%3A00%3A00Z' in request.url
    
    @responses.activate
    def test_get_metrics_includes_organization_header(self, client):
        """Test that organization ID is sent in header."""
        # Given
        experiment_id = 'exp-123'
        organization_id = 'org-special'
        start_time = '2024-01-01T00:00:00Z'
        end_time = '2024-01-01T12:00:00Z'
        
        responses.add(
            responses.GET,
            'https://test-metrics.turaf.com/api/v1/metrics',
            json=[],
            status=200
        )
        
        # When
        client.get_metrics(experiment_id, start_time, end_time, organization_id)
        
        # Then
        request_headers = responses.calls[0].request.headers
        assert request_headers['X-Organization-Id'] == 'org-special'
    
    @responses.activate
    def test_get_metrics_with_retry_on_500(self, client):
        """Test that client retries on 500 errors."""
        # Given
        experiment_id = 'exp-123'
        organization_id = 'org-456'
        start_time = '2024-01-01T00:00:00Z'
        end_time = '2024-01-01T12:00:00Z'
        expected_data = [{'id': 'metric-1'}]
        
        # First call fails, second succeeds
        responses.add(
            responses.GET,
            'https://test-metrics.turaf.com/api/v1/metrics',
            json={'error': 'Server error'},
            status=500
        )
        responses.add(
            responses.GET,
            'https://test-metrics.turaf.com/api/v1/metrics',
            json=expected_data,
            status=200
        )
        
        # When
        result = client.get_metrics(
            experiment_id,
            start_time,
            end_time,
            organization_id
        )
        
        # Then
        assert result == expected_data
        assert len(responses.calls) == 2
    
    @responses.activate
    def test_get_aggregated_metrics_success(self, client):
        """Test successful aggregated metrics fetch."""
        # Given
        experiment_id = 'exp-123'
        organization_id = 'org-456'
        start_time = '2024-01-01T00:00:00Z'
        end_time = '2024-01-01T12:00:00Z'
        expected_data = {
            'latency': {
                'count': 100,
                'average': 125.5,
                'min': 50.0,
                'max': 200.0,
                'p50': 120.0,
                'p95': 180.0,
                'p99': 195.0
            }
        }
        
        responses.add(
            responses.GET,
            'https://test-metrics.turaf.com/api/v1/metrics/aggregated',
            json=expected_data,
            status=200
        )
        
        # When
        result = client.get_aggregated_metrics(
            experiment_id,
            start_time,
            end_time,
            organization_id
        )
        
        # Then
        assert result == expected_data
        assert len(responses.calls) == 1
    
    @responses.activate
    def test_get_aggregated_metrics_includes_query_params(self, client):
        """Test that query parameters are correctly sent."""
        # Given
        experiment_id = 'exp-123'
        organization_id = 'org-456'
        start_time = '2024-01-01T00:00:00Z'
        end_time = '2024-01-01T12:00:00Z'
        
        responses.add(
            responses.GET,
            'https://test-metrics.turaf.com/api/v1/metrics/aggregated',
            json={},
            status=200
        )
        
        # When
        client.get_aggregated_metrics(
            experiment_id,
            start_time,
            end_time,
            organization_id
        )
        
        # Then
        request = responses.calls[0].request
        assert 'experimentId=exp-123' in request.url
        assert 'startTime=2024-01-01T00%3A00%3A00Z' in request.url
        assert 'endTime=2024-01-01T12%3A00%3A00Z' in request.url
    
    @responses.activate
    def test_get_metrics_handles_404(self, client):
        """Test that client raises HTTPError for 404."""
        # Given
        experiment_id = 'exp-999'
        organization_id = 'org-456'
        start_time = '2024-01-01T00:00:00Z'
        end_time = '2024-01-01T12:00:00Z'
        
        responses.add(
            responses.GET,
            'https://test-metrics.turaf.com/api/v1/metrics',
            json={'error': 'Not found'},
            status=404
        )
        
        # When/Then
        with pytest.raises(HTTPError):
            client.get_metrics(experiment_id, start_time, end_time, organization_id)
    
    @responses.activate
    def test_get_metrics_fails_after_max_retries(self, client):
        """Test that client fails after 3 retry attempts."""
        # Given
        experiment_id = 'exp-123'
        organization_id = 'org-456'
        start_time = '2024-01-01T00:00:00Z'
        end_time = '2024-01-01T12:00:00Z'
        
        # All calls fail
        for _ in range(3):
            responses.add(
                responses.GET,
                'https://test-metrics.turaf.com/api/v1/metrics',
                json={'error': 'Server error'},
                status=500
            )
        
        # When/Then
        with pytest.raises(HTTPError):
            client.get_metrics(experiment_id, start_time, end_time, organization_id)
        
        assert len(responses.calls) == 3
    
    def test_client_initialization_with_defaults(self):
        """Test client initialization with default values."""
        # When
        client = MetricsServiceClient()
        
        # Then
        assert client.base_url == 'https://api.turaf.com'
        assert client.timeout == 10
    
    def test_client_initialization_with_custom_values(self):
        """Test client initialization with custom values."""
        # When
        client = MetricsServiceClient(
            base_url='https://custom-metrics.api.com',
            timeout=20
        )
        
        # Then
        assert client.base_url == 'https://custom-metrics.api.com'
        assert client.timeout == 20
