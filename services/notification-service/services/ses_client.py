"""
SES client wrapper for sending emails.
Provides abstraction over AWS SES API.
"""
import logging
from typing import List, Optional
import boto3
from botocore.exceptions import ClientError
from tenacity import retry, stop_after_attempt, wait_exponential, retry_if_exception_type

from config import config
from models.email import EmailMessage

logger = logging.getLogger(__name__)


class SesClientError(Exception):
    """Exception raised for SES client errors."""
    pass


class SesClient:
    """
    AWS SES client wrapper for sending emails.
    Implements retry logic and error handling.
    """
    
    def __init__(self, from_email: Optional[str] = None):
        """
        Initialize SES client.
        
        Args:
            from_email: Default from email address, uses config if not provided
        """
        self.ses_client = boto3.client('ses', region_name=config.aws_region)
        self.from_email = from_email or config.ses_from_email
        
        if not self.from_email:
            raise ValueError("FROM_EMAIL must be configured")
    
    @retry(
        stop=stop_after_attempt(3),
        wait=wait_exponential(multiplier=1, min=2, max=10),
        retry=retry_if_exception_type(ClientError),
        reraise=True
    )
    def send_email(self, message: EmailMessage) -> str:
        """
        Send email via AWS SES with retry logic.
        
        Args:
            message: EmailMessage to send
            
        Returns:
            Message ID from SES
            
        Raises:
            SesClientError: If email sending fails after retries
            ValueError: If message validation fails
        """
        # Validate message
        message.validate()
        
        # Use message from_email if provided, otherwise use default
        from_email = message.from_email or self.from_email
        
        try:
            # Build SES request
            request_params = {
                'Source': from_email,
                'Destination': {
                    'ToAddresses': message.recipients
                },
                'Message': {
                    'Subject': {
                        'Data': message.subject,
                        'Charset': 'UTF-8'
                    },
                    'Body': {
                        'Html': {
                            'Data': message.html_body,
                            'Charset': 'UTF-8'
                        }
                    }
                }
            }
            
            # Add text body if provided
            if message.text_body:
                request_params['Message']['Body']['Text'] = {
                    'Data': message.text_body,
                    'Charset': 'UTF-8'
                }
            
            # Add reply-to if provided
            if message.reply_to:
                request_params['ReplyToAddresses'] = [message.reply_to]
            
            # Send email
            response = self.ses_client.send_email(**request_params)
            message_id = response['MessageId']
            
            logger.info(
                'Email sent successfully',
                extra={
                    'message_id': message_id,
                    'recipients': message.recipients,
                    'subject': message.subject
                }
            )
            
            return message_id
            
        except ClientError as e:
            error_code = e.response['Error']['Code']
            error_message = e.response['Error']['Message']
            
            logger.error(
                'SES client error',
                extra={
                    'error_code': error_code,
                    'error_message': error_message,
                    'recipients': message.recipients
                },
                exc_info=True
            )
            
            raise SesClientError(f"Failed to send email: {error_message}") from e
        
        except Exception as e:
            logger.error(
                'Unexpected error sending email',
                extra={'error': str(e), 'recipients': message.recipients},
                exc_info=True
            )
            raise SesClientError(f"Unexpected error: {str(e)}") from e
    
    def send_bulk_email(self, messages: List[EmailMessage]) -> List[str]:
        """
        Send multiple emails.
        
        Args:
            messages: List of EmailMessage objects
            
        Returns:
            List of message IDs
        """
        message_ids = []
        
        for message in messages:
            try:
                message_id = self.send_email(message)
                message_ids.append(message_id)
            except SesClientError as e:
                logger.error(
                    'Failed to send email in bulk operation',
                    extra={'error': str(e)},
                    exc_info=True
                )
                # Continue with other emails
                continue
        
        return message_ids
