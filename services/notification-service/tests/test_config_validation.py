"""
Unit tests for configuration validation.
"""
import pytest
import os
from unittest.mock import patch

from config import Config, validate_config


class TestConfigValidation:
    """Test suite for configuration validation."""
    
    def test_config_loads_from_environment(self):
        """Test configuration loads from environment variables."""
        env_vars = {
            'AWS_REGION': 'us-east-1',
            'SES_FROM_EMAIL': 'noreply@turaf.com',
            'FRONTEND_URL': 'https://app.turaf.com',
            'IDEMPOTENCY_TABLE_NAME': 'processed-events',
            'EXPERIMENT_SERVICE_URL': 'https://api.turaf.com/experiments',
            'ORGANIZATION_SERVICE_URL': 'https://api.turaf.com/organizations'
        }
        
        with patch.dict(os.environ, env_vars, clear=True):
            config = Config()
            
            assert config.aws_region == 'us-east-1'
            assert config.ses_from_email == 'noreply@turaf.com'
            assert config.frontend_url == 'https://app.turaf.com'
    
    def test_config_uses_defaults_when_not_set(self):
        """Test configuration uses defaults for optional values."""
        env_vars = {
            'AWS_REGION': 'us-east-1',
            'SES_FROM_EMAIL': 'noreply@turaf.com',
            'FRONTEND_URL': 'https://app.turaf.com'
        }
        
        with patch.dict(os.environ, env_vars, clear=True):
            config = Config()
            
            assert config.idempotency_table_name == 'processed_notification_events'
    
    def test_validate_config_succeeds_with_valid_config(self):
        """Test validate_config succeeds with valid configuration."""
        env_vars = {
            'AWS_REGION': 'us-east-1',
            'SES_FROM_EMAIL': 'noreply@turaf.com',
            'FRONTEND_URL': 'https://app.turaf.com'
        }
        
        with patch.dict(os.environ, env_vars, clear=True):
            # Should not raise
            validate_config()
    
    def test_validate_config_fails_without_aws_region(self):
        """Test validate_config fails without AWS region."""
        env_vars = {
            'SES_FROM_EMAIL': 'noreply@turaf.com',
            'FRONTEND_URL': 'https://app.turaf.com'
        }
        
        with patch.dict(os.environ, env_vars, clear=True):
            with pytest.raises(ValueError) as exc_info:
                validate_config()
            
            assert 'AWS_REGION' in str(exc_info.value)
    
    def test_validate_config_fails_without_ses_from_email(self):
        """Test validate_config fails without SES from email."""
        env_vars = {
            'AWS_REGION': 'us-east-1',
            'FRONTEND_URL': 'https://app.turaf.com'
        }
        
        with patch.dict(os.environ, env_vars, clear=True):
            with pytest.raises(ValueError) as exc_info:
                validate_config()
            
            assert 'SES_FROM_EMAIL' in str(exc_info.value)
    
    def test_validate_config_fails_without_frontend_url(self):
        """Test validate_config fails without frontend URL."""
        env_vars = {
            'AWS_REGION': 'us-east-1',
            'SES_FROM_EMAIL': 'noreply@turaf.com'
        }
        
        with patch.dict(os.environ, env_vars, clear=True):
            with pytest.raises(ValueError) as exc_info:
                validate_config()
            
            assert 'FRONTEND_URL' in str(exc_info.value)
    
    def test_config_validates_email_format(self):
        """Test configuration validates email format."""
        env_vars = {
            'AWS_REGION': 'us-east-1',
            'SES_FROM_EMAIL': 'invalid-email',
            'FRONTEND_URL': 'https://app.turaf.com'
        }
        
        with patch.dict(os.environ, env_vars, clear=True):
            with pytest.raises(ValueError) as exc_info:
                validate_config()
            
            assert 'email' in str(exc_info.value).lower()
    
    def test_config_validates_url_format(self):
        """Test configuration validates URL format."""
        env_vars = {
            'AWS_REGION': 'us-east-1',
            'SES_FROM_EMAIL': 'noreply@turaf.com',
            'FRONTEND_URL': 'not-a-url'
        }
        
        with patch.dict(os.environ, env_vars, clear=True):
            with pytest.raises(ValueError) as exc_info:
                validate_config()
            
            assert 'URL' in str(exc_info.value)


class TestConfigSingleton:
    """Test configuration singleton behavior."""
    
    def test_config_is_singleton(self):
        """Test config module provides singleton instance."""
        from config import config as config1
        from config import config as config2
        
        assert config1 is config2
    
    def test_config_immutable_after_initialization(self):
        """Test configuration values are immutable."""
        from config import config
        
        original_region = config.aws_region
        
        # Attempting to modify should not affect the config
        with pytest.raises(AttributeError):
            config.aws_region = 'us-west-2'
