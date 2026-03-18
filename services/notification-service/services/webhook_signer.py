"""
Webhook signature service for secure webhook delivery.
Implements HMAC-SHA256 signature generation and verification.
"""
import hmac
import hashlib
import base64
import logging
from typing import Optional

logger = logging.getLogger(__name__)


class WebhookSigner:
    """
    Service for generating and verifying webhook signatures.
    Uses HMAC-SHA256 for cryptographic signing.
    """
    
    ALGORITHM = 'sha256'
    SIGNATURE_PREFIX = 'sha256='
    
    @staticmethod
    def generate_signature(payload: str, secret: str) -> str:
        """
        Generate HMAC-SHA256 signature for webhook payload.
        
        Args:
            payload: JSON string payload to sign
            secret: Secret key for signing
            
        Returns:
            Base64-encoded signature with algorithm prefix
            
        Raises:
            ValueError: If payload or secret is empty
        """
        if not payload:
            raise ValueError("Payload cannot be empty")
        
        if not secret:
            raise ValueError("Secret cannot be empty")
        
        try:
            # Create HMAC-SHA256 hash
            mac = hmac.new(
                secret.encode('utf-8'),
                payload.encode('utf-8'),
                hashlib.sha256
            )
            
            # Encode to base64
            signature = base64.b64encode(mac.digest()).decode('utf-8')
            
            # Add algorithm prefix
            return f"{WebhookSigner.SIGNATURE_PREFIX}{signature}"
            
        except Exception as e:
            logger.error(
                'Error generating webhook signature',
                extra={'error': str(e)},
                exc_info=True
            )
            raise
    
    @staticmethod
    def verify_signature(
        payload: str,
        signature: str,
        secret: str
    ) -> bool:
        """
        Verify webhook signature.
        
        Args:
            payload: JSON string payload
            signature: Signature to verify
            secret: Secret key used for signing
            
        Returns:
            True if signature is valid, False otherwise
        """
        try:
            expected_signature = WebhookSigner.generate_signature(payload, secret)
            
            # Constant-time comparison to prevent timing attacks
            return hmac.compare_digest(signature, expected_signature)
            
        except Exception as e:
            logger.error(
                'Error verifying webhook signature',
                extra={'error': str(e)},
                exc_info=True
            )
            return False
    
    @staticmethod
    def generate_signature_hex(payload: str, secret: str) -> str:
        """
        Generate HMAC-SHA256 signature in hexadecimal format.
        Alternative format for compatibility.
        
        Args:
            payload: JSON string payload to sign
            secret: Secret key for signing
            
        Returns:
            Hex-encoded signature with algorithm prefix
        """
        if not payload or not secret:
            raise ValueError("Payload and secret are required")
        
        try:
            mac = hmac.new(
                secret.encode('utf-8'),
                payload.encode('utf-8'),
                hashlib.sha256
            )
            
            signature = mac.hexdigest()
            return f"{WebhookSigner.SIGNATURE_PREFIX}{signature}"
            
        except Exception as e:
            logger.error(
                'Error generating hex signature',
                extra={'error': str(e)},
                exc_info=True
            )
            raise
