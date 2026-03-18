"""
Idempotency service for notification-service.
Prevents duplicate processing of events using DynamoDB.
"""
import os
import logging
from datetime import datetime, timedelta
from typing import Optional
import boto3
from botocore.exceptions import ClientError

logger = logging.getLogger(__name__)


class IdempotencyService:
    """
    Service for tracking processed events to ensure idempotent processing.
    Uses DynamoDB with TTL for automatic cleanup of old records.
    """
    
    def __init__(self, table_name: Optional[str] = None):
        """
        Initialize idempotency service.
        
        Args:
            table_name: DynamoDB table name, defaults to env var
        """
        self.dynamodb = boto3.resource('dynamodb')
        self.table_name = table_name or os.environ.get(
            'IDEMPOTENCY_TABLE_NAME',
            'processed_notification_events'
        )
        self.table = self.dynamodb.Table(self.table_name)
        self.ttl_days = 30  # Keep records for 30 days
    
    def is_already_processed(self, event_id: str) -> bool:
        """
        Check if an event has already been processed.
        
        Args:
            event_id: Unique identifier for the event
            
        Returns:
            True if event was already processed, False otherwise
        """
        try:
            response = self.table.get_item(
                Key={'eventId': event_id}
            )
            
            if 'Item' in response:
                logger.info(
                    'Event already processed',
                    extra={'event_id': event_id, 'processed_at': response['Item'].get('processedAt')}
                )
                return True
            
            return False
            
        except ClientError as e:
            logger.error(
                'Error checking idempotency',
                extra={'event_id': event_id, 'error': str(e)},
                exc_info=True
            )
            # Fail open - allow processing if we can't check
            return False
    
    def mark_as_processed(self, event_id: str, event_type: str) -> None:
        """
        Mark an event as processed.
        
        Args:
            event_id: Unique identifier for the event
            event_type: Type of event (e.g., 'ExperimentCompleted')
        """
        try:
            now = datetime.utcnow()
            ttl_timestamp = int((now + timedelta(days=self.ttl_days)).timestamp())
            
            self.table.put_item(
                Item={
                    'eventId': event_id,
                    'eventType': event_type,
                    'processedAt': now.isoformat(),
                    'ttl': ttl_timestamp
                }
            )
            
            logger.info(
                'Marked event as processed',
                extra={
                    'event_id': event_id,
                    'event_type': event_type,
                    'ttl': ttl_timestamp
                }
            )
            
        except ClientError as e:
            logger.error(
                'Error marking event as processed',
                extra={'event_id': event_id, 'error': str(e)},
                exc_info=True
            )
            # Don't raise - processing already succeeded
    
    def delete_processed_event(self, event_id: str) -> None:
        """
        Delete a processed event record (for testing/cleanup).
        
        Args:
            event_id: Unique identifier for the event
        """
        try:
            self.table.delete_item(
                Key={'eventId': event_id}
            )
            logger.info('Deleted processed event record', extra={'event_id': event_id})
            
        except ClientError as e:
            logger.error(
                'Error deleting processed event',
                extra={'event_id': event_id, 'error': str(e)},
                exc_info=True
            )


# Module-level functions for convenience
_service: Optional[IdempotencyService] = None


def get_idempotency_service() -> IdempotencyService:
    """Get or create the idempotency service singleton."""
    global _service
    if _service is None:
        _service = IdempotencyService()
    return _service


def is_already_processed(event_id: str) -> bool:
    """Check if event was already processed."""
    return get_idempotency_service().is_already_processed(event_id)


def mark_as_processed(event_id: str, event_type: str) -> None:
    """Mark event as processed."""
    get_idempotency_service().mark_as_processed(event_id, event_type)
