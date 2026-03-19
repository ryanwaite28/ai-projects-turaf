# Task: Implement Event Handler

**Service**: Reporting Service  
**Phase**: 7  
**Estimated Time**: 2-3 hours  

## Objective

Implement EventBridge event handler for ExperimentCompleted events to trigger report generation.

## Prerequisites

- [x] Task 001: Lambda project setup

## Scope

**Files to Create**:
- `services/reporting-service/src/handlers/experiment_completed_handler.py`
- `services/reporting-service/src/models/events.py`
- `services/reporting-service/src/services/report_generation.py`

## Implementation Details

### Event Handler

```python
import json
import logging
from typing import Dict, Any
from services.idempotency import IdempotencyService
from services.report_generation import ReportGenerationService

logger = logging.getLogger(__name__)

class ExperimentCompletedHandler:
    """Handler for ExperimentCompleted events from EventBridge"""
    
    def __init__(self):
        self.report_service = ReportGenerationService()
        self.idempotency_service = IdempotencyService()
    
    def handle(self, event: Dict[str, Any], context: Any) -> Dict[str, Any]:
        """
        Process ExperimentCompleted event.
        
        Args:
            event: EventBridge event payload
            context: Lambda context
            
        Returns:
            Response dictionary
        """
        try:
            # Parse event
            event_id = event['id']
            detail = event['detail']
            
            logger.info(f"Processing ExperimentCompleted event: {event_id}")
            
            # Check idempotency
            if self.idempotency_service.is_processed(event_id):
                logger.info(f"Event already processed: {event_id}")
                return {
                    'statusCode': 200,
                    'body': json.dumps({'message': 'Already processed'})
                }
            
            # Extract experiment data
            experiment_event = self._parse_experiment_event(detail)
            
            # Generate report
            report = self.report_service.generate_report(experiment_event)
            
            # Mark as processed
            self.idempotency_service.mark_processed(event_id, report['id'])
            
            logger.info(f"Report generated for experiment: {experiment_event['experimentId']}")
            
            return {
                'statusCode': 200,
                'body': json.dumps({
                    'message': 'Report generated successfully',
                    'reportId': report['id']
                })
            }
            
        except Exception as e:
            logger.error(f"Error processing event: {str(e)}", exc_info=True)
            raise RuntimeError(f"Failed to process event: {str(e)}")
    
    def _parse_experiment_event(self, detail: Dict[str, Any]) -> Dict[str, Any]:
        """Parse experiment event from detail payload"""
        payload = detail.get('payload', {})
        
        return {
            'experimentId': payload.get('experimentId'),
            'organizationId': detail.get('organizationId'),
            'completedAt': payload.get('completedAt'),
            'result': payload.get('result')
        }
```

### Event Models

```python
from dataclasses import dataclass
from typing import Optional
from datetime import datetime

@dataclass
class ExperimentCompletedEvent:
    """Model for ExperimentCompleted event"""
    event_id: str
    experiment_id: str
    organization_id: str
    completed_at: datetime
    result: Optional[str] = None
    
    @classmethod
    def from_dict(cls, data: dict) -> 'ExperimentCompletedEvent':
        """Create event from dictionary"""
        return cls(
            event_id=data['eventId'],
            experiment_id=data['experimentId'],
            organization_id=data['organizationId'],
            completed_at=datetime.fromisoformat(data['completedAt']),
            result=data.get('result')
        )
```

### Report Generation Service (Stub)

```python
import logging
from typing import Dict, Any
import uuid

logger = logging.getLogger(__name__)

class ReportGenerationService:
    """Service for generating experiment reports"""
    
    def generate_report(self, experiment_event: Dict[str, Any]) -> Dict[str, Any]:
        """
        Generate report for completed experiment.
        
        Args:
            experiment_event: Experiment event data
            
        Returns:
            Generated report metadata
        """
        logger.info(f"Generating report for experiment: {experiment_event['experimentId']}")
        
        # Report generation logic will be implemented in subsequent tasks
        report_id = str(uuid.uuid4())
        
        return {
            'id': report_id,
            'experimentId': experiment_event['experimentId'],
            'organizationId': experiment_event['organizationId'],
            'status': 'generated'
        }
```

## Acceptance Criteria

- [x] Event handler processes ExperimentCompleted events
- [x] Event parsing works correctly
- [x] Idempotency check implemented
- [x] Error handling implemented
- [x] Logging implemented
- [x] Unit tests pass

## Testing Requirements

**Unit Tests**:
- Test event parsing
- Test idempotency check
- Test error handling
- Test successful report generation

**Test Files to Create**:
- `tests/test_experiment_completed_handler.py`

## References

- Specification: `specs/reporting-service.md` (Event Handler section)
- Related Tasks: 003-implement-data-fetching
