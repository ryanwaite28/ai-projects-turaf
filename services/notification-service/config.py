"""
Configuration module for notification-service Lambda function.
Manages environment variables and AWS service configurations.
"""
import os
from typing import Optional


class Config:
    """Configuration class for notification service."""
    
    def __init__(self):
        """Initialize configuration from environment variables."""
        self.environment = os.environ.get('ENVIRONMENT', 'dev')
        self.ses_from_email = os.environ.get('SES_FROM_EMAIL', 'notifications@turaf.com')
        self.experiment_service_url = os.environ.get('EXPERIMENT_SERVICE_URL', '')
        self.organization_service_url = os.environ.get('ORGANIZATION_SERVICE_URL', '')
        self.frontend_url = os.environ.get('FRONTEND_URL', '')
        self.aws_region = os.environ.get('AWS_REGION', 'us-east-1')
        self.idempotency_table_name = os.environ.get('IDEMPOTENCY_TABLE_NAME', 'processed_notification_events')
        self.log_level = os.environ.get('LOG_LEVEL', 'INFO')
    
    def validate(self) -> bool:
        """
        Validate that required configuration values are present.
        
        Returns:
            bool: True if configuration is valid, False otherwise
        """
        required_fields = [
            ('EXPERIMENT_SERVICE_URL', self.experiment_service_url),
            ('ORGANIZATION_SERVICE_URL', self.organization_service_url),
            ('FRONTEND_URL', self.frontend_url),
        ]
        
        for field_name, field_value in required_fields:
            if not field_value:
                raise ValueError(f'Missing required configuration: {field_name}')
        
        return True
    
    def get_service_url(self, service_name: str) -> Optional[str]:
        """
        Get service URL by service name.
        
        Args:
            service_name: Name of the service (e.g., 'experiment', 'organization')
            
        Returns:
            Service URL or None if not found
        """
        service_urls = {
            'experiment': self.experiment_service_url,
            'organization': self.organization_service_url,
        }
        return service_urls.get(service_name)


# Global configuration instance
config = Config()
