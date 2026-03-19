"""
Unit tests for ReportGenerationService.

Tests cover report generation, validation, and error handling.
"""

import pytest
from src.services.report_generation import ReportGenerationService


class TestReportGenerationService:
    """Test suite for ReportGenerationService."""
    
    @pytest.fixture
    def service(self):
        """Create service instance for tests."""
        return ReportGenerationService()
    
    def test_service_initialization(self, service):
        """Test that service initializes correctly."""
        assert service is not None
    
    def test_generate_report_with_valid_data(self, service):
        """Test generating report with valid experiment data."""
        # Given
        experiment_event = {
            'experimentId': 'exp-123',
            'organizationId': 'org-456',
            'completedAt': '2024-01-01T12:00:00Z',
            'result': 'SUCCESS'
        }
        
        # When
        report = service.generate_report(experiment_event)
        
        # Then
        assert report is not None
        assert 'id' in report
        assert report['experimentId'] == 'exp-123'
        assert report['organizationId'] == 'org-456'
        assert report['status'] == 'generated'
    
    def test_generate_report_creates_unique_id(self, service):
        """Test that each report gets a unique ID."""
        # Given
        experiment_event = {
            'experimentId': 'exp-123',
            'organizationId': 'org-456',
            'completedAt': '2024-01-01T12:00:00Z'
        }
        
        # When
        report1 = service.generate_report(experiment_event)
        report2 = service.generate_report(experiment_event)
        
        # Then
        assert report1['id'] != report2['id']
    
    def test_generate_report_raises_error_for_missing_experiment_id(self, service):
        """Test that service raises error when experimentId is missing."""
        # Given
        experiment_event = {
            'organizationId': 'org-456',
            'completedAt': '2024-01-01T12:00:00Z'
        }
        
        # When/Then
        with pytest.raises(ValueError) as exc_info:
            service.generate_report(experiment_event)
        assert 'experimentId is required' in str(exc_info.value)
    
    def test_generate_report_raises_error_for_missing_organization_id(self, service):
        """Test that service raises error when organizationId is missing."""
        # Given
        experiment_event = {
            'experimentId': 'exp-123',
            'completedAt': '2024-01-01T12:00:00Z'
        }
        
        # When/Then
        with pytest.raises(ValueError) as exc_info:
            service.generate_report(experiment_event)
        assert 'organizationId is required' in str(exc_info.value)
    
    def test_generate_report_includes_optional_fields(self, service):
        """Test that report includes optional fields from event."""
        # Given
        experiment_event = {
            'experimentId': 'exp-123',
            'organizationId': 'org-456',
            'completedAt': '2024-01-01T12:00:00Z',
            'result': 'FAILURE'
        }
        
        # When
        report = service.generate_report(experiment_event)
        
        # Then
        assert report['completedAt'] == '2024-01-01T12:00:00Z'
        assert report['result'] == 'FAILURE'
    
    def test_generate_report_logs_activity(self, service, caplog):
        """Test that service logs report generation activity."""
        # Given
        experiment_event = {
            'experimentId': 'exp-123',
            'organizationId': 'org-456',
            'completedAt': '2024-01-01T12:00:00Z'
        }
        
        # When
        with caplog.at_level('INFO'):
            report = service.generate_report(experiment_event)
        
        # Then
        assert 'Generating report for experiment: exp-123' in caplog.text
        assert f'Report {report["id"]} generated successfully' in caplog.text
