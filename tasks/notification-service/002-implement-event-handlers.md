# Task: Implement Event Handlers

**Service**: Notification Service  
**Phase**: 8  
**Estimated Time**: 3 hours  

## Objective

Implement EventBridge event handlers for various domain events that trigger notifications.

## Prerequisites

- [x] Task 001: Lambda project setup

## Scope

**Files to Create**:
- `services/notification-service/handlers/experiment_completed.py`
- `services/notification-service/handlers/report_generated.py`
- `services/notification-service/handlers/member_added.py`
- `services/notification-service/services/idempotency.py`

## Implementation Details

### Main Lambda Handler

```python
import json
from typing import Dict, Any
import logging
from services.idempotency import is_already_processed, mark_as_processed

logger = logging.getLogger(__name__)

def lambda_handler(event: Dict[str, Any], context: Any) -> Dict[str, Any]:
    """Route EventBridge events to appropriate handlers"""
    try:
        event_type = event.get('detail-type')
        event_id = event['id']
        
        # Check idempotency
        if is_already_processed(event_id):
            logger.info(f'Event already processed: {event_id}')
            return {'statusCode': 200, 'body': 'Already processed'}
        
        # Route to appropriate handler
        if event_type == 'ExperimentCompleted':
            from handlers.experiment_completed import handle_experiment_completed
            result = handle_experiment_completed(event, context)
        elif event_type == 'ReportGenerated':
            from handlers.report_generated import handle_report_generated
            result = handle_report_generated(event, context)
        elif event_type == 'MemberAdded':
            from handlers.member_added import handle_member_added
            result = handle_member_added(event, context)
        else:
            logger.warning(f'No handler for event type: {event_type}')
            return {'statusCode': 400, 'body': 'Unknown event type'}
        
        # Mark as processed
        mark_as_processed(event_id, event_type)
        return result
        
    except Exception as e:
        logger.error(f'Error processing event: {str(e)}', exc_info=True)
        raise
```

### Experiment Completed Handler

```python
from typing import Dict, Any
import logging
from services.email_service import send_experiment_completed_email
from services.webhook_service import send_webhooks
from clients.experiment_client import get_experiment
from clients.user_client import get_recipients

logger = logging.getLogger(__name__)

def handle_experiment_completed(event: Dict[str, Any], context: Any) -> Dict[str, Any]:
    """Handle ExperimentCompleted event"""
    detail = event['detail']
    experiment_id = detail['experimentId']
    organization_id = detail['organizationId']
    
    # Fetch experiment details
    experiment = get_experiment(experiment_id)
    
    # Get notification recipients
    recipients = get_recipients(experiment)
    
    # Send email notifications
    for user in recipients:
        send_experiment_completed_email(user, experiment)
    
    # Send webhook notifications
    send_webhooks(organization_id, 'experiment.completed', detail)
    
    logger.info(f'Sent notifications for experiment {experiment_id}')
    return {'statusCode': 200, 'body': 'Success'}
```

## Acceptance Criteria

- [x] Event router dispatches to correct handlers
- [x] All event handlers implemented
- [x] Idempotency check works
- [x] Error handling implemented
- [x] Unit tests pass

## Testing Requirements

**Unit Tests**:
- Test event routing
- Test each handler
- Test idempotency

**Test Files to Create**:
- `test_lambda_handler.py`
- `test_experiment_completed_handler.py`

## References

- Specification: `specs/notification-service.md` (Event Handlers section)
- Related Tasks: 003-implement-email-service
