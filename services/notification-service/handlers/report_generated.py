"""
Handler for ReportGenerated events.
Sends notifications when a report is generated.
"""
import logging
from typing import Dict, Any

logger = logging.getLogger(__name__)


def handle_report_generated(event: Dict[str, Any], context: Any) -> Dict[str, Any]:
    """
    Handle ReportGenerated event from EventBridge.
    
    This handler:
    1. Extracts report and experiment details from event
    2. Determines notification recipients
    3. Sends email notifications with report link
    4. Sends webhook notifications
    
    Args:
        event: EventBridge event containing report generation details
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
        report_id = detail.get('reportId')
        experiment_id = detail.get('experimentId')
        organization_id = detail.get('organizationId')
        report_url = detail.get('reportUrl')
        
        # Validate required fields
        if not report_id:
            raise ValueError('Missing reportId in event detail')
        if not experiment_id:
            raise ValueError('Missing experimentId in event detail')
        if not organization_id:
            raise ValueError('Missing organizationId in event detail')
        
        logger.info(
            'Processing ReportGenerated event',
            extra={
                'report_id': report_id,
                'experiment_id': experiment_id,
                'organization_id': organization_id,
                'event_id': event.get('id')
            }
        )
        
        # TODO: Implement in subsequent tasks:
        # 1. Get notification recipients
        # 2. Send email notifications with report link
        # 3. Send webhook notifications
        
        # Placeholder implementation
        logger.info(
            'ReportGenerated notification processing complete',
            extra={
                'report_id': report_id,
                'experiment_id': experiment_id
            }
        )
        
        return {
            'statusCode': 200,
            'body': 'ReportGenerated notification processed successfully'
        }
        
    except ValueError as e:
        logger.error(
            'Validation error in ReportGenerated handler',
            extra={'error': str(e)},
            exc_info=True
        )
        return {
            'statusCode': 400,
            'body': f'Validation error: {str(e)}'
        }
    except Exception as e:
        logger.error(
            'Error processing ReportGenerated event',
            extra={'error': str(e), 'error_type': type(e).__name__},
            exc_info=True
        )
        raise
