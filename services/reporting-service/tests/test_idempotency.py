"""
Unit tests for IdempotencyService.

Tests cover idempotency checking, event marking, TTL calculation, and error handling.
Uses moto for DynamoDB mocking.
"""

import pytest
import boto3
from moto import mock_aws
from datetime import datetime, timedelta
from src.services.idempotency import IdempotencyService


class TestIdempotencyService:
    """Test suite for IdempotencyService."""
    
    @pytest.fixture
    def aws_credentials(self, monkeypatch):
        """Set up fake AWS credentials for moto."""
        monkeypatch.setenv('AWS_ACCESS_KEY_ID', 'testing')
        monkeypatch.setenv('AWS_SECRET_ACCESS_KEY', 'testing')
        monkeypatch.setenv('AWS_SECURITY_TOKEN', 'testing')
        monkeypatch.setenv('AWS_SESSION_TOKEN', 'testing')
        monkeypatch.setenv('AWS_DEFAULT_REGION', 'us-east-1')
    
    @pytest.fixture
    def dynamodb_table(self, aws_credentials):
        """Create a mock DynamoDB table."""
        with mock_aws():
            dynamodb = boto3.resource('dynamodb', region_name='us-east-1')
            table_name = 'test-idempotency-table'
            
            # Create table
            table = dynamodb.create_table(
                TableName=table_name,
                KeySchema=[
                    {'AttributeName': 'eventId', 'KeyType': 'HASH'}
                ],
                AttributeDefinitions=[
                    {'AttributeName': 'eventId', 'AttributeType': 'S'}
                ],
                BillingMode='PAY_PER_REQUEST'
            )
            
            yield table_name
    
    @pytest.fixture
    def service(self, dynamodb_table):
        """Create service instance with mock DynamoDB."""
        with mock_aws():
            dynamodb = boto3.resource('dynamodb', region_name='us-east-1')
            
            # Create table
            dynamodb.create_table(
                TableName=dynamodb_table,
                KeySchema=[
                    {'AttributeName': 'eventId', 'KeyType': 'HASH'}
                ],
                AttributeDefinitions=[
                    {'AttributeName': 'eventId', 'AttributeType': 'S'}
                ],
                BillingMode='PAY_PER_REQUEST'
            )
            
            return IdempotencyService(
                table_name=dynamodb_table,
                dynamodb_resource=dynamodb
            )
    
    def test_service_initialization(self):
        """Test that service initializes correctly."""
        # When
        service = IdempotencyService(table_name='test-table')
        
        # Then
        assert service is not None
        assert service.table_name == 'test-table'
        assert service.dynamodb is not None
    
    def test_service_initialization_with_env_var(self, monkeypatch):
        """Test service initialization with environment variable."""
        # Given
        monkeypatch.setenv('IDEMPOTENCY_TABLE_NAME', 'env-table')
        
        # When
        service = IdempotencyService()
        
        # Then
        assert service.table_name == 'env-table'
    
    def test_is_processed_returns_false_for_new_event(self, service):
        """Test that is_processed returns False for new event."""
        # When
        result = service.is_processed('evt-123')
        
        # Then
        assert result is False
    
    def test_is_processed_returns_true_for_processed_event(self, service):
        """Test that is_processed returns True for processed event."""
        # Given
        event_id = 'evt-123'
        service.mark_processed(event_id)
        
        # When
        result = service.is_processed(event_id)
        
        # Then
        assert result is True
    
    def test_mark_processed_stores_event(self, service):
        """Test that mark_processed stores event in DynamoDB."""
        # Given
        event_id = 'evt-123'
        
        # When
        service.mark_processed(event_id)
        
        # Then
        assert service.is_processed(event_id) is True
    
    def test_mark_processed_with_report_id(self, service):
        """Test that mark_processed stores report ID."""
        # Given
        event_id = 'evt-123'
        report_id = 'rpt-456'
        
        # When
        service.mark_processed(event_id, report_id)
        
        # Then
        event = service.get_processed_event(event_id)
        assert event is not None
        assert event['reportId'] == report_id
    
    def test_mark_processed_stores_timestamp(self, service):
        """Test that mark_processed stores processedAt timestamp."""
        # Given
        event_id = 'evt-123'
        
        # When
        service.mark_processed(event_id)
        
        # Then
        event = service.get_processed_event(event_id)
        assert event is not None
        assert 'processedAt' in event
        # Verify it's a valid ISO timestamp
        datetime.fromisoformat(event['processedAt'])
    
    def test_mark_processed_stores_ttl(self, service):
        """Test that mark_processed stores TTL."""
        # Given
        event_id = 'evt-123'
        
        # When
        service.mark_processed(event_id)
        
        # Then
        event = service.get_processed_event(event_id)
        assert event is not None
        assert 'ttl' in event
        assert isinstance(event['ttl'], int)
        assert event['ttl'] > 0
    
    def test_calculate_ttl_returns_future_timestamp(self, service):
        """Test that TTL calculation returns future timestamp."""
        # When
        ttl = service._calculate_ttl(30)
        
        # Then
        current_timestamp = int(datetime.utcnow().timestamp())
        assert ttl > current_timestamp
    
    def test_calculate_ttl_approximately_30_days(self, service):
        """Test that TTL is approximately 30 days in the future."""
        # When
        ttl = service._calculate_ttl(30)
        
        # Then
        expected_ttl = int((datetime.utcnow() + timedelta(days=30)).timestamp())
        # Allow 1 second tolerance
        assert abs(ttl - expected_ttl) <= 1
    
    def test_calculate_ttl_with_custom_days(self, service):
        """Test TTL calculation with custom days."""
        # When
        ttl = service._calculate_ttl(7)
        
        # Then
        expected_ttl = int((datetime.utcnow() + timedelta(days=7)).timestamp())
        assert abs(ttl - expected_ttl) <= 1
    
    def test_is_processed_handles_empty_event_id(self, service):
        """Test that is_processed handles empty event ID gracefully."""
        # When
        result = service.is_processed('')
        
        # Then
        assert result is False
    
    def test_mark_processed_handles_empty_event_id(self, service):
        """Test that mark_processed handles empty event ID gracefully."""
        # When/Then - Should not raise exception
        service.mark_processed('')
    
    def test_get_processed_event_returns_none_for_nonexistent_event(self, service):
        """Test that get_processed_event returns None for nonexistent event."""
        # When
        event = service.get_processed_event('evt-nonexistent')
        
        # Then
        assert event is None
    
    def test_get_processed_event_returns_event_details(self, service):
        """Test that get_processed_event returns event details."""
        # Given
        event_id = 'evt-123'
        report_id = 'rpt-456'
        service.mark_processed(event_id, report_id)
        
        # When
        event = service.get_processed_event(event_id)
        
        # Then
        assert event is not None
        assert event['eventId'] == event_id
        assert event['reportId'] == report_id
        assert 'processedAt' in event
        assert 'ttl' in event
    
    def test_idempotency_prevents_duplicate_processing(self, service):
        """Test that idempotency prevents duplicate processing."""
        # Given
        event_id = 'evt-123'
        
        # When - First processing
        is_processed_first = service.is_processed(event_id)
        service.mark_processed(event_id, 'rpt-first')
        
        # When - Second processing attempt
        is_processed_second = service.is_processed(event_id)
        
        # Then
        assert is_processed_first is False  # First time should process
        assert is_processed_second is True  # Second time should skip
    
    def test_mark_processed_logs_activity(self, service, caplog):
        """Test that mark_processed logs activity."""
        # When
        with caplog.at_level('INFO'):
            service.mark_processed('evt-123', 'rpt-456')
        
        # Then
        assert 'Marked event evt-123 as processed' in caplog.text
    
    def test_is_processed_logs_when_event_exists(self, service, caplog):
        """Test that is_processed logs when event already exists."""
        # Given
        service.mark_processed('evt-123', 'rpt-456')
        
        # When
        with caplog.at_level('INFO'):
            service.is_processed('evt-123')
        
        # Then
        assert 'Event evt-123 already processed' in caplog.text
