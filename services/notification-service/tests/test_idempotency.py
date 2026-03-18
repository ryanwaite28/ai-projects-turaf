"""
Unit tests for idempotency service.
Tests DynamoDB-based event deduplication.
"""
import os
import pytest
from unittest.mock import Mock, patch, MagicMock
from datetime import datetime, timedelta
from botocore.exceptions import ClientError

from services.idempotency import IdempotencyService, is_already_processed, mark_as_processed


class TestIdempotencyService:
    """Test suite for IdempotencyService."""
    
    @pytest.fixture
    def mock_dynamodb_table(self):
        """Create mock DynamoDB table."""
        table = Mock()
        table.get_item = Mock()
        table.put_item = Mock()
        table.delete_item = Mock()
        return table
    
    @pytest.fixture
    def idempotency_service(self, mock_dynamodb_table):
        """Create IdempotencyService with mocked DynamoDB."""
        with patch('services.idempotency.boto3.resource') as mock_resource:
            mock_dynamodb = Mock()
            mock_dynamodb.Table.return_value = mock_dynamodb_table
            mock_resource.return_value = mock_dynamodb
            
            service = IdempotencyService(table_name='test-table')
            service.table = mock_dynamodb_table
            return service
    
    def test_is_already_processed_returns_false_for_new_event(
        self, idempotency_service, mock_dynamodb_table
    ):
        """Test that is_already_processed returns False for new event."""
        mock_dynamodb_table.get_item.return_value = {}
        
        result = idempotency_service.is_already_processed('event-123')
        
        assert result is False
        mock_dynamodb_table.get_item.assert_called_once_with(
            Key={'eventId': 'event-123'}
        )
    
    def test_is_already_processed_returns_true_for_existing_event(
        self, idempotency_service, mock_dynamodb_table
    ):
        """Test that is_already_processed returns True for existing event."""
        mock_dynamodb_table.get_item.return_value = {
            'Item': {
                'eventId': 'event-123',
                'processedAt': '2024-03-18T10:00:00',
                'eventType': 'ExperimentCompleted'
            }
        }
        
        result = idempotency_service.is_already_processed('event-123')
        
        assert result is True
    
    def test_is_already_processed_fails_open_on_error(
        self, idempotency_service, mock_dynamodb_table
    ):
        """Test that is_already_processed returns False on DynamoDB error."""
        mock_dynamodb_table.get_item.side_effect = ClientError(
            {'Error': {'Code': 'ServiceUnavailable'}},
            'GetItem'
        )
        
        result = idempotency_service.is_already_processed('event-123')
        
        assert result is False
    
    def test_mark_as_processed_stores_event_with_ttl(
        self, idempotency_service, mock_dynamodb_table
    ):
        """Test that mark_as_processed stores event with TTL."""
        with patch('services.idempotency.datetime') as mock_datetime:
            mock_now = datetime(2024, 3, 18, 10, 0, 0)
            mock_datetime.utcnow.return_value = mock_now
            
            idempotency_service.mark_as_processed('event-123', 'ExperimentCompleted')
            
            mock_dynamodb_table.put_item.assert_called_once()
            call_args = mock_dynamodb_table.put_item.call_args
            item = call_args.kwargs['Item']
            
            assert item['eventId'] == 'event-123'
            assert item['eventType'] == 'ExperimentCompleted'
            assert item['processedAt'] == mock_now.isoformat()
            assert 'ttl' in item
    
    def test_mark_as_processed_handles_error_gracefully(
        self, idempotency_service, mock_dynamodb_table
    ):
        """Test that mark_as_processed doesn't raise on DynamoDB error."""
        mock_dynamodb_table.put_item.side_effect = ClientError(
            {'Error': {'Code': 'ServiceUnavailable'}},
            'PutItem'
        )
        
        # Should not raise exception
        idempotency_service.mark_as_processed('event-123', 'ExperimentCompleted')
    
    def test_delete_processed_event_removes_record(
        self, idempotency_service, mock_dynamodb_table
    ):
        """Test that delete_processed_event removes the record."""
        idempotency_service.delete_processed_event('event-123')
        
        mock_dynamodb_table.delete_item.assert_called_once_with(
            Key={'eventId': 'event-123'}
        )
    
    def test_module_level_functions_use_singleton(self):
        """Test that module-level functions use singleton service."""
        with patch('services.idempotency.IdempotencyService') as mock_service_class:
            mock_instance = Mock()
            mock_service_class.return_value = mock_instance
            
            # Reset singleton
            import services.idempotency as idempotency_module
            idempotency_module._service = None
            
            # Call module-level function
            is_already_processed('event-123')
            
            # Should create service once
            mock_service_class.assert_called_once()
            mock_instance.is_already_processed.assert_called_once_with('event-123')
