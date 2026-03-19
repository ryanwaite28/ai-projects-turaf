"""
Main Lambda handler for the Reporting Service.

This module serves as the entry point for AWS Lambda, processing EventBridge
events to generate experiment reports.
"""

import json
import logging
from typing import Dict, Any
from handlers.experiment_completed_handler import ExperimentCompletedHandler

logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)

# Initialize handler at module level for Lambda container reuse
experiment_handler = ExperimentCompletedHandler()


def lambda_handler(event: Dict[str, Any], context: Any) -> Dict[str, Any]:
    """
    Main Lambda handler for EventBridge events.
    
    This handler receives events from EventBridge and processes them to generate
    experiment reports. It follows the event-driven architecture pattern and
    maintains idempotency through event ID tracking.
    
    Args:
        event: EventBridge event payload containing:
            - id: Unique event identifier
            - detail-type: Type of event (e.g., "ExperimentCompleted")
            - source: Event source (e.g., "turaf.experiment-service")
            - detail: Event-specific data
        context: Lambda context object with runtime information
        
    Returns:
        Response dictionary with:
            - statusCode: HTTP status code (200 for success, 500 for error)
            - body: JSON string with response message
            
    Example:
        >>> event = {
        ...     'id': 'event-123',
        ...     'detail-type': 'ExperimentCompleted',
        ...     'source': 'turaf.experiment-service',
        ...     'detail': {'experimentId': 'exp-456'}
        ... }
        >>> response = lambda_handler(event, context)
        >>> response['statusCode']
        200
    """
    logger.info(f"Received event: {json.dumps(event)}")
    
    try:
        # Extract event metadata
        event_id = event.get('id')
        detail_type = event.get('detail-type')
        source = event.get('source')
        
        if not event_id:
            logger.error("Event missing required 'id' field")
            return {
                'statusCode': 400,
                'body': json.dumps({'error': 'Event ID is required'})
            }
        
        logger.info(f"Processing event {event_id} of type {detail_type} from {source}")
        
        # Route to appropriate handler based on event type
        if detail_type == 'ExperimentCompleted':
            return experiment_handler.handle(event, context)
        else:
            logger.warning(f"Unhandled event type: {detail_type}")
            return {
                'statusCode': 200,
                'body': json.dumps({
                    'message': f'Event type {detail_type} not handled',
                    'eventId': event_id
                })
            }
        
    except KeyError as e:
        logger.error(f"Missing required field in event: {str(e)}", exc_info=True)
        return {
            'statusCode': 400,
            'body': json.dumps({'error': f'Missing required field: {str(e)}'})
        }
        
    except Exception as e:
        logger.error(f"Error processing event: {str(e)}", exc_info=True)
        return {
            'statusCode': 500,
            'body': json.dumps({'error': 'Internal server error'})
        }
