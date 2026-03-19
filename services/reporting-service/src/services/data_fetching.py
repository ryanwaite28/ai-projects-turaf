"""
Data fetching service for report generation.

This module orchestrates fetching data from multiple services,
following the Application Service pattern from DDD.
"""

import logging
from typing import Dict, Any
from clients.experiment_client import ExperimentServiceClient
from clients.metrics_client import MetricsServiceClient
from models.report_data import ReportData

logger = logging.getLogger(__name__)


class DataFetchingService:
    """
    Application service for fetching all data needed for report generation.
    
    This service orchestrates calls to multiple external services,
    following the Application Service pattern from DDD. It coordinates
    the workflow but delegates actual HTTP communication to client classes.
    
    Responsibilities:
    - Orchestrate data fetching from multiple services
    - Handle the sequence of dependent calls (experiment → hypothesis → problem)
    - Aggregate all data into a single ReportData object
    - Provide error context for failures
    
    The service follows the Single Responsibility Principle (SOLID),
    focusing solely on data orchestration without business logic.
    
    Attributes:
        experiment_client: Client for Experiment Service API
        metrics_client: Client for Metrics Service API
    """
    
    def __init__(
        self,
        experiment_client: ExperimentServiceClient = None,
        metrics_client: MetricsServiceClient = None
    ):
        """
        Initialize the data fetching service.
        
        Args:
            experiment_client: Optional client override for testing
            metrics_client: Optional client override for testing
        """
        self.experiment_client = experiment_client or ExperimentServiceClient()
        self.metrics_client = metrics_client or MetricsServiceClient()
        logger.info("DataFetchingService initialized")
    
    def fetch_report_data(self, experiment_id: str, organization_id: str) -> ReportData:
        """
        Fetch all data needed for report generation.
        
        This method orchestrates multiple service calls in sequence:
        1. Fetch experiment details
        2. Fetch hypothesis (using ID from experiment)
        3. Fetch problem (using ID from hypothesis)
        4. Fetch raw metrics (using time range from experiment)
        5. Fetch aggregated metrics (using time range from experiment)
        
        All calls are made with the organization ID for proper authorization
        and tenant isolation.
        
        Args:
            experiment_id: Unique experiment identifier
            organization_id: Organization identifier for authorization
            
        Returns:
            ReportData object containing all fetched data
            
        Raises:
            ValueError: If required fields are missing from responses
            HTTPError: If any HTTP request fails
            Timeout: If any request times out
            RequestException: For other request failures
            
        Example:
            >>> service = DataFetchingService()
            >>> data = service.fetch_report_data('exp-123', 'org-456')
            >>> data.experiment_id
            'exp-123'
            >>> data.has_metrics()
            True
        """
        logger.info(
            f"Fetching report data for experiment {experiment_id} "
            f"in organization {organization_id}"
        )
        
        try:
            # Step 1: Fetch experiment details
            logger.debug(f"Step 1: Fetching experiment {experiment_id}")
            experiment = self.experiment_client.get_experiment(
                experiment_id,
                organization_id
            )
            
            # Validate experiment has required fields
            if 'hypothesisId' not in experiment:
                raise ValueError(
                    f"Experiment {experiment_id} missing required field 'hypothesisId'"
                )
            if 'startedAt' not in experiment:
                raise ValueError(
                    f"Experiment {experiment_id} missing required field 'startedAt'"
                )
            if 'completedAt' not in experiment:
                raise ValueError(
                    f"Experiment {experiment_id} missing required field 'completedAt'"
                )
            
            # Step 2: Fetch hypothesis
            hypothesis_id = experiment['hypothesisId']
            logger.debug(f"Step 2: Fetching hypothesis {hypothesis_id}")
            hypothesis = self.experiment_client.get_hypothesis(
                hypothesis_id,
                organization_id
            )
            
            # Validate hypothesis has required fields
            if 'problemId' not in hypothesis:
                raise ValueError(
                    f"Hypothesis {hypothesis_id} missing required field 'problemId'"
                )
            
            # Step 3: Fetch problem
            problem_id = hypothesis['problemId']
            logger.debug(f"Step 3: Fetching problem {problem_id}")
            problem = self.experiment_client.get_problem(
                problem_id,
                organization_id
            )
            
            # Step 4: Fetch raw metrics
            start_time = experiment['startedAt']
            end_time = experiment['completedAt']
            logger.debug(
                f"Step 4: Fetching metrics for time range {start_time} to {end_time}"
            )
            metrics = self.metrics_client.get_metrics(
                experiment_id,
                start_time,
                end_time,
                organization_id
            )
            
            # Step 5: Fetch aggregated metrics
            logger.debug(f"Step 5: Fetching aggregated metrics")
            aggregated_metrics = self.metrics_client.get_aggregated_metrics(
                experiment_id,
                start_time,
                end_time,
                organization_id
            )
            
            # Create and return ReportData value object
            report_data = ReportData(
                experiment=experiment,
                hypothesis=hypothesis,
                problem=problem,
                metrics=metrics,
                aggregated_metrics=aggregated_metrics
            )
            
            logger.info(
                f"Successfully fetched all data for experiment {experiment_id}: "
                f"{report_data.metrics_count} metrics collected"
            )
            
            return report_data
            
        except ValueError as e:
            logger.error(f"Validation error fetching report data: {str(e)}")
            raise
            
        except Exception as e:
            logger.error(
                f"Error fetching report data for experiment {experiment_id}: {str(e)}",
                exc_info=True
            )
            raise
