"""
Integration tests for Lambda handler with idempotency.
Tests the complete flow including idempotency checks.
"""
import os
import pytest
from unittest.mock import Mock, patch, MagicMock
from notification_handler import lambda_handler


class TestLambdaHandlerWithIdempotency:
    """Test suite for Lambda handler with idempotency integration."""
    
    @pytest.fixture
    def mock_context(self):
        """Create mock Lambda context."""
        context = Mock()
        context.request_id = 'test-request-id-123'
        context.function_name = 'notification-service'
        return context
    
    @pytest.fixture
    def experiment_completed_event(self):
        """Create ExperimentCompleted event."""
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
                'organizationId': 'org-123'
            }
        }
    
    @pytest.fixture(autouse=True)
    def setup_env_vars(self):
        """Setup required environment variables."""
        os.environ['ENVIRONMENT'] = 'test'
        os.environ['SES_FROM_EMAIL'] = 'test@turaf.com'
        os.environ['EXPERIMENT_SERVICE_URL'] = 'https://api.test.turaf.com'
        os.environ['ORGANIZATION_SERVICE_URL'] = 'https://api.test.turaf.com'
        os.environ['FRONTEND_URL'] = 'https://app.test.turaf.com'
        os.environ['AWS_REGION'] = 'us-east-1'
        os.environ['LOG_LEVEL'] = 'INFO'
        yield
        for key in ['ENVIRONMENT', 'SES_FROM_EMAIL', 'EXPERIMENT_SERVICE_URL',
                    'ORGANIZATION_SERVICE_URL', 'FRONTEND_URL', 'AWS_REGION', 'LOG_LEVEL']:
            os.environ.pop(key, None)
    
    def test_processes_event_when_not_already_processed(
        self, experiment_completed_event, mock_context
    ):
        """Test that event is processed when not already processed."""
        with patch('notification_handler.is_already_processed') as mock_is_processed, \
             patch('notification_handler.mark_as_processed') as mock_mark_processed:
            
            mock_is_processed.return_value = False
            
            result = lambda_handler(experiment_completed_event, mock_context)
            
            assert result['statusCode'] == 200
            mock_is_processed.assert_called_once_with('event-123')
            mock_mark_processed.assert_called_once_with('event-123', 'ExperimentCompleted')
    
    def test_skips_processing_when_already_processed(
        self, experiment_completed_event, mock_context
    ):
        """Test that event is skipped when already processed."""
        with patch('notification_handler.is_already_processed') as mock_is_processed, \
             patch('notification_handler.mark_as_processed') as mock_mark_processed, \
             patch('notification_handler.handle_experiment_completed') as mock_handler:
            
            mock_is_processed.return_value = True
            
            result = lambda_handler(experiment_completed_event, mock_context)
            
            assert result['statusCode'] == 200
            assert 'already processed' in result['body'].lower()
            mock_handler.assert_not_called()
            mock_mark_processed.assert_not_called()
    
    def test_marks_event_as_processed_after_successful_handling(
        self, experiment_completed_event, mock_context
    ):
        """Test that event is marked as processed after successful handling."""
        with patch('notification_handler.is_already_processed') as mock_is_processed, \
             patch('notification_handler.mark_as_processed') as mock_mark_processed:
            
            mock_is_processed.return_value = False
            
            result = lambda_handler(experiment_completed_event, mock_context)
            
            assert result['statusCode'] == 200
            mock_mark_processed.assert_called_once_with('event-123', 'ExperimentCompleted')
    
    def test_does_not_mark_as_processed_on_handler_error(
        self, experiment_completed_event, mock_context
    ):
        """Test that event is not marked as processed if handler raises error."""
        with patch('notification_handler.is_already_processed') as mock_is_processed, \
             patch('notification_handler.mark_as_processed') as mock_mark_processed, \
             patch('notification_handler.handle_experiment_completed') as mock_handler:
            
            mock_is_processed.return_value = False
            mock_handler.side_effect = Exception('Handler error')
            
            with pytest.raises(Exception):
                lambda_handler(experiment_completed_event, mock_context)
            
            mock_mark_processed.assert_not_called()
    
    def test_idempotency_check_for_report_generated_event(self, mock_context):
        """Test idempotency check for ReportGenerated event."""
        event = {
            'id': 'event-456',
            'detail-type': 'ReportGenerated',
            'detail': {
                'reportId': 'rpt-123',
                'experimentId': 'exp-123',
                'organizationId': 'org-123'
            }
        }
        
        with patch('notification_handler.is_already_processed') as mock_is_processed, \
             patch('notification_handler.mark_as_processed') as mock_mark_processed:
            
            mock_is_processed.return_value = False
            
            result = lambda_handler(event, mock_context)
            
            assert result['statusCode'] == 200
            mock_is_processed.assert_called_once_with('event-456')
            mock_mark_processed.assert_called_once_with('event-456', 'ReportGenerated')
    
    def test_idempotency_check_for_member_added_event(self, mock_context):
        """Test idempotency check for MemberAdded event."""
        event = {
            'id': 'event-789',
            'detail-type': 'MemberAdded',
            'detail': {
                'memberId': 'usr-456',
                'organizationId': 'org-123',
                'memberEmail': 'test@example.com'
            }
        }
        
        with patch('notification_handler.is_already_processed') as mock_is_processed, \
             patch('notification_handler.mark_as_processed') as mock_mark_processed:
            
            mock_is_processed.return_value = False
            
            result = lambda_handler(event, mock_context)
            
            assert result['statusCode'] == 200
            mock_is_processed.assert_called_once_with('event-789')
            mock_mark_processed.assert_called_once_with('event-789', 'MemberAdded')
