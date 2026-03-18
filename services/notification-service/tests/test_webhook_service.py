"""
Unit tests for webhook service.
"""
import pytest
from unittest.mock import Mock, patch

from services.webhook_service import WebhookService
from models.webhook import WebhookConfig, WebhookDeliveryResult
from clients.webhook_client import WebhookClientError


class TestWebhookService:
    """Test suite for WebhookService."""
    
    @pytest.fixture
    def mock_webhook_client(self):
        """Create mock webhook client."""
        client = Mock()
        client.deliver.return_value = WebhookDeliveryResult(
            success=True,
            status_code=200
        )
        return client
    
    @pytest.fixture
    def webhook_service(self, mock_webhook_client):
        """Create WebhookService with mocked client."""
        return WebhookService(webhook_client=mock_webhook_client)
    
    @pytest.fixture
    def webhook_configs(self):
        """Create test webhook configurations."""
        return [
            WebhookConfig(
                url='https://example.com/webhook1',
                secret='secret1',
                organization_id='org-123',
                event_types=['experiment.completed'],
                enabled=True
            ),
            WebhookConfig(
                url='https://example.com/webhook2',
                secret='secret2',
                organization_id='org-123',
                event_types=['*'],
                enabled=True
            )
        ]
    
    def test_send_webhooks_delivers_to_all_configs(
        self, webhook_service, mock_webhook_client, webhook_configs
    ):
        """Test send_webhooks delivers to all configured webhooks."""
        results = webhook_service.send_webhooks(
            organization_id='org-123',
            event_type='experiment.completed',
            event_id='evt-123',
            event_data={'experimentId': 'exp-123'},
            webhook_configs=webhook_configs
        )
        
        assert len(results) == 2
        assert mock_webhook_client.deliver.call_count == 2
    
    def test_send_webhooks_filters_by_event_type(
        self, webhook_service, mock_webhook_client
    ):
        """Test send_webhooks filters webhooks by event type."""
        configs = [
            WebhookConfig(
                url='https://example.com/webhook1',
                secret='secret1',
                organization_id='org-123',
                event_types=['experiment.completed'],
                enabled=True
            ),
            WebhookConfig(
                url='https://example.com/webhook2',
                secret='secret2',
                organization_id='org-123',
                event_types=['report.generated'],
                enabled=True
            )
        ]
        
        results = webhook_service.send_webhooks(
            organization_id='org-123',
            event_type='experiment.completed',
            event_id='evt-123',
            event_data={},
            webhook_configs=configs
        )
        
        # Only one webhook supports experiment.completed
        assert len(results) == 1
        assert mock_webhook_client.deliver.call_count == 1
    
    def test_send_webhooks_skips_disabled_webhooks(
        self, webhook_service, mock_webhook_client
    ):
        """Test send_webhooks skips disabled webhooks."""
        configs = [
            WebhookConfig(
                url='https://example.com/webhook1',
                secret='secret1',
                organization_id='org-123',
                event_types=['experiment.completed'],
                enabled=True
            ),
            WebhookConfig(
                url='https://example.com/webhook2',
                secret='secret2',
                organization_id='org-123',
                event_types=['experiment.completed'],
                enabled=False  # Disabled
            )
        ]
        
        results = webhook_service.send_webhooks(
            organization_id='org-123',
            event_type='experiment.completed',
            event_id='evt-123',
            event_data={},
            webhook_configs=configs
        )
        
        # Only enabled webhook should be called
        assert len(results) == 1
        assert mock_webhook_client.deliver.call_count == 1
    
    def test_send_webhooks_returns_empty_list_for_no_configs(
        self, webhook_service, mock_webhook_client
    ):
        """Test send_webhooks returns empty list when no configs."""
        results = webhook_service.send_webhooks(
            organization_id='org-123',
            event_type='experiment.completed',
            event_id='evt-123',
            event_data={},
            webhook_configs=[]
        )
        
        assert results == []
        mock_webhook_client.deliver.assert_not_called()
    
    def test_send_webhooks_handles_delivery_failure(
        self, webhook_service, mock_webhook_client, webhook_configs
    ):
        """Test send_webhooks handles delivery failures gracefully."""
        mock_webhook_client.deliver.side_effect = [
            WebhookDeliveryResult(success=True, status_code=200),
            WebhookClientError('Connection failed')
        ]
        
        results = webhook_service.send_webhooks(
            organization_id='org-123',
            event_type='experiment.completed',
            event_id='evt-123',
            event_data={},
            webhook_configs=webhook_configs
        )
        
        assert len(results) == 2
        assert results[0].success is True
        assert results[1].success is False
    
    def test_send_webhook_delivers_single_webhook(
        self, webhook_service, mock_webhook_client
    ):
        """Test send_webhook delivers to single URL."""
        result = webhook_service.send_webhook(
            url='https://example.com/webhook',
            secret='secret123',
            event_type='experiment.completed',
            event_id='evt-123',
            event_data={'key': 'value'}
        )
        
        assert result.success is True
        mock_webhook_client.deliver.assert_called_once()
    
    def test_send_webhook_handles_exception(
        self, webhook_service, mock_webhook_client
    ):
        """Test send_webhook handles exceptions."""
        mock_webhook_client.deliver.side_effect = Exception('Network error')
        
        result = webhook_service.send_webhook(
            url='https://example.com/webhook',
            secret='secret123',
            event_type='experiment.completed',
            event_id='evt-123',
            event_data={}
        )
        
        assert result.success is False
        assert result.error_message is not None
    
    def test_send_webhooks_supports_wildcard_event_type(
        self, webhook_service, mock_webhook_client
    ):
        """Test webhooks with wildcard event type receive all events."""
        configs = [
            WebhookConfig(
                url='https://example.com/webhook',
                secret='secret',
                organization_id='org-123',
                event_types=['*'],
                enabled=True
            )
        ]
        
        results = webhook_service.send_webhooks(
            organization_id='org-123',
            event_type='any.event.type',
            event_id='evt-123',
            event_data={},
            webhook_configs=configs
        )
        
        assert len(results) == 1
        mock_webhook_client.deliver.assert_called_once()
    
    def test_close_closes_webhook_client(
        self, webhook_service, mock_webhook_client
    ):
        """Test close method closes webhook client."""
        webhook_service.close()
        
        mock_webhook_client.close.assert_called_once()
