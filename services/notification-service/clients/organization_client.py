"""
Organization service client for fetching organization data.
"""
import logging
from typing import List, Optional
import requests
from requests.exceptions import RequestException, Timeout

from config import config
from models.notification_preference import MemberDto

logger = logging.getLogger(__name__)


class OrganizationClientError(Exception):
    """Exception raised for organization client errors."""
    pass


class OrganizationClient:
    """
    HTTP client for organization-service API.
    Fetches organization members and related data.
    """
    
    DEFAULT_TIMEOUT = 10  # seconds
    
    def __init__(
        self,
        base_url: Optional[str] = None,
        timeout: int = DEFAULT_TIMEOUT
    ):
        """
        Initialize organization client.
        
        Args:
            base_url: Base URL for organization service (uses config if None)
            timeout: Request timeout in seconds
        """
        self.base_url = base_url or config.organization_service_url
        self.timeout = timeout
        self.session = requests.Session()
        
        if not self.base_url:
            raise ValueError("Organization service URL must be configured")
        
        # Remove trailing slash
        self.base_url = self.base_url.rstrip('/')
    
    def get_members(self, organization_id: str) -> List[MemberDto]:
        """
        Get all members of an organization.
        
        Args:
            organization_id: Organization ID
            
        Returns:
            List of organization members
            
        Raises:
            OrganizationClientError: If request fails
        """
        if not organization_id:
            raise ValueError("Organization ID is required")
        
        url = f"{self.base_url}/api/v1/organizations/{organization_id}/members"
        
        try:
            logger.info(
                'Fetching organization members',
                extra={'organization_id': organization_id, 'url': url}
            )
            
            response = self.session.get(
                url,
                headers={'X-Organization-Id': organization_id},
                timeout=self.timeout
            )
            
            response.raise_for_status()
            
            data = response.json()
            
            # Handle different response formats
            if isinstance(data, dict) and 'members' in data:
                members_data = data['members']
            elif isinstance(data, list):
                members_data = data
            else:
                logger.warning(
                    'Unexpected response format',
                    extra={'organization_id': organization_id}
                )
                members_data = []
            
            members = [MemberDto.from_dict(m) for m in members_data]
            
            logger.info(
                'Fetched organization members',
                extra={
                    'organization_id': organization_id,
                    'member_count': len(members)
                }
            )
            
            return members
            
        except Timeout:
            logger.error(
                'Timeout fetching organization members',
                extra={'organization_id': organization_id, 'timeout': self.timeout},
                exc_info=True
            )
            raise OrganizationClientError(
                f"Timeout fetching members for organization {organization_id}"
            )
        
        except RequestException as e:
            logger.error(
                'Error fetching organization members',
                extra={
                    'organization_id': organization_id,
                    'error': str(e),
                    'status_code': getattr(e.response, 'status_code', None)
                },
                exc_info=True
            )
            raise OrganizationClientError(
                f"Failed to fetch members: {str(e)}"
            ) from e
        
        except Exception as e:
            logger.error(
                'Unexpected error fetching organization members',
                extra={'organization_id': organization_id, 'error': str(e)},
                exc_info=True
            )
            raise OrganizationClientError(
                f"Unexpected error: {str(e)}"
            ) from e
    
    def get_organization(self, organization_id: str) -> dict:
        """
        Get organization details.
        
        Args:
            organization_id: Organization ID
            
        Returns:
            Organization data dictionary
        """
        if not organization_id:
            raise ValueError("Organization ID is required")
        
        url = f"{self.base_url}/api/v1/organizations/{organization_id}"
        
        try:
            logger.info(
                'Fetching organization details',
                extra={'organization_id': organization_id}
            )
            
            response = self.session.get(url, timeout=self.timeout)
            response.raise_for_status()
            
            return response.json()
            
        except Exception as e:
            logger.error(
                'Error fetching organization',
                extra={'organization_id': organization_id, 'error': str(e)},
                exc_info=True
            )
            raise OrganizationClientError(
                f"Failed to fetch organization: {str(e)}"
            ) from e
    
    def close(self):
        """Close the HTTP session."""
        self.session.close()
