"""
Unit tests for webhook signature service.
"""
import pytest
from services.webhook_signer import WebhookSigner


class TestWebhookSigner:
    """Test suite for WebhookSigner."""
    
    def test_generates_signature_successfully(self):
        """Test signature generation."""
        payload = '{"event": "test"}'
        secret = 'my-secret-key'
        
        signature = WebhookSigner.generate_signature(payload, secret)
        
        assert signature is not None
        assert signature.startswith('sha256=')
        assert len(signature) > 10
    
    def test_generates_consistent_signatures(self):
        """Test same payload and secret produce same signature."""
        payload = '{"event": "test"}'
        secret = 'my-secret-key'
        
        sig1 = WebhookSigner.generate_signature(payload, secret)
        sig2 = WebhookSigner.generate_signature(payload, secret)
        
        assert sig1 == sig2
    
    def test_different_payloads_produce_different_signatures(self):
        """Test different payloads produce different signatures."""
        secret = 'my-secret-key'
        
        sig1 = WebhookSigner.generate_signature('{"event": "test1"}', secret)
        sig2 = WebhookSigner.generate_signature('{"event": "test2"}', secret)
        
        assert sig1 != sig2
    
    def test_different_secrets_produce_different_signatures(self):
        """Test different secrets produce different signatures."""
        payload = '{"event": "test"}'
        
        sig1 = WebhookSigner.generate_signature(payload, 'secret1')
        sig2 = WebhookSigner.generate_signature(payload, 'secret2')
        
        assert sig1 != sig2
    
    def test_raises_error_for_empty_payload(self):
        """Test error raised for empty payload."""
        with pytest.raises(ValueError) as exc_info:
            WebhookSigner.generate_signature('', 'secret')
        
        assert 'Payload cannot be empty' in str(exc_info.value)
    
    def test_raises_error_for_empty_secret(self):
        """Test error raised for empty secret."""
        with pytest.raises(ValueError) as exc_info:
            WebhookSigner.generate_signature('payload', '')
        
        assert 'Secret cannot be empty' in str(exc_info.value)
    
    def test_verify_signature_returns_true_for_valid_signature(self):
        """Test signature verification succeeds for valid signature."""
        payload = '{"event": "test"}'
        secret = 'my-secret-key'
        
        signature = WebhookSigner.generate_signature(payload, secret)
        is_valid = WebhookSigner.verify_signature(payload, signature, secret)
        
        assert is_valid is True
    
    def test_verify_signature_returns_false_for_invalid_signature(self):
        """Test signature verification fails for invalid signature."""
        payload = '{"event": "test"}'
        secret = 'my-secret-key'
        
        is_valid = WebhookSigner.verify_signature(
            payload,
            'sha256=invalidsignature',
            secret
        )
        
        assert is_valid is False
    
    def test_verify_signature_returns_false_for_wrong_secret(self):
        """Test signature verification fails with wrong secret."""
        payload = '{"event": "test"}'
        
        signature = WebhookSigner.generate_signature(payload, 'secret1')
        is_valid = WebhookSigner.verify_signature(payload, signature, 'secret2')
        
        assert is_valid is False
    
    def test_verify_signature_returns_false_for_tampered_payload(self):
        """Test signature verification fails for tampered payload."""
        original_payload = '{"event": "test"}'
        tampered_payload = '{"event": "hacked"}'
        secret = 'my-secret-key'
        
        signature = WebhookSigner.generate_signature(original_payload, secret)
        is_valid = WebhookSigner.verify_signature(tampered_payload, signature, secret)
        
        assert is_valid is False
    
    def test_generates_hex_signature_successfully(self):
        """Test hex signature generation."""
        payload = '{"event": "test"}'
        secret = 'my-secret-key'
        
        signature = WebhookSigner.generate_signature_hex(payload, secret)
        
        assert signature is not None
        assert signature.startswith('sha256=')
        # Hex signatures are longer than base64
        assert len(signature) > 10
    
    def test_hex_signature_differs_from_base64(self):
        """Test hex and base64 signatures are different formats."""
        payload = '{"event": "test"}'
        secret = 'my-secret-key'
        
        sig_base64 = WebhookSigner.generate_signature(payload, secret)
        sig_hex = WebhookSigner.generate_signature_hex(payload, secret)
        
        # Both should have prefix but different encoding
        assert sig_base64.startswith('sha256=')
        assert sig_hex.startswith('sha256=')
        assert sig_base64 != sig_hex
