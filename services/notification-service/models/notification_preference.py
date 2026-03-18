"""
Notification preference models for notification-service.
Represents user preferences for receiving notifications.
"""
from dataclasses import dataclass
from typing import Optional, Dict, Any
from enum import Enum


class NotificationChannel(Enum):
    """Notification delivery channels."""
    EMAIL = "email"
    WEBHOOK = "webhook"
    SMS = "sms"
    IN_APP = "in_app"


class NotificationEventType(Enum):
    """Types of notification events."""
    EXPERIMENT_COMPLETED = "experiment.completed"
    REPORT_GENERATED = "report.generated"
    MEMBER_ADDED = "member.added"
    EXPERIMENT_STARTED = "experiment.started"
    EXPERIMENT_FAILED = "experiment.failed"


@dataclass
class NotificationPreference:
    """
    Represents a user's notification preference for a specific event type.
    """
    user_id: str
    event_type: str
    enabled: bool = True
    channels: list[str] = None
    
    def __post_init__(self):
        """Initialize default channels if not provided."""
        if self.channels is None:
            self.channels = [NotificationChannel.EMAIL.value]
    
    def is_enabled(self) -> bool:
        """Check if notifications are enabled for this preference."""
        return self.enabled
    
    def supports_channel(self, channel: str) -> bool:
        """
        Check if a specific channel is enabled.
        
        Args:
            channel: Channel name (e.g., 'email', 'webhook')
            
        Returns:
            True if channel is enabled, False otherwise
        """
        return channel in self.channels
    
    def to_dict(self) -> Dict[str, Any]:
        """Convert preference to dictionary."""
        return {
            'user_id': self.user_id,
            'event_type': self.event_type,
            'enabled': self.enabled,
            'channels': self.channels
        }


@dataclass
class UserPreferences:
    """
    Collection of notification preferences for a user.
    """
    user_id: str
    preferences: Dict[str, NotificationPreference]
    global_enabled: bool = True
    
    def get_preference(self, event_type: str) -> Optional[NotificationPreference]:
        """
        Get preference for specific event type.
        
        Args:
            event_type: Event type to get preference for
            
        Returns:
            NotificationPreference if found, None otherwise
        """
        return self.preferences.get(event_type)
    
    def should_notify(self, event_type: str, channel: str = 'email') -> bool:
        """
        Check if user should be notified for event type and channel.
        
        Args:
            event_type: Type of event
            channel: Notification channel
            
        Returns:
            True if user should be notified, False otherwise
        """
        # Check global setting
        if not self.global_enabled:
            return False
        
        # Get event-specific preference
        preference = self.get_preference(event_type)
        
        # If no preference set, default to enabled
        if preference is None:
            return True
        
        # Check if preference is enabled and supports channel
        return preference.is_enabled() and preference.supports_channel(channel)


@dataclass
class MemberDto:
    """
    DTO representing an organization member.
    """
    user_id: str
    email: str
    name: Optional[str] = None
    role: Optional[str] = None
    organization_id: Optional[str] = None
    
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> 'MemberDto':
        """Create MemberDto from dictionary."""
        return cls(
            user_id=data.get('userId') or data.get('user_id'),
            email=data['email'],
            name=data.get('name'),
            role=data.get('role'),
            organization_id=data.get('organizationId') or data.get('organization_id')
        )
