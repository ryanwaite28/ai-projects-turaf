"""
Unit tests for webhook domain models.
"""
import pytest
from models.webhook import WebhookConfig, WebhookPayload, WebhookDeliveryResult, WebhookEventType


class TestWebhookConfig:
    """Test suite for WebhookConfig model."""
    
    def test_creates_webhook_config(self):
        """Test creating webhook configuration."""
        config = WebhookConfig(
            url='https://example.com/webhook',
            secret='secret123',
            organization_id='org-123',
            event_types=['experiment.completed']
        )
        
        assert config.url == 'https://example.com/webhook'
        assert config.secret == 'secret123'
        assert config.organization_id == 'org-123'
        assert config.enabled is True
    
    def test_supports_event_returns_true_for_matching_event(self):
        """Test supports_event returns True for configured event."""
        config = WebhookConfig(
            url='https://example.com/webhook',
            secret='secret',
            organization_id='org-123',
            event_types=['experiment.completed', 'report.generated']
        )
        
        assert config.supports_event('experiment.completed') is True
        assert config.supports_event('report.generated') is True
    
    def test_supports_event_returns_false_for_non_matching_event(self):
        """Test supports_event returns False for non-configured event."""
        config = WebhookConfig(
            url='https://example.com/webhook',
            secret='secret',
            organization_id='org-123',
            event_types=['experiment.completed']
        )
        
        assert config.supports_event('member.added') is False
    
    def test_supports_event_with_wildcard(self):
        """Test supports_event with wildcard event type."""
        config = WebhookConfig(
            url='https://example.com/webhook',
            secret='secret',
            organization_id='org-123',
            event_types=['*']
        )
        
        assert config.supports_event('experiment.completed') is True
        assert config.supports_event('any.event') is True
    
    def test_validate_succeeds_with_valid_config(self):
        """Test validation passes with valid configuration."""
        config = WebhookConfig(
            url='https://example.com/webhook',
            secret='secret',
            organization_id='org-123',
            event_types=['experiment.completed']
        )
        
        # Should not raise
        config.validate()
    
    def test_validate_fails_without_url(self):
        """Test validation fails without URL."""
        config = WebhookConfig(
            url='',
            secret='secret',
            organization_id='org-123',
            event_types=['experiment.completed']
        )
        
        with pytest.raises(ValueError) as exc_info:
            config.validate()
        
        assert 'URL is required' in str(exc_info.value)
    
    def test_validate_fails_with_invalid_protocol(self):
        """Test validation fails with invalid URL protocol."""
        config = WebhookConfig(
            url='ftp://example.com/webhook',
            secret='secret',
            organization_id='org-123',
            event_types=['experiment.completed']
        )
        
        with pytest.raises(ValueError) as exc_info:
            config.validate()
        
        assert 'HTTP or HTTPS' in str(exc_info.value)
    
    def test_validate_fails_without_secret(self):
        """Test validation fails without secret."""
        config = WebhookConfig(
            url='https://example.com/webhook',
            secret='',
            organization_id='org-123',
            event_types=['experiment.completed']
        )
        
        with pytest.raises(ValueError) as exc_info:
            config.validate()
        
        assert 'secret' in str(exc_info.value).lower()


class TestWebhookPayload:
    """Test suite for WebhookPayload model."""
    
    def test_creates_webhook_payload(self):
        """Test creating webhook payload."""
        payload = WebhookPayload(
            event_type='experiment.completed',
            event_id='evt-123',
            data={'experimentId': 'exp-123'}
        )
        
        assert payload.event_type == 'experiment.completed'
        assert payload.event_id == 'evt-123'
        assert payload.data == {'experimentId': 'exp-123'}
        assert payload.timestamp is not None
    
    def test_to_dict_returns_correct_structure(self):
        """Test to_dict returns proper dictionary."""
        payload = WebhookPayload(
            event_type='experiment.completed',
            event_id='evt-123',
            data={'key': 'value'},
            timestamp='2024-03-18T10:00:00'
        )
        
        result = payload.to_dict()
        
        assert result['event_type'] == 'experiment.completed'
        assert result['event_id'] == 'evt-123'
        assert result['data'] == {'key': 'value'}
        assert result['timestamp'] == '2024-03-18T10:00:00'
    
    def test_timestamp_auto_generated(self):
        """Test timestamp is automatically generated."""
        payload = WebhookPayload(
            event_type='test',
            event_id='evt-123',
            data={}
        )
        
        assert payload.timestamp is not None
        assert len(payload.timestamp) > 0


class TestWebhookDeliveryResult:
    """Test suite for WebhookDeliveryResult model."""
    
    def test_creates_successful_result(self):
        """Test creating successful delivery result."""
        result = WebhookDeliveryResult(
            success=True,
            status_code=200,
            response_body='OK'
        )
        
        assert result.success is True
        assert result.status_code == 200
        assert result.response_body == 'OK'
        assert result.delivered_at is not None
    
    def test_creates_failed_result(self):
        """Test creating failed delivery result."""
        result = WebhookDeliveryResult(
            success=False,
            error_message='Connection timeout'
        )
        
        assert result.success is False
        assert result.error_message == 'Connection timeout'
        assert result.delivered_at is None
    
    def test_tracks_attempt_count(self):
        """Test delivery result tracks attempt count."""
        result = WebhookDeliveryResult(
            success=True,
            status_code=200,
            attempts=3
        )
        
        assert result.attempts == 3
