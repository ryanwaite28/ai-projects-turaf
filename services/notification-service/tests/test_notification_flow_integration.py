"""
End-to-end integration tests for notification flows.
Tests complete notification delivery pipeline.
"""
import pytest
from unittest.mock import Mock, patch, MagicMock
from datetime import datetime

from notification_handler import lambda_handler
from services.idempotency import IdempotencyService
from services.email_service import EmailService
from services.webhook_service import WebhookService
from services.recipient_service import RecipientService
from models.notification_preference import MemberDto, UserPreferences


class TestNotificationFlowIntegration:
    """Integration tests for complete notification flows."""
    
    @pytest.fixture
    def mock_idempotency_service(self):
        """Mock idempotency service."""
        service = Mock(spec=IdempotencyService)
        service.is_already_processed.return_value = False
        service.mark_as_processed.return_value = None
        return service
    
    @pytest.fixture
    def mock_email_service(self):
        """Mock email service."""
        service = Mock(spec=EmailService)
        service.send_experiment_completed_email.return_value = None
        service.send_report_generated_email.return_value = None
        service.send_member_added_email.return_value = None
        return service
    
    @pytest.fixture
    def mock_webhook_service(self):
        """Mock webhook service."""
        service = Mock(spec=WebhookService)
        service.send_webhooks.return_value = []
        return service
    
    @pytest.fixture
    def mock_recipient_service(self):
        """Mock recipient service."""
        service = Mock(spec=RecipientService)
        service.get_recipients.return_value = [
            'user1@example.com',
            'user2@example.com'
        ]
        service.get_recipient_members.return_value = [
            MemberDto(
                user_id='user-1',
                email='user1@example.com',
                name='User One',
                role='ADMIN'
            )
        ]
        return service
    
    def test_experiment_completed_flow_end_to_end(
        self,
        mock_idempotency_service,
        mock_email_service,
        mock_webhook_service,
        mock_recipient_service
    ):
        """Test complete flow for ExperimentCompleted event."""
        event = {
            'version': '0',
            'id': 'evt-123',
            'detail-type': 'ExperimentCompleted',
            'source': 'experiment-service',
            'detail': {
                'experimentId': 'exp-123',
                'experimentName': 'Test Experiment',
                'organizationId': 'org-123',
                'completedAt': '2024-03-18T10:00:00Z'
            }
        }
        
        with patch('notification_handler.get_idempotency_service', return_value=mock_idempotency_service), \
             patch('handlers.experiment_completed.EmailService', return_value=mock_email_service), \
             patch('handlers.experiment_completed.WebhookService', return_value=mock_webhook_service), \
             patch('handlers.experiment_completed.RecipientService', return_value=mock_recipient_service):
            
            response = lambda_handler(event, {})
            
            # Verify idempotency check
            mock_idempotency_service.is_already_processed.assert_called_once_with('evt-123')
            
            # Verify marked as processed
            mock_idempotency_service.mark_as_processed.assert_called_once()
            
            # Verify success response
            assert response['statusCode'] == 200
    
    def test_report_generated_flow_end_to_end(
        self,
        mock_idempotency_service,
        mock_email_service,
        mock_webhook_service,
        mock_recipient_service
    ):
        """Test complete flow for ReportGenerated event."""
        event = {
            'version': '0',
            'id': 'evt-456',
            'detail-type': 'ReportGenerated',
            'source': 'reporting-service',
            'detail': {
                'experimentId': 'exp-123',
                'reportId': 'rpt-456',
                'organizationId': 'org-123',
                'reportUrl': 'https://s3.amazonaws.com/reports/rpt-456.pdf'
            }
        }
        
        with patch('notification_handler.get_idempotency_service', return_value=mock_idempotency_service), \
             patch('handlers.report_generated.EmailService', return_value=mock_email_service), \
             patch('handlers.report_generated.WebhookService', return_value=mock_webhook_service), \
             patch('handlers.report_generated.RecipientService', return_value=mock_recipient_service):
            
            response = lambda_handler(event, {})
            
            assert response['statusCode'] == 200
            mock_idempotency_service.is_already_processed.assert_called_once_with('evt-456')
    
    def test_member_added_flow_end_to_end(
        self,
        mock_idempotency_service,
        mock_email_service,
        mock_webhook_service,
        mock_recipient_service
    ):
        """Test complete flow for MemberAdded event."""
        event = {
            'version': '0',
            'id': 'evt-789',
            'detail-type': 'MemberAdded',
            'source': 'organization-service',
            'detail': {
                'organizationId': 'org-123',
                'userId': 'user-123',
                'userEmail': 'newuser@example.com',
                'userName': 'New User',
                'role': 'MEMBER'
            }
        }
        
        with patch('notification_handler.get_idempotency_service', return_value=mock_idempotency_service), \
             patch('handlers.member_added.EmailService', return_value=mock_email_service), \
             patch('handlers.member_added.WebhookService', return_value=mock_webhook_service):
            
            response = lambda_handler(event, {})
            
            assert response['statusCode'] == 200
            mock_idempotency_service.is_already_processed.assert_called_once_with('evt-789')
    
    def test_duplicate_event_prevented_by_idempotency(
        self,
        mock_idempotency_service,
        mock_email_service
    ):
        """Test duplicate event is prevented by idempotency check."""
        mock_idempotency_service.is_already_processed.return_value = True
        
        event = {
            'version': '0',
            'id': 'evt-duplicate',
            'detail-type': 'ExperimentCompleted',
            'source': 'experiment-service',
            'detail': {
                'experimentId': 'exp-123',
                'organizationId': 'org-123'
            }
        }
        
        with patch('notification_handler.get_idempotency_service', return_value=mock_idempotency_service):
            response = lambda_handler(event, {})
            
            # Should return success without processing
            assert response['statusCode'] == 200
            assert 'already processed' in response['body']
            
            # Email service should not be called
            mock_email_service.send_experiment_completed_email.assert_not_called()
    
    def test_unsupported_event_type_returns_error(self):
        """Test unsupported event type returns error response."""
        event = {
            'version': '0',
            'id': 'evt-unknown',
            'detail-type': 'UnsupportedEvent',
            'source': 'unknown-service',
            'detail': {}
        }
        
        response = lambda_handler(event, {})
        
        assert response['statusCode'] == 400
        assert 'Unsupported event type' in response['body']
    
    def test_missing_required_fields_returns_error(self):
        """Test event with missing required fields returns error."""
        event = {
            'version': '0',
            'id': 'evt-invalid',
            'detail-type': 'ExperimentCompleted',
            'source': 'experiment-service',
            'detail': {}  # Missing required fields
        }
        
        response = lambda_handler(event, {})
        
        assert response['statusCode'] == 400
    
    def test_handler_error_returns_500(self):
        """Test handler error returns 500 status."""
        event = {
            'version': '0',
            'id': 'evt-error',
            'detail-type': 'ExperimentCompleted',
            'source': 'experiment-service',
            'detail': {
                'experimentId': 'exp-123',
                'organizationId': 'org-123'
            }
        }
        
        with patch('notification_handler.get_idempotency_service') as mock_idem:
            mock_idem.return_value.is_already_processed.side_effect = Exception('DynamoDB error')
            
            response = lambda_handler(event, {})
            
            assert response['statusCode'] == 500


class TestMultiChannelNotificationIntegration:
    """Integration tests for multi-channel notification delivery."""
    
    @pytest.fixture
    def mock_services(self):
        """Create all mocked services."""
        return {
            'idempotency': Mock(spec=IdempotencyService),
            'email': Mock(spec=EmailService),
            'webhook': Mock(spec=WebhookService),
            'recipient': Mock(spec=RecipientService)
        }
    
    def test_sends_both_email_and_webhook(self, mock_services):
        """Test notification sent via both email and webhook channels."""
        mock_services['idempotency'].is_already_processed.return_value = False
        mock_services['recipient'].get_recipients.return_value = ['user@example.com']
        
        event = {
            'version': '0',
            'id': 'evt-multi',
            'detail-type': 'ExperimentCompleted',
            'source': 'experiment-service',
            'detail': {
                'experimentId': 'exp-123',
                'organizationId': 'org-123',
                'experimentName': 'Test'
            }
        }
        
        with patch('notification_handler.get_idempotency_service', return_value=mock_services['idempotency']), \
             patch('handlers.experiment_completed.EmailService', return_value=mock_services['email']), \
             patch('handlers.experiment_completed.WebhookService', return_value=mock_services['webhook']), \
             patch('handlers.experiment_completed.RecipientService', return_value=mock_services['recipient']):
            
            response = lambda_handler(event, {})
            
            assert response['statusCode'] == 200
            
            # Both channels should be called
            mock_services['email'].send_experiment_completed_email.assert_called_once()
            mock_services['webhook'].send_webhooks.assert_called_once()
    
    def test_email_failure_does_not_prevent_webhook(self, mock_services):
        """Test webhook delivery continues even if email fails."""
        mock_services['idempotency'].is_already_processed.return_value = False
        mock_services['email'].send_experiment_completed_email.side_effect = Exception('Email error')
        mock_services['recipient'].get_recipients.return_value = ['user@example.com']
        
        event = {
            'version': '0',
            'id': 'evt-partial',
            'detail-type': 'ExperimentCompleted',
            'source': 'experiment-service',
            'detail': {
                'experimentId': 'exp-123',
                'organizationId': 'org-123',
                'experimentName': 'Test'
            }
        }
        
        with patch('notification_handler.get_idempotency_service', return_value=mock_services['idempotency']), \
             patch('handlers.experiment_completed.EmailService', return_value=mock_services['email']), \
             patch('handlers.experiment_completed.WebhookService', return_value=mock_services['webhook']), \
             patch('handlers.experiment_completed.RecipientService', return_value=mock_services['recipient']):
            
            # Should not raise exception
            response = lambda_handler(event, {})
            
            # Webhook should still be attempted
            mock_services['webhook'].send_webhooks.assert_called_once()
