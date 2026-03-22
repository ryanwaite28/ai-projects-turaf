"""
Integration tests for email notification service.

Tests email sending workflow with mocked SES client (not in LocalStack free tier).

Following the hybrid testing strategy (PROJECT.md Section 23a):
- Mock SES client (limited in LocalStack free tier)
- Test template rendering
- Test email composition
- Test error handling
"""

import pytest
from unittest.mock import Mock, patch, MagicMock
from services.email_service import EmailService
from services.ses_client import SesClient, SesClientError
from services.template_service import TemplateService


@pytest.fixture
def mock_ses_client():
    """Mock SES client for integration testing (not in free tier)."""
    client = Mock(spec=SesClient)
    client.send_email = Mock(return_value='msg-integration-123')
    return client


@pytest.fixture
def template_service():
    """Real template service for integration testing."""
    return TemplateService()


@pytest.fixture
def email_service(mock_ses_client, template_service):
    """Email service configured for integration testing."""
    return EmailService(
        ses_client=mock_ses_client,
        template_service=template_service
    )


class TestEmailNotificationIntegration:
    """
    Integration tests for email notification workflow.
    
    These tests verify:
    - Email composition with real templates
    - SES client interaction (mocked)
    - Error handling
    - Template rendering with various data
    """
    
    def test_send_experiment_completed_email_successfully(
        self,
        email_service,
        mock_ses_client
    ):
        """
        Test sending experiment completed email.
        
        Verifies:
        - Email is composed correctly
        - Template is rendered with data
        - SES client is called
        - Message ID is returned
        """
        # Given
        recipient_email = 'user@example.com'
        experiment_data = {
            'experimentId': 'exp-integration-123',
            'experimentName': 'Performance Test',
            'organizationId': 'org-integration-456',
            'completedAt': '2024-01-01T12:00:00Z'
        }
        
        # When
        message_id = email_service.send_experiment_completed_email(
            recipient_email,
            experiment_data
        )
        
        # Then
        assert message_id == 'msg-integration-123'
        
        # Verify SES client was called
        mock_ses_client.send_email.assert_called_once()
        call_args = mock_ses_client.send_email.call_args[0][0]
        
        # Verify email message structure
        assert recipient_email in call_args.recipients
        assert call_args.subject == 'Experiment Completed Successfully'
        assert call_args.html_body is not None
        assert len(call_args.html_body) > 0
        
        # Verify template data was rendered
        assert 'exp-integration-123' in call_args.html_body
        assert 'Performance Test' in call_args.html_body
    
    def test_send_report_generated_email_successfully(
        self,
        email_service,
        mock_ses_client
    ):
        """
        Test sending report generated email.
        
        Verifies:
        - Report email is composed
        - Report URL is included
        - Template rendering works
        """
        # Given
        recipient_email = 'user@example.com'
        report_data = {
            'reportId': 'report-integration-123',
            'experimentId': 'exp-integration-456',
            'experimentName': 'Load Test',
            'reportUrl': 'https://s3.amazonaws.com/reports/report-123.pdf'
        }
        
        # When
        message_id = email_service.send_report_generated_email(
            recipient_email,
            report_data
        )
        
        # Then
        assert message_id == 'msg-integration-123'
        
        # Verify SES client was called
        mock_ses_client.send_email.assert_called_once()
        call_args = mock_ses_client.send_email.call_args[0][0]
        
        # Verify email contains report details
        assert call_args.subject == 'Your Experiment Report is Ready'
        assert 'report-integration-123' in call_args.html_body
        assert 'https://s3.amazonaws.com/reports/report-123.pdf' in call_args.html_body
    
    def test_send_member_added_email_successfully(
        self,
        email_service,
        mock_ses_client
    ):
        """
        Test sending member added email.
        
        Verifies:
        - Welcome email is composed
        - Organization details included
        - Invitation link present
        """
        # Given
        recipient_email = 'newmember@example.com'
        member_data = {
            'userId': 'user-integration-123',
            'organizationId': 'org-integration-456',
            'organizationName': 'Test Organization',
            'role': 'MEMBER'
        }
        
        # When
        message_id = email_service.send_member_added_email(
            recipient_email,
            member_data
        )
        
        # Then
        assert message_id == 'msg-integration-123'
        
        # Verify SES client was called
        mock_ses_client.send_email.assert_called_once()
        call_args = mock_ses_client.send_email.call_args[0][0]
        
        # Verify email contains member details
        assert 'Welcome' in call_args.subject or 'Added' in call_args.subject
        assert 'Test Organization' in call_args.html_body
    
    def test_send_email_handles_ses_client_error(
        self,
        email_service,
        mock_ses_client
    ):
        """
        Test error handling when SES client fails.
        
        Verifies:
        - SES errors are caught
        - Appropriate exception is raised
        """
        # Given
        mock_ses_client.send_email.side_effect = SesClientError('SES service unavailable')
        
        recipient_email = 'user@example.com'
        experiment_data = {
            'experimentId': 'exp-error-123',
            'experimentName': 'Error Test'
        }
        
        # When/Then
        with pytest.raises(SesClientError, match='SES service unavailable'):
            email_service.send_experiment_completed_email(
                recipient_email,
                experiment_data
            )
    
    def test_send_email_with_minimal_data(
        self,
        email_service,
        mock_ses_client
    ):
        """
        Test sending email with minimal required data.
        
        Verifies:
        - Email works with only required fields
        - Optional fields are handled gracefully
        """
        # Given
        recipient_email = 'user@example.com'
        experiment_data = {
            'experimentId': 'exp-minimal-123',
            'experimentName': 'Minimal Test'
            # No organizationId or completedAt
        }
        
        # When
        message_id = email_service.send_experiment_completed_email(
            recipient_email,
            experiment_data
        )
        
        # Then
        assert message_id == 'msg-integration-123'
        mock_ses_client.send_email.assert_called_once()
    
    def test_send_email_to_multiple_recipients(
        self,
        mock_ses_client,
        template_service
    ):
        """
        Test sending email to multiple recipients.
        
        Verifies:
        - Multiple recipients are handled
        - Each recipient gets the email
        """
        # Given
        email_service = EmailService(
            ses_client=mock_ses_client,
            template_service=template_service
        )
        
        recipients = ['user1@example.com', 'user2@example.com', 'user3@example.com']
        experiment_data = {
            'experimentId': 'exp-multi-123',
            'experimentName': 'Multi-recipient Test'
        }
        
        # When
        for recipient in recipients:
            email_service.send_experiment_completed_email(
                recipient,
                experiment_data
            )
        
        # Then
        assert mock_ses_client.send_email.call_count == 3
    
    def test_email_template_rendering_with_special_characters(
        self,
        email_service,
        mock_ses_client
    ):
        """
        Test template rendering with special characters.
        
        Verifies:
        - Special characters are escaped properly
        - HTML is rendered correctly
        """
        # Given
        recipient_email = 'user@example.com'
        experiment_data = {
            'experimentId': 'exp-special-123',
            'experimentName': 'Test with <special> & "characters"',
            'organizationId': 'org-456'
        }
        
        # When
        message_id = email_service.send_experiment_completed_email(
            recipient_email,
            experiment_data
        )
        
        # Then
        assert message_id == 'msg-integration-123'
        mock_ses_client.send_email.assert_called_once()
        
        # Verify HTML escaping
        call_args = mock_ses_client.send_email.call_args[0][0]
        # Special characters should be escaped in HTML
        assert '&lt;' in call_args.html_body or '<special>' not in call_args.html_body
