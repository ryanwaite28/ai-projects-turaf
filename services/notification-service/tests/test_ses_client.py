"""
Unit tests for SES client.
"""
import pytest
from unittest.mock import Mock, patch, MagicMock
from botocore.exceptions import ClientError

from services.ses_client import SesClient, SesClientError
from models.email import EmailMessage


class TestSesClient:
    """Test suite for SesClient."""
    
    @pytest.fixture
    def mock_ses_boto_client(self):
        """Create mock boto3 SES client."""
        mock_client = Mock()
        mock_client.send_email.return_value = {'MessageId': 'test-message-id-123'}
        return mock_client
    
    @pytest.fixture
    def ses_client(self, mock_ses_boto_client):
        """Create SesClient with mocked boto3 client."""
        with patch('services.ses_client.boto3.client', return_value=mock_ses_boto_client):
            with patch('services.ses_client.config') as mock_config:
                mock_config.aws_region = 'us-east-1'
                mock_config.ses_from_email = 'test@turaf.com'
                client = SesClient()
                client.ses_client = mock_ses_boto_client
                return client
    
    @pytest.fixture
    def valid_email_message(self):
        """Create valid email message."""
        return EmailMessage(
            recipients=['recipient@example.com'],
            subject='Test Subject',
            html_body='<p>Test Body</p>'
        )
    
    def test_initializes_with_from_email(self):
        """Test SesClient initializes with from email."""
        with patch('services.ses_client.boto3.client'):
            with patch('services.ses_client.config') as mock_config:
                mock_config.aws_region = 'us-east-1'
                mock_config.ses_from_email = 'test@turaf.com'
                
                client = SesClient()
                
                assert client.from_email == 'test@turaf.com'
    
    def test_sends_email_successfully(self, ses_client, mock_ses_boto_client, valid_email_message):
        """Test successful email sending."""
        message_id = ses_client.send_email(valid_email_message)
        
        assert message_id == 'test-message-id-123'
        mock_ses_boto_client.send_email.assert_called_once()
    
    def test_sends_email_with_correct_parameters(
        self, ses_client, mock_ses_boto_client, valid_email_message
    ):
        """Test email is sent with correct SES parameters."""
        ses_client.send_email(valid_email_message)
        
        call_args = mock_ses_boto_client.send_email.call_args
        params = call_args.kwargs
        
        assert params['Source'] == 'test@turaf.com'
        assert params['Destination']['ToAddresses'] == ['recipient@example.com']
        assert params['Message']['Subject']['Data'] == 'Test Subject'
        assert params['Message']['Body']['Html']['Data'] == '<p>Test Body</p>'
    
    def test_sends_email_with_text_body(self, ses_client, mock_ses_boto_client):
        """Test email with text body."""
        message = EmailMessage(
            recipients=['test@example.com'],
            subject='Test',
            html_body='<p>HTML</p>',
            text_body='Plain text'
        )
        
        ses_client.send_email(message)
        
        call_args = mock_ses_boto_client.send_email.call_args
        params = call_args.kwargs
        
        assert 'Text' in params['Message']['Body']
        assert params['Message']['Body']['Text']['Data'] == 'Plain text'
    
    def test_sends_email_with_reply_to(self, ses_client, mock_ses_boto_client):
        """Test email with reply-to address."""
        message = EmailMessage(
            recipients=['test@example.com'],
            subject='Test',
            html_body='<p>Body</p>',
            reply_to='reply@example.com'
        )
        
        ses_client.send_email(message)
        
        call_args = mock_ses_boto_client.send_email.call_args
        params = call_args.kwargs
        
        assert params['ReplyToAddresses'] == ['reply@example.com']
    
    def test_raises_error_on_ses_client_error(self, ses_client, mock_ses_boto_client):
        """Test error handling for SES client errors."""
        mock_ses_boto_client.send_email.side_effect = ClientError(
            {'Error': {'Code': 'MessageRejected', 'Message': 'Email rejected'}},
            'SendEmail'
        )
        
        message = EmailMessage(
            recipients=['test@example.com'],
            subject='Test',
            html_body='<p>Body</p>'
        )
        
        with pytest.raises(SesClientError) as exc_info:
            ses_client.send_email(message)
        
        assert 'Email rejected' in str(exc_info.value)
    
    def test_validates_message_before_sending(self, ses_client, mock_ses_boto_client):
        """Test message validation before sending."""
        invalid_message = EmailMessage(
            recipients=[],  # Invalid: no recipients
            subject='Test',
            html_body='<p>Body</p>'
        )
        
        with pytest.raises(ValueError):
            ses_client.send_email(invalid_message)
        
        # Should not call SES
        mock_ses_boto_client.send_email.assert_not_called()
    
    def test_send_bulk_email_sends_multiple_messages(self, ses_client):
        """Test bulk email sending."""
        messages = [
            EmailMessage(
                recipients=[f'test{i}@example.com'],
                subject='Test',
                html_body='<p>Body</p>'
            )
            for i in range(3)
        ]
        
        message_ids = ses_client.send_bulk_email(messages)
        
        assert len(message_ids) == 3
        assert all(mid == 'test-message-id-123' for mid in message_ids)
    
    def test_send_bulk_email_continues_on_individual_failures(self, ses_client, mock_ses_boto_client):
        """Test bulk email continues even if individual emails fail."""
        # First call fails, second succeeds
        mock_ses_boto_client.send_email.side_effect = [
            ClientError({'Error': {'Code': 'Error', 'Message': 'Failed'}}, 'SendEmail'),
            {'MessageId': 'success-id'}
        ]
        
        messages = [
            EmailMessage(recipients=['test1@example.com'], subject='Test', html_body='<p>Body</p>'),
            EmailMessage(recipients=['test2@example.com'], subject='Test', html_body='<p>Body</p>')
        ]
        
        message_ids = ses_client.send_bulk_email(messages)
        
        # Should have one successful send
        assert len(message_ids) == 1
        assert message_ids[0] == 'success-id'
    
    def test_uses_message_from_email_if_provided(self, ses_client, mock_ses_boto_client):
        """Test uses message-specific from email if provided."""
        message = EmailMessage(
            recipients=['test@example.com'],
            subject='Test',
            html_body='<p>Body</p>',
            from_email='custom@example.com'
        )
        
        ses_client.send_email(message)
        
        call_args = mock_ses_boto_client.send_email.call_args
        params = call_args.kwargs
        
        assert params['Source'] == 'custom@example.com'
