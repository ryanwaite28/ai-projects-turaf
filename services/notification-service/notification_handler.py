"""
Main Lambda handler for notification-service.
Routes EventBridge events to appropriate handlers.
"""
import json
import os
from typing import Dict, Any
import logging
from pythonjsonlogger import jsonlogger

from config import config
from services.idempotency import is_already_processed, mark_as_processed

# Configure structured logging
logger = logging.getLogger()
logHandler = logging.StreamHandler()
formatter = jsonlogger.JsonFormatter()
logHandler.setFormatter(formatter)
logger.addHandler(logHandler)
logger.setLevel(config.log_level)


def lambda_handler(event: Dict[str, Any], context: Any) -> Dict[str, Any]:
    """
    Main Lambda handler for notification events.
    Routes EventBridge events to appropriate handlers based on event type.
    Implements idempotency to prevent duplicate processing.
    
    Args:
        event: EventBridge event containing notification trigger
        context: Lambda context object
        
    Returns:
        Dict containing statusCode and body
        
    Raises:
        ValueError: If event structure is invalid
    """
    try:
        # Validate configuration on cold start
        config.validate()
        
        # Extract event metadata
        event_id = event.get('id')
        event_type = event.get('detail-type')
        
        logger.info(
            'Processing notification event',
            extra={
                'event_id': event_id,
                'event_type': event_type,
                'request_id': context.request_id if context else None
            }
        )
        
        if not event_type:
            logger.error('Missing detail-type in event', extra={'event': event})
            return {
                'statusCode': 400,
                'body': json.dumps({'error': 'Missing detail-type in event'})
            }
        
        # Check idempotency - prevent duplicate processing
        if is_already_processed(event_id):
            logger.info(
                'Event already processed, skipping',
                extra={'event_id': event_id, 'event_type': event_type}
            )
            return {
                'statusCode': 200,
                'body': json.dumps({'message': 'Event already processed'})
            }
        
        # Route to appropriate handler based on event type
        result = None
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
            logger.warning(
                'Unknown event type received',
                extra={'event_type': event_type, 'event_id': event_id}
            )
            return {
                'statusCode': 400,
                'body': json.dumps({'error': f'Unknown event type: {event_type}'})
            }
        
        # Mark event as processed after successful handling
        mark_as_processed(event_id, event_type)
        
        return result
            
    except ValueError as e:
        logger.error(
            'Configuration validation error',
            extra={'error': str(e)},
            exc_info=True
        )
        return {
            'statusCode': 500,
            'body': json.dumps({'error': 'Configuration error'})
        }
    except Exception as e:
        logger.error(
            'Unexpected error processing event',
            extra={'error': str(e), 'error_type': type(e).__name__},
            exc_info=True
        )
        return {
            'statusCode': 500,
            'body': json.dumps({'error': 'Internal server error'})
        }
