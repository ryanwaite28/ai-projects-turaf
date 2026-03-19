"""
Event handler for ExperimentCompleted events.

This module implements the handler for processing ExperimentCompleted events
from EventBridge, following Clean Architecture and DDD principles.
"""

import json
import logging
from typing import Dict, Any
from services.idempotency import IdempotencyService
from services.report_generation import ReportGenerationService

logger = logging.getLogger(__name__)


class ExperimentCompletedHandler:
    """
    Handler for ExperimentCompleted events from EventBridge.
    
    This handler follows the Single Responsibility Principle (SOLID),
    focusing solely on processing ExperimentCompleted events and
    delegating business logic to appropriate services.
    
    Responsibilities:
    - Parse and validate incoming events
    - Check for duplicate processing (idempotency)
    - Delegate report generation to ReportGenerationService
    - Track processing status
    - Handle errors and logging
    
    Attributes:
        report_service: Service for generating reports
        idempotency_service: Service for tracking processed events
    """
    
    def __init__(self):
        """
        Initialize the handler with required services.
        
        Services are injected through constructor, following
        Dependency Inversion Principle (SOLID).
        """
        self.report_service = ReportGenerationService()
        self.idempotency_service = IdempotencyService()
        logger.info("ExperimentCompletedHandler initialized")
    
    def handle(self, event: Dict[str, Any], context: Any) -> Dict[str, Any]:
        """
        Process an ExperimentCompleted event.
        
        This method orchestrates the event processing workflow:
        1. Parse and validate the event
        2. Check if already processed (idempotency)
        3. Generate the report
        4. Mark event as processed
        5. Return success response
        
        Args:
            event: EventBridge event payload with structure:
                {
                    'id': 'event-uuid',
                    'detail-type': 'ExperimentCompleted',
                    'source': 'turaf.experiment-service',
                    'detail': {
                        'eventId': 'uuid',
                        'organizationId': 'org-id',
                        'payload': {
                            'experimentId': 'exp-id',
                            'completedAt': 'ISO-8601',
                            'result': 'SUCCESS|FAILURE'
                        }
                    }
                }
            context: Lambda context object
            
        Returns:
            Response dictionary:
                {
                    'statusCode': 200,
                    'body': JSON string with message and reportId
                }
                
        Raises:
            RuntimeError: If event processing fails
            
        Example:
            >>> handler = ExperimentCompletedHandler()
            >>> event = {
            ...     'id': 'evt-123',
            ...     'detail': {
            ...         'organizationId': 'org-456',
            ...         'payload': {
            ...             'experimentId': 'exp-789',
            ...             'completedAt': '2024-01-01T12:00:00Z'
            ...         }
            ...     }
            ... }
            >>> response = handler.handle(event, None)
            >>> response['statusCode']
            200
        """
        try:
            # Parse event metadata
            event_id = event['id']
            detail = event['detail']
            
            logger.info(f"Processing ExperimentCompleted event: {event_id}")
            
            # Check idempotency - prevent duplicate processing
            if self.idempotency_service.is_processed(event_id):
                logger.info(f"Event already processed: {event_id}")
                return {
                    'statusCode': 200,
                    'body': json.dumps({
                        'message': 'Already processed',
                        'eventId': event_id
                    })
                }
            
            # Extract and validate experiment data
            experiment_event = self._parse_experiment_event(detail)
            
            # Validate required fields
            if not experiment_event.get('experimentId'):
                raise ValueError("experimentId is required in event payload")
            if not experiment_event.get('organizationId'):
                raise ValueError("organizationId is required in event detail")
            
            # Generate report (delegates to application service)
            report = self.report_service.generate_report(experiment_event)
            
            # Mark event as processed for idempotency
            self.idempotency_service.mark_processed(event_id, report['id'])
            
            logger.info(
                f"Report {report['id']} generated successfully for "
                f"experiment {experiment_event['experimentId']}"
            )
            
            return {
                'statusCode': 200,
                'body': json.dumps({
                    'message': 'Report generated successfully',
                    'reportId': report['id'],
                    'experimentId': experiment_event['experimentId']
                })
            }
            
        except KeyError as e:
            logger.error(f"Missing required field in event: {str(e)}", exc_info=True)
            raise RuntimeError(f"Invalid event structure: missing field {str(e)}")
            
        except ValueError as e:
            logger.error(f"Validation error: {str(e)}", exc_info=True)
            raise RuntimeError(f"Event validation failed: {str(e)}")
            
        except Exception as e:
            logger.error(f"Error processing event: {str(e)}", exc_info=True)
            raise RuntimeError(f"Failed to process event: {str(e)}")
    
    def _parse_experiment_event(self, detail: Dict[str, Any]) -> Dict[str, Any]:
        """
        Parse experiment event data from EventBridge detail.
        
        Extracts the relevant experiment information from the nested
        event structure, following the event schema defined in the
        specification.
        
        Args:
            detail: EventBridge event detail containing:
                - organizationId: Organization identifier
                - payload: Nested payload with experiment data
                
        Returns:
            Dictionary with flattened experiment data:
                - experimentId: Experiment identifier
                - organizationId: Organization identifier
                - completedAt: Completion timestamp
                - result: Optional result status
                
        Example:
            >>> detail = {
            ...     'organizationId': 'org-123',
            ...     'payload': {
            ...         'experimentId': 'exp-456',
            ...         'completedAt': '2024-01-01T12:00:00Z',
            ...         'result': 'SUCCESS'
            ...     }
            ... }
            >>> handler = ExperimentCompletedHandler()
            >>> parsed = handler._parse_experiment_event(detail)
            >>> parsed['experimentId']
            'exp-456'
        """
        payload = detail.get('payload', {})
        
        return {
            'experimentId': payload.get('experimentId'),
            'organizationId': detail.get('organizationId'),
            'completedAt': payload.get('completedAt'),
            'result': payload.get('result')
        }
