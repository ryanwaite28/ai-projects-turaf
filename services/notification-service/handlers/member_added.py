"""
Handler for MemberAdded events.
Sends notifications when a member is added to an organization.
"""
import json
import logging
from typing import Dict, Any

from services.email_service import EmailService
from services.webhook_service import WebhookService
from clients.organization_client import OrganizationClient, OrganizationClientError

logger = logging.getLogger(__name__)


def handle_member_added(event: Dict[str, Any], context: Any) -> Dict[str, Any]:
    """
    Handle MemberAdded event from EventBridge.

    Event envelope structure (turaf standard, published by organization-service):
        {
            "id": "event-uuid",
            "detail-type": "MemberAdded",
            "detail": {
                "organizationId": "org-id",
                "payload": {
                    "memberId": "user-id",
                    "memberEmail": "user@example.com",
                    "memberName": "Jane Doe",
                    "role": "MEMBER|ADMIN|OWNER",
                    "invitedBy": "inviter-name (optional)"
                }
            }
        }

    Pipeline:
        1. Extract member and organization details from event envelope
        2. Fetch organization name for the welcome email
        3. Send welcome email directly to the new member
        4. Deliver webhooks to organization-configured endpoints

    Note: Recipients are not resolved via RecipientService here because the
    target is the new member themselves — not a filtered org-wide recipient list.

    Args:
        event: EventBridge event containing member addition details
        context: Lambda context object

    Returns:
        Dict with statusCode and body
    """
    try:
        # Extract from standard event envelope
        detail = event.get('detail', {})
        payload = detail.get('payload', {})
        event_id = event.get('id')

        organization_id = detail.get('organizationId')
        member_id = payload.get('memberId')
        member_email = payload.get('memberEmail')
        member_name = payload.get('memberName')
        role = payload.get('role', 'MEMBER')

        # Validate required fields
        if not member_id:
            raise ValueError('Missing memberId in event detail.payload')
        if not organization_id:
            raise ValueError('Missing organizationId in event detail')
        if not member_email:
            raise ValueError('Missing memberEmail in event detail.payload')

        logger.info(
            'Processing MemberAdded event',
            extra={
                'member_id': member_id,
                'organization_id': organization_id,
                'role': role,
                'event_id': event_id
            }
        )

        # Attempt to fetch organization name for the welcome email
        organization_name = f'Organization {organization_id}'
        org_client = OrganizationClient()
        try:
            org_data = org_client.get_organization(organization_id)
            organization_name = org_data.get('name', organization_name)
        except OrganizationClientError as e:
            logger.warning(
                'Could not fetch organization name, using fallback',
                extra={'organization_id': organization_id, 'error': str(e)}
            )
        finally:
            org_client.close()

        # Build normalized member data for templates
        member_data = {
            'memberId': member_id,
            'memberEmail': member_email,
            'memberName': member_name or member_email,
            'organizationId': organization_id,
            'organizationName': organization_name,
            'role': role,
            'invitedBy': payload.get('invitedBy'),
        }

        email_sent = False
        webhook_count = 0

        # Send welcome email directly to the new member
        email_service = EmailService()
        try:
            email_service.send_member_added_email(member_email, member_data)
            email_sent = True
            logger.info(
                'Welcome email sent to new member',
                extra={'member_email': member_email, 'organization_id': organization_id}
            )
        except Exception as e:
            logger.error(
                'Failed to send member welcome email',
                extra={
                    'member_email': member_email,
                    'organization_id': organization_id,
                    'error': str(e)
                },
                exc_info=True
            )

        # Deliver webhook notifications
        webhook_service = WebhookService()
        try:
            webhook_results = webhook_service.send_webhooks(
                organization_id=organization_id,
                event_type='member.added',
                event_id=event_id,
                event_data=member_data
            )
            webhook_count = sum(1 for r in webhook_results if r.success)
        finally:
            webhook_service.close()

        logger.info(
            'MemberAdded notification processing complete',
            extra={
                'member_id': member_id,
                'organization_id': organization_id,
                'email_sent': email_sent,
                'webhooks_delivered': webhook_count
            }
        )

        return {
            'statusCode': 200,
            'body': json.dumps({
                'message': 'MemberAdded notification processed successfully',
                'memberId': member_id,
                'emailSent': email_sent,
                'webhooksDelivered': webhook_count
            })
        }

    except ValueError as e:
        logger.error(
            'Validation error in MemberAdded handler',
            extra={'error': str(e)},
            exc_info=True
        )
        return {
            'statusCode': 400,
            'body': json.dumps({'error': f'Validation error: {str(e)}'})
        }
    except Exception as e:
        logger.error(
            'Error processing MemberAdded event',
            extra={'error': str(e), 'error_type': type(e).__name__},
            exc_info=True
        )
        raise
