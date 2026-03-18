"""
Handler for MemberAdded events.
Sends notifications when a member is added to an organization.
"""
import logging
from typing import Dict, Any

logger = logging.getLogger(__name__)


def handle_member_added(event: Dict[str, Any], context: Any) -> Dict[str, Any]:
    """
    Handle MemberAdded event from EventBridge.
    
    This handler:
    1. Extracts member and organization details from event
    2. Sends welcome email to new member
    3. Sends notification to organization admins
    4. Sends webhook notifications
    
    Args:
        event: EventBridge event containing member addition details
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
        member_id = detail.get('memberId')
        organization_id = detail.get('organizationId')
        member_email = detail.get('memberEmail')
        member_name = detail.get('memberName')
        role = detail.get('role')
        
        # Validate required fields
        if not member_id:
            raise ValueError('Missing memberId in event detail')
        if not organization_id:
            raise ValueError('Missing organizationId in event detail')
        if not member_email:
            raise ValueError('Missing memberEmail in event detail')
        
        logger.info(
            'Processing MemberAdded event',
            extra={
                'member_id': member_id,
                'organization_id': organization_id,
                'role': role,
                'event_id': event.get('id')
            }
        )
        
        # TODO: Implement in subsequent tasks:
        # 1. Send welcome email to new member
        # 2. Notify organization admins
        # 3. Send webhook notifications
        
        # Placeholder implementation
        logger.info(
            'MemberAdded notification processing complete',
            extra={
                'member_id': member_id,
                'organization_id': organization_id
            }
        )
        
        return {
            'statusCode': 200,
            'body': 'MemberAdded notification processed successfully'
        }
        
    except ValueError as e:
        logger.error(
            'Validation error in MemberAdded handler',
            extra={'error': str(e)},
            exc_info=True
        )
        return {
            'statusCode': 400,
            'body': f'Validation error: {str(e)}'
        }
    except Exception as e:
        logger.error(
            'Error processing MemberAdded event',
            extra={'error': str(e), 'error_type': type(e).__name__},
            exc_info=True
        )
        raise
