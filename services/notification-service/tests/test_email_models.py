"""
Unit tests for email domain models.
"""
import pytest
from models.email import EmailRecipient, EmailMessage, EmailTemplate


class TestEmailRecipient:
    """Test suite for EmailRecipient model."""
    
    def test_creates_recipient_with_email_only(self):
        """Test creating recipient with email only."""
        recipient = EmailRecipient(email='test@example.com')
        
        assert recipient.email == 'test@example.com'
        assert recipient.name is None
    
    def test_creates_recipient_with_name(self):
        """Test creating recipient with name."""
        recipient = EmailRecipient(email='test@example.com', name='Test User')
        
        assert recipient.email == 'test@example.com'
        assert recipient.name == 'Test User'
    
    def test_formats_recipient_with_name(self):
        """Test string formatting with name."""
        recipient = EmailRecipient(email='test@example.com', name='Test User')
        
        assert str(recipient) == 'Test User <test@example.com>'
    
    def test_formats_recipient_without_name(self):
        """Test string formatting without name."""
        recipient = EmailRecipient(email='test@example.com')
        
        assert str(recipient) == 'test@example.com'


class TestEmailMessage:
    """Test suite for EmailMessage model."""
    
    def test_creates_valid_email_message(self):
        """Test creating valid email message."""
        message = EmailMessage(
            recipients=['test@example.com'],
            subject='Test Subject',
            html_body='<p>Test Body</p>'
        )
        
        assert message.recipients == ['test@example.com']
        assert message.subject == 'Test Subject'
        assert message.html_body == '<p>Test Body</p>'
    
    def test_validates_successfully_with_valid_data(self):
        """Test validation passes with valid data."""
        message = EmailMessage(
            recipients=['test@example.com'],
            subject='Test',
            html_body='<p>Body</p>'
        )
        
        # Should not raise
        message.validate()
    
    def test_validation_fails_without_recipients(self):
        """Test validation fails without recipients."""
        message = EmailMessage(
            recipients=[],
            subject='Test',
            html_body='<p>Body</p>'
        )
        
        with pytest.raises(ValueError) as exc_info:
            message.validate()
        
        assert 'at least one recipient' in str(exc_info.value)
    
    def test_validation_fails_without_subject(self):
        """Test validation fails without subject."""
        message = EmailMessage(
            recipients=['test@example.com'],
            subject='',
            html_body='<p>Body</p>'
        )
        
        with pytest.raises(ValueError) as exc_info:
            message.validate()
        
        assert 'subject' in str(exc_info.value)
    
    def test_validation_fails_without_html_body(self):
        """Test validation fails without HTML body."""
        message = EmailMessage(
            recipients=['test@example.com'],
            subject='Test',
            html_body=''
        )
        
        with pytest.raises(ValueError) as exc_info:
            message.validate()
        
        assert 'HTML body' in str(exc_info.value)
    
    def test_validation_fails_with_invalid_email(self):
        """Test validation fails with invalid email address."""
        message = EmailMessage(
            recipients=['invalid-email'],
            subject='Test',
            html_body='<p>Body</p>'
        )
        
        with pytest.raises(ValueError) as exc_info:
            message.validate()
        
        assert 'Invalid email address' in str(exc_info.value)
    
    def test_supports_multiple_recipients(self):
        """Test message with multiple recipients."""
        message = EmailMessage(
            recipients=['test1@example.com', 'test2@example.com'],
            subject='Test',
            html_body='<p>Body</p>'
        )
        
        message.validate()
        assert len(message.recipients) == 2
    
    def test_supports_optional_text_body(self):
        """Test message with optional text body."""
        message = EmailMessage(
            recipients=['test@example.com'],
            subject='Test',
            html_body='<p>Body</p>',
            text_body='Plain text body'
        )
        
        assert message.text_body == 'Plain text body'
    
    def test_supports_optional_reply_to(self):
        """Test message with optional reply-to."""
        message = EmailMessage(
            recipients=['test@example.com'],
            subject='Test',
            html_body='<p>Body</p>',
            reply_to='reply@example.com'
        )
        
        assert message.reply_to == 'reply@example.com'


class TestEmailTemplate:
    """Test suite for EmailTemplate model."""
    
    def test_creates_email_template(self):
        """Test creating email template."""
        template = EmailTemplate(
            template_name='test-template',
            data={'key': 'value'},
            subject='Test Subject'
        )
        
        assert template.template_name == 'test-template'
        assert template.data == {'key': 'value'}
        assert template.subject == 'Test Subject'
