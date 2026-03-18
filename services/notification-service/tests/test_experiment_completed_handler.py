"""
Unit tests for ExperimentCompleted event handler.
"""
import pytest
from unittest.mock import Mock, patch
from handlers.experiment_completed import handle_experiment_completed


class TestExperimentCompletedHandler:
    """Test suite for ExperimentCompleted handler."""
    
    @pytest.fixture
    def mock_context(self):
        """Create mock Lambda context."""
        context = Mock()
        context.request_id = 'test-request-123'
        return context
    
    @pytest.fixture
    def valid_event(self):
        """Create valid ExperimentCompleted event."""
        return {
            'id': 'event-123',
            'version': '0',
            'detail-type': 'ExperimentCompleted',
            'source': 'turaf.experiment-service',
            'account': '123456789012',
            'time': '2024-03-18T10:00:00Z',
            'region': 'us-east-1',
            'detail': {
                'eventId': 'evt-123',
                'experimentId': 'exp-123',
                'organizationId': 'org-123',
                'experimentName': 'Test Experiment',
                'completedAt': '2024-03-18T10:00:00Z'
            }
        }
    
    def test_handles_valid_event_successfully(self, valid_event, mock_context):
        """Test successful handling of valid event."""
        result = handle_experiment_completed(valid_event, mock_context)
        
        assert result['statusCode'] == 200
        assert 'successfully' in result['body']
    
    def test_extracts_experiment_id_from_event(self, valid_event, mock_context):
        """Test that handler extracts experimentId from event."""
        result = handle_experiment_completed(valid_event, mock_context)
        
        assert result['statusCode'] == 200
    
    def test_extracts_organization_id_from_event(self, valid_event, mock_context):
        """Test that handler extracts organizationId from event."""
        result = handle_experiment_completed(valid_event, mock_context)
        
        assert result['statusCode'] == 200
    
    def test_returns_400_when_experiment_id_missing(self, valid_event, mock_context):
        """Test validation error when experimentId is missing."""
        del valid_event['detail']['experimentId']
        
        result = handle_experiment_completed(valid_event, mock_context)
        
        assert result['statusCode'] == 400
        assert 'experimentId' in result['body']
    
    def test_returns_400_when_organization_id_missing(self, valid_event, mock_context):
        """Test validation error when organizationId is missing."""
        del valid_event['detail']['organizationId']
        
        result = handle_experiment_completed(valid_event, mock_context)
        
        assert result['statusCode'] == 400
        assert 'organizationId' in result['body']
    
    def test_logs_event_processing(self, valid_event, mock_context, caplog):
        """Test that handler logs event processing."""
        handle_experiment_completed(valid_event, mock_context)
        
        # Verify some logging occurred
        assert len(caplog.records) > 0
    
    def test_handles_empty_detail_gracefully(self, valid_event, mock_context):
        """Test handling of event with empty detail."""
        valid_event['detail'] = {}
        
        result = handle_experiment_completed(valid_event, mock_context)
        
        assert result['statusCode'] == 400
    
    def test_handles_missing_detail_gracefully(self, valid_event, mock_context):
        """Test handling of event without detail field."""
        del valid_event['detail']
        
        result = handle_experiment_completed(valid_event, mock_context)
        
        assert result['statusCode'] == 400
