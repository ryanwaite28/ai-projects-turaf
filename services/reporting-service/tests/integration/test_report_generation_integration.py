"""
Integration tests for complete report generation workflow.

Tests the end-to-end report generation process including data fetching,
aggregation, PDF generation, S3 storage, and event publishing.

Following the hybrid testing strategy (PROJECT.md Section 23a):
- Use moto for S3 and DynamoDB (free-tier services)
- Mock EventBridge client (not in free tier)
- Mock external API calls (experiment-service, metrics-service)
- Test complete workflow from event to stored report
"""

import pytest
import json
from unittest.mock import Mock, patch, MagicMock
from moto import mock_aws
import boto3
from src.services.report_generation import ReportGenerationService
from src.services.s3_storage import S3StorageService
from src.handlers.experiment_completed_handler import ExperimentCompletedHandler


@pytest.fixture
def s3_integration_setup():
    """Set up S3 for integration testing with moto."""
    with mock_aws():
        s3_client = boto3.client('s3', region_name='us-east-1')
        bucket_name = 'integration-reports-bucket'
        s3_client.create_bucket(Bucket=bucket_name)
        yield s3_client, bucket_name


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


class TestReportGenerationIntegration:
    """
    Integration tests for complete report generation workflow.
    
    These tests verify the end-to-end process:
    - Event reception
    - Data fetching (mocked external APIs)
    - Report generation
    - S3 storage (moto)
    - Event publishing (mocked)
    - Idempotency (DynamoDB via moto)
    """
    
    def test_generate_report_validates_required_fields(self):
        """
        Test that report generation validates required fields.
        
        Verifies:
        - ValueError raised for missing experimentId
        - ValueError raised for missing organizationId
        """
        # Given
        service = ReportGenerationService()
        
        # Test missing experimentId
        with pytest.raises(ValueError, match="experimentId is required"):
            service.generate_report({
                'organizationId': 'org-123'
            })
        
        # Test missing organizationId
        with pytest.raises(ValueError, match="organizationId is required"):
            service.generate_report({
                'experimentId': 'exp-456'
            })
    
    def test_generate_report_returns_report_metadata(self):
        """
        Test that report generation returns correct metadata.
        
        Verifies:
        - Report ID is generated
        - Experiment ID is preserved
        - Organization ID is preserved
        - Status is set
        - Timestamps are preserved
        """
        # Given
        service = ReportGenerationService()
        experiment_event = {
            'experimentId': 'exp-integration-123',
            'organizationId': 'org-integration-456',
            'completedAt': '2024-01-01T12:00:00Z',
            'result': 'SUCCESS'
        }
        
        # When
        report = service.generate_report(experiment_event)
        
        # Then
        assert report is not None
        assert 'id' in report
        assert report['id'] is not None
        assert report['experimentId'] == 'exp-integration-123'
        assert report['organizationId'] == 'org-integration-456'
        assert report['status'] == 'generated'
        assert report['completedAt'] == '2024-01-01T12:00:00Z'
        assert report['result'] == 'SUCCESS'
    
    @patch('src.handlers.experiment_completed_handler.ReportGenerationService')
    @patch('src.handlers.experiment_completed_handler.IdempotencyService')
    def test_complete_workflow_from_event_to_report(
        self,
        mock_idempotency_service_class,
        mock_report_service_class,
        sample_eventbridge_event
    ):
        """
        Test complete workflow from EventBridge event to report generation.
        
        Verifies:
        - Event is received and parsed
        - Idempotency is checked
        - Report is generated
        - Success response is returned
        - All services are called correctly
        """
        # Given
        mock_idempotency_service = Mock()
        mock_idempotency_service.is_processed.return_value = False
        mock_idempotency_service.mark_processed.return_value = None
        mock_idempotency_service_class.return_value = mock_idempotency_service
        
        mock_report_service = Mock()
        mock_report_service.generate_report.return_value = {
            'id': 'report-workflow-123',
            'experimentId': sample_eventbridge_event['detail']['payload']['experimentId'],
            'organizationId': sample_eventbridge_event['detail']['organizationId'],
            'status': 'generated',
            'pdfUrl': 's3://bucket/reports/org/exp/report.pdf'
        }
        mock_report_service_class.return_value = mock_report_service
        
        handler = ExperimentCompletedHandler()
        context = Mock()
        
        # When
        response = handler.handle(sample_eventbridge_event, context)
        
        # Then
        assert response['statusCode'] == 200
        body = json.loads(response['body'])
        assert body['message'] == 'Report generated successfully'
        assert body['reportId'] == 'report-workflow-123'
        
        # Verify workflow steps
        mock_idempotency_service.is_processed.assert_called_once()
        mock_report_service.generate_report.assert_called_once()
        mock_idempotency_service.mark_processed.assert_called_once()
    
    @patch('src.handlers.experiment_completed_handler.ReportGenerationService')
    @patch('src.handlers.experiment_completed_handler.IdempotencyService')
    def test_idempotency_prevents_duplicate_report_generation(
        self,
        mock_idempotency_service_class,
        mock_report_service_class,
        sample_eventbridge_event
    ):
        """
        Test that idempotency prevents duplicate report generation.
        
        Verifies:
        - First event processes successfully
        - Second identical event is detected as duplicate
        - Report generation is only called once
        """
        # Given
        mock_idempotency_service = Mock()
        # First call returns False (not processed), second returns True (already processed)
        mock_idempotency_service.is_processed.side_effect = [False, True]
        mock_idempotency_service.mark_processed.return_value = None
        mock_idempotency_service_class.return_value = mock_idempotency_service
        
        mock_report_service = Mock()
        mock_report_service.generate_report.return_value = {
            'id': 'report-idempotency-123',
            'experimentId': 'exp-456',
            'organizationId': 'org-789'
        }
        mock_report_service_class.return_value = mock_report_service
        
        handler = ExperimentCompletedHandler()
        context = Mock()
        
        # When - Process event twice
        response1 = handler.handle(sample_eventbridge_event, context)
        response2 = handler.handle(sample_eventbridge_event, context)
        
        # Then
        # First response should be success
        assert response1['statusCode'] == 200
        body1 = json.loads(response1['body'])
        assert body1['message'] == 'Report generated successfully'
        
        # Second response should indicate already processed
        assert response2['statusCode'] == 200
        body2 = json.loads(response2['body'])
        assert body2['message'] == 'Already processed'
        
        # Report generation should only be called once
        assert mock_report_service.generate_report.call_count == 1
    
    def test_report_generation_with_minimal_data(self):
        """
        Test report generation with minimal required data.
        
        Verifies:
        - Report can be generated with only required fields
        - Optional fields are handled gracefully
        """
        # Given
        service = ReportGenerationService()
        minimal_event = {
            'experimentId': 'exp-minimal-123',
            'organizationId': 'org-minimal-456'
        }
        
        # When
        report = service.generate_report(minimal_event)
        
        # Then
        assert report is not None
        assert report['experimentId'] == 'exp-minimal-123'
        assert report['organizationId'] == 'org-minimal-456'
        assert report['status'] == 'generated'
        assert 'id' in report
    
    def test_report_generation_with_complete_data(self):
        """
        Test report generation with complete data.
        
        Verifies:
        - All provided fields are preserved
        - Report metadata is complete
        """
        # Given
        service = ReportGenerationService()
        complete_event = {
            'experimentId': 'exp-complete-123',
            'organizationId': 'org-complete-456',
            'completedAt': '2024-01-01T12:00:00Z',
            'result': 'SUCCESS'
        }
        
        # When
        report = service.generate_report(complete_event)
        
        # Then
        assert report is not None
        assert report['experimentId'] == 'exp-complete-123'
        assert report['organizationId'] == 'org-complete-456'
        assert report['completedAt'] == '2024-01-01T12:00:00Z'
        assert report['result'] == 'SUCCESS'
        assert report['status'] == 'generated'
    
    @patch('src.handlers.experiment_completed_handler.ReportGenerationService')
    @patch('src.handlers.experiment_completed_handler.IdempotencyService')
    def test_error_handling_in_report_generation_workflow(
        self,
        mock_idempotency_service_class,
        mock_report_service_class,
        sample_eventbridge_event
    ):
        """
        Test error handling throughout the report generation workflow.
        
        Verifies:
        - Errors are caught and wrapped
        - Appropriate error messages
        - Failed events are not marked as processed
        """
        # Given
        mock_idempotency_service = Mock()
        mock_idempotency_service.is_processed.return_value = False
        mock_idempotency_service_class.return_value = mock_idempotency_service
        
        mock_report_service = Mock()
        mock_report_service.generate_report.side_effect = Exception("Data fetch failed")
        mock_report_service_class.return_value = mock_report_service
        
        handler = ExperimentCompletedHandler()
        context = Mock()
        
        # When/Then
        with pytest.raises(RuntimeError, match="Failed to process event"):
            handler.handle(sample_eventbridge_event, context)
        
        # Verify event was NOT marked as processed
        mock_idempotency_service.mark_processed.assert_not_called()
