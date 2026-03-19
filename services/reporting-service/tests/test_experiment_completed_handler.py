"""
Unit tests for ExperimentCompletedHandler.

Tests cover event processing, idempotency, error handling, and validation.
"""

import json
import pytest
from unittest.mock import Mock, patch
from src.handlers.experiment_completed_handler import ExperimentCompletedHandler


class TestExperimentCompletedHandler:
    """Test suite for ExperimentCompletedHandler."""
    
    @pytest.fixture
    def handler(self):
        """Create handler instance for tests."""
        return ExperimentCompletedHandler()
    
    @pytest.fixture
    def valid_event(self):
        """Create a valid ExperimentCompleted event."""
        return {
            'id': 'event-123',
            'detail-type': 'ExperimentCompleted',
            'source': 'turaf.experiment-service',
            'detail': {
                'eventId': 'evt-456',
                'organizationId': 'org-789',
                'payload': {
                    'experimentId': 'exp-101',
                    'completedAt': '2024-01-01T12:00:00Z',
                    'result': 'SUCCESS'
                }
            }
        }
    
    def test_handler_initialization(self, handler):
        """Test that handler initializes with required services."""
        assert handler.report_service is not None
        assert handler.idempotency_service is not None
    
    def test_handle_processes_valid_event(self, handler, valid_event):
        """Test successful processing of valid event."""
        # Given
        context = Mock()
        
        # When
        response = handler.handle(valid_event, context)
        
        # Then
        assert response['statusCode'] == 200
        body = json.loads(response['body'])
        assert body['message'] == 'Report generated successfully'
        assert 'reportId' in body
        assert body['experimentId'] == 'exp-101'
    
    def test_handle_checks_idempotency(self, handler, valid_event):
        """Test that handler checks for duplicate events."""
        # Given
        context = Mock()
        handler.idempotency_service.is_processed = Mock(return_value=True)
        
        # When
        response = handler.handle(valid_event, context)
        
        # Then
        assert response['statusCode'] == 200
        body = json.loads(response['body'])
        assert body['message'] == 'Already processed'
        assert body['eventId'] == 'event-123'
        handler.idempotency_service.is_processed.assert_called_once_with('event-123')
    
    def test_handle_marks_event_as_processed(self, handler, valid_event):
        """Test that handler marks event as processed after success."""
        # Given
        context = Mock()
        handler.idempotency_service.mark_processed = Mock()
        
        # When
        response = handler.handle(valid_event, context)
        
        # Then
        assert response['statusCode'] == 200
        handler.idempotency_service.mark_processed.assert_called_once()
        call_args = handler.idempotency_service.mark_processed.call_args
        assert call_args[0][0] == 'event-123'  # event_id
        assert call_args[0][1] is not None  # report_id
    
    def test_handle_raises_error_for_missing_event_id(self, handler):
        """Test that handler raises error when event ID is missing."""
        # Given
        event = {
            'detail-type': 'ExperimentCompleted',
            'detail': {
                'organizationId': 'org-123',
                'payload': {'experimentId': 'exp-456'}
            }
        }
        context = Mock()
        
        # When/Then
        with pytest.raises(RuntimeError) as exc_info:
            handler.handle(event, context)
        assert 'Invalid event structure' in str(exc_info.value)
    
    def test_handle_raises_error_for_missing_experiment_id(self, handler):
        """Test that handler raises error when experimentId is missing."""
        # Given
        event = {
            'id': 'event-123',
            'detail': {
                'organizationId': 'org-456',
                'payload': {}  # Missing experimentId
            }
        }
        context = Mock()
        
        # When/Then
        with pytest.raises(RuntimeError) as exc_info:
            handler.handle(event, context)
        assert 'experimentId is required' in str(exc_info.value)
    
    def test_handle_raises_error_for_missing_organization_id(self, handler):
        """Test that handler raises error when organizationId is missing."""
        # Given
        event = {
            'id': 'event-123',
            'detail': {
                'payload': {'experimentId': 'exp-456'}
                # Missing organizationId
            }
        }
        context = Mock()
        
        # When/Then
        with pytest.raises(RuntimeError) as exc_info:
            handler.handle(event, context)
        assert 'organizationId is required' in str(exc_info.value)
    
    def test_parse_experiment_event(self, handler):
        """Test parsing of experiment event from detail."""
        # Given
        detail = {
            'organizationId': 'org-123',
            'payload': {
                'experimentId': 'exp-456',
                'completedAt': '2024-01-01T12:00:00Z',
                'result': 'SUCCESS'
            }
        }
        
        # When
        parsed = handler._parse_experiment_event(detail)
        
        # Then
        assert parsed['experimentId'] == 'exp-456'
        assert parsed['organizationId'] == 'org-123'
        assert parsed['completedAt'] == '2024-01-01T12:00:00Z'
        assert parsed['result'] == 'SUCCESS'
    
    def test_parse_experiment_event_with_missing_optional_fields(self, handler):
        """Test parsing handles missing optional fields."""
        # Given
        detail = {
            'organizationId': 'org-123',
            'payload': {
                'experimentId': 'exp-456',
                'completedAt': '2024-01-01T12:00:00Z'
                # result is optional
            }
        }
        
        # When
        parsed = handler._parse_experiment_event(detail)
        
        # Then
        assert parsed['experimentId'] == 'exp-456'
        assert parsed['result'] is None
    
    def test_handle_logs_processing_info(self, handler, valid_event, caplog):
        """Test that handler logs event processing information."""
        # Given
        context = Mock()
        
        # When
        with caplog.at_level('INFO'):
            handler.handle(valid_event, context)
        
        # Then
        assert 'Processing ExperimentCompleted event: event-123' in caplog.text
        assert 'Report' in caplog.text
        assert 'generated successfully' in caplog.text
    
    def test_handle_delegates_to_report_service(self, handler, valid_event):
        """Test that handler delegates report generation to service."""
        # Given
        context = Mock()
        handler.report_service.generate_report = Mock(return_value={
            'id': 'report-999',
            'experimentId': 'exp-101',
            'organizationId': 'org-789',
            'status': 'generated'
        })
        
        # When
        response = handler.handle(valid_event, context)
        
        # Then
        handler.report_service.generate_report.assert_called_once()
        call_args = handler.report_service.generate_report.call_args[0][0]
        assert call_args['experimentId'] == 'exp-101'
        assert call_args['organizationId'] == 'org-789'
        
        body = json.loads(response['body'])
        assert body['reportId'] == 'report-999'
