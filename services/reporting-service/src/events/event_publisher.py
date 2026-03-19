"""
Event publisher for publishing domain events to EventBridge.

This module provides services for publishing events to AWS EventBridge,
following Clean Architecture and DDD principles.
"""

import boto3
import json
import os
import logging
import uuid
from datetime import datetime
from typing import Dict, Any, Optional
from botocore.exceptions import ClientError, BotoCoreError

logger = logging.getLogger(__name__)


class EventPublisher:
    """
    Service for publishing domain events to AWS EventBridge.
    
    This service acts as an adapter between the application and AWS EventBridge,
    following the Adapter pattern. It provides a clean interface for event
    publishing while handling AWS-specific details internally.
    
    Events follow a standard envelope structure:
    - eventId: Unique event identifier
    - eventType: Type of event (e.g., 'ReportGenerated')
    - eventVersion: Schema version for backward compatibility
    - timestamp: When the event occurred
    - sourceService: Service that published the event
    - organizationId: Tenant identifier
    - payload: Event-specific data
    
    The service follows the Single Responsibility Principle (SOLID),
    focusing solely on event publishing.
    
    Attributes:
        eventbridge_client: Boto3 EventBridge client
        event_bus_name: Name of the EventBridge event bus
    """
    
    def __init__(self, event_bus_name: str = None, eventbridge_client=None):
        """
        Initialize event publisher.
        
        Args:
            event_bus_name: Optional event bus name override.
                           Defaults to EVENT_BUS_NAME env var.
            eventbridge_client: Optional boto3 EventBridge client for testing.
                               Defaults to creating a new client.
                               
        Example:
            >>> publisher = EventPublisher()
            >>> # Uses environment variable for event bus
            
            >>> publisher = EventPublisher(event_bus_name='custom-bus')
            >>> # Uses custom event bus name
        """
        self.eventbridge_client = eventbridge_client or boto3.client('events')
        self.event_bus_name = event_bus_name or os.environ.get(
            'EVENT_BUS_NAME',
            'turaf-event-bus-dev'
        )
        
        logger.info(f"EventPublisher initialized with bus: {self.event_bus_name}")
    
    def publish_report_generated(
        self,
        organization_id: str,
        experiment_id: str,
        report_id: str,
        report_location: str,
        report_format: str = 'PDF'
    ) -> str:
        """
        Publish ReportGenerated event to EventBridge.
        
        This method publishes a domain event indicating that a report has been
        successfully generated and stored. Other services can subscribe to this
        event to trigger downstream actions.
        
        Args:
            organization_id: Organization identifier for tenant isolation
            experiment_id: Experiment identifier
            report_id: Generated report identifier
            report_location: S3 location of the report
            report_format: Format of the report (default: 'PDF')
            
        Returns:
            Event ID of the published event
            
        Raises:
            ValueError: If required parameters are missing
            Exception: If event publishing fails
            
        Example:
            >>> publisher = EventPublisher()
            >>> event_id = publisher.publish_report_generated(
            ...     organization_id='org-123',
            ...     experiment_id='exp-456',
            ...     report_id='rpt-789',
            ...     report_location='s3://bucket/path/report.pdf'
            ... )
            >>> len(event_id) > 0
            True
        """
        # Validate inputs
        if not organization_id:
            raise ValueError("organization_id is required")
        if not experiment_id:
            raise ValueError("experiment_id is required")
        if not report_id:
            raise ValueError("report_id is required")
        if not report_location:
            raise ValueError("report_location is required")
        
        try:
            logger.info(
                f"Publishing ReportGenerated event for experiment {experiment_id}, "
                f"report {report_id}"
            )
            
            # Generate unique event ID
            event_id = str(uuid.uuid4())
            
            # Create event payload
            event_payload = {
                'reportId': report_id,
                'experimentId': experiment_id,
                'reportLocation': report_location,
                'reportFormat': report_format,
                'generatedAt': datetime.utcnow().isoformat()
            }
            
            # Create event envelope following standard structure
            event = {
                'eventId': event_id,
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
            failed_count = response.get('FailedEntryCount', 0)
            if failed_count > 0:
                failed_entries = response.get('Entries', [])
                logger.error(f"Failed to publish event: {failed_entries}")
                raise Exception(f"Event publishing failed: {failed_count} entries failed")
            
            logger.info(
                f"Successfully published ReportGenerated event {event_id} "
                f"for experiment {experiment_id}"
            )
            
            return event_id
            
        except ClientError as e:
            error_code = e.response.get('Error', {}).get('Code', 'Unknown')
            logger.error(
                f"EventBridge ClientError: {error_code} - {str(e)}",
                exc_info=True
            )
            raise Exception(f"Event publishing failed: {error_code} - {str(e)}")
            
        except BotoCoreError as e:
            logger.error(f"BotoCore error publishing event: {str(e)}", exc_info=True)
            raise Exception(f"Event publishing failed: {str(e)}")
            
        except Exception as e:
            logger.error(f"Unexpected error publishing event: {str(e)}", exc_info=True)
            raise
    
    def publish_custom_event(
        self,
        event_type: str,
        organization_id: str,
        payload: Dict[str, Any],
        detail_type: Optional[str] = None
    ) -> str:
        """
        Publish a custom event to EventBridge.
        
        This method provides flexibility to publish any type of event,
        not just ReportGenerated events.
        
        Args:
            event_type: Type of event (e.g., 'ReportFailed')
            organization_id: Organization identifier
            payload: Event-specific data
            detail_type: Optional EventBridge DetailType (defaults to event_type)
            
        Returns:
            Event ID of the published event
            
        Raises:
            ValueError: If required parameters are missing
            Exception: If event publishing fails
        """
        if not event_type:
            raise ValueError("event_type is required")
        if not organization_id:
            raise ValueError("organization_id is required")
        if not payload:
            raise ValueError("payload is required")
        
        try:
            event_id = str(uuid.uuid4())
            
            event = {
                'eventId': event_id,
                'eventType': event_type,
                'eventVersion': 1,
                'timestamp': datetime.utcnow().isoformat(),
                'sourceService': 'reporting-service',
                'organizationId': organization_id,
                'payload': payload
            }
            
            response = self.eventbridge_client.put_events(
                Entries=[
                    {
                        'EventBusName': self.event_bus_name,
                        'Source': 'turaf.reporting-service',
                        'DetailType': detail_type or event_type,
                        'Detail': json.dumps(event)
                    }
                ]
            )
            
            failed_count = response.get('FailedEntryCount', 0)
            if failed_count > 0:
                raise Exception(f"Event publishing failed: {failed_count} entries failed")
            
            logger.info(f"Published {event_type} event {event_id}")
            
            return event_id
            
        except Exception as e:
            logger.error(f"Failed to publish custom event: {str(e)}")
            raise
