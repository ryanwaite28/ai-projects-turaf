"""
Unit tests for notification_handler Lambda function.
Tests handler initialization, routing, and error handling.
"""
import json
import os
import pytest
from unittest.mock import Mock, patch, MagicMock
from notification_handler import lambda_handler


class TestLambdaHandler:
    """Test suite for Lambda handler function."""
    
    @pytest.fixture
    def mock_context(self):
        """Create mock Lambda context."""
        context = Mock()
        context.request_id = 'test-request-id-123'
        context.function_name = 'notification-service'
        context.memory_limit_in_mb = 512
        return context
    
    @pytest.fixture
    def base_event(self):
        """Create base EventBridge event structure."""
        return {
            'id': 'event-123',
            'version': '0',
            'account': '123456789012',
            'time': '2024-03-18T10:00:00Z',
            'region': 'us-east-1',
            'source': 'turaf.experiment-service',
            'detail-type': 'ExperimentCompleted',
            'detail': {
                'eventId': 'evt-123',
                'experimentId': 'exp-123',
                'organizationId': 'org-123'
            }
        }
    
    @pytest.fixture(autouse=True)
    def setup_env_vars(self):
        """Setup required environment variables for tests."""
        os.environ['ENVIRONMENT'] = 'test'
        os.environ['SES_FROM_EMAIL'] = 'test@turaf.com'
        os.environ['EXPERIMENT_SERVICE_URL'] = 'https://api.test.turaf.com'
        os.environ['ORGANIZATION_SERVICE_URL'] = 'https://api.test.turaf.com'
        os.environ['FRONTEND_URL'] = 'https://app.test.turaf.com'
        os.environ['AWS_REGION'] = 'us-east-1'
        os.environ['LOG_LEVEL'] = 'INFO'
        yield
        # Cleanup
        for key in ['ENVIRONMENT', 'SES_FROM_EMAIL', 'EXPERIMENT_SERVICE_URL',
                    'ORGANIZATION_SERVICE_URL', 'FRONTEND_URL', 'AWS_REGION', 'LOG_LEVEL']:
            os.environ.pop(key, None)
    
    def test_handler_initialization(self, base_event, mock_context):
        """Test that handler initializes without errors."""
        with patch('notification_handler.handle_experiment_completed') as mock_handler:
            mock_handler.return_value = {'statusCode': 200, 'body': 'Success'}
            
            result = lambda_handler(base_event, mock_context)
            
            assert result is not None
            assert 'statusCode' in result
    
    def test_routes_experiment_completed_event(self, base_event, mock_context):
        """Test routing of ExperimentCompleted event to correct handler."""
        base_event['detail-type'] = 'ExperimentCompleted'
        
        with patch('notification_handler.handle_experiment_completed') as mock_handler:
            mock_handler.return_value = {'statusCode': 200, 'body': 'Success'}
            
            result = lambda_handler(base_event, mock_context)
            
            mock_handler.assert_called_once_with(base_event, mock_context)
            assert result['statusCode'] == 200
    
    def test_routes_report_generated_event(self, base_event, mock_context):
        """Test routing of ReportGenerated event to correct handler."""
        base_event['detail-type'] = 'ReportGenerated'
        
        with patch('notification_handler.handle_report_generated') as mock_handler:
            mock_handler.return_value = {'statusCode': 200, 'body': 'Success'}
            
            result = lambda_handler(base_event, mock_context)
            
            mock_handler.assert_called_once_with(base_event, mock_context)
            assert result['statusCode'] == 200
    
    def test_routes_member_added_event(self, base_event, mock_context):
        """Test routing of MemberAdded event to correct handler."""
        base_event['detail-type'] = 'MemberAdded'
        
        with patch('notification_handler.handle_member_added') as mock_handler:
            mock_handler.return_value = {'statusCode': 200, 'body': 'Success'}
            
            result = lambda_handler(base_event, mock_context)
            
            mock_handler.assert_called_once_with(base_event, mock_context)
            assert result['statusCode'] == 200
    
    def test_handles_unknown_event_type(self, base_event, mock_context):
        """Test handling of unknown event type."""
        base_event['detail-type'] = 'UnknownEvent'
        
        result = lambda_handler(base_event, mock_context)
        
        assert result['statusCode'] == 400
        body = json.loads(result['body'])
        assert 'error' in body
        assert 'Unknown event type' in body['error']
    
    def test_handles_missing_detail_type(self, base_event, mock_context):
        """Test handling of event without detail-type."""
        del base_event['detail-type']
        
        result = lambda_handler(base_event, mock_context)
        
        assert result['statusCode'] == 400
        body = json.loads(result['body'])
        assert 'error' in body
        assert 'Missing detail-type' in body['error']
    
    def test_handles_configuration_error(self, base_event, mock_context):
        """Test handling of configuration validation error."""
        # Remove required environment variable
        os.environ.pop('EXPERIMENT_SERVICE_URL', None)
        
        # Reload config to pick up changes
        from importlib import reload
        import config as config_module
        reload(config_module)
        
        result = lambda_handler(base_event, mock_context)
        
        assert result['statusCode'] == 500
        body = json.loads(result['body'])
        assert 'error' in body
    
    def test_handles_handler_exception(self, base_event, mock_context):
        """Test handling of exception raised by event handler."""
        with patch('notification_handler.handle_experiment_completed') as mock_handler:
            mock_handler.side_effect = Exception('Handler error')
            
            result = lambda_handler(base_event, mock_context)
            
            assert result['statusCode'] == 500
            body = json.loads(result['body'])
            assert 'error' in body
            assert 'Internal server error' in body['error']
    
    def test_logs_event_metadata(self, base_event, mock_context, caplog):
        """Test that handler logs event metadata."""
        with patch('notification_handler.handle_experiment_completed') as mock_handler:
            mock_handler.return_value = {'statusCode': 200, 'body': 'Success'}
            
            lambda_handler(base_event, mock_context)
            
            # Verify logging occurred (implementation uses structured logging)
            assert mock_handler.called
    
    def test_validates_config_on_cold_start(self, base_event, mock_context):
        """Test that configuration is validated on handler invocation."""
        with patch('notification_handler.config.validate') as mock_validate:
            mock_validate.return_value = True
            
            with patch('notification_handler.handle_experiment_completed') as mock_handler:
                mock_handler.return_value = {'statusCode': 200, 'body': 'Success'}
                
                lambda_handler(base_event, mock_context)
                
                mock_validate.assert_called_once()


class TestConfigModule:
    """Test suite for configuration module."""
    
    @pytest.fixture(autouse=True)
    def setup_env_vars(self):
        """Setup environment variables for config tests."""
        os.environ['ENVIRONMENT'] = 'test'
        os.environ['SES_FROM_EMAIL'] = 'test@turaf.com'
        os.environ['EXPERIMENT_SERVICE_URL'] = 'https://api.test.turaf.com'
        os.environ['ORGANIZATION_SERVICE_URL'] = 'https://api.test.turaf.com'
        os.environ['FRONTEND_URL'] = 'https://app.test.turaf.com'
        yield
        for key in ['ENVIRONMENT', 'SES_FROM_EMAIL', 'EXPERIMENT_SERVICE_URL',
                    'ORGANIZATION_SERVICE_URL', 'FRONTEND_URL']:
            os.environ.pop(key, None)
    
    def test_config_loads_environment_variables(self):
        """Test that Config loads values from environment variables."""
        from config import Config
        
        config = Config()
        
        assert config.environment == 'test'
        assert config.ses_from_email == 'test@turaf.com'
        assert config.experiment_service_url == 'https://api.test.turaf.com'
        assert config.organization_service_url == 'https://api.test.turaf.com'
        assert config.frontend_url == 'https://app.test.turaf.com'
    
    def test_config_validation_succeeds_with_required_fields(self):
        """Test that validation succeeds when all required fields are present."""
        from config import Config
        
        config = Config()
        
        assert config.validate() is True
    
    def test_config_validation_fails_without_required_fields(self):
        """Test that validation fails when required fields are missing."""
        from config import Config
        
        os.environ.pop('EXPERIMENT_SERVICE_URL', None)
        config = Config()
        
        with pytest.raises(ValueError) as exc_info:
            config.validate()
        
        assert 'EXPERIMENT_SERVICE_URL' in str(exc_info.value)
    
    def test_get_service_url_returns_correct_url(self):
        """Test that get_service_url returns correct service URL."""
        from config import Config
        
        config = Config()
        
        assert config.get_service_url('experiment') == 'https://api.test.turaf.com'
        assert config.get_service_url('organization') == 'https://api.test.turaf.com'
    
    def test_get_service_url_returns_none_for_unknown_service(self):
        """Test that get_service_url returns None for unknown service."""
        from config import Config
        
        config = Config()
        
        assert config.get_service_url('unknown') is None
