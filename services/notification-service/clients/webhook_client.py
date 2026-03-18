"""
Webhook HTTP client for delivering webhook payloads.
Implements retry logic with exponential backoff.
"""
import json
import logging
from typing import Dict, Any, Optional
import requests
from requests.exceptions import RequestException, Timeout, ConnectionError
from tenacity import (
    retry,
    stop_after_attempt,
    wait_exponential,
    retry_if_exception_type,
    before_sleep_log
)

from models.webhook import WebhookPayload, WebhookDeliveryResult
from services.webhook_signer import WebhookSigner

logger = logging.getLogger(__name__)


class WebhookClientError(Exception):
    """Exception raised for webhook client errors."""
    pass


class WebhookClient:
    """
    HTTP client for delivering webhooks with retry logic.
    Implements exponential backoff and timeout handling.
    """
    
    DEFAULT_TIMEOUT = 30  # seconds
    MAX_RETRIES = 3
    
    def __init__(
        self,
        timeout: int = DEFAULT_TIMEOUT,
        max_retries: int = MAX_RETRIES
    ):
        """
        Initialize webhook client.
        
        Args:
            timeout: Request timeout in seconds
            max_retries: Maximum number of retry attempts
        """
        self.timeout = timeout
        self.max_retries = max_retries
        self.session = requests.Session()
        
        # Set default headers
        self.session.headers.update({
            'Content-Type': 'application/json',
            'User-Agent': 'Turaf-Webhook-Service/1.0'
        })
    
    @retry(
        stop=stop_after_attempt(MAX_RETRIES),
        wait=wait_exponential(multiplier=2, min=2, max=30),
        retry=retry_if_exception_type((RequestException, Timeout, ConnectionError)),
        before_sleep=before_sleep_log(logger, logging.WARNING),
        reraise=True
    )
    def deliver(
        self,
        url: str,
        payload: WebhookPayload,
        secret: str
    ) -> WebhookDeliveryResult:
        """
        Deliver webhook payload to URL with retry logic.
        
        Args:
            url: Webhook endpoint URL
            payload: Webhook payload to deliver
            secret: Secret for signature generation
            
        Returns:
            WebhookDeliveryResult with delivery status
            
        Raises:
            WebhookClientError: If delivery fails after all retries
        """
        try:
            # Serialize payload
            payload_json = json.dumps(payload.to_dict())
            
            # Generate signature
            signature = WebhookSigner.generate_signature(payload_json, secret)
            
            # Prepare headers
            headers = {
                'X-Webhook-Signature': signature,
                'X-Webhook-Event': payload.event_type,
                'X-Webhook-ID': payload.event_id
            }
            
            logger.info(
                'Delivering webhook',
                extra={
                    'url': url,
                    'event_type': payload.event_type,
                    'event_id': payload.event_id
                }
            )
            
            # Send POST request
            response = self.session.post(
                url,
                data=payload_json,
                headers=headers,
                timeout=self.timeout
            )
            
            # Check response status
            success = 200 <= response.status_code < 300
            
            if success:
                logger.info(
                    'Webhook delivered successfully',
                    extra={
                        'url': url,
                        'status_code': response.status_code,
                        'event_id': payload.event_id
                    }
                )
            else:
                logger.warning(
                    'Webhook delivery returned non-2xx status',
                    extra={
                        'url': url,
                        'status_code': response.status_code,
                        'response': response.text[:200]
                    }
                )
            
            return WebhookDeliveryResult(
                success=success,
                status_code=response.status_code,
                response_body=response.text[:500],  # Limit response size
                attempts=1
            )
            
        except Timeout as e:
            logger.error(
                'Webhook delivery timeout',
                extra={'url': url, 'timeout': self.timeout},
                exc_info=True
            )
            raise
        
        except ConnectionError as e:
            logger.error(
                'Webhook delivery connection error',
                extra={'url': url, 'error': str(e)},
                exc_info=True
            )
            raise
        
        except RequestException as e:
            logger.error(
                'Webhook delivery request error',
                extra={'url': url, 'error': str(e)},
                exc_info=True
            )
            raise
        
        except Exception as e:
            logger.error(
                'Unexpected error delivering webhook',
                extra={'url': url, 'error': str(e)},
                exc_info=True
            )
            raise WebhookClientError(f"Failed to deliver webhook: {str(e)}") from e
    
    def deliver_without_retry(
        self,
        url: str,
        payload: WebhookPayload,
        secret: str
    ) -> WebhookDeliveryResult:
        """
        Deliver webhook without retry logic (single attempt).
        
        Args:
            url: Webhook endpoint URL
            payload: Webhook payload to deliver
            secret: Secret for signature generation
            
        Returns:
            WebhookDeliveryResult with delivery status
        """
        try:
            payload_json = json.dumps(payload.to_dict())
            signature = WebhookSigner.generate_signature(payload_json, secret)
            
            headers = {
                'X-Webhook-Signature': signature,
                'X-Webhook-Event': payload.event_type,
                'X-Webhook-ID': payload.event_id
            }
            
            response = self.session.post(
                url,
                data=payload_json,
                headers=headers,
                timeout=self.timeout
            )
            
            success = 200 <= response.status_code < 300
            
            return WebhookDeliveryResult(
                success=success,
                status_code=response.status_code,
                response_body=response.text[:500],
                attempts=1
            )
            
        except Exception as e:
            logger.error(
                'Webhook delivery failed',
                extra={'url': url, 'error': str(e)},
                exc_info=True
            )
            
            return WebhookDeliveryResult(
                success=False,
                error_message=str(e),
                attempts=1
            )
    
    def close(self):
        """Close the HTTP session."""
        self.session.close()
