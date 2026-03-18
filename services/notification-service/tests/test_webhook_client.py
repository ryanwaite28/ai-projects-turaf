"""
Unit tests for webhook HTTP client.
"""
import pytest
from unittest.mock import Mock, patch, MagicMock
import requests
from requests.exceptions import Timeout, ConnectionError

from clients.webhook_client import WebhookClient, WebhookClientError
from models.webhook import WebhookPayload


class TestWebhookClient:
    """Test suite for WebhookClient."""
    
    @pytest.fixture
    def webhook_client(self):
        """Create WebhookClient instance."""
        return WebhookClient(timeout=10, max_retries=3)
    
    @pytest.fixture
    def webhook_payload(self):
        """Create test webhook payload."""
        return WebhookPayload(
            event_type='experiment.completed',
            event_id='evt-123',
            data={'experimentId': 'exp-123'},
            timestamp='2024-03-18T10:00:00'
        )
    
    @pytest.fixture
    def mock_response(self):
        """Create mock HTTP response."""
        response = Mock()
        response.status_code = 200
        response.text = 'OK'
        return response
    
    def test_initializes_with_default_values(self):
        """Test client initializes with default values."""
        client = WebhookClient()
        
        assert client.timeout == 30
        assert client.max_retries == 3
        assert client.session is not None
    
    def test_initializes_with_custom_values(self):
        """Test client initializes with custom values."""
        client = WebhookClient(timeout=60, max_retries=5)
        
        assert client.timeout == 60
        assert client.max_retries == 5
    
    def test_deliver_sends_post_request(
        self, webhook_client, webhook_payload, mock_response
    ):
        """Test deliver sends POST request to URL."""
        with patch.object(webhook_client.session, 'post', return_value=mock_response):
            result = webhook_client.deliver(
                'https://example.com/webhook',
                webhook_payload,
                'secret123'
            )
            
            assert result.success is True
            assert result.status_code == 200
            webhook_client.session.post.assert_called_once()
    
    def test_deliver_includes_signature_header(
        self, webhook_client, webhook_payload, mock_response
    ):
        """Test deliver includes webhook signature in headers."""
        with patch.object(webhook_client.session, 'post', return_value=mock_response) as mock_post:
            webhook_client.deliver(
                'https://example.com/webhook',
                webhook_payload,
                'secret123'
            )
            
            call_args = mock_post.call_args
            headers = call_args.kwargs['headers']
            
            assert 'X-Webhook-Signature' in headers
            assert headers['X-Webhook-Signature'].startswith('sha256=')
    
    def test_deliver_includes_event_headers(
        self, webhook_client, webhook_payload, mock_response
    ):
        """Test deliver includes event metadata in headers."""
        with patch.object(webhook_client.session, 'post', return_value=mock_response) as mock_post:
            webhook_client.deliver(
                'https://example.com/webhook',
                webhook_payload,
                'secret123'
            )
            
            call_args = mock_post.call_args
            headers = call_args.kwargs['headers']
            
            assert headers['X-Webhook-Event'] == 'experiment.completed'
            assert headers['X-Webhook-ID'] == 'evt-123'
    
    def test_deliver_returns_success_for_2xx_status(
        self, webhook_client, webhook_payload
    ):
        """Test deliver returns success for 2xx status codes."""
        mock_response = Mock()
        mock_response.status_code = 201
        mock_response.text = 'Created'
        
        with patch.object(webhook_client.session, 'post', return_value=mock_response):
            result = webhook_client.deliver(
                'https://example.com/webhook',
                webhook_payload,
                'secret123'
            )
            
            assert result.success is True
            assert result.status_code == 201
    
    def test_deliver_returns_failure_for_4xx_status(
        self, webhook_client, webhook_payload
    ):
        """Test deliver returns failure for 4xx status codes."""
        mock_response = Mock()
        mock_response.status_code = 400
        mock_response.text = 'Bad Request'
        
        with patch.object(webhook_client.session, 'post', return_value=mock_response):
            result = webhook_client.deliver(
                'https://example.com/webhook',
                webhook_payload,
                'secret123'
            )
            
            assert result.success is False
            assert result.status_code == 400
    
    def test_deliver_returns_failure_for_5xx_status(
        self, webhook_client, webhook_payload
    ):
        """Test deliver returns failure for 5xx status codes."""
        mock_response = Mock()
        mock_response.status_code = 500
        mock_response.text = 'Internal Server Error'
        
        with patch.object(webhook_client.session, 'post', return_value=mock_response):
            result = webhook_client.deliver(
                'https://example.com/webhook',
                webhook_payload,
                'secret123'
            )
            
            assert result.success is False
            assert result.status_code == 500
    
    def test_deliver_without_retry_single_attempt(
        self, webhook_client, webhook_payload, mock_response
    ):
        """Test deliver_without_retry makes single attempt."""
        with patch.object(webhook_client.session, 'post', return_value=mock_response):
            result = webhook_client.deliver_without_retry(
                'https://example.com/webhook',
                webhook_payload,
                'secret123'
            )
            
            assert result.success is True
            assert result.attempts == 1
    
    def test_deliver_without_retry_handles_exception(
        self, webhook_client, webhook_payload
    ):
        """Test deliver_without_retry handles exceptions gracefully."""
        with patch.object(webhook_client.session, 'post', side_effect=Timeout()):
            result = webhook_client.deliver_without_retry(
                'https://example.com/webhook',
                webhook_payload,
                'secret123'
            )
            
            assert result.success is False
            assert result.error_message is not None
    
    def test_close_closes_session(self, webhook_client):
        """Test close method closes HTTP session."""
        with patch.object(webhook_client.session, 'close') as mock_close:
            webhook_client.close()
            
            mock_close.assert_called_once()
    
    def test_deliver_respects_timeout(
        self, webhook_client, webhook_payload, mock_response
    ):
        """Test deliver uses configured timeout."""
        with patch.object(webhook_client.session, 'post', return_value=mock_response) as mock_post:
            webhook_client.deliver(
                'https://example.com/webhook',
                webhook_payload,
                'secret123'
            )
            
            call_args = mock_post.call_args
            assert call_args.kwargs['timeout'] == 10
    
    def test_deliver_limits_response_body_size(
        self, webhook_client, webhook_payload
    ):
        """Test deliver limits response body size."""
        mock_response = Mock()
        mock_response.status_code = 200
        mock_response.text = 'A' * 1000  # Large response
        
        with patch.object(webhook_client.session, 'post', return_value=mock_response):
            result = webhook_client.deliver(
                'https://example.com/webhook',
                webhook_payload,
                'secret123'
            )
            
            # Response should be truncated to 500 chars
            assert len(result.response_body) == 500
