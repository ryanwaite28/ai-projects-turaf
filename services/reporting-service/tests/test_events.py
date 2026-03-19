"""
Unit tests for event models.

Tests cover event creation, parsing, and validation.
"""

import pytest
from datetime import datetime
from src.models.events import ExperimentCompletedEvent


class TestExperimentCompletedEvent:
    """Test suite for ExperimentCompletedEvent model."""
    
    def test_event_creation(self):
        """Test creating event with all fields."""
        # Given/When
        event = ExperimentCompletedEvent(
            event_id='evt-123',
            experiment_id='exp-456',
            organization_id='org-789',
            completed_at=datetime(2024, 1, 1, 12, 0, 0),
            result='SUCCESS'
        )
        
        # Then
        assert event.event_id == 'evt-123'
        assert event.experiment_id == 'exp-456'
        assert event.organization_id == 'org-789'
        assert event.completed_at == datetime(2024, 1, 1, 12, 0, 0)
        assert event.result == 'SUCCESS'
    
    def test_event_creation_without_optional_result(self):
        """Test creating event without optional result field."""
        # Given/When
        event = ExperimentCompletedEvent(
            event_id='evt-123',
            experiment_id='exp-456',
            organization_id='org-789',
            completed_at=datetime(2024, 1, 1, 12, 0, 0)
        )
        
        # Then
        assert event.result is None
    
    def test_from_dict_with_all_fields(self):
        """Test creating event from dictionary with all fields."""
        # Given
        data = {
            'eventId': 'evt-123',
            'experimentId': 'exp-456',
            'organizationId': 'org-789',
            'completedAt': '2024-01-01T12:00:00Z',
            'result': 'SUCCESS'
        }
        
        # When
        event = ExperimentCompletedEvent.from_dict(data)
        
        # Then
        assert event.event_id == 'evt-123'
        assert event.experiment_id == 'exp-456'
        assert event.organization_id == 'org-789'
        assert event.result == 'SUCCESS'
        assert isinstance(event.completed_at, datetime)
    
    def test_from_dict_without_optional_result(self):
        """Test creating event from dictionary without result."""
        # Given
        data = {
            'eventId': 'evt-123',
            'experimentId': 'exp-456',
            'organizationId': 'org-789',
            'completedAt': '2024-01-01T12:00:00Z'
        }
        
        # When
        event = ExperimentCompletedEvent.from_dict(data)
        
        # Then
        assert event.result is None
    
    def test_from_dict_raises_error_for_missing_required_field(self):
        """Test that from_dict raises KeyError for missing required fields."""
        # Given
        data = {
            'eventId': 'evt-123',
            'experimentId': 'exp-456'
            # Missing organizationId and completedAt
        }
        
        # When/Then
        with pytest.raises(KeyError):
            ExperimentCompletedEvent.from_dict(data)
    
    def test_from_dict_parses_iso_timestamp(self):
        """Test that from_dict correctly parses ISO-8601 timestamps."""
        # Given
        data = {
            'eventId': 'evt-123',
            'experimentId': 'exp-456',
            'organizationId': 'org-789',
            'completedAt': '2024-01-01T12:30:45Z'
        }
        
        # When
        event = ExperimentCompletedEvent.from_dict(data)
        
        # Then
        assert event.completed_at.year == 2024
        assert event.completed_at.month == 1
        assert event.completed_at.day == 1
        assert event.completed_at.hour == 12
        assert event.completed_at.minute == 30
        assert event.completed_at.second == 45
    
    def test_to_dict(self):
        """Test converting event to dictionary."""
        # Given
        event = ExperimentCompletedEvent(
            event_id='evt-123',
            experiment_id='exp-456',
            organization_id='org-789',
            completed_at=datetime(2024, 1, 1, 12, 0, 0),
            result='SUCCESS'
        )
        
        # When
        data = event.to_dict()
        
        # Then
        assert data['eventId'] == 'evt-123'
        assert data['experimentId'] == 'exp-456'
        assert data['organizationId'] == 'org-789'
        assert data['result'] == 'SUCCESS'
        assert 'completedAt' in data
    
    def test_to_dict_includes_none_result(self):
        """Test that to_dict includes result even when None."""
        # Given
        event = ExperimentCompletedEvent(
            event_id='evt-123',
            experiment_id='exp-456',
            organization_id='org-789',
            completed_at=datetime(2024, 1, 1, 12, 0, 0)
        )
        
        # When
        data = event.to_dict()
        
        # Then
        assert 'result' in data
        assert data['result'] is None
