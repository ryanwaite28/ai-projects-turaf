"""
Handler for ExperimentCompleted events.
Sends notifications when an experiment completes.
"""
import json
import logging
from typing import Dict, Any

from services.recipient_service import RecipientService
from services.email_service import EmailService
from services.webhook_service import WebhookService

logger = logging.getLogger(__name__)


def handle_experiment_completed(event: Dict[str, Any], context: Any) -> Dict[str, Any]:
    """
    Handle ExperimentCompleted event from EventBridge.

    Event envelope structure (turaf standard):
        {
            "id": "event-uuid",
            "detail-type": "ExperimentCompleted",
            "detail": {
                "organizationId": "org-id",
                "payload": {
                    "experimentId": "exp-id",
                    "experimentName": "optional name",
                    "completedAt": "ISO-8601",
                    "result": "SUCCESS|FAILURE"
                }
            }
        }

    Pipeline:
        1. Extract organization and experiment details from event envelope
        2. Resolve notification recipients via RecipientService
        3. Send email to each recipient via EmailService
        4. Deliver webhooks via WebhookService

    Args:
        event: EventBridge event containing experiment completion details
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
        experiment_id = payload.get('experimentId')

        # Validate required fields
        if not experiment_id:
            raise ValueError('Missing experimentId in event detail.payload')
        if not organization_id:
            raise ValueError('Missing organizationId in event detail')

        logger.info(
            'Processing ExperimentCompleted event',
            extra={
                'experiment_id': experiment_id,
                'organization_id': organization_id,
                'event_id': event_id
            }
        )

        # Build normalized experiment data for templates
        experiment_data = {
            'experimentId': experiment_id,
            'experimentName': payload.get('experimentName', f'Experiment {experiment_id}'),
            'organizationId': organization_id,
            'completedAt': payload.get('completedAt'),
            'result': payload.get('result'),
        }

        email_count = 0
        email_errors = 0

        # Resolve recipients and send email notifications
        recipient_service = RecipientService()
        try:
            recipients = recipient_service.get_recipients(
                organization_id, 'experiment.completed', channel='email'
            )

            email_service = EmailService()
            for recipient_email in recipients:
                try:
                    email_service.send_experiment_completed_email(
                        recipient_email, experiment_data
                    )
                    email_count += 1
                except Exception as e:
                    email_errors += 1
                    logger.error(
                        'Failed to send experiment completed email',
                        extra={
                            'recipient': recipient_email,
                            'experiment_id': experiment_id,
                            'error': str(e)
                        },
                        exc_info=True
                    )
        finally:
            recipient_service.close()

        # Deliver webhook notifications
        webhook_service = WebhookService()
        try:
            webhook_results = webhook_service.send_webhooks(
                organization_id=organization_id,
                event_type='experiment.completed',
                event_id=event_id,
                event_data=experiment_data
            )
            webhook_count = sum(1 for r in webhook_results if r.success)
        finally:
            webhook_service.close()

        logger.info(
            'ExperimentCompleted notification processing complete',
            extra={
                'experiment_id': experiment_id,
                'organization_id': organization_id,
                'emails_sent': email_count,
                'email_errors': email_errors,
                'webhooks_delivered': webhook_count
            }
        )

        return {
            'statusCode': 200,
            'body': json.dumps({
                'message': 'ExperimentCompleted notification processed successfully',
                'experimentId': experiment_id,
                'emailsSent': email_count,
                'webhooksDelivered': webhook_count
            })
        }

    except ValueError as e:
        logger.error(
            'Validation error in ExperimentCompleted handler',
            extra={'error': str(e)},
            exc_info=True
        )
        return {
            'statusCode': 400,
            'body': json.dumps({'error': f'Validation error: {str(e)}'})
        }
    except Exception as e:
        logger.error(
            'Error processing ExperimentCompleted event',
            extra={'error': str(e), 'error_type': type(e).__name__},
            exc_info=True
        )
        raise
