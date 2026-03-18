"""
Handler for ExperimentCompleted events.
Sends notifications when an experiment completes.
"""
import logging
from typing import Dict, Any

logger = logging.getLogger(__name__)


def handle_experiment_completed(event: Dict[str, Any], context: Any) -> Dict[str, Any]:
    """
    Handle ExperimentCompleted event from EventBridge.
    
    This handler:
    1. Extracts experiment and organization details from event
    2. Fetches full experiment data from experiment-service
    3. Determines notification recipients based on preferences
    4. Sends email notifications to recipients
    5. Sends webhook notifications to configured endpoints
    
    Args:
        event: EventBridge event containing experiment completion details
        context: Lambda context object
        
    Returns:
        Dict with statusCode and body
        
    Raises:
        ValueError: If required event fields are missing
        Exception: For other processing errors
    """
    try:
        # Extract event details
        detail = event.get('detail', {})
        experiment_id = detail.get('experimentId')
        organization_id = detail.get('organizationId')
        
        # Validate required fields
        if not experiment_id:
            raise ValueError('Missing experimentId in event detail')
        if not organization_id:
            raise ValueError('Missing organizationId in event detail')
        
        logger.info(
            'Processing ExperimentCompleted event',
            extra={
                'experiment_id': experiment_id,
                'organization_id': organization_id,
                'event_id': event.get('id')
            }
        )
        
        # TODO: Implement in subsequent tasks:
        # 1. Fetch experiment details from experiment-service
        # 2. Get notification recipients based on preferences
        # 3. Send email notifications
        # 4. Send webhook notifications
        
        # Placeholder implementation
        logger.info(
            'ExperimentCompleted notification processing complete',
            extra={
                'experiment_id': experiment_id,
                'organization_id': organization_id
            }
        )
        
        return {
            'statusCode': 200,
            'body': 'ExperimentCompleted notification processed successfully'
        }
        
    except ValueError as e:
        logger.error(
            'Validation error in ExperimentCompleted handler',
            extra={'error': str(e)},
            exc_info=True
        )
        return {
            'statusCode': 400,
            'body': f'Validation error: {str(e)}'
        }
    except Exception as e:
        logger.error(
            'Error processing ExperimentCompleted event',
            extra={'error': str(e), 'error_type': type(e).__name__},
            exc_info=True
        )
        raise
