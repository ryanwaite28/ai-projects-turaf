"""
Unit tests for the Lambda handler.

Tests cover event processing, error handling, and response formatting.
"""

import json
import pytest
from unittest.mock import Mock
from src.lambda_handler import lambda_handler


class TestLambdaHandler:
    """Test suite for lambda_handler function."""
    
    def test_handler_processes_valid_event(self):
        """Test that handler successfully processes a valid event."""
        # Given
        event = {
            'id': 'event-123',
            'detail-type': 'ExperimentCompleted',
            'source': 'turaf.experiment-service',
            'detail': {
                'experimentId': 'exp-456',
                'organizationId': 'org-789'
            }
        }
        context = Mock()
        
        # When
        response = lambda_handler(event, context)
        
        # Then
        assert response['statusCode'] == 200
        body = json.loads(response['body'])
        assert body['message'] == 'Event received successfully'
        assert body['eventId'] == 'event-123'
        assert body['detailType'] == 'ExperimentCompleted'
    
    def test_handler_returns_error_for_missing_event_id(self):
        """Test that handler returns 400 when event ID is missing."""
        # Given
        event = {
            'detail-type': 'ExperimentCompleted',
            'source': 'turaf.experiment-service',
            'detail': {}
        }
        context = Mock()
        
        # When
        response = lambda_handler(event, context)
        
        # Then
        assert response['statusCode'] == 400
        body = json.loads(response['body'])
        assert 'error' in body
        assert 'Event ID is required' in body['error']
    
    def test_handler_logs_event_details(self, caplog):
        """Test that handler logs event information."""
        # Given
        event = {
            'id': 'event-123',
            'detail-type': 'ExperimentCompleted',
            'source': 'turaf.experiment-service',
            'detail': {}
        }
        context = Mock()
        
        # When
        with caplog.at_level('INFO'):
            lambda_handler(event, context)
        
        # Then
        assert 'Processing event event-123' in caplog.text
        assert 'ExperimentCompleted' in caplog.text
    
    def test_handler_handles_exceptions_gracefully(self):
        """Test that handler catches and handles unexpected exceptions."""
        # Given
        event = None  # This will cause an exception
        context = Mock()
        
        # When
        response = lambda_handler(event, context)
        
        # Then
        assert response['statusCode'] == 500
        body = json.loads(response['body'])
        assert 'error' in body
    
    def test_handler_accepts_event_without_detail_type(self):
        """Test that handler processes event even if detail-type is missing."""
        # Given
        event = {
            'id': 'event-123',
            'source': 'turaf.experiment-service',
            'detail': {}
        }
        context = Mock()
        
        # When
        response = lambda_handler(event, context)
        
        # Then
        assert response['statusCode'] == 200
        body = json.loads(response['body'])
        assert body['eventId'] == 'event-123'
        assert body['detailType'] is None
    
    def test_handler_response_format(self):
        """Test that handler returns properly formatted response."""
        # Given
        event = {
            'id': 'event-123',
            'detail-type': 'ExperimentCompleted',
            'source': 'turaf.experiment-service',
            'detail': {}
        }
        context = Mock()
        
        # When
        response = lambda_handler(event, context)
        
        # Then
        assert isinstance(response, dict)
        assert 'statusCode' in response
        assert 'body' in response
        assert isinstance(response['statusCode'], int)
        assert isinstance(response['body'], str)
        
        # Verify body is valid JSON
        body = json.loads(response['body'])
        assert isinstance(body, dict)
