# Task: Setup Lambda Project

**Service**: Notification Service  
**Phase**: 8  
**Estimated Time**: 2 hours  

## Objective

Setup AWS Lambda project structure for the Notification Service using Python 3.11.

## Prerequisites

- [x] Task 001: Clean Architecture layers established
- [x] AWS Lambda Python runtime knowledge

## Scope

**Files to Create**:
- `services/notification-service/requirements.txt`
- `services/notification-service/notification_handler.py`
- `services/notification-service/config.py`

## Implementation Details

### Requirements File

```txt
boto3==1.34.0
jinja2==3.1.3
requests==2.31.0
tenacity==8.2.3
python-json-logger==2.0.7
```

### Lambda Handler

```python
import json
import os
from typing import Dict, Any
import logging

logger = logging.getLogger(__name__)

def lambda_handler(event: Dict[str, Any], context: Any) -> Dict[str, Any]:
    """Main Lambda handler for notification events"""
    logger.info(f'Processing event: {event["detail-type"]}')
    
    event_type = event.get('detail-type')
    
    if event_type == 'ExperimentCompleted':
        from handlers.experiment_completed import handle_experiment_completed
        return handle_experiment_completed(event, context)
    elif event_type == 'ReportGenerated':
        from handlers.report_generated import handle_report_generated
        return handle_report_generated(event, context)
    elif event_type == 'MemberAdded':
        from handlers.member_added import handle_member_added
        return handle_member_added(event, context)
    else:
        logger.warning(f'Unknown event type: {event_type}')
        return {'statusCode': 400, 'body': 'Unknown event type'}
```

## Acceptance Criteria

- [x] Python project configured
- [x] Lambda dependencies added to requirements.txt
- [x] Handler function created
- [x] Project structure established
- [x] Dependencies install successfully

## Testing Requirements

**Unit Tests**:
- Test handler initialization

**Test Files to Create**:
- `test_notification_handler.py`

## References

- Specification: `specs/notification-service.md` (Lambda Configuration section)
- Related Tasks: 002-implement-event-handlers
