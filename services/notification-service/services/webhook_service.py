"""
Webhook service for notification-service.
Orchestrates webhook delivery to configured endpoints.
"""
import logging
from typing import List, Dict, Any, Optional

from models.webhook import WebhookConfig, WebhookPayload, WebhookDeliveryResult
from clients.webhook_client import WebhookClient, WebhookClientError

logger = logging.getLogger(__name__)


class WebhookService:
    """
    High-level webhook service.
    Manages webhook delivery to multiple endpoints.
    """
    
    def __init__(self, webhook_client: Optional[WebhookClient] = None):
        """
        Initialize webhook service.
        
        Args:
            webhook_client: HTTP client for webhook delivery (creates default if None)
        """
        self.webhook_client = webhook_client or WebhookClient()
    
    def send_webhooks(
        self,
        organization_id: str,
        event_type: str,
        event_id: str,
        event_data: Dict[str, Any],
        webhook_configs: Optional[List[WebhookConfig]] = None
    ) -> List[WebhookDeliveryResult]:
        """
        Send webhooks to all configured endpoints for an organization.
        
        Args:
            organization_id: Organization ID
            event_type: Type of event (e.g., 'experiment.completed')
            event_id: Unique event identifier
            event_data: Event payload data
            webhook_configs: List of webhook configurations (if None, fetches from config service)
            
        Returns:
            List of delivery results for each webhook
        """
        logger.info(
            'Sending webhooks',
            extra={
                'organization_id': organization_id,
                'event_type': event_type,
                'event_id': event_id
            }
        )
        
        # Get webhook configurations
        if webhook_configs is None:
            webhook_configs = self._get_webhook_configs(organization_id, event_type)
        
        if not webhook_configs:
            logger.info(
                'No webhooks configured for event',
                extra={
                    'organization_id': organization_id,
                    'event_type': event_type
                }
            )
            return []
        
        # Filter enabled webhooks that support this event type
        active_webhooks = [
            config for config in webhook_configs
            if config.enabled and config.supports_event(event_type)
        ]
        
        if not active_webhooks:
            logger.info(
                'No active webhooks for event type',
                extra={
                    'organization_id': organization_id,
                    'event_type': event_type
                }
            )
            return []
        
        # Create webhook payload
        payload = WebhookPayload(
            event_type=event_type,
            event_id=event_id,
            data=event_data
        )
        
        # Deliver to each webhook
        results = []
        for config in active_webhooks:
            try:
                result = self.webhook_client.deliver(
                    url=config.url,
                    payload=payload,
                    secret=config.secret
                )
                results.append(result)
                
                if result.success:
                    logger.info(
                        'Webhook delivered successfully',
                        extra={
                            'url': config.url,
                            'event_type': event_type,
                            'status_code': result.status_code
                        }
                    )
                else:
                    logger.warning(
                        'Webhook delivery failed',
                        extra={
                            'url': config.url,
                            'event_type': event_type,
                            'status_code': result.status_code,
                            'error': result.error_message
                        }
                    )
                    
            except WebhookClientError as e:
                logger.error(
                    'Webhook client error',
                    extra={
                        'url': config.url,
                        'event_type': event_type,
                        'error': str(e)
                    },
                    exc_info=True
                )
                
                results.append(WebhookDeliveryResult(
                    success=False,
                    error_message=str(e)
                ))
            
            except Exception as e:
                logger.error(
                    'Unexpected error sending webhook',
                    extra={
                        'url': config.url,
                        'event_type': event_type,
                        'error': str(e)
                    },
                    exc_info=True
                )
                
                results.append(WebhookDeliveryResult(
                    success=False,
                    error_message=f"Unexpected error: {str(e)}"
                ))
        
        # Log summary
        success_count = sum(1 for r in results if r.success)
        logger.info(
            'Webhook delivery complete',
            extra={
                'organization_id': organization_id,
                'event_type': event_type,
                'total': len(results),
                'successful': success_count,
                'failed': len(results) - success_count
            }
        )
        
        return results
    
    def send_webhook(
        self,
        url: str,
        secret: str,
        event_type: str,
        event_id: str,
        event_data: Dict[str, Any]
    ) -> WebhookDeliveryResult:
        """
        Send a single webhook to a specific URL.
        
        Args:
            url: Webhook endpoint URL
            secret: Secret for signature generation
            event_type: Type of event
            event_id: Unique event identifier
            event_data: Event payload data
            
        Returns:
            WebhookDeliveryResult with delivery status
        """
        payload = WebhookPayload(
            event_type=event_type,
            event_id=event_id,
            data=event_data
        )
        
        try:
            return self.webhook_client.deliver(url, payload, secret)
        except Exception as e:
            logger.error(
                'Failed to send webhook',
                extra={'url': url, 'error': str(e)},
                exc_info=True
            )
            return WebhookDeliveryResult(
                success=False,
                error_message=str(e)
            )
    
    def _get_webhook_configs(
        self,
        organization_id: str,
        event_type: str
    ) -> List[WebhookConfig]:
        """
        Get webhook configurations for organization and event type.
        
        This is a placeholder - in production, this would fetch from a database
        or configuration service.
        
        Args:
            organization_id: Organization ID
            event_type: Event type to filter by
            
        Returns:
            List of webhook configurations
        """
        # TODO: Implement actual config retrieval from database/service
        # For now, return empty list
        logger.debug(
            'Fetching webhook configs (placeholder)',
            extra={
                'organization_id': organization_id,
                'event_type': event_type
            }
        )
        return []
    
    def close(self):
        """Close webhook client resources."""
        if self.webhook_client:
            self.webhook_client.close()
