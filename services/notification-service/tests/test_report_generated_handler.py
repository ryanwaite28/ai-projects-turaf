"""
Unit tests for ReportGenerated event handler.
"""
import pytest
from unittest.mock import Mock
from handlers.report_generated import handle_report_generated


class TestReportGeneratedHandler:
    """Test suite for ReportGenerated handler."""
    
    @pytest.fixture
    def mock_context(self):
        """Create mock Lambda context."""
        context = Mock()
        context.request_id = 'test-request-123'
        return context
    
    @pytest.fixture
    def valid_event(self):
        """Create valid ReportGenerated event."""
        return {
            'id': 'event-456',
            'version': '0',
            'detail-type': 'ReportGenerated',
            'source': 'turaf.reporting-service',
            'account': '123456789012',
            'time': '2024-03-18T10:05:00Z',
            'region': 'us-east-1',
            'detail': {
                'eventId': 'evt-456',
                'reportId': 'rpt-123',
                'experimentId': 'exp-123',
                'organizationId': 'org-123',
                'reportUrl': 'https://s3.amazonaws.com/reports/rpt-123.pdf',
                'generatedAt': '2024-03-18T10:05:00Z'
            }
        }
    
    def test_handles_valid_event_successfully(self, valid_event, mock_context):
        """Test successful handling of valid event."""
        result = handle_report_generated(valid_event, mock_context)
        
        assert result['statusCode'] == 200
        assert 'successfully' in result['body']
    
    def test_extracts_report_id_from_event(self, valid_event, mock_context):
        """Test that handler extracts reportId from event."""
        result = handle_report_generated(valid_event, mock_context)
        
        assert result['statusCode'] == 200
    
    def test_returns_400_when_report_id_missing(self, valid_event, mock_context):
        """Test validation error when reportId is missing."""
        del valid_event['detail']['reportId']
        
        result = handle_report_generated(valid_event, mock_context)
        
        assert result['statusCode'] == 400
        assert 'reportId' in result['body']
    
    def test_returns_400_when_experiment_id_missing(self, valid_event, mock_context):
        """Test validation error when experimentId is missing."""
        del valid_event['detail']['experimentId']
        
        result = handle_report_generated(valid_event, mock_context)
        
        assert result['statusCode'] == 400
        assert 'experimentId' in result['body']
    
    def test_returns_400_when_organization_id_missing(self, valid_event, mock_context):
        """Test validation error when organizationId is missing."""
        del valid_event['detail']['organizationId']
        
        result = handle_report_generated(valid_event, mock_context)
        
        assert result['statusCode'] == 400
        assert 'organizationId' in result['body']
    
    def test_handles_optional_report_url(self, valid_event, mock_context):
        """Test that reportUrl is optional."""
        del valid_event['detail']['reportUrl']
        
        result = handle_report_generated(valid_event, mock_context)
        
        assert result['statusCode'] == 200
    
    def test_logs_event_processing(self, valid_event, mock_context, caplog):
        """Test that handler logs event processing."""
        handle_report_generated(valid_event, mock_context)
        
        assert len(caplog.records) > 0
