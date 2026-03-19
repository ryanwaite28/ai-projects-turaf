# Task: Setup Lambda Project

**Service**: Reporting Service  
**Phase**: 7  
**Estimated Time**: 2 hours  

## Objective

Setup AWS Lambda project structure for the Reporting Service using Python 3.11.

## Prerequisites

- [x] Task 001: Clean Architecture layers established
- [x] AWS Lambda Python runtime knowledge

## Scope

**Files to Create**:
- `services/reporting-service/requirements.txt`
- `services/reporting-service/requirements-dev.txt`
- `services/reporting-service/src/lambda_handler.py`
- `services/reporting-service/src/__init__.py`

## Implementation Details

### Python Dependencies (requirements.txt)

```txt
boto3>=1.28.0
jinja2>=3.1.0
weasyprint>=59.0
requests>=2.31.0
python-json-logger>=2.0.0
tenacity>=8.2.0
```

### Development Dependencies (requirements-dev.txt)

```txt
pytest>=7.4.0
pytest-mock>=3.11.0
pytest-cov>=4.1.0
moto>=4.2.0
black>=23.0.0
flake8>=6.0.0
mypy>=1.5.0
```

### Lambda Handler

```python
import json
import logging
from typing import Dict, Any

logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)

def lambda_handler(event: Dict[str, Any], context: Any) -> Dict[str, Any]:
    """
    Main Lambda handler for EventBridge events.
    
    Args:
        event: EventBridge event payload
        context: Lambda context object
        
    Returns:
        Response dictionary with statusCode and body
    """
    logger.info(f"Received event: {json.dumps(event)}")
    
    try:
        # Event processing will be implemented in subsequent tasks
        event_id = event.get('id')
        detail_type = event.get('detail-type')
        
        logger.info(f"Processing event {event_id} of type {detail_type}")
        
        return {
            'statusCode': 200,
            'body': json.dumps({'message': 'Event received successfully'})
        }
        
    except Exception as e:
        logger.error(f"Error processing event: {str(e)}", exc_info=True)
        return {
            'statusCode': 500,
            'body': json.dumps({'error': str(e)})
        }
```

### Project Structure

```
services/reporting-service/
├── requirements.txt
├── requirements-dev.txt
├── src/
│   ├── __init__.py
│   └── lambda_handler.py
└── tests/
    └── __init__.py
```

## Acceptance Criteria

- [x] Python dependencies configured
- [x] Lambda handler created
- [x] Project structure established
- [x] Handler logs events correctly
- [x] Basic error handling implemented

## Testing Requirements

**Unit Tests**:
- Test handler initialization
- Test event logging
- Test error handling

**Test Files to Create**:
- `tests/test_lambda_handler.py`

## References

- Specification: `specs/reporting-service.md` (Lambda Configuration section)
- Related Tasks: 002-implement-event-handler
