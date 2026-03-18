"""
Unit tests for organization service client.
"""
import pytest
from unittest.mock import Mock, patch
import requests

from clients.organization_client import OrganizationClient, OrganizationClientError


class TestOrganizationClient:
    """Test suite for OrganizationClient."""
    
    @pytest.fixture
    def mock_config(self):
        """Mock config."""
        with patch('clients.organization_client.config') as mock_cfg:
            mock_cfg.organization_service_url = 'https://api.turaf.com'
            yield mock_cfg
    
    @pytest.fixture
    def organization_client(self, mock_config):
        """Create OrganizationClient instance."""
        return OrganizationClient()
    
    @pytest.fixture
    def mock_response(self):
        """Create mock HTTP response."""
        response = Mock()
        response.status_code = 200
        response.json.return_value = {
            'members': [
                {
                    'userId': 'user-1',
                    'email': 'user1@example.com',
                    'name': 'User One',
                    'role': 'ADMIN'
                },
                {
                    'userId': 'user-2',
                    'email': 'user2@example.com',
                    'name': 'User Two',
                    'role': 'MEMBER'
                }
            ]
        }
        return response
    
    def test_initializes_with_config_url(self, mock_config):
        """Test client initializes with URL from config."""
        client = OrganizationClient()
        
        assert client.base_url == 'https://api.turaf.com'
    
    def test_initializes_with_custom_url(self):
        """Test client initializes with custom URL."""
        client = OrganizationClient(base_url='https://custom.com')
        
        assert client.base_url == 'https://custom.com'
    
    def test_raises_error_without_url(self):
        """Test raises error when URL not configured."""
        with patch('clients.organization_client.config') as mock_cfg:
            mock_cfg.organization_service_url = None
            
            with pytest.raises(ValueError) as exc_info:
                OrganizationClient()
            
            assert 'must be configured' in str(exc_info.value)
    
    def test_get_members_returns_member_list(
        self, organization_client, mock_response
    ):
        """Test get_members returns list of members."""
        with patch.object(organization_client.session, 'get', return_value=mock_response):
            members = organization_client.get_members('org-123')
            
            assert len(members) == 2
            assert members[0].user_id == 'user-1'
            assert members[0].email == 'user1@example.com'
            assert members[1].user_id == 'user-2'
    
    def test_get_members_makes_correct_request(
        self, organization_client, mock_response
    ):
        """Test get_members makes correct HTTP request."""
        with patch.object(organization_client.session, 'get', return_value=mock_response) as mock_get:
            organization_client.get_members('org-123')
            
            call_args = mock_get.call_args
            url = call_args[0][0]
            headers = call_args.kwargs['headers']
            
            assert 'org-123' in url
            assert headers['X-Organization-Id'] == 'org-123'
    
    def test_get_members_handles_list_response(self, organization_client):
        """Test get_members handles response as list."""
        mock_response = Mock()
        mock_response.status_code = 200
        mock_response.json.return_value = [
            {'userId': 'user-1', 'email': 'user1@example.com'}
        ]
        
        with patch.object(organization_client.session, 'get', return_value=mock_response):
            members = organization_client.get_members('org-123')
            
            assert len(members) == 1
            assert members[0].user_id == 'user-1'
    
    def test_get_members_raises_error_on_timeout(self, organization_client):
        """Test get_members raises error on timeout."""
        with patch.object(
            organization_client.session,
            'get',
            side_effect=requests.Timeout()
        ):
            with pytest.raises(OrganizationClientError) as exc_info:
                organization_client.get_members('org-123')
            
            assert 'Timeout' in str(exc_info.value)
    
    def test_get_members_raises_error_on_request_exception(
        self, organization_client
    ):
        """Test get_members raises error on request exception."""
        with patch.object(
            organization_client.session,
            'get',
            side_effect=requests.RequestException('Connection error')
        ):
            with pytest.raises(OrganizationClientError) as exc_info:
                organization_client.get_members('org-123')
            
            assert 'Failed to fetch members' in str(exc_info.value)
    
    def test_get_members_raises_error_for_empty_org_id(self, organization_client):
        """Test get_members raises error for empty organization ID."""
        with pytest.raises(ValueError) as exc_info:
            organization_client.get_members('')
        
        assert 'Organization ID is required' in str(exc_info.value)
    
    def test_get_organization_returns_org_data(self, organization_client):
        """Test get_organization returns organization data."""
        mock_response = Mock()
        mock_response.status_code = 200
        mock_response.json.return_value = {
            'id': 'org-123',
            'name': 'Test Org'
        }
        
        with patch.object(organization_client.session, 'get', return_value=mock_response):
            org = organization_client.get_organization('org-123')
            
            assert org['id'] == 'org-123'
            assert org['name'] == 'Test Org'
    
    def test_close_closes_session(self, organization_client):
        """Test close method closes HTTP session."""
        with patch.object(organization_client.session, 'close') as mock_close:
            organization_client.close()
            
            mock_close.assert_called_once()
