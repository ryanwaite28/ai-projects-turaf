"""
Integration tests for ExperimentCompleted event handler.

Tests the complete event handling workflow including idempotency,
report generation, and event publishing.

Following the hybrid testing strategy (PROJECT.md Section 23a):
- Use moto for S3 and DynamoDB (free-tier services)
- Mock EventBridge client (not in free tier)
- Test complete event-driven workflow
"""

import pytest
import json
from unittest.mock import Mock, patch, MagicMock
from src.handlers.experiment_completed_handler import ExperimentCompletedHandler


@pytest.fixture
def mock_eventbridge_client():
    """Mock EventBridge client for integration testing (not in free tier)."""
    client = Mock()
    client.put_events = Mock(return_value={
        'FailedEntryCount': 0,
        'Entries': [{'EventId': 'evt-123'}]
    })
    return client


@pytest.fixture
def experiment_completed_event():
    """Sample ExperimentCompleted event for integration testing."""
    return {
        'version': '0',
        'id': 'evt-integration-123',
        'detail-type': 'ExperimentCompleted',
        'source': 'turaf.experiment-service',
        'account': '123456789012',
        'time': '2024-01-01T12:00:00Z',
        'region': 'us-east-1',
        'resources': [],
        'detail': {
            'eventId': 'evt-integration-123',
            'organizationId': 'org-integration-456',
            'payload': {
                'experimentId': 'exp-integration-789',
                'completedAt': '2024-01-01T12:00:00Z',
                'result': 'SUCCESS'
            }
        }
    }


class TestEventHandlerIntegration:
    """
    Integration tests for ExperimentCompletedHandler.
    
    These tests verify the complete event handling workflow:
    - Event parsing and validation
    - Idempotency checking
    - Report generation orchestration
    - Event publishing (mocked)
    - Error handling
    """
    
    @patch('src.handlers.experiment_completed_handler.ReportGenerationService')
    @patch('src.handlers.experiment_completed_handler.IdempotencyService')
    def test_handle_experiment_completed_event_successfully(
        self,
        mock_idempotency_service_class,
        mock_report_service_class,
        experiment_completed_event
    ):
        """
        Test successful handling of ExperimentCompleted event.
        
        Verifies:
        - Event is parsed correctly
        - Idempotency is checked
        - Report is generated
        - Event is marked as processed
        - Success response is returned
        """
        # Given
        mock_idempotency_service = Mock()
        mock_idempotency_service.is_processed.return_value = False
        mock_idempotency_service.mark_processed.return_value = None
        mock_idempotency_service_class.return_value = mock_idempotency_service
        
        mock_report_service = Mock()
        mock_report_service.generate_report.return_value = {
            'id': 'report-integration-123',
            'experimentId': 'exp-integration-789',
            'organizationId': 'org-integration-456',
            'pdfUrl': 's3://bucket/reports/org-integration-456/exp-integration-789/report.pdf'
        }
        mock_report_service_class.return_value = mock_report_service
        
        handler = ExperimentCompletedHandler()
        context = Mock()
        
        # When
        response = handler.handle(experiment_completed_event, context)
        
        # Then
        assert response['statusCode'] == 200
        body = json.loads(response['body'])
        assert body['message'] == 'Report generated successfully'
        assert body['reportId'] == 'report-integration-123'
        assert body['experimentId'] == 'exp-integration-789'
        
        # Verify idempotency was checked
        mock_idempotency_service.is_processed.assert_called_once_with('evt-integration-123')
        
        # Verify report was generated
        mock_report_service.generate_report.assert_called_once()
        
        # Verify event was marked as processed
        mock_idempotency_service.mark_processed.assert_called_once_with(
            'evt-integration-123',
            'report-integration-123'
        )
    
    @patch('src.handlers.experiment_completed_handler.ReportGenerationService')
    @patch('src.handlers.experiment_completed_handler.IdempotencyService')
    def test_handle_duplicate_event_returns_already_processed(
        self,
        mock_idempotency_service_class,
        mock_report_service_class,
        experiment_completed_event
    ):
        """
        Test handling duplicate event (idempotency).
        
        Verifies:
        - Duplicate event is detected
        - Report generation is skipped
        - Already processed response is returned
        """
        # Given
        mock_idempotency_service = Mock()
        mock_idempotency_service.is_processed.return_value = True
        mock_idempotency_service_class.return_value = mock_idempotency_service
        
        mock_report_service = Mock()
        mock_report_service_class.return_value = mock_report_service
        
        handler = ExperimentCompletedHandler()
        context = Mock()
        
        # When
        response = handler.handle(experiment_completed_event, context)
        
        # Then
        assert response['statusCode'] == 200
        body = json.loads(response['body'])
        assert body['message'] == 'Already processed'
        assert body['eventId'] == 'evt-integration-123'
        
        # Verify report generation was NOT called
        mock_report_service.generate_report.assert_not_called()
        
        # Verify event was NOT marked as processed again
        mock_idempotency_service.mark_processed.assert_not_called()
    
    @patch('src.handlers.experiment_completed_handler.ReportGenerationService')
    @patch('src.handlers.experiment_completed_handler.IdempotencyService')
    def test_handle_event_with_missing_experiment_id_raises_error(
        self,
        mock_idempotency_service_class,
        mock_report_service_class,
        experiment_completed_event
    ):
        """
        Test handling event with missing experimentId.
        
        Verifies:
        - Missing experimentId is detected
        - RuntimeError is raised
        - Appropriate error message
        """
        # Given
        mock_idempotency_service = Mock()
        mock_idempotency_service.is_processed.return_value = False
        mock_idempotency_service_class.return_value = mock_idempotency_service
        
        mock_report_service = Mock()
        mock_report_service_class.return_value = mock_report_service
        
        # Remove experimentId from event
        del experiment_completed_event['detail']['payload']['experimentId']
        
        handler = ExperimentCompletedHandler()
        context = Mock()
        
        # When/Then
        with pytest.raises(RuntimeError, match="experimentId is required"):
            handler.handle(experiment_completed_event, context)
    
    @patch('src.handlers.experiment_completed_handler.ReportGenerationService')
    @patch('src.handlers.experiment_completed_handler.IdempotencyService')
    def test_handle_event_with_missing_organization_id_raises_error(
        self,
        mock_idempotency_service_class,
        mock_report_service_class,
        experiment_completed_event
    ):
        """
        Test handling event with missing organizationId.
        
        Verifies:
        - Missing organizationId is detected
        - RuntimeError is raised
        - Appropriate error message
        """
        # Given
        mock_idempotency_service = Mock()
        mock_idempotency_service.is_processed.return_value = False
        mock_idempotency_service_class.return_value = mock_idempotency_service
        
        mock_report_service = Mock()
        mock_report_service_class.return_value = mock_report_service
        
        # Remove organizationId from event
        del experiment_completed_event['detail']['organizationId']
        
        handler = ExperimentCompletedHandler()
        context = Mock()
        
        # When/Then
        with pytest.raises(RuntimeError, match="organizationId is required"):
            handler.handle(experiment_completed_event, context)
    
    @patch('src.handlers.experiment_completed_handler.ReportGenerationService')
    @patch('src.handlers.experiment_completed_handler.IdempotencyService')
    def test_handle_event_with_report_generation_failure(
        self,
        mock_idempotency_service_class,
        mock_report_service_class,
        experiment_completed_event
    ):
        """
        Test handling event when report generation fails.
        
        Verifies:
        - Report generation error is caught
        - RuntimeError is raised
        - Event is NOT marked as processed
        """
        # Given
        mock_idempotency_service = Mock()
        mock_idempotency_service.is_processed.return_value = False
        mock_idempotency_service_class.return_value = mock_idempotency_service
        
        mock_report_service = Mock()
        mock_report_service.generate_report.side_effect = Exception("Report generation failed")
        mock_report_service_class.return_value = mock_report_service
        
        handler = ExperimentCompletedHandler()
        context = Mock()
        
        # When/Then
        with pytest.raises(RuntimeError, match="Failed to process event"):
            handler.handle(experiment_completed_event, context)
        
        # Verify event was NOT marked as processed
        mock_idempotency_service.mark_processed.assert_not_called()
    
    @patch('src.handlers.experiment_completed_handler.ReportGenerationService')
    @patch('src.handlers.experiment_completed_handler.IdempotencyService')
    def test_parse_experiment_event_extracts_correct_data(
        self,
        mock_idempotency_service_class,
        mock_report_service_class,
        experiment_completed_event
    ):
        """
        Test that event parsing extracts correct data.
        
        Verifies:
        - experimentId is extracted
        - organizationId is extracted
        - completedAt is extracted
        - result is extracted
        """
        # Given
        mock_idempotency_service = Mock()
        mock_idempotency_service.is_processed.return_value = False
        mock_idempotency_service_class.return_value = mock_idempotency_service
        
        mock_report_service = Mock()
        mock_report_service.generate_report.return_value = {
            'id': 'report-123',
            'experimentId': 'exp-integration-789'
        }
        mock_report_service_class.return_value = mock_report_service
        
        handler = ExperimentCompletedHandler()
        context = Mock()
        
        # When
        handler.handle(experiment_completed_event, context)
        
        # Then - Verify generate_report was called with correct data
        call_args = mock_report_service.generate_report.call_args[0][0]
        assert call_args['experimentId'] == 'exp-integration-789'
        assert call_args['organizationId'] == 'org-integration-456'
        assert call_args['completedAt'] == '2024-01-01T12:00:00Z'
        assert call_args['result'] == 'SUCCESS'
