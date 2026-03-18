"""
Unit tests for user service client.
"""
import pytest
from unittest.mock import Mock, patch
import requests

from clients.user_client import UserClient, UserClientError


class TestUserClient:
    """Test suite for UserClient."""
    
    @pytest.fixture
    def mock_config(self):
        """Mock config."""
        with patch('clients.user_client.config') as mock_cfg:
            mock_cfg.experiment_service_url = 'https://api.turaf.com'
            yield mock_cfg
    
    @pytest.fixture
    def user_client(self, mock_config):
        """Create UserClient instance."""
        return UserClient()
    
    def test_initializes_with_config_url(self, mock_config):
        """Test client initializes with URL from config."""
        client = UserClient()
        
        assert client.base_url == 'https://api.turaf.com'
    
    def test_get_user_preferences_returns_preferences(self, user_client):
        """Test get_user_preferences returns UserPreferences."""
        mock_response = Mock()
        mock_response.status_code = 200
        mock_response.json.return_value = {
            'global_enabled': True,
            'preferences': [
                {
                    'event_type': 'experiment.completed',
                    'enabled': True,
                    'channels': ['email']
                }
            ]
        }
        
        with patch.object(user_client.session, 'get', return_value=mock_response):
            prefs = user_client.get_user_preferences('user-123')
            
            assert prefs.user_id == 'user-123'
            assert prefs.global_enabled is True
            assert 'experiment.completed' in prefs.preferences
    
    def test_get_user_preferences_returns_defaults_on_404(self, user_client):
        """Test get_user_preferences returns defaults when not found."""
        mock_response = Mock()
        mock_response.status_code = 404
        
        with patch.object(user_client.session, 'get', return_value=mock_response):
            prefs = user_client.get_user_preferences('user-123')
            
            assert prefs.user_id == 'user-123'
            assert prefs.global_enabled is True
            assert len(prefs.preferences) == 0
    
    def test_get_user_preferences_returns_defaults_on_timeout(self, user_client):
        """Test get_user_preferences returns defaults on timeout."""
        with patch.object(
            user_client.session,
            'get',
            side_effect=requests.Timeout()
        ):
            prefs = user_client.get_user_preferences('user-123')
            
            assert prefs.user_id == 'user-123'
            assert prefs.global_enabled is True
    
    def test_get_user_preferences_returns_defaults_on_error(self, user_client):
        """Test get_user_preferences returns defaults on error."""
        with patch.object(
            user_client.session,
            'get',
            side_effect=requests.RequestException()
        ):
            prefs = user_client.get_user_preferences('user-123')
            
            assert prefs.user_id == 'user-123'
            assert prefs.global_enabled is True
    
    def test_get_user_preferences_raises_error_for_empty_user_id(self, user_client):
        """Test get_user_preferences raises error for empty user ID."""
        with pytest.raises(ValueError) as exc_info:
            user_client.get_user_preferences('')
        
        assert 'User ID is required' in str(exc_info.value)
    
    def test_get_user_returns_user_data(self, user_client):
        """Test get_user returns user data."""
        mock_response = Mock()
        mock_response.status_code = 200
        mock_response.json.return_value = {
            'id': 'user-123',
            'email': 'test@example.com',
            'name': 'Test User'
        }
        
        with patch.object(user_client.session, 'get', return_value=mock_response):
            user = user_client.get_user('user-123')
            
            assert user['id'] == 'user-123'
            assert user['email'] == 'test@example.com'
    
    def test_get_user_raises_error_on_failure(self, user_client):
        """Test get_user raises error on failure."""
        with patch.object(
            user_client.session,
            'get',
            side_effect=requests.RequestException()
        ):
            with pytest.raises(UserClientError):
                user_client.get_user('user-123')
    
    def test_close_closes_session(self, user_client):
        """Test close method closes HTTP session."""
        with patch.object(user_client.session, 'close') as mock_close:
            user_client.close()
            
            mock_close.assert_called_once()
