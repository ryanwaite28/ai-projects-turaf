"""
Domain event models for the Reporting Service.

This module defines event models following DDD principles, representing
domain events that occur in the system.
"""

from dataclasses import dataclass, asdict
from typing import Optional
from datetime import datetime
import json


@dataclass
class ExperimentCompletedEvent:
    """
    Domain event representing a completed experiment.
    
    This event is published by the Experiment Service when an experiment
    completes and triggers report generation in the Reporting Service.
    
    Attributes:
        event_id: Unique identifier for this event instance
        experiment_id: Identifier of the completed experiment
        organization_id: Identifier of the organization owning the experiment
        completed_at: Timestamp when the experiment completed
        result: Optional result status (SUCCESS, FAILURE, etc.)
    """
    event_id: str
    experiment_id: str
    organization_id: str
    completed_at: datetime
    result: Optional[str] = None
    
    @classmethod
    def from_dict(cls, data: dict) -> 'ExperimentCompletedEvent':
        """
        Create event from dictionary representation.
        
        Args:
            data: Dictionary containing event data with keys:
                - eventId: Event identifier
                - experimentId: Experiment identifier
                - organizationId: Organization identifier
                - completedAt: ISO-8601 timestamp string
                - result: Optional result status
                
        Returns:
            ExperimentCompletedEvent instance
            
        Raises:
            KeyError: If required fields are missing
            ValueError: If timestamp format is invalid
            
        Example:
            >>> data = {
            ...     'eventId': 'evt-123',
            ...     'experimentId': 'exp-456',
            ...     'organizationId': 'org-789',
            ...     'completedAt': '2024-01-01T12:00:00Z'
            ... }
            >>> event = ExperimentCompletedEvent.from_dict(data)
            >>> event.experiment_id
            'exp-456'
        """
        return cls(
            event_id=data['eventId'],
            experiment_id=data['experimentId'],
            organization_id=data['organizationId'],
            completed_at=datetime.fromisoformat(data['completedAt'].replace('Z', '+00:00')),
            result=data.get('result')
        )
    
    def to_dict(self) -> dict:
        """
        Convert event to dictionary representation.
        
        Returns:
            Dictionary with event data
        """
        return {
            'eventId': self.event_id,
            'experimentId': self.experiment_id,
            'organizationId': self.organization_id,
            'completedAt': self.completed_at.isoformat(),
            'result': self.result
        }


@dataclass
class ReportGeneratedEvent:
    """
    Domain event representing a generated report.
    
    This event is published by the Reporting Service when a report has been
    successfully generated and stored, allowing other services to react to
    report availability.
    
    Attributes:
        event_id: Unique identifier for this event instance
        organization_id: Identifier of the organization owning the report
        experiment_id: Identifier of the experiment the report is for
        report_id: Unique identifier for the generated report
        report_location: S3 location where the report is stored
        report_format: Format of the report (e.g., 'PDF', 'HTML')
        generated_at: Timestamp when the report was generated
    """
    event_id: str
    organization_id: str
    experiment_id: str
    report_id: str
    report_location: str
    report_format: str
    generated_at: datetime
    
    def to_dict(self) -> dict:
        """
        Convert event to dictionary representation.
        
        Returns:
            Dictionary with event data, timestamps as ISO-8601 strings
            
        Example:
            >>> event = ReportGeneratedEvent(
            ...     event_id='evt-123',
            ...     organization_id='org-456',
            ...     experiment_id='exp-789',
            ...     report_id='rpt-101',
            ...     report_location='s3://bucket/path',
            ...     report_format='PDF',
            ...     generated_at=datetime(2024, 1, 1, 12, 0, 0)
            ... )
            >>> data = event.to_dict()
            >>> data['report_id']
            'rpt-101'
        """
        data = asdict(self)
        data['generated_at'] = self.generated_at.isoformat()
        return data
    
    def to_json(self) -> str:
        """
        Convert event to JSON string.
        
        Returns:
            JSON string representation of the event
            
        Example:
            >>> event = ReportGeneratedEvent(...)
            >>> json_str = event.to_json()
            >>> 'report_id' in json_str
            True
        """
        return json.dumps(self.to_dict())
