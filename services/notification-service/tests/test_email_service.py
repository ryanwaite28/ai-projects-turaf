"""
Unit tests for email service.
"""
import pytest
from unittest.mock import Mock, patch, MagicMock

from services.email_service import EmailService
from services.ses_client import SesClientError


class TestEmailService:
    """Test suite for EmailService."""
    
    @pytest.fixture
    def mock_ses_client(self):
        """Create mock SES client."""
        mock_client = Mock()
        mock_client.send_email.return_value = 'test-message-id-123'
        return mock_client
    
    @pytest.fixture
    def mock_template_service(self):
        """Create mock template service."""
        mock_service = Mock()
        mock_service.render.return_value = '<p>Rendered template</p>'
        return mock_service
    
    @pytest.fixture
    def email_service(self, mock_ses_client, mock_template_service):
        """Create EmailService with mocked dependencies."""
        return EmailService(
            ses_client=mock_ses_client,
            template_service=mock_template_service
        )
    
    @pytest.fixture
    def mock_config(self):
        """Mock config."""
        with patch('services.email_service.config') as mock_cfg:
            mock_cfg.frontend_url = 'https://app.turaf.com'
            yield mock_cfg
    
    def test_send_experiment_completed_email_successfully(
        self, email_service, mock_ses_client, mock_template_service, mock_config
    ):
        """Test sending experiment completed email."""
        experiment_data = {
            'experimentId': 'exp-123',
            'experimentName': 'Test Experiment',
            'organizationId': 'org-123',
            'completedAt': '2024-03-18T10:00:00Z'
        }
        
        message_id = email_service.send_experiment_completed_email(
            'user@example.com',
            experiment_data
        )
        
        assert message_id == 'test-message-id-123'
        mock_template_service.render.assert_called_once()
        mock_ses_client.send_email.assert_called_once()
    
    def test_experiment_completed_email_renders_correct_template(
        self, email_service, mock_template_service, mock_config
    ):
        """Test correct template is rendered for experiment completed."""
        experiment_data = {
            'experimentId': 'exp-123',
            'experimentName': 'Test Experiment'
        }
        
        email_service.send_experiment_completed_email('user@example.com', experiment_data)
        
        call_args = mock_template_service.render.call_args
        template_name = call_args[0][0]
        template_data = call_args[0][1]
        
        assert template_name == 'email/experiment-completed'
        assert template_data['experiment_id'] == 'exp-123'
        assert template_data['experiment_name'] == 'Test Experiment'
    
    def test_experiment_completed_email_has_correct_subject(
        self, email_service, mock_ses_client, mock_config
    ):
        """Test experiment completed email has correct subject."""
        experiment_data = {'experimentId': 'exp-123', 'experimentName': 'Test'}
        
        email_service.send_experiment_completed_email('user@example.com', experiment_data)
        
        call_args = mock_ses_client.send_email.call_args
        message = call_args[0][0]
        
        assert message.subject == 'Experiment Completed Successfully'
    
    def test_send_report_generated_email_successfully(
        self, email_service, mock_ses_client, mock_template_service, mock_config
    ):
        """Test sending report generated email."""
        report_data = {
            'reportId': 'rpt-123',
            'experimentId': 'exp-123',
            'experimentName': 'Test Experiment',
            'reportUrl': 'https://s3.amazonaws.com/reports/rpt-123.pdf',
            'generatedAt': '2024-03-18T10:05:00Z'
        }
        
        message_id = email_service.send_report_generated_email(
            'user@example.com',
            report_data
        )
        
        assert message_id == 'test-message-id-123'
        mock_template_service.render.assert_called_once()
        mock_ses_client.send_email.assert_called_once()
    
    def test_report_generated_email_renders_correct_template(
        self, email_service, mock_template_service, mock_config
    ):
        """Test correct template is rendered for report generated."""
        report_data = {
            'reportId': 'rpt-123',
            'experimentId': 'exp-123',
            'reportUrl': 'https://example.com/report.pdf'
        }
        
        email_service.send_report_generated_email('user@example.com', report_data)
        
        call_args = mock_template_service.render.call_args
        template_name = call_args[0][0]
        template_data = call_args[0][1]
        
        assert template_name == 'email/report-generated'
        assert template_data['report_id'] == 'rpt-123'
        assert template_data['report_url'] == 'https://example.com/report.pdf'
    
    def test_send_member_added_email_successfully(
        self, email_service, mock_ses_client, mock_template_service, mock_config
    ):
        """Test sending member added email."""
        member_data = {
            'organizationId': 'org-123',
            'organizationName': 'Test Org',
            'memberName': 'New User',
            'role': 'MEMBER',
            'invitedBy': 'Admin User'
        }
        
        message_id = email_service.send_member_added_email(
            'newuser@example.com',
            member_data
        )
        
        assert message_id == 'test-message-id-123'
        mock_template_service.render.assert_called_once()
        mock_ses_client.send_email.assert_called_once()
    
    def test_member_added_email_renders_correct_template(
        self, email_service, mock_template_service, mock_config
    ):
        """Test correct template is rendered for member added."""
        member_data = {
            'organizationId': 'org-123',
            'organizationName': 'Test Org',
            'memberName': 'New User'
        }
        
        email_service.send_member_added_email('user@example.com', member_data)
        
        call_args = mock_template_service.render.call_args
        template_name = call_args[0][0]
        template_data = call_args[0][1]
        
        assert template_name == 'email/member-added'
        assert template_data['organization_name'] == 'Test Org'
        assert template_data['member_name'] == 'New User'
    
    def test_member_added_email_has_dynamic_subject(
        self, email_service, mock_ses_client, mock_config
    ):
        """Test member added email has organization name in subject."""
        member_data = {
            'organizationId': 'org-123',
            'organizationName': 'Acme Corp'
        }
        
        email_service.send_member_added_email('user@example.com', member_data)
        
        call_args = mock_ses_client.send_email.call_args
        message = call_args[0][0]
        
        assert message.subject == 'Welcome to Acme Corp'
    
    def test_send_bulk_emails_sends_to_multiple_recipients(
        self, email_service, mock_ses_client, mock_template_service, mock_config
    ):
        """Test bulk email sending."""
        recipients = ['user1@example.com', 'user2@example.com', 'user3@example.com']
        mock_ses_client.send_bulk_email.return_value = ['id1', 'id2', 'id3']
        
        message_ids = email_service.send_bulk_emails(
            recipients,
            'Test Subject',
            'email/test-template',
            {'key': 'value'}
        )
        
        assert len(message_ids) == 3
        mock_template_service.render.assert_called_once()
        mock_ses_client.send_bulk_email.assert_called_once()
    
    def test_handles_ses_client_error_gracefully(
        self, email_service, mock_ses_client, mock_config
    ):
        """Test error handling when SES client fails."""
        mock_ses_client.send_email.side_effect = SesClientError('SES error')
        
        experiment_data = {'experimentId': 'exp-123', 'experimentName': 'Test'}
        
        with pytest.raises(SesClientError):
            email_service.send_experiment_completed_email('user@example.com', experiment_data)
    
    def test_handles_template_rendering_error(
        self, email_service, mock_template_service, mock_config
    ):
        """Test error handling when template rendering fails."""
        mock_template_service.render.side_effect = Exception('Template error')
        
        experiment_data = {'experimentId': 'exp-123', 'experimentName': 'Test'}
        
        with pytest.raises(Exception):
            email_service.send_experiment_completed_email('user@example.com', experiment_data)
    
    def test_uses_default_values_for_missing_optional_fields(
        self, email_service, mock_template_service, mock_config
    ):
        """Test uses default values when optional fields are missing."""
        experiment_data = {'experimentId': 'exp-123'}  # Missing experimentName
        
        email_service.send_experiment_completed_email('user@example.com', experiment_data)
        
        call_args = mock_template_service.render.call_args
        template_data = call_args[0][1]
        
        assert template_data['experiment_name'] == 'Your Experiment'  # Default value
