"""
Recipient service for notification-service.
Determines who should receive notifications based on preferences.
"""
import logging
from typing import List, Optional, Set

from models.notification_preference import MemberDto
from clients.organization_client import OrganizationClient, OrganizationClientError
from clients.user_client import UserClient, UserClientError

logger = logging.getLogger(__name__)


class RecipientService:
    """
    Service for selecting notification recipients.
    Filters recipients based on organization membership and user preferences.
    """
    
    def __init__(
        self,
        organization_client: Optional[OrganizationClient] = None,
        user_client: Optional[UserClient] = None
    ):
        """
        Initialize recipient service.
        
        Args:
            organization_client: Client for organization service
            user_client: Client for user service
        """
        self.organization_client = organization_client or OrganizationClient()
        self.user_client = user_client or UserClient()
    
    def get_recipients(
        self,
        organization_id: str,
        event_type: str,
        channel: str = 'email'
    ) -> List[str]:
        """
        Get list of recipient email addresses for a notification.
        
        Args:
            organization_id: Organization ID
            event_type: Type of event (e.g., 'experiment.completed')
            channel: Notification channel (default: 'email')
            
        Returns:
            List of email addresses that should receive the notification
        """
        try:
            logger.info(
                'Getting recipients',
                extra={
                    'organization_id': organization_id,
                    'event_type': event_type,
                    'channel': channel
                }
            )
            
            # Get organization members
            members = self.organization_client.get_members(organization_id)
            
            if not members:
                logger.info(
                    'No members found for organization',
                    extra={'organization_id': organization_id}
                )
                return []
            
            # Filter members based on preferences
            recipients = []
            for member in members:
                if self._should_notify(member.user_id, event_type, channel):
                    recipients.append(member.email)
            
            logger.info(
                'Recipients selected',
                extra={
                    'organization_id': organization_id,
                    'event_type': event_type,
                    'total_members': len(members),
                    'recipients': len(recipients)
                }
            )
            
            return recipients
            
        except OrganizationClientError as e:
            logger.error(
                'Error fetching organization members',
                extra={
                    'organization_id': organization_id,
                    'error': str(e)
                },
                exc_info=True
            )
            # Return empty list on error
            return []
        
        except Exception as e:
            logger.error(
                'Unexpected error getting recipients',
                extra={
                    'organization_id': organization_id,
                    'event_type': event_type,
                    'error': str(e)
                },
                exc_info=True
            )
            return []
    
    def get_recipient_members(
        self,
        organization_id: str,
        event_type: str,
        channel: str = 'email'
    ) -> List[MemberDto]:
        """
        Get list of recipient members (with full member data).
        
        Args:
            organization_id: Organization ID
            event_type: Type of event
            channel: Notification channel
            
        Returns:
            List of MemberDto objects that should receive the notification
        """
        try:
            members = self.organization_client.get_members(organization_id)
            
            recipients = [
                member for member in members
                if self._should_notify(member.user_id, event_type, channel)
            ]
            
            return recipients
            
        except Exception as e:
            logger.error(
                'Error getting recipient members',
                extra={
                    'organization_id': organization_id,
                    'error': str(e)
                },
                exc_info=True
            )
            return []
    
    def _should_notify(
        self,
        user_id: str,
        event_type: str,
        channel: str = 'email'
    ) -> bool:
        """
        Check if a user should be notified for an event.
        
        Args:
            user_id: User ID
            event_type: Type of event
            channel: Notification channel
            
        Returns:
            True if user should be notified, False otherwise
        """
        try:
            # Get user preferences
            preferences = self.user_client.get_user_preferences(user_id)
            
            # Check if user should be notified
            should_notify = preferences.should_notify(event_type, channel)
            
            logger.debug(
                'Checked notification preference',
                extra={
                    'user_id': user_id,
                    'event_type': event_type,
                    'channel': channel,
                    'should_notify': should_notify
                }
            )
            
            return should_notify
            
        except Exception as e:
            logger.warning(
                'Error checking user preferences, defaulting to notify',
                extra={
                    'user_id': user_id,
                    'event_type': event_type,
                    'error': str(e)
                }
            )
            # Default to notifying on error (fail open)
            return True
    
    def get_recipients_by_role(
        self,
        organization_id: str,
        event_type: str,
        roles: List[str],
        channel: str = 'email'
    ) -> List[str]:
        """
        Get recipients filtered by role.
        
        Args:
            organization_id: Organization ID
            event_type: Type of event
            roles: List of roles to include (e.g., ['ADMIN', 'OWNER'])
            channel: Notification channel
            
        Returns:
            List of email addresses for members with specified roles
        """
        try:
            members = self.organization_client.get_members(organization_id)
            
            # Filter by role and preferences
            recipients = [
                member.email
                for member in members
                if member.role in roles
                and self._should_notify(member.user_id, event_type, channel)
            ]
            
            logger.info(
                'Recipients selected by role',
                extra={
                    'organization_id': organization_id,
                    'roles': roles,
                    'recipients': len(recipients)
                }
            )
            
            return recipients
            
        except Exception as e:
            logger.error(
                'Error getting recipients by role',
                extra={
                    'organization_id': organization_id,
                    'roles': roles,
                    'error': str(e)
                },
                exc_info=True
            )
            return []
    
    def close(self):
        """Close client resources."""
        if self.organization_client:
            self.organization_client.close()
        if self.user_client:
            self.user_client.close()
