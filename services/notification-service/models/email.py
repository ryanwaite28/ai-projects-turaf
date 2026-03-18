"""
Email domain models for notification-service.
Represents email messages and related data structures.
"""
from dataclasses import dataclass
from typing import List, Optional, Dict, Any


@dataclass
class EmailRecipient:
    """Represents an email recipient."""
    email: str
    name: Optional[str] = None
    
    def __str__(self) -> str:
        """Format recipient for email headers."""
        if self.name:
            return f"{self.name} <{self.email}>"
        return self.email


@dataclass
class EmailMessage:
    """
    Represents an email message to be sent.
    Encapsulates all data needed for email delivery.
    """
    recipients: List[str]
    subject: str
    html_body: str
    text_body: Optional[str] = None
    from_email: Optional[str] = None
    reply_to: Optional[str] = None
    
    def validate(self) -> None:
        """
        Validate email message data.
        
        Raises:
            ValueError: If validation fails
        """
        if not self.recipients:
            raise ValueError("Email must have at least one recipient")
        
        if not self.subject:
            raise ValueError("Email must have a subject")
        
        if not self.html_body:
            raise ValueError("Email must have HTML body")
        
        # Validate email addresses
        for recipient in self.recipients:
            if not recipient or '@' not in recipient:
                raise ValueError(f"Invalid email address: {recipient}")


@dataclass
class EmailTemplate:
    """Represents an email template with data."""
    template_name: str
    data: Dict[str, Any]
    subject: str
