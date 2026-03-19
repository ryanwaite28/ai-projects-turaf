# Task: Implement Event Publishing

**Service**: Reporting Service  
**Phase**: 7  
**Estimated Time**: 2 hours  

## Objective

Implement EventBridge event publishing for ReportGenerated events.

## Prerequisites

- [x] Task 007: S3 storage implemented

## Scope

**Files to Create**:
- `services/reporting-service/src/events/event_publisher.py`
- `services/reporting-service/src/models/events.py`

## Implementation Details

### Event Publisher

```python
import boto3
import json
import os
import logging
import uuid
from datetime import datetime
from typing import Dict, Any
from botocore.exceptions import ClientError

logger = logging.getLogger(__name__)

class EventPublisher:
    """Publisher for EventBridge events"""
    
    def __init__(self):
        self.eventbridge_client = boto3.client('events')
        self.event_bus_name = os.environ.get('EVENT_BUS_NAME', 'turaf-event-bus-dev')
        
        logger.info(f"Event publisher initialized with bus: {self.event_bus_name}")
    
    def publish_report_generated(self, organization_id: str, experiment_id: str, 
                                report_id: str, report_location: str):
        """
        Publish ReportGenerated event to EventBridge.
        
        Args:
            organization_id: Organization identifier
            experiment_id: Experiment identifier
            report_id: Generated report identifier
            report_location: S3 location of report
        """
        try:
            # Create event payload
            event_payload = {
                'reportId': report_id,
                'experimentId': experiment_id,
                'reportLocation': report_location,
                'reportFormat': 'PDF',
                'generatedAt': datetime.utcnow().isoformat()
            }
            
            # Create event envelope
            event = {
                'eventId': str(uuid.uuid4()),
                'eventType': 'ReportGenerated',
                'eventVersion': 1,
                'timestamp': datetime.utcnow().isoformat(),
                'sourceService': 'reporting-service',
                'organizationId': organization_id,
                'payload': event_payload
            }
            
            # Publish to EventBridge
            response = self.eventbridge_client.put_events(
                Entries=[
                    {
                        'EventBusName': self.event_bus_name,
                        'Source': 'turaf.reporting-service',
                        'DetailType': 'ReportGenerated',
                        'Detail': json.dumps(event)
                    }
                ]
            )
            
            # Check for failures
            if response.get('FailedEntryCount', 0) > 0:
                failed_entries = response.get('Entries', [])
                logger.error(f"Failed to publish event: {failed_entries}")
                raise Exception(f"Event publishing failed: {failed_entries}")
            
            logger.info(f"Published ReportGenerated event for experiment {experiment_id}")
            
        except ClientError as e:
            logger.error(f"Failed to publish event: {str(e)}")
            raise Exception(f"Event publishing failed: {str(e)}")
```

### Event Models

```python
from dataclasses import dataclass, asdict
from datetime import datetime
from typing import Optional
import json

@dataclass
class ReportGeneratedEvent:
    """Model for ReportGenerated event"""
    event_id: str
    organization_id: str
    experiment_id: str
    report_id: str
    report_location: str
    report_format: str
    generated_at: datetime
    
    def to_dict(self) -> dict:
        """Convert event to dictionary"""
        data = asdict(self)
        data['generated_at'] = self.generated_at.isoformat()
        return data
    
    def to_json(self) -> str:
        """Convert event to JSON string"""
        return json.dumps(self.to_dict())
```

## Acceptance Criteria

- [x] Event publisher implemented
- [x] ReportGenerated events published
- [x] Event structure correct (envelope + payload)
- [x] Event source set to "turaf.reporting-service"
- [x] Failed entry count checked
- [x] Error handling implemented
- [x] Unit tests pass

## Testing Requirements

**Unit Tests**:
- Test event publishing success
- Test event serialization
- Test failed entry handling
- Test event structure
- Use moto for EventBridge mocking

**Test Files to Create**:
- `tests/test_event_publisher.py`

## References

- Specification: `specs/reporting-service.md` (Event Publishing section)
- Specification: `specs/event-schemas.md`
- Related Tasks: 009-add-idempotency
