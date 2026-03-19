# Task: Add Idempotency

**Service**: Reporting Service  
**Phase**: 7  
**Estimated Time**: 2 hours  

## Objective

Implement idempotency tracking using DynamoDB to prevent duplicate report generation.

## Prerequisites

- [x] Task 002: Event handler implemented

## Scope

**Files to Create**:
- `services/reporting-service/src/services/idempotency.py`

## Implementation Details

### Idempotency Service

```python
import boto3
import os
import logging
from datetime import datetime, timedelta
from typing import Optional

logger = logging.getLogger(__name__)

class IdempotencyService:
    """Service for tracking processed events to prevent duplicates"""
    
    def __init__(self):
        self.dynamodb = boto3.resource('dynamodb')
        self.table_name = os.environ.get('IDEMPOTENCY_TABLE_NAME', 'processed_events')
        self.table = self.dynamodb.Table(self.table_name)
    
    def is_processed(self, event_id: str) -> bool:
        """
        Check if event has already been processed.
        
        Args:
            event_id: Unique event identifier
            
        Returns:
            True if event was already processed, False otherwise
        """
        try:
            response = self.table.get_item(
                Key={'eventId': event_id}
            )
            
            exists = 'Item' in response
            
            if exists:
                logger.info(f"Event {event_id} already processed")
            
            return exists
            
        except Exception as e:
            logger.error(f"Error checking idempotency for {event_id}: {str(e)}")
            # Fail open - allow processing if check fails
            return False
    
    def mark_processed(self, event_id: str, report_id: Optional[str] = None):
        """
        Mark event as processed in DynamoDB.
        
        Args:
            event_id: Unique event identifier
            report_id: Optional report ID that was generated
        """
        try:
            item = {
                'eventId': event_id,
                'processedAt': datetime.utcnow().isoformat(),
                'ttl': self._calculate_ttl()
            }
            
            if report_id:
                item['reportId'] = report_id
            
            self.table.put_item(Item=item)
            
            logger.info(f"Marked event {event_id} as processed")
            
        except Exception as e:
            logger.error(f"Error marking event {event_id} as processed: {str(e)}")
            # Don't raise - idempotency tracking failure shouldn't block processing
    
    def _calculate_ttl(self) -> int:
        """
        Calculate TTL for DynamoDB item (30 days from now).
        
        Returns:
            Unix timestamp for TTL
        """
        ttl_date = datetime.utcnow() + timedelta(days=30)
        return int(ttl_date.timestamp())
```

### Environment Configuration

```python
# Environment variables required:
# IDEMPOTENCY_TABLE_NAME - DynamoDB table name for idempotency tracking
```

## Acceptance Criteria

- [x] Idempotency check works
- [x] Events marked as processed
- [x] TTL configured (30 days)
- [x] Duplicate processing prevented
- [x] Graceful error handling
- [x] Unit tests pass

## Testing Requirements

**Unit Tests**:
- Test idempotency check (event exists)
- Test idempotency check (event doesn't exist)
- Test mark processed
- Test TTL calculation
- Test error handling

**Test Files to Create**:
- `tests/test_idempotency.py`

## References

- Specification: `specs/reporting-service.md` (Idempotency section)
- Related Tasks: 010-add-unit-tests
