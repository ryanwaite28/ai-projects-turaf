"""
Email service for notification-service.
Orchestrates email sending with template rendering and SES delivery.
"""
import logging
from typing import List, Dict, Any, Optional

from config import config
from models.email import EmailMessage
from services.ses_client import SesClient, SesClientError
from services.template_service import TemplateService

logger = logging.getLogger(__name__)


class EmailService:
    """
    High-level email service.
    Coordinates template rendering and email delivery.
    """
    
    def __init__(
        self,
        ses_client: Optional[SesClient] = None,
        template_service: Optional[TemplateService] = None
    ):
        """
        Initialize email service.
        
        Args:
            ses_client: SES client for sending emails (creates default if None)
            template_service: Template service for rendering (creates default if None)
        """
        self.ses_client = ses_client or SesClient()
        self.template_service = template_service or TemplateService()
    
    def send_experiment_completed_email(
        self,
        recipient_email: str,
        experiment_data: Dict[str, Any]
    ) -> str:
        """
        Send experiment completed notification email.
        
        Args:
            recipient_email: Email address of recipient
            experiment_data: Dictionary containing experiment details
                Required keys: experimentId, experimentName
                Optional keys: organizationId, completedAt
        
        Returns:
            Message ID from SES
            
        Raises:
            SesClientError: If email sending fails
        """
        try:
            # Prepare template data
            template_data = {
                'experiment_id': experiment_data.get('experimentId'),
                'experiment_name': experiment_data.get('experimentName', 'Your Experiment'),
                'organization_id': experiment_data.get('organizationId'),
                'completed_at': experiment_data.get('completedAt'),
                'frontend_url': config.frontend_url
            }
            
            # Render template
            html_body = self.template_service.render(
                'email/experiment-completed',
                template_data
            )
            
            # Create email message
            message = EmailMessage(
                recipients=[recipient_email],
                subject='Experiment Completed Successfully',
                html_body=html_body
            )
            
            # Send email
            message_id = self.ses_client.send_email(message)
            
            logger.info(
                'Experiment completed email sent',
                extra={
                    'recipient': recipient_email,
                    'experiment_id': experiment_data.get('experimentId'),
                    'message_id': message_id
                }
            )
            
            return message_id
            
        except Exception as e:
            logger.error(
                'Failed to send experiment completed email',
                extra={
                    'recipient': recipient_email,
                    'experiment_id': experiment_data.get('experimentId'),
                    'error': str(e)
                },
                exc_info=True
            )
            raise
    
    def send_report_generated_email(
        self,
        recipient_email: str,
        report_data: Dict[str, Any]
    ) -> str:
        """
        Send report generated notification email.
        
        Args:
            recipient_email: Email address of recipient
            report_data: Dictionary containing report details
                Required keys: reportId, experimentId, reportUrl
                Optional keys: experimentName, generatedAt
        
        Returns:
            Message ID from SES
        """
        try:
            # Prepare template data
            template_data = {
                'report_id': report_data.get('reportId'),
                'experiment_id': report_data.get('experimentId'),
                'experiment_name': report_data.get('experimentName', 'Your Experiment'),
                'report_url': report_data.get('reportUrl'),
                'generated_at': report_data.get('generatedAt'),
                'frontend_url': config.frontend_url
            }
            
            # Render template
            html_body = self.template_service.render(
                'email/report-generated',
                template_data
            )
            
            # Create email message
            message = EmailMessage(
                recipients=[recipient_email],
                subject='Experiment Report Available',
                html_body=html_body
            )
            
            # Send email
            message_id = self.ses_client.send_email(message)
            
            logger.info(
                'Report generated email sent',
                extra={
                    'recipient': recipient_email,
                    'report_id': report_data.get('reportId'),
                    'message_id': message_id
                }
            )
            
            return message_id
            
        except Exception as e:
            logger.error(
                'Failed to send report generated email',
                extra={
                    'recipient': recipient_email,
                    'report_id': report_data.get('reportId'),
                    'error': str(e)
                },
                exc_info=True
            )
            raise
    
    def send_member_added_email(
        self,
        recipient_email: str,
        member_data: Dict[str, Any]
    ) -> str:
        """
        Send member added notification email.
        
        Args:
            recipient_email: Email address of new member
            member_data: Dictionary containing member details
                Required keys: organizationId, organizationName
                Optional keys: memberName, role, invitedBy
        
        Returns:
            Message ID from SES
        """
        try:
            # Prepare template data
            template_data = {
                'organization_id': member_data.get('organizationId'),
                'organization_name': member_data.get('organizationName', 'the organization'),
                'member_name': member_data.get('memberName', 'there'),
                'role': member_data.get('role', 'Member'),
                'invited_by': member_data.get('invitedBy'),
                'frontend_url': config.frontend_url
            }
            
            # Render template
            html_body = self.template_service.render(
                'email/member-added',
                template_data
            )
            
            # Create email message
            message = EmailMessage(
                recipients=[recipient_email],
                subject=f"Welcome to {template_data['organization_name']}",
                html_body=html_body
            )
            
            # Send email
            message_id = self.ses_client.send_email(message)
            
            logger.info(
                'Member added email sent',
                extra={
                    'recipient': recipient_email,
                    'organization_id': member_data.get('organizationId'),
                    'message_id': message_id
                }
            )
            
            return message_id
            
        except Exception as e:
            logger.error(
                'Failed to send member added email',
                extra={
                    'recipient': recipient_email,
                    'organization_id': member_data.get('organizationId'),
                    'error': str(e)
                },
                exc_info=True
            )
            raise
    
    def send_bulk_emails(
        self,
        recipients: List[str],
        subject: str,
        template_name: str,
        template_data: Dict[str, Any]
    ) -> List[str]:
        """
        Send same email to multiple recipients.
        
        Args:
            recipients: List of email addresses
            subject: Email subject
            template_name: Name of template to render
            template_data: Data for template rendering
        
        Returns:
            List of message IDs
        """
        try:
            # Render template once
            html_body = self.template_service.render(template_name, template_data)
            
            # Create messages for each recipient
            messages = [
                EmailMessage(
                    recipients=[recipient],
                    subject=subject,
                    html_body=html_body
                )
                for recipient in recipients
            ]
            
            # Send bulk
            message_ids = self.ses_client.send_bulk_email(messages)
            
            logger.info(
                'Bulk emails sent',
                extra={
                    'recipient_count': len(recipients),
                    'success_count': len(message_ids)
                }
            )
            
            return message_ids
            
        except Exception as e:
            logger.error(
                'Failed to send bulk emails',
                extra={'error': str(e)},
                exc_info=True
            )
            raise
