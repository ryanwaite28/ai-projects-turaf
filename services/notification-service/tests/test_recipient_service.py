"""
Unit tests for recipient service.
"""
import pytest
from unittest.mock import Mock, patch

from services.recipient_service import RecipientService
from models.notification_preference import MemberDto, NotificationPreference, UserPreferences
from clients.organization_client import OrganizationClientError


class TestRecipientService:
    """Test suite for RecipientService."""
    
    @pytest.fixture
    def mock_organization_client(self):
        """Create mock organization client."""
        client = Mock()
        client.get_members.return_value = [
            MemberDto(
                user_id='user-1',
                email='user1@example.com',
                name='User One',
                role='ADMIN'
            ),
            MemberDto(
                user_id='user-2',
                email='user2@example.com',
                name='User Two',
                role='MEMBER'
            ),
            MemberDto(
                user_id='user-3',
                email='user3@example.com',
                name='User Three',
                role='MEMBER'
            )
        ]
        return client
    
    @pytest.fixture
    def mock_user_client(self):
        """Create mock user client."""
        client = Mock()
        # Default: all users have notifications enabled
        client.get_user_preferences.return_value = UserPreferences(
            user_id='user-1',
            preferences={},
            global_enabled=True
        )
        return client
    
    @pytest.fixture
    def recipient_service(self, mock_organization_client, mock_user_client):
        """Create RecipientService with mocked clients."""
        return RecipientService(
            organization_client=mock_organization_client,
            user_client=mock_user_client
        )
    
    def test_get_recipients_returns_all_members_when_all_enabled(
        self, recipient_service, mock_organization_client, mock_user_client
    ):
        """Test get_recipients returns all members when all have notifications enabled."""
        recipients = recipient_service.get_recipients(
            'org-123',
            'experiment.completed'
        )
        
        assert len(recipients) == 3
        assert 'user1@example.com' in recipients
        assert 'user2@example.com' in recipients
        assert 'user3@example.com' in recipients
    
    def test_get_recipients_filters_disabled_users(
        self, recipient_service, mock_organization_client, mock_user_client
    ):
        """Test get_recipients filters users with disabled notifications."""
        def get_prefs(user_id):
            if user_id == 'user-2':
                # User 2 has notifications disabled
                return UserPreferences(
                    user_id=user_id,
                    preferences={},
                    global_enabled=False
                )
            return UserPreferences(
                user_id=user_id,
                preferences={},
                global_enabled=True
            )
        
        mock_user_client.get_user_preferences.side_effect = get_prefs
        
        recipients = recipient_service.get_recipients(
            'org-123',
            'experiment.completed'
        )
        
        assert len(recipients) == 2
        assert 'user1@example.com' in recipients
        assert 'user2@example.com' not in recipients
        assert 'user3@example.com' in recipients
    
    def test_get_recipients_respects_event_specific_preferences(
        self, recipient_service, mock_organization_client, mock_user_client
    ):
        """Test get_recipients respects event-specific preferences."""
        def get_prefs(user_id):
            if user_id == 'user-2':
                # User 2 has experiment.completed disabled
                pref = NotificationPreference(
                    user_id=user_id,
                    event_type='experiment.completed',
                    enabled=False
                )
                return UserPreferences(
                    user_id=user_id,
                    preferences={'experiment.completed': pref},
                    global_enabled=True
                )
            return UserPreferences(
                user_id=user_id,
                preferences={},
                global_enabled=True
            )
        
        mock_user_client.get_user_preferences.side_effect = get_prefs
        
        recipients = recipient_service.get_recipients(
            'org-123',
            'experiment.completed'
        )
        
        assert len(recipients) == 2
        assert 'user2@example.com' not in recipients
    
    def test_get_recipients_respects_channel_preferences(
        self, recipient_service, mock_organization_client, mock_user_client
    ):
        """Test get_recipients respects channel preferences."""
        def get_prefs(user_id):
            if user_id == 'user-2':
                # User 2 only wants webhook, not email
                pref = NotificationPreference(
                    user_id=user_id,
                    event_type='experiment.completed',
                    enabled=True,
                    channels=['webhook']
                )
                return UserPreferences(
                    user_id=user_id,
                    preferences={'experiment.completed': pref},
                    global_enabled=True
                )
            return UserPreferences(
                user_id=user_id,
                preferences={},
                global_enabled=True
            )
        
        mock_user_client.get_user_preferences.side_effect = get_prefs
        
        recipients = recipient_service.get_recipients(
            'org-123',
            'experiment.completed',
            channel='email'
        )
        
        assert len(recipients) == 2
        assert 'user2@example.com' not in recipients
    
    def test_get_recipients_returns_empty_list_for_no_members(
        self, recipient_service, mock_organization_client
    ):
        """Test get_recipients returns empty list when no members."""
        mock_organization_client.get_members.return_value = []
        
        recipients = recipient_service.get_recipients(
            'org-123',
            'experiment.completed'
        )
        
        assert recipients == []
    
    def test_get_recipients_returns_empty_list_on_client_error(
        self, recipient_service, mock_organization_client
    ):
        """Test get_recipients returns empty list on client error."""
        mock_organization_client.get_members.side_effect = OrganizationClientError('Error')
        
        recipients = recipient_service.get_recipients(
            'org-123',
            'experiment.completed'
        )
        
        assert recipients == []
    
    def test_get_recipient_members_returns_member_objects(
        self, recipient_service, mock_organization_client, mock_user_client
    ):
        """Test get_recipient_members returns MemberDto objects."""
        members = recipient_service.get_recipient_members(
            'org-123',
            'experiment.completed'
        )
        
        assert len(members) == 3
        assert all(isinstance(m, MemberDto) for m in members)
        assert members[0].user_id == 'user-1'
    
    def test_get_recipients_by_role_filters_by_role(
        self, recipient_service, mock_organization_client, mock_user_client
    ):
        """Test get_recipients_by_role filters by role."""
        recipients = recipient_service.get_recipients_by_role(
            'org-123',
            'experiment.completed',
            roles=['ADMIN']
        )
        
        assert len(recipients) == 1
        assert 'user1@example.com' in recipients
    
    def test_get_recipients_by_role_supports_multiple_roles(
        self, recipient_service, mock_organization_client, mock_user_client
    ):
        """Test get_recipients_by_role supports multiple roles."""
        recipients = recipient_service.get_recipients_by_role(
            'org-123',
            'experiment.completed',
            roles=['ADMIN', 'MEMBER']
        )
        
        assert len(recipients) == 3
    
    def test_should_notify_defaults_to_true_on_error(
        self, recipient_service, mock_user_client
    ):
        """Test _should_notify defaults to True on error (fail open)."""
        mock_user_client.get_user_preferences.side_effect = Exception('Error')
        
        result = recipient_service._should_notify('user-123', 'experiment.completed')
        
        assert result is True
    
    def test_close_closes_clients(
        self, recipient_service, mock_organization_client, mock_user_client
    ):
        """Test close method closes client resources."""
        recipient_service.close()
        
        mock_organization_client.close.assert_called_once()
        mock_user_client.close.assert_called_once()
