"""
Integration tests for error handling across the notification service.
"""
import pytest
from unittest.mock import Mock, patch
from botocore.exceptions import ClientError

from services.idempotency import IdempotencyService
from services.email_service import EmailService
from services.webhook_service import WebhookService
from services.recipient_service import RecipientService
from clients.ses_client import SESClient
from clients.webhook_client import WebhookClient
from clients.organization_client import OrganizationClient
from clients.user_client import UserClient


class TestErrorHandlingResilience:
    """Test error handling and resilience patterns."""
    
    def test_idempotency_service_fails_open_on_dynamodb_error(self):
        """Test idempotency service allows processing on DynamoDB error."""
        with patch('services.idempotency.boto3.resource') as mock_boto:
            mock_table = Mock()
            mock_table.get_item.side_effect = ClientError(
                {'Error': {'Code': 'ServiceUnavailable'}},
                'GetItem'
            )
            mock_dynamodb = Mock()
            mock_dynamodb.Table.return_value = mock_table
            mock_boto.return_value = mock_dynamodb
            
            service = IdempotencyService()
            service.table = mock_table
            
            # Should return False (allow processing) on error
            result = service.is_already_processed('evt-123')
            
            assert result is False
    
    def test_email_service_handles_ses_errors_gracefully(self):
        """Test email service handles SES errors without crashing."""
        mock_ses_client = Mock(spec=SESClient)
        mock_ses_client.send_email.side_effect = Exception('SES error')
        
        email_service = EmailService(ses_client=mock_ses_client)
        
        # Should not raise exception
        email_service.send_experiment_completed_email(
            to_addresses=['user@example.com'],
            experiment_id='exp-123',
            experiment_name='Test'
        )
    
    def test_webhook_service_continues_on_individual_webhook_failure(self):
        """Test webhook service continues delivery on individual failures."""
        mock_client = Mock(spec=WebhookClient)
        mock_client.deliver.side_effect = [
            Exception('First webhook failed'),
            Mock(success=True, status_code=200)  # Second succeeds
        ]
        
        from models.webhook import WebhookConfig
        configs = [
            WebhookConfig(
                url='https://webhook1.com',
                secret='secret1',
                organization_id='org-123',
                event_types=['*']
            ),
            WebhookConfig(
                url='https://webhook2.com',
                secret='secret2',
                organization_id='org-123',
                event_types=['*']
            )
        ]
        
        webhook_service = WebhookService(webhook_client=mock_client)
        
        results = webhook_service.send_webhooks(
            organization_id='org-123',
            event_type='experiment.completed',
            event_id='evt-123',
            event_data={},
            webhook_configs=configs
        )
        
        # Should have results for both webhooks
        assert len(results) == 2
        assert results[0].success is False
        assert results[1].success is True
    
    def test_recipient_service_returns_empty_on_org_client_error(self):
        """Test recipient service returns empty list on organization client error."""
        mock_org_client = Mock(spec=OrganizationClient)
        mock_org_client.get_members.side_effect = Exception('Service unavailable')
        
        recipient_service = RecipientService(organization_client=mock_org_client)
        
        recipients = recipient_service.get_recipients(
            'org-123',
            'experiment.completed'
        )
        
        # Should return empty list, not raise
        assert recipients == []
    
    def test_recipient_service_defaults_to_notify_on_preference_error(self):
        """Test recipient service defaults to notifying on preference fetch error."""
        mock_org_client = Mock(spec=OrganizationClient)
        mock_org_client.get_members.return_value = [
            Mock(user_id='user-1', email='user1@example.com')
        ]
        
        mock_user_client = Mock(spec=UserClient)
        mock_user_client.get_user_preferences.side_effect = Exception('Preference error')
        
        recipient_service = RecipientService(
            organization_client=mock_org_client,
            user_client=mock_user_client
        )
        
        recipients = recipient_service.get_recipients(
            'org-123',
            'experiment.completed'
        )
        
        # Should include user (fail open)
        assert len(recipients) == 1
        assert 'user1@example.com' in recipients
    
    def test_user_client_returns_defaults_on_timeout(self):
        """Test user client returns default preferences on timeout."""
        import requests
        
        with patch('clients.user_client.config') as mock_cfg:
            mock_cfg.experiment_service_url = 'https://api.turaf.com'
            
            user_client = UserClient()
            
            with patch.object(
                user_client.session,
                'get',
                side_effect=requests.Timeout()
            ):
                prefs = user_client.get_user_preferences('user-123')
                
                # Should return defaults
                assert prefs.user_id == 'user-123'
                assert prefs.global_enabled is True
    
    def test_template_service_handles_missing_template(self):
        """Test template service handles missing template gracefully."""
        from services.template_service import TemplateService
        from jinja2.exceptions import TemplateNotFound
        
        template_service = TemplateService(template_dir='nonexistent')
        
        with pytest.raises(Exception):
            template_service.render('nonexistent/template', {})


class TestConcurrentEventProcessing:
    """Test concurrent event processing scenarios."""
    
    def test_concurrent_duplicate_events_handled(self):
        """Test concurrent duplicate events are handled by idempotency."""
        mock_table = Mock()
        
        # First call: not processed, second call: already processed
        mock_table.get_item.side_effect = [
            {},  # First check: not found
            {'Item': {'eventId': 'evt-123'}}  # Second check: found
        ]
        
        with patch('services.idempotency.boto3.resource') as mock_boto:
            mock_dynamodb = Mock()
            mock_dynamodb.Table.return_value = mock_table
            mock_boto.return_value = mock_dynamodb
            
            service1 = IdempotencyService()
            service1.table = mock_table
            service2 = IdempotencyService()
            service2.table = mock_table
            
            # First service: not processed
            result1 = service1.is_already_processed('evt-123')
            assert result1 is False
            
            # Second service: already processed
            result2 = service2.is_already_processed('evt-123')
            assert result2 is True
