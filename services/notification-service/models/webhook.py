"""
Webhook domain models for notification-service.
Represents webhook configurations and payloads.
"""
from dataclasses import dataclass, field
from typing import Dict, Any, Optional
from datetime import datetime
from enum import Enum


class WebhookEventType(Enum):
    """Supported webhook event types."""
    EXPERIMENT_COMPLETED = "experiment.completed"
    REPORT_GENERATED = "report.generated"
    MEMBER_ADDED = "member.added"


@dataclass
class WebhookConfig:
    """
    Represents a webhook configuration for an organization.
    """
    url: str
    secret: str
    organization_id: str
    event_types: list[str]
    enabled: bool = True
    
    def supports_event(self, event_type: str) -> bool:
        """Check if this webhook supports the given event type."""
        return event_type in self.event_types or '*' in self.event_types
    
    def validate(self) -> None:
        """
        Validate webhook configuration.
        
        Raises:
            ValueError: If validation fails
        """
        if not self.url:
            raise ValueError("Webhook URL is required")
        
        if not self.url.startswith(('http://', 'https://')):
            raise ValueError("Webhook URL must use HTTP or HTTPS protocol")
        
        if not self.secret:
            raise ValueError("Webhook secret is required")
        
        if not self.organization_id:
            raise ValueError("Organization ID is required")
        
        if not self.event_types:
            raise ValueError("At least one event type is required")


@dataclass
class WebhookPayload:
    """
    Represents a webhook payload to be delivered.
    """
    event_type: str
    event_id: str
    data: Dict[str, Any]
    timestamp: str = field(default_factory=lambda: datetime.utcnow().isoformat())
    
    def to_dict(self) -> Dict[str, Any]:
        """Convert payload to dictionary for JSON serialization."""
        return {
            'event_type': self.event_type,
            'event_id': self.event_id,
            'data': self.data,
            'timestamp': self.timestamp
        }


@dataclass
class WebhookDeliveryResult:
    """
    Represents the result of a webhook delivery attempt.
    """
    success: bool
    status_code: Optional[int] = None
    response_body: Optional[str] = None
    error_message: Optional[str] = None
    attempts: int = 1
    delivered_at: Optional[str] = None
    
    def __post_init__(self):
        """Set delivered_at timestamp if successful."""
        if self.success and not self.delivered_at:
            self.delivered_at = datetime.utcnow().isoformat()
