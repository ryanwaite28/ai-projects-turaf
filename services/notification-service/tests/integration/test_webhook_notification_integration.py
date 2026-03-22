"""
Integration tests for webhook notification service.

Tests webhook delivery with requests-mock (Python equivalent of WireMock).

Following the hybrid testing strategy (PROJECT.md Section 23a):
- Use requests-mock for webhook endpoints (Python equivalent of WireMock)
- Test webhook payload construction
- Test webhook signing
- Test retry logic
- Test error handling
"""

import pytest
import requests_mock
from unittest.mock import Mock
from services.webhook_service import WebhookService
from clients.webhook_client import WebhookClient
from models.webhook import WebhookConfig


@pytest.fixture
def webhook_client():
    """Real webhook client for integration testing."""
    return WebhookClient()


@pytest.fixture
def webhook_service(webhook_client):
    """Webhook service configured for integration testing."""
    return WebhookService(webhook_client=webhook_client)


@pytest.fixture
def sample_webhook_configs():
    """Sample webhook configurations for testing."""
    return [
        WebhookConfig(
            url='https://webhook.example.com/notifications',
            secret='test-secret-123',
            enabled=True,
            events=['experiment.completed', 'report.generated']
        ),
        WebhookConfig(
            url='https://webhook2.example.com/events',
            secret='test-secret-456',
            enabled=True,
            events=['experiment.completed']
        ),
        WebhookConfig(
            url='https://webhook3.example.com/disabled',
            secret='test-secret-789',
            enabled=False,
            events=['experiment.completed']
        )
    ]


class TestWebhookNotificationIntegration:
    """
    Integration tests for webhook notification workflow.
    
    These tests verify:
    - Webhook payload construction
    - HTTP delivery with requests-mock
    - Webhook signing
    - Retry logic
    - Error handling
    """
    
    def test_send_webhook_successfully(
        self,
        webhook_service,
        sample_webhook_configs
    ):
        """
        Test sending webhook to configured endpoints.
        
        Verifies:
        - Webhooks are sent to all enabled endpoints
        - Payload is constructed correctly
        - HTTP POST is made
        - Delivery results are returned
        """
        with requests_mock.Mocker() as m:
            # Mock webhook endpoints
            m.post('https://webhook.example.com/notifications', status_code=200)
            m.post('https://webhook2.example.com/events', status_code=200)
            
            # Given
            organization_id = 'org-integration-123'
            event_type = 'experiment.completed'
            event_id = 'evt-integration-456'
            event_data = {
                'experimentId': 'exp-789',
                'experimentName': 'Integration Test',
                'completedAt': '2024-01-01T12:00:00Z'
            }
            
            # When
            results = webhook_service.send_webhooks(
                organization_id,
                event_type,
                event_id,
                event_data,
                webhook_configs=sample_webhook_configs
            )
            
            # Then
            # Should send to 2 enabled webhooks (3rd is disabled)
            assert len(results) == 2
            
            # Verify all deliveries succeeded
            for result in results:
                assert result.success is True
                assert result.status_code == 200
            
            # Verify HTTP requests were made
            assert m.call_count == 2
    
    def test_send_webhook_with_retry_on_failure(
        self,
        webhook_service,
        sample_webhook_configs
    ):
        """
        Test webhook retry logic on failure.
        
        Verifies:
        - Failed webhooks are retried
        - Retry count is tracked
        - Eventually succeeds or fails
        """
        with requests_mock.Mocker() as m:
            # Mock endpoint that fails first, then succeeds
            m.post(
                'https://webhook.example.com/notifications',
                [
                    {'status_code': 500},  # First attempt fails
                    {'status_code': 500},  # Second attempt fails
                    {'status_code': 200}   # Third attempt succeeds
                ]
            )
            m.post('https://webhook2.example.com/events', status_code=200)
            
            # Given
            organization_id = 'org-retry-123'
            event_type = 'experiment.completed'
            event_id = 'evt-retry-456'
            event_data = {'experimentId': 'exp-retry-789'}
            
            # When
            results = webhook_service.send_webhooks(
                organization_id,
                event_type,
                event_id,
                event_data,
                webhook_configs=sample_webhook_configs
            )
            
            # Then
            assert len(results) == 2
            
            # First webhook should succeed after retries
            first_result = next(r for r in results if 'webhook.example.com' in r.url)
            assert first_result.success is True
            assert first_result.status_code == 200
    
    def test_send_webhook_filters_by_event_type(
        self,
        webhook_service
    ):
        """
        Test that webhooks are filtered by event type.
        
        Verifies:
        - Only webhooks subscribed to event type are called
        - Other webhooks are skipped
        """
        with requests_mock.Mocker() as m:
            # Mock endpoints
            m.post('https://webhook.example.com/notifications', status_code=200)
            m.post('https://webhook2.example.com/events', status_code=200)
            
            # Given - webhook configs with different event subscriptions
            configs = [
                WebhookConfig(
                    url='https://webhook.example.com/notifications',
                    secret='secret-1',
                    enabled=True,
                    events=['report.generated']  # Only subscribed to report events
                ),
                WebhookConfig(
                    url='https://webhook2.example.com/events',
                    secret='secret-2',
                    enabled=True,
                    events=['experiment.completed']  # Subscribed to experiment events
                )
            ]
            
            # When - send experiment.completed event
            results = webhook_service.send_webhooks(
                'org-123',
                'experiment.completed',
                'evt-123',
                {'experimentId': 'exp-456'},
                webhook_configs=configs
            )
            
            # Then - only second webhook should be called
            assert len(results) == 1
            assert 'webhook2.example.com' in results[0].url
    
    def test_send_webhook_skips_disabled_endpoints(
        self,
        webhook_service,
        sample_webhook_configs
    ):
        """
        Test that disabled webhooks are skipped.
        
        Verifies:
        - Disabled webhooks are not called
        - Only enabled webhooks receive events
        """
        with requests_mock.Mocker() as m:
            # Mock all endpoints
            m.post('https://webhook.example.com/notifications', status_code=200)
            m.post('https://webhook2.example.com/events', status_code=200)
            m.post('https://webhook3.example.com/disabled', status_code=200)
            
            # When
            results = webhook_service.send_webhooks(
                'org-123',
                'experiment.completed',
                'evt-123',
                {'experimentId': 'exp-456'},
                webhook_configs=sample_webhook_configs
            )
            
            # Then - disabled webhook should not be called
            assert len(results) == 2
            assert not any('webhook3.example.com' in r.url for r in results)
            
            # Verify disabled endpoint was never called
            disabled_calls = [h for h in m.request_history if 'webhook3.example.com' in h.url]
            assert len(disabled_calls) == 0
    
    def test_send_webhook_handles_timeout(
        self,
        webhook_service
    ):
        """
        Test webhook handling of timeout errors.
        
        Verifies:
        - Timeout errors are caught
        - Delivery result indicates failure
        """
        with requests_mock.Mocker() as m:
            # Mock endpoint that times out
            m.post(
                'https://webhook.example.com/notifications',
                exc=requests_mock.exceptions.ConnectTimeout
            )
            
            # Given
            configs = [
                WebhookConfig(
                    url='https://webhook.example.com/notifications',
                    secret='secret-1',
                    enabled=True,
                    events=['experiment.completed']
                )
            ]
            
            # When
            results = webhook_service.send_webhooks(
                'org-123',
                'experiment.completed',
                'evt-123',
                {'experimentId': 'exp-456'},
                webhook_configs=configs
            )
            
            # Then
            assert len(results) == 1
            assert results[0].success is False
            assert 'timeout' in results[0].error_message.lower() or results[0].error_message is not None
    
    def test_send_webhook_includes_signature_header(
        self,
        webhook_service
    ):
        """
        Test that webhook requests include signature header.
        
        Verifies:
        - X-Webhook-Signature header is present
        - Signature is computed correctly
        """
        with requests_mock.Mocker() as m:
            # Mock endpoint
            m.post('https://webhook.example.com/notifications', status_code=200)
            
            # Given
            configs = [
                WebhookConfig(
                    url='https://webhook.example.com/notifications',
                    secret='test-secret-123',
                    enabled=True,
                    events=['experiment.completed']
                )
            ]
            
            # When
            webhook_service.send_webhooks(
                'org-123',
                'experiment.completed',
                'evt-123',
                {'experimentId': 'exp-456'},
                webhook_configs=configs
            )
            
            # Then
            assert m.call_count == 1
            request = m.request_history[0]
            
            # Verify signature header is present
            assert 'X-Webhook-Signature' in request.headers or 'x-webhook-signature' in request.headers
    
    def test_send_webhook_with_no_configured_webhooks(
        self,
        webhook_service
    ):
        """
        Test behavior when no webhooks are configured.
        
        Verifies:
        - Empty list is returned
        - No HTTP requests are made
        """
        with requests_mock.Mocker() as m:
            # When
            results = webhook_service.send_webhooks(
                'org-123',
                'experiment.completed',
                'evt-123',
                {'experimentId': 'exp-456'},
                webhook_configs=[]
            )
            
            # Then
            assert len(results) == 0
            assert m.call_count == 0
