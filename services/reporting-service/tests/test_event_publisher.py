"""
Unit tests for EventPublisher.

Tests cover event publishing, serialization, and error handling.
Uses moto for EventBridge mocking.
"""

import pytest
import json
import boto3
from moto import mock_aws
from src.events.event_publisher import EventPublisher


class TestEventPublisher:
    """Test suite for EventPublisher."""
    
    @pytest.fixture
    def aws_credentials(self, monkeypatch):
        """Set up fake AWS credentials for moto."""
        monkeypatch.setenv('AWS_ACCESS_KEY_ID', 'testing')
        monkeypatch.setenv('AWS_SECRET_ACCESS_KEY', 'testing')
        monkeypatch.setenv('AWS_SECURITY_TOKEN', 'testing')
        monkeypatch.setenv('AWS_SESSION_TOKEN', 'testing')
        monkeypatch.setenv('AWS_DEFAULT_REGION', 'us-east-1')
    
    @pytest.fixture
    def event_bus(self, aws_credentials):
        """Create a mock EventBridge event bus."""
        with mock_aws():
            events = boto3.client('events', region_name='us-east-1')
            bus_name = 'test-event-bus'
            events.create_event_bus(Name=bus_name)
            yield bus_name
    
    @pytest.fixture
    def publisher(self, event_bus):
        """Create publisher instance with mock EventBridge."""
        with mock_aws():
            eventbridge_client = boto3.client('events', region_name='us-east-1')
            eventbridge_client.create_event_bus(Name=event_bus)
            return EventPublisher(
                event_bus_name=event_bus,
                eventbridge_client=eventbridge_client
            )
    
    def test_publisher_initialization(self):
        """Test that publisher initializes correctly."""
        # When
        publisher = EventPublisher(event_bus_name='test-bus')
        
        # Then
        assert publisher is not None
        assert publisher.event_bus_name == 'test-bus'
        assert publisher.eventbridge_client is not None
    
    def test_publisher_initialization_with_env_var(self, monkeypatch):
        """Test publisher initialization with environment variable."""
        # Given
        monkeypatch.setenv('EVENT_BUS_NAME', 'env-bus')
        
        # When
        publisher = EventPublisher()
        
        # Then
        assert publisher.event_bus_name == 'env-bus'
    
    def test_publish_report_generated_success(self, publisher):
        """Test successful ReportGenerated event publishing."""
        # When
        event_id = publisher.publish_report_generated(
            organization_id='org-123',
            experiment_id='exp-456',
            report_id='rpt-789',
            report_location='s3://bucket/path/report.pdf'
        )
        
        # Then
        assert event_id is not None
        assert len(event_id) > 0
    
    def test_publish_report_generated_with_custom_format(self, publisher):
        """Test publishing with custom report format."""
        # When
        event_id = publisher.publish_report_generated(
            organization_id='org-123',
            experiment_id='exp-456',
            report_id='rpt-789',
            report_location='s3://bucket/path/report.html',
            report_format='HTML'
        )
        
        # Then
        assert event_id is not None
    
    def test_publish_report_generated_raises_error_for_missing_organization_id(
        self,
        publisher
    ):
        """Test that publishing raises error for missing organization ID."""
        # When/Then
        with pytest.raises(ValueError) as exc_info:
            publisher.publish_report_generated(
                organization_id='',
                experiment_id='exp-456',
                report_id='rpt-789',
                report_location='s3://bucket/path/report.pdf'
            )
        assert 'organization_id is required' in str(exc_info.value)
    
    def test_publish_report_generated_raises_error_for_missing_experiment_id(
        self,
        publisher
    ):
        """Test that publishing raises error for missing experiment ID."""
        # When/Then
        with pytest.raises(ValueError) as exc_info:
            publisher.publish_report_generated(
                organization_id='org-123',
                experiment_id='',
                report_id='rpt-789',
                report_location='s3://bucket/path/report.pdf'
            )
        assert 'experiment_id is required' in str(exc_info.value)
    
    def test_publish_report_generated_raises_error_for_missing_report_id(
        self,
        publisher
    ):
        """Test that publishing raises error for missing report ID."""
        # When/Then
        with pytest.raises(ValueError) as exc_info:
            publisher.publish_report_generated(
                organization_id='org-123',
                experiment_id='exp-456',
                report_id='',
                report_location='s3://bucket/path/report.pdf'
            )
        assert 'report_id is required' in str(exc_info.value)
    
    def test_publish_report_generated_raises_error_for_missing_report_location(
        self,
        publisher
    ):
        """Test that publishing raises error for missing report location."""
        # When/Then
        with pytest.raises(ValueError) as exc_info:
            publisher.publish_report_generated(
                organization_id='org-123',
                experiment_id='exp-456',
                report_id='rpt-789',
                report_location=''
            )
        assert 'report_location is required' in str(exc_info.value)
    
    def test_publish_custom_event_success(self, publisher):
        """Test publishing custom event."""
        # When
        event_id = publisher.publish_custom_event(
            event_type='ReportFailed',
            organization_id='org-123',
            payload={'error': 'Test error'}
        )
        
        # Then
        assert event_id is not None
        assert len(event_id) > 0
    
    def test_publish_custom_event_with_detail_type(self, publisher):
        """Test publishing custom event with custom detail type."""
        # When
        event_id = publisher.publish_custom_event(
            event_type='CustomEvent',
            organization_id='org-123',
            payload={'data': 'test'},
            detail_type='CustomDetailType'
        )
        
        # Then
        assert event_id is not None
    
    def test_publish_custom_event_raises_error_for_missing_event_type(
        self,
        publisher
    ):
        """Test that custom event raises error for missing event type."""
        # When/Then
        with pytest.raises(ValueError) as exc_info:
            publisher.publish_custom_event(
                event_type='',
                organization_id='org-123',
                payload={'data': 'test'}
            )
        assert 'event_type is required' in str(exc_info.value)
    
    def test_publish_custom_event_raises_error_for_missing_payload(
        self,
        publisher
    ):
        """Test that custom event raises error for missing payload."""
        # When/Then
        with pytest.raises(ValueError) as exc_info:
            publisher.publish_custom_event(
                event_type='TestEvent',
                organization_id='org-123',
                payload=None
            )
        assert 'payload is required' in str(exc_info.value)
    
    def test_publish_report_generated_logs_activity(self, publisher, caplog):
        """Test that event publishing logs activity."""
        # When
        with caplog.at_level('INFO'):
            publisher.publish_report_generated(
                organization_id='org-123',
                experiment_id='exp-456',
                report_id='rpt-789',
                report_location='s3://bucket/path/report.pdf'
            )
        
        # Then
        assert 'Publishing ReportGenerated event' in caplog.text
        assert 'Successfully published ReportGenerated event' in caplog.text
    
    def test_publish_report_generated_returns_unique_event_ids(self, publisher):
        """Test that each published event gets a unique ID."""
        # When
        event_id1 = publisher.publish_report_generated(
            organization_id='org-123',
            experiment_id='exp-456',
            report_id='rpt-789',
            report_location='s3://bucket/path/report.pdf'
        )
        event_id2 = publisher.publish_report_generated(
            organization_id='org-123',
            experiment_id='exp-456',
            report_id='rpt-789',
            report_location='s3://bucket/path/report.pdf'
        )
        
        # Then
        assert event_id1 != event_id2
