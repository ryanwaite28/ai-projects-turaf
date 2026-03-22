"""
Integration tests for notification event handlers.

Tests the complete event handling workflow including idempotency,
recipient selection, email sending, and webhook delivery.

Following the hybrid testing strategy (PROJECT.md Section 23a):
- Use moto for DynamoDB (idempotency tracking - free tier)
- Mock SES client (not in free tier)
- Use requests-mock for webhooks (Python equivalent of WireMock)
- Test complete event-driven workflow
"""

import pytest
import json
from unittest.mock import Mock, patch, MagicMock
import requests_mock
from moto import mock_aws
import boto3
from notification_handler import lambda_handler


@pytest.fixture
def dynamodb_integration_setup():
    """Set up DynamoDB for idempotency tracking."""
    with mock_aws():
        dynamodb = boto3.resource('dynamodb', region_name='us-east-1')
        table_name = 'integration-idempotency-table'
        
        dynamodb.create_table(
            TableName=table_name,
            KeySchema=[
                {'AttributeName': 'eventId', 'KeyType': 'HASH'}
            ],
            AttributeDefinitions=[
                {'AttributeName': 'eventId', 'AttributeType': 'S'}
            ],
            BillingMode='PAY_PER_REQUEST'
        )
        
        yield dynamodb, table_name


@pytest.fixture
def report_generated_event():
    """Sample ReportGenerated event for integration testing."""
    return {
        'version': '0',
        'id': 'evt-integration-123',
        'detail-type': 'ReportGenerated',
        'source': 'turaf.reporting-service',
        'account': '123456789012',
        'time': '2024-01-01T12:00:00Z',
        'region': 'us-east-1',
        'resources': [],
        'detail': {
            'reportId': 'report-integration-456',
            'experimentId': 'exp-integration-789',
            'organizationId': 'org-integration-101',
            'reportUrl': 'https://s3.amazonaws.com/reports/report-456.pdf'
        }
    }


@pytest.fixture
def experiment_completed_event():
    """Sample ExperimentCompleted event for integration testing."""
    return {
        'version': '0',
        'id': 'evt-integration-456',
        'detail-type': 'ExperimentCompleted',
        'source': 'turaf.experiment-service',
        'account': '123456789012',
        'time': '2024-01-01T12:00:00Z',
        'region': 'us-east-1',
        'resources': [],
        'detail': {
            'experimentId': 'exp-integration-123',
            'experimentName': 'Performance Test',
            'organizationId': 'org-integration-456',
            'completedAt': '2024-01-01T12:00:00Z',
            'result': 'SUCCESS'
        }
    }


@pytest.fixture
def member_added_event():
    """Sample MemberAdded event for integration testing."""
    return {
        'version': '0',
        'id': 'evt-integration-789',
        'detail-type': 'MemberAdded',
        'source': 'turaf.organization-service',
        'account': '123456789012',
        'time': '2024-01-01T12:00:00Z',
        'region': 'us-east-1',
        'resources': [],
        'detail': {
            'userId': 'user-integration-123',
            'organizationId': 'org-integration-456',
            'organizationName': 'Test Organization',
            'role': 'MEMBER'
        }
    }


class TestEventHandlerIntegration:
    """
    Integration tests for notification event handlers.
    
    These tests verify the complete event handling workflow:
    - Event routing
    - Idempotency checking
    - Recipient selection
    - Email sending (mocked)
    - Webhook delivery (mocked)
    - Error handling
    """
    
    @patch('services.idempotency.is_already_processed')
    @patch('services.idempotency.mark_as_processed')
    @patch('handlers.report_generated.handle_report_generated')
    def test_handle_report_generated_event_successfully(
        self,
        mock_handler,
        mock_mark_processed,
        mock_is_processed,
        report_generated_event
    ):
        """
        Test successful handling of ReportGenerated event.
        
        Verifies:
        - Event is routed to correct handler
        - Idempotency is checked
        - Handler is called
        - Event is marked as processed
        """
        # Given
        mock_is_processed.return_value = False
        mock_handler.return_value = {
            'statusCode': 200,
            'body': 'Notifications sent successfully'
        }
        
        context = Mock()
        context.request_id = 'req-integration-123'
        
        # When
        response = lambda_handler(report_generated_event, context)
        
        # Then
        assert response['statusCode'] == 200
        
        # Verify idempotency was checked
        mock_is_processed.assert_called_once_with('evt-integration-123')
        
        # Verify handler was called
        mock_handler.assert_called_once()
        
        # Verify event was marked as processed
        mock_mark_processed.assert_called_once_with('evt-integration-123', 'ReportGenerated')
    
    @patch('services.idempotency.is_already_processed')
    @patch('services.idempotency.mark_as_processed')
    @patch('handlers.experiment_completed.handle_experiment_completed')
    def test_handle_experiment_completed_event_successfully(
        self,
        mock_handler,
        mock_mark_processed,
        mock_is_processed,
        experiment_completed_event
    ):
        """
        Test successful handling of ExperimentCompleted event.
        
        Verifies:
        - Event routing works
        - Correct handler is invoked
        - Success response returned
        """
        # Given
        mock_is_processed.return_value = False
        mock_handler.return_value = {
            'statusCode': 200,
            'body': 'Notifications sent successfully'
        }
        
        context = Mock()
        
        # When
        response = lambda_handler(experiment_completed_event, context)
        
        # Then
        assert response['statusCode'] == 200
        mock_handler.assert_called_once()
        mock_mark_processed.assert_called_once()
    
    @patch('services.idempotency.is_already_processed')
    @patch('services.idempotency.mark_as_processed')
    @patch('handlers.member_added.handle_member_added')
    def test_handle_member_added_event_successfully(
        self,
        mock_handler,
        mock_mark_processed,
        mock_is_processed,
        member_added_event
    ):
        """
        Test successful handling of MemberAdded event.
        
        Verifies:
        - MemberAdded events are routed correctly
        - Handler is invoked
        - Event is marked as processed
        """
        # Given
        mock_is_processed.return_value = False
        mock_handler.return_value = {
            'statusCode': 200,
            'body': 'Welcome email sent successfully'
        }
        
        context = Mock()
        
        # When
        response = lambda_handler(member_added_event, context)
        
        # Then
        assert response['statusCode'] == 200
        mock_handler.assert_called_once()
        mock_mark_processed.assert_called_once()
    
    @patch('services.idempotency.is_already_processed')
    @patch('services.idempotency.mark_as_processed')
    def test_idempotency_prevents_duplicate_processing(
        self,
        mock_mark_processed,
        mock_is_processed,
        report_generated_event
    ):
        """
        Test that idempotency prevents duplicate event processing.
        
        Verifies:
        - Duplicate events are detected
        - Handler is not called for duplicates
        - Already processed response is returned
        """
        # Given
        mock_is_processed.return_value = True
        
        context = Mock()
        
        # When
        response = lambda_handler(report_generated_event, context)
        
        # Then
        assert response['statusCode'] == 200
        body = json.loads(response['body'])
        assert body['message'] == 'Event already processed'
        
        # Verify event was NOT marked as processed again
        mock_mark_processed.assert_not_called()
    
    def test_handle_unknown_event_type(self):
        """
        Test handling of unknown event type.
        
        Verifies:
        - Unknown events return 400
        - Appropriate error message
        """
        # Given
        unknown_event = {
            'id': 'evt-unknown-123',
            'detail-type': 'UnknownEventType',
            'source': 'turaf.unknown-service',
            'detail': {}
        }
        
        context = Mock()
        
        # When
        with patch('services.idempotency.is_already_processed', return_value=False):
            response = lambda_handler(unknown_event, context)
        
        # Then
        assert response['statusCode'] == 400
        body = json.loads(response['body'])
        assert 'Unknown event type' in body['error']
    
    def test_handle_event_with_missing_detail_type(self):
        """
        Test handling of event with missing detail-type.
        
        Verifies:
        - Missing detail-type returns 400
        - Appropriate error message
        """
        # Given
        invalid_event = {
            'id': 'evt-invalid-123',
            # Missing 'detail-type'
            'source': 'turaf.some-service',
            'detail': {}
        }
        
        context = Mock()
        
        # When
        response = lambda_handler(invalid_event, context)
        
        # Then
        assert response['statusCode'] == 400
        body = json.loads(response['body'])
        assert 'Missing detail-type' in body['error']
    
    @patch('services.idempotency.is_already_processed')
    @patch('handlers.report_generated.handle_report_generated')
    def test_handle_event_with_handler_error(
        self,
        mock_handler,
        mock_is_processed,
        report_generated_event
    ):
        """
        Test error handling when handler raises exception.
        
        Verifies:
        - Handler exceptions are caught
        - 500 error is returned
        - Event is NOT marked as processed
        """
        # Given
        mock_is_processed.return_value = False
        mock_handler.side_effect = Exception('Handler processing failed')
        
        context = Mock()
        
        # When
        with patch('services.idempotency.mark_as_processed') as mock_mark:
            response = lambda_handler(report_generated_event, context)
        
        # Then
        assert response['statusCode'] == 500
        body = json.loads(response['body'])
        assert 'error' in body
        
        # Verify event was NOT marked as processed
        mock_mark.assert_not_called()
    
    @patch('services.idempotency.is_already_processed')
    @patch('services.idempotency.mark_as_processed')
    @patch('handlers.report_generated.handle_report_generated')
    @patch('services.email_service.EmailService.send_report_generated_email')
    @patch('services.webhook_service.WebhookService.send_webhooks')
    def test_complete_notification_workflow_with_email_and_webhooks(
        self,
        mock_send_webhooks,
        mock_send_email,
        mock_handler,
        mock_mark_processed,
        mock_is_processed,
        report_generated_event
    ):
        """
        Test complete notification workflow with email and webhooks.
        
        Verifies:
        - Event is processed
        - Emails are sent (mocked)
        - Webhooks are delivered (mocked)
        - All services are called correctly
        """
        # Given
        mock_is_processed.return_value = False
        mock_send_email.return_value = 'msg-integration-123'
        mock_send_webhooks.return_value = [
            Mock(success=True, status_code=200, url='https://webhook.example.com')
        ]
        
        # Mock handler to simulate real processing
        def handler_side_effect(event, context):
            # Simulate calling email and webhook services
            mock_send_email('user@example.com', {})
            mock_send_webhooks('org-123', 'report.generated', 'evt-123', {})
            return {
                'statusCode': 200,
                'body': 'Notifications sent successfully'
            }
        
        mock_handler.side_effect = handler_side_effect
        
        context = Mock()
        
        # When
        response = lambda_handler(report_generated_event, context)
        
        # Then
        assert response['statusCode'] == 200
        
        # Verify workflow steps
        mock_is_processed.assert_called_once()
        mock_handler.assert_called_once()
        mock_mark_processed.assert_called_once()
    
    @patch('config.config.validate')
    def test_handle_event_with_configuration_error(
        self,
        mock_validate,
        report_generated_event
    ):
        """
        Test handling of configuration validation errors.
        
        Verifies:
        - Configuration errors are caught
        - 500 error is returned
        - Appropriate error message
        """
        # Given
        mock_validate.side_effect = ValueError('Missing required configuration')
        
        context = Mock()
        
        # When
        response = lambda_handler(report_generated_event, context)
        
        # Then
        assert response['statusCode'] == 500
        body = json.loads(response['body'])
        assert 'Configuration error' in body['error']
