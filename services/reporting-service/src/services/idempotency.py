"""
Idempotency service for tracking processed events.

This module provides idempotency guarantees to prevent duplicate processing
of events using DynamoDB as the backing store.
"""

import boto3
import os
import logging
from datetime import datetime, timedelta
from typing import Optional
from botocore.exceptions import ClientError, BotoCoreError

logger = logging.getLogger(__name__)


class IdempotencyService:
    """
    Service for tracking processed events to prevent duplicates.
    
    This service implements the Idempotency pattern using DynamoDB as a
    persistent store. It ensures that events are processed exactly once,
    even if they are received multiple times due to retries or failures.
    
    Features:
    - DynamoDB-based event tracking
    - TTL-based automatic cleanup (30 days)
    - Graceful error handling (fail-open strategy)
    - Report ID tracking for audit trail
    
    DynamoDB Table Schema:
    - Primary Key: eventId (String)
    - Attributes: processedAt (String), reportId (String), ttl (Number)
    
    The service follows the Single Responsibility Principle (SOLID),
    focusing solely on idempotency tracking.
    
    Attributes:
        dynamodb: Boto3 DynamoDB resource
        table_name: Name of the DynamoDB table
        table: DynamoDB Table resource
    """
    
    def __init__(self, table_name: str = None, dynamodb_resource=None):
        """
        Initialize the idempotency service.
        
        Args:
            table_name: Optional table name override.
                       Defaults to IDEMPOTENCY_TABLE_NAME env var.
            dynamodb_resource: Optional boto3 DynamoDB resource for testing.
                              Defaults to creating a new resource.
                              
        Example:
            >>> service = IdempotencyService()
            >>> # Uses environment variable for table name
            
            >>> service = IdempotencyService(table_name='custom-table')
            >>> # Uses custom table name
        """
        self.dynamodb = dynamodb_resource or boto3.resource('dynamodb')
        self.table_name = table_name or os.environ.get(
            'IDEMPOTENCY_TABLE_NAME',
            'processed_events'
        )
        self.table = self.dynamodb.Table(self.table_name)
        
        logger.info(f"IdempotencyService initialized with table: {self.table_name}")
    
    def is_processed(self, event_id: str) -> bool:
        """
        Check if an event has already been processed.
        
        This method queries DynamoDB to determine if the event has been
        processed before. It implements a fail-open strategy: if the check
        fails due to DynamoDB errors, it returns False to allow processing
        rather than blocking legitimate requests.
        
        Args:
            event_id: Unique event identifier
            
        Returns:
            True if event was already processed, False otherwise
            
        Example:
            >>> service = IdempotencyService()
            >>> service.is_processed('evt-123')
            False
            >>> service.mark_processed('evt-123')
            >>> service.is_processed('evt-123')
            True
        """
        if not event_id:
            logger.warning("is_processed called with empty event_id")
            return False
        
        try:
            logger.debug(f"Checking idempotency for event {event_id}")
            
            response = self.table.get_item(
                Key={'eventId': event_id}
            )
            
            exists = 'Item' in response
            
            if exists:
                item = response['Item']
                processed_at = item.get('processedAt', 'unknown')
                report_id = item.get('reportId', 'none')
                logger.info(
                    f"Event {event_id} already processed at {processed_at}, "
                    f"report: {report_id}"
                )
            else:
                logger.debug(f"Event {event_id} not yet processed")
            
            return exists
            
        except ClientError as e:
            error_code = e.response.get('Error', {}).get('Code', 'Unknown')
            logger.error(
                f"DynamoDB ClientError checking idempotency for {event_id}: "
                f"{error_code} - {str(e)}"
            )
            # Fail open - allow processing if check fails
            return False
            
        except BotoCoreError as e:
            logger.error(
                f"BotoCore error checking idempotency for {event_id}: {str(e)}"
            )
            return False
            
        except Exception as e:
            logger.error(
                f"Unexpected error checking idempotency for {event_id}: {str(e)}",
                exc_info=True
            )
            return False
    
    def mark_processed(self, event_id: str, report_id: Optional[str] = None):
        """
        Mark an event as processed in DynamoDB.
        
        This method stores a record in DynamoDB indicating that the event
        has been processed. The record includes a TTL for automatic cleanup
        after 30 days. If marking fails, the error is logged but not raised,
        as idempotency tracking failure should not block successful processing.
        
        Args:
            event_id: Unique event identifier
            report_id: Optional report ID that was generated
            
        Example:
            >>> service = IdempotencyService()
            >>> service.mark_processed('evt-123', 'rpt-456')
        """
        if not event_id:
            logger.warning("mark_processed called with empty event_id")
            return
        
        try:
            logger.debug(f"Marking event {event_id} as processed")
            
            # Build item with required fields
            item = {
                'eventId': event_id,
                'processedAt': datetime.utcnow().isoformat(),
                'ttl': self._calculate_ttl()
            }
            
            # Add optional report ID
            if report_id:
                item['reportId'] = report_id
            
            # Store in DynamoDB
            self.table.put_item(Item=item)
            
            logger.info(
                f"Marked event {event_id} as processed"
                f"{f' with report {report_id}' if report_id else ''}"
            )
            
        except ClientError as e:
            error_code = e.response.get('Error', {}).get('Code', 'Unknown')
            logger.error(
                f"DynamoDB ClientError marking event {event_id} as processed: "
                f"{error_code} - {str(e)}"
            )
            # Don't raise - idempotency tracking failure shouldn't block processing
            
        except BotoCoreError as e:
            logger.error(
                f"BotoCore error marking event {event_id} as processed: {str(e)}"
            )
            
        except Exception as e:
            logger.error(
                f"Unexpected error marking event {event_id} as processed: {str(e)}",
                exc_info=True
            )
    
    def _calculate_ttl(self, days: int = 30) -> int:
        """
        Calculate TTL for DynamoDB item.
        
        DynamoDB TTL is specified as a Unix timestamp (seconds since epoch).
        Items with expired TTL are automatically deleted by DynamoDB.
        
        Args:
            days: Number of days until expiration (default: 30)
            
        Returns:
            Unix timestamp for TTL
            
        Example:
            >>> service = IdempotencyService()
            >>> ttl = service._calculate_ttl(30)
            >>> ttl > 0
            True
        """
        ttl_date = datetime.utcnow() + timedelta(days=days)
        return int(ttl_date.timestamp())
    
    def get_processed_event(self, event_id: str) -> Optional[dict]:
        """
        Retrieve processed event details from DynamoDB.
        
        Args:
            event_id: Unique event identifier
            
        Returns:
            Dictionary with event details if found, None otherwise
            
        Example:
            >>> service = IdempotencyService()
            >>> service.mark_processed('evt-123', 'rpt-456')
            >>> event = service.get_processed_event('evt-123')
            >>> event['reportId']
            'rpt-456'
        """
        try:
            response = self.table.get_item(
                Key={'eventId': event_id}
            )
            
            return response.get('Item')
            
        except Exception as e:
            logger.error(f"Error retrieving event {event_id}: {str(e)}")
            return None
