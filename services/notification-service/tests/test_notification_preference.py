"""
Unit tests for notification preference models.
"""
import pytest
from models.notification_preference import (
    NotificationPreference,
    UserPreferences,
    MemberDto,
    NotificationChannel
)


class TestNotificationPreference:
    """Test suite for NotificationPreference model."""
    
    def test_creates_preference_with_defaults(self):
        """Test creating preference with default values."""
        pref = NotificationPreference(
            user_id='user-123',
            event_type='experiment.completed'
        )
        
        assert pref.user_id == 'user-123'
        assert pref.event_type == 'experiment.completed'
        assert pref.enabled is True
        assert 'email' in pref.channels
    
    def test_creates_preference_with_custom_channels(self):
        """Test creating preference with custom channels."""
        pref = NotificationPreference(
            user_id='user-123',
            event_type='experiment.completed',
            channels=['email', 'webhook']
        )
        
        assert len(pref.channels) == 2
        assert 'email' in pref.channels
        assert 'webhook' in pref.channels
    
    def test_is_enabled_returns_correct_value(self):
        """Test is_enabled method."""
        enabled_pref = NotificationPreference(
            user_id='user-123',
            event_type='test',
            enabled=True
        )
        disabled_pref = NotificationPreference(
            user_id='user-123',
            event_type='test',
            enabled=False
        )
        
        assert enabled_pref.is_enabled() is True
        assert disabled_pref.is_enabled() is False
    
    def test_supports_channel_returns_true_for_enabled_channel(self):
        """Test supports_channel for enabled channel."""
        pref = NotificationPreference(
            user_id='user-123',
            event_type='test',
            channels=['email', 'webhook']
        )
        
        assert pref.supports_channel('email') is True
        assert pref.supports_channel('webhook') is True
    
    def test_supports_channel_returns_false_for_disabled_channel(self):
        """Test supports_channel for disabled channel."""
        pref = NotificationPreference(
            user_id='user-123',
            event_type='test',
            channels=['email']
        )
        
        assert pref.supports_channel('sms') is False
        assert pref.supports_channel('webhook') is False
    
    def test_to_dict_returns_correct_structure(self):
        """Test to_dict method."""
        pref = NotificationPreference(
            user_id='user-123',
            event_type='experiment.completed',
            enabled=True,
            channels=['email']
        )
        
        result = pref.to_dict()
        
        assert result['user_id'] == 'user-123'
        assert result['event_type'] == 'experiment.completed'
        assert result['enabled'] is True
        assert result['channels'] == ['email']


class TestUserPreferences:
    """Test suite for UserPreferences model."""
    
    def test_creates_user_preferences(self):
        """Test creating user preferences."""
        prefs = UserPreferences(
            user_id='user-123',
            preferences={},
            global_enabled=True
        )
        
        assert prefs.user_id == 'user-123'
        assert prefs.global_enabled is True
    
    def test_get_preference_returns_existing_preference(self):
        """Test get_preference returns existing preference."""
        pref = NotificationPreference(
            user_id='user-123',
            event_type='experiment.completed'
        )
        prefs = UserPreferences(
            user_id='user-123',
            preferences={'experiment.completed': pref}
        )
        
        result = prefs.get_preference('experiment.completed')
        
        assert result is not None
        assert result.event_type == 'experiment.completed'
    
    def test_get_preference_returns_none_for_missing(self):
        """Test get_preference returns None for missing preference."""
        prefs = UserPreferences(
            user_id='user-123',
            preferences={}
        )
        
        result = prefs.get_preference('nonexistent')
        
        assert result is None
    
    def test_should_notify_returns_false_when_globally_disabled(self):
        """Test should_notify returns False when global setting is disabled."""
        prefs = UserPreferences(
            user_id='user-123',
            preferences={},
            global_enabled=False
        )
        
        result = prefs.should_notify('experiment.completed', 'email')
        
        assert result is False
    
    def test_should_notify_returns_true_for_missing_preference(self):
        """Test should_notify defaults to True for missing preference."""
        prefs = UserPreferences(
            user_id='user-123',
            preferences={},
            global_enabled=True
        )
        
        result = prefs.should_notify('experiment.completed', 'email')
        
        assert result is True
    
    def test_should_notify_respects_preference_enabled_flag(self):
        """Test should_notify respects preference enabled flag."""
        disabled_pref = NotificationPreference(
            user_id='user-123',
            event_type='experiment.completed',
            enabled=False
        )
        prefs = UserPreferences(
            user_id='user-123',
            preferences={'experiment.completed': disabled_pref}
        )
        
        result = prefs.should_notify('experiment.completed', 'email')
        
        assert result is False
    
    def test_should_notify_respects_channel_preference(self):
        """Test should_notify respects channel preference."""
        pref = NotificationPreference(
            user_id='user-123',
            event_type='experiment.completed',
            enabled=True,
            channels=['email']  # Only email, not webhook
        )
        prefs = UserPreferences(
            user_id='user-123',
            preferences={'experiment.completed': pref}
        )
        
        assert prefs.should_notify('experiment.completed', 'email') is True
        assert prefs.should_notify('experiment.completed', 'webhook') is False


class TestMemberDto:
    """Test suite for MemberDto model."""
    
    def test_creates_member_dto(self):
        """Test creating MemberDto."""
        member = MemberDto(
            user_id='user-123',
            email='test@example.com',
            name='Test User',
            role='MEMBER'
        )
        
        assert member.user_id == 'user-123'
        assert member.email == 'test@example.com'
        assert member.name == 'Test User'
        assert member.role == 'MEMBER'
    
    def test_from_dict_with_camelCase(self):
        """Test from_dict with camelCase keys."""
        data = {
            'userId': 'user-123',
            'email': 'test@example.com',
            'name': 'Test User',
            'role': 'ADMIN',
            'organizationId': 'org-123'
        }
        
        member = MemberDto.from_dict(data)
        
        assert member.user_id == 'user-123'
        assert member.email == 'test@example.com'
        assert member.organization_id == 'org-123'
    
    def test_from_dict_with_snake_case(self):
        """Test from_dict with snake_case keys."""
        data = {
            'user_id': 'user-123',
            'email': 'test@example.com',
            'name': 'Test User',
            'role': 'MEMBER',
            'organization_id': 'org-123'
        }
        
        member = MemberDto.from_dict(data)
        
        assert member.user_id == 'user-123'
        assert member.organization_id == 'org-123'
    
    def test_from_dict_with_minimal_data(self):
        """Test from_dict with only required fields."""
        data = {
            'userId': 'user-123',
            'email': 'test@example.com'
        }
        
        member = MemberDto.from_dict(data)
        
        assert member.user_id == 'user-123'
        assert member.email == 'test@example.com'
        assert member.name is None
        assert member.role is None
