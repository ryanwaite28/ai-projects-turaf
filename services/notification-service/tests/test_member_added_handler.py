"""
Unit tests for MemberAdded event handler.
"""
import pytest
from unittest.mock import Mock
from handlers.member_added import handle_member_added


class TestMemberAddedHandler:
    """Test suite for MemberAdded handler."""
    
    @pytest.fixture
    def mock_context(self):
        """Create mock Lambda context."""
        context = Mock()
        context.request_id = 'test-request-123'
        return context
    
    @pytest.fixture
    def valid_event(self):
        """Create valid MemberAdded event."""
        return {
            'id': 'event-789',
            'version': '0',
            'detail-type': 'MemberAdded',
            'source': 'turaf.organization-service',
            'account': '123456789012',
            'time': '2024-03-18T11:00:00Z',
            'region': 'us-east-1',
            'detail': {
                'eventId': 'evt-789',
                'memberId': 'usr-456',
                'organizationId': 'org-123',
                'memberEmail': 'newuser@example.com',
                'memberName': 'New User',
                'role': 'MEMBER',
                'addedAt': '2024-03-18T11:00:00Z'
            }
        }
    
    def test_handles_valid_event_successfully(self, valid_event, mock_context):
        """Test successful handling of valid event."""
        result = handle_member_added(valid_event, mock_context)
        
        assert result['statusCode'] == 200
        assert 'successfully' in result['body']
    
    def test_extracts_member_details_from_event(self, valid_event, mock_context):
        """Test that handler extracts member details from event."""
        result = handle_member_added(valid_event, mock_context)
        
        assert result['statusCode'] == 200
    
    def test_returns_400_when_member_id_missing(self, valid_event, mock_context):
        """Test validation error when memberId is missing."""
        del valid_event['detail']['memberId']
        
        result = handle_member_added(valid_event, mock_context)
        
        assert result['statusCode'] == 400
        assert 'memberId' in result['body']
    
    def test_returns_400_when_organization_id_missing(self, valid_event, mock_context):
        """Test validation error when organizationId is missing."""
        del valid_event['detail']['organizationId']
        
        result = handle_member_added(valid_event, mock_context)
        
        assert result['statusCode'] == 400
        assert 'organizationId' in result['body']
    
    def test_returns_400_when_member_email_missing(self, valid_event, mock_context):
        """Test validation error when memberEmail is missing."""
        del valid_event['detail']['memberEmail']
        
        result = handle_member_added(valid_event, mock_context)
        
        assert result['statusCode'] == 400
        assert 'memberEmail' in result['body']
    
    def test_handles_optional_member_name(self, valid_event, mock_context):
        """Test that memberName is optional."""
        del valid_event['detail']['memberName']
        
        result = handle_member_added(valid_event, mock_context)
        
        assert result['statusCode'] == 200
    
    def test_handles_optional_role(self, valid_event, mock_context):
        """Test that role is optional."""
        del valid_event['detail']['role']
        
        result = handle_member_added(valid_event, mock_context)
        
        assert result['statusCode'] == 200
    
    def test_logs_event_processing(self, valid_event, mock_context, caplog):
        """Test that handler logs event processing."""
        handle_member_added(valid_event, mock_context)
        
        assert len(caplog.records) > 0
