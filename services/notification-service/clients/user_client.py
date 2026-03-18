"""
User service client for fetching user preferences and data.
"""
import logging
from typing import Optional, Dict, Any
import requests
from requests.exceptions import RequestException, Timeout

from config import config
from models.notification_preference import NotificationPreference, UserPreferences

logger = logging.getLogger(__name__)


class UserClientError(Exception):
    """Exception raised for user client errors."""
    pass


class UserClient:
    """
    HTTP client for user/identity service API.
    Fetches user preferences and profile data.
    """
    
    DEFAULT_TIMEOUT = 10  # seconds
    
    def __init__(
        self,
        base_url: Optional[str] = None,
        timeout: int = DEFAULT_TIMEOUT
    ):
        """
        Initialize user client.
        
        Args:
            base_url: Base URL for user service (uses experiment service URL if None)
            timeout: Request timeout in seconds
        """
        # Note: In production, this would be a dedicated user/identity service
        # For now, using experiment service as placeholder
        self.base_url = base_url or config.experiment_service_url
        self.timeout = timeout
        self.session = requests.Session()
        
        if not self.base_url:
            raise ValueError("User service URL must be configured")
        
        self.base_url = self.base_url.rstrip('/')
    
    def get_user_preferences(self, user_id: str) -> UserPreferences:
        """
        Get notification preferences for a user.
        
        Args:
            user_id: User ID
            
        Returns:
            UserPreferences object
            
        Raises:
            UserClientError: If request fails
        """
        if not user_id:
            raise ValueError("User ID is required")
        
        url = f"{self.base_url}/api/v1/users/{user_id}/preferences"
        
        try:
            logger.debug(
                'Fetching user preferences',
                extra={'user_id': user_id}
            )
            
            response = self.session.get(url, timeout=self.timeout)
            
            # If preferences don't exist (404), return default preferences
            if response.status_code == 404:
                logger.debug(
                    'No preferences found, using defaults',
                    extra={'user_id': user_id}
                )
                return UserPreferences(
                    user_id=user_id,
                    preferences={},
                    global_enabled=True
                )
            
            response.raise_for_status()
            data = response.json()
            
            # Parse preferences
            preferences = {}
            for pref_data in data.get('preferences', []):
                pref = NotificationPreference(
                    user_id=user_id,
                    event_type=pref_data['event_type'],
                    enabled=pref_data.get('enabled', True),
                    channels=pref_data.get('channels', ['email'])
                )
                preferences[pref.event_type] = pref
            
            return UserPreferences(
                user_id=user_id,
                preferences=preferences,
                global_enabled=data.get('global_enabled', True)
            )
            
        except Timeout:
            logger.warning(
                'Timeout fetching user preferences, using defaults',
                extra={'user_id': user_id}
            )
            # Return default preferences on timeout
            return UserPreferences(
                user_id=user_id,
                preferences={},
                global_enabled=True
            )
        
        except RequestException as e:
            logger.warning(
                'Error fetching user preferences, using defaults',
                extra={'user_id': user_id, 'error': str(e)}
            )
            # Return default preferences on error
            return UserPreferences(
                user_id=user_id,
                preferences={},
                global_enabled=True
            )
        
        except Exception as e:
            logger.error(
                'Unexpected error fetching user preferences',
                extra={'user_id': user_id, 'error': str(e)},
                exc_info=True
            )
            # Return default preferences on unexpected error
            return UserPreferences(
                user_id=user_id,
                preferences={},
                global_enabled=True
            )
    
    def get_user(self, user_id: str) -> Dict[str, Any]:
        """
        Get user profile data.
        
        Args:
            user_id: User ID
            
        Returns:
            User data dictionary
        """
        if not user_id:
            raise ValueError("User ID is required")
        
        url = f"{self.base_url}/api/v1/users/{user_id}"
        
        try:
            response = self.session.get(url, timeout=self.timeout)
            response.raise_for_status()
            
            return response.json()
            
        except Exception as e:
            logger.error(
                'Error fetching user',
                extra={'user_id': user_id, 'error': str(e)},
                exc_info=True
            )
            raise UserClientError(f"Failed to fetch user: {str(e)}") from e
    
    def close(self):
        """Close the HTTP session."""
        self.session.close()
