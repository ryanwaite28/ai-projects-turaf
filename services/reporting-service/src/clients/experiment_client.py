"""
HTTP client for Experiment Service API.

This module provides a client for fetching experiment, hypothesis, and problem
data from the Experiment Service, with automatic retry logic and error handling.
"""

import os
import requests
import logging
from typing import Dict, Any
from tenacity import retry, stop_after_attempt, wait_exponential, retry_if_exception_type
from requests.exceptions import RequestException, Timeout, HTTPError

logger = logging.getLogger(__name__)


class ExperimentServiceClient:
    """
    Client for Experiment Service API.
    
    This client follows the Anti-Corruption Layer pattern from DDD,
    protecting the reporting service from changes in the experiment
    service's API structure.
    
    Features:
    - Automatic retry with exponential backoff (3 attempts)
    - Configurable timeout (10 seconds)
    - Organization-scoped requests via headers
    - Comprehensive error handling and logging
    
    Attributes:
        base_url: Base URL for the Experiment Service API
        timeout: Request timeout in seconds
    """
    
    def __init__(self, base_url: str = None, timeout: int = 10):
        """
        Initialize the Experiment Service client.
        
        Args:
            base_url: Optional base URL override (defaults to env var)
            timeout: Request timeout in seconds (default: 10)
        """
        self.base_url = base_url or os.environ.get(
            'EXPERIMENT_SERVICE_URL',
            'https://api.turaf.com'
        )
        self.timeout = timeout
        logger.info(f"ExperimentServiceClient initialized with base_url: {self.base_url}")
    
    @retry(
        stop=stop_after_attempt(3),
        wait=wait_exponential(multiplier=1, min=1, max=10),
        retry=retry_if_exception_type((RequestException, Timeout)),
        reraise=True
    )
    def get_experiment(self, experiment_id: str, organization_id: str) -> Dict[str, Any]:
        """
        Fetch experiment by ID.
        
        Retrieves detailed experiment information including status, timestamps,
        hypothesis reference, and configuration.
        
        Args:
            experiment_id: Unique experiment identifier
            organization_id: Organization identifier for authorization
            
        Returns:
            Dictionary containing experiment data:
                - id: Experiment identifier
                - hypothesisId: Associated hypothesis ID
                - startedAt: Start timestamp (ISO-8601)
                - completedAt: Completion timestamp (ISO-8601)
                - status: Experiment status
                - configuration: Experiment configuration
                
        Raises:
            HTTPError: If the request fails (4xx, 5xx)
            Timeout: If the request times out
            RequestException: For other request failures
            
        Example:
            >>> client = ExperimentServiceClient()
            >>> experiment = client.get_experiment('exp-123', 'org-456')
            >>> experiment['hypothesisId']
            'hyp-789'
        """
        url = f"{self.base_url}/api/v1/experiments/{experiment_id}"
        headers = {'X-Organization-Id': organization_id}
        
        logger.info(f"Fetching experiment {experiment_id} for organization {organization_id}")
        
        try:
            response = requests.get(url, headers=headers, timeout=self.timeout)
            response.raise_for_status()
            
            data = response.json()
            logger.debug(f"Successfully fetched experiment {experiment_id}")
            
            return data
            
        except HTTPError as e:
            logger.error(
                f"HTTP error fetching experiment {experiment_id}: "
                f"{e.response.status_code} - {e.response.text}"
            )
            raise
            
        except Timeout:
            logger.error(f"Timeout fetching experiment {experiment_id} after {self.timeout}s")
            raise
            
        except RequestException as e:
            logger.error(f"Request failed for experiment {experiment_id}: {str(e)}")
            raise
    
    @retry(
        stop=stop_after_attempt(3),
        wait=wait_exponential(multiplier=1, min=1, max=10),
        retry=retry_if_exception_type((RequestException, Timeout)),
        reraise=True
    )
    def get_hypothesis(self, hypothesis_id: str, organization_id: str) -> Dict[str, Any]:
        """
        Fetch hypothesis by ID.
        
        Retrieves hypothesis information including statement, problem reference,
        and validation criteria.
        
        Args:
            hypothesis_id: Unique hypothesis identifier
            organization_id: Organization identifier for authorization
            
        Returns:
            Dictionary containing hypothesis data:
                - id: Hypothesis identifier
                - problemId: Associated problem ID
                - statement: Hypothesis statement
                - criteria: Validation criteria
                
        Raises:
            HTTPError: If the request fails (4xx, 5xx)
            Timeout: If the request times out
            RequestException: For other request failures
        """
        url = f"{self.base_url}/api/v1/hypotheses/{hypothesis_id}"
        headers = {'X-Organization-Id': organization_id}
        
        logger.info(f"Fetching hypothesis {hypothesis_id} for organization {organization_id}")
        
        try:
            response = requests.get(url, headers=headers, timeout=self.timeout)
            response.raise_for_status()
            
            data = response.json()
            logger.debug(f"Successfully fetched hypothesis {hypothesis_id}")
            
            return data
            
        except HTTPError as e:
            logger.error(
                f"HTTP error fetching hypothesis {hypothesis_id}: "
                f"{e.response.status_code} - {e.response.text}"
            )
            raise
            
        except Timeout:
            logger.error(f"Timeout fetching hypothesis {hypothesis_id} after {self.timeout}s")
            raise
            
        except RequestException as e:
            logger.error(f"Request failed for hypothesis {hypothesis_id}: {str(e)}")
            raise
    
    @retry(
        stop=stop_after_attempt(3),
        wait=wait_exponential(multiplier=1, min=1, max=10),
        retry=retry_if_exception_type((RequestException, Timeout)),
        reraise=True
    )
    def get_problem(self, problem_id: str, organization_id: str) -> Dict[str, Any]:
        """
        Fetch problem by ID.
        
        Retrieves problem information including description, context,
        and business impact.
        
        Args:
            problem_id: Unique problem identifier
            organization_id: Organization identifier for authorization
            
        Returns:
            Dictionary containing problem data:
                - id: Problem identifier
                - title: Problem title
                - description: Detailed description
                - context: Business context
                
        Raises:
            HTTPError: If the request fails (4xx, 5xx)
            Timeout: If the request times out
            RequestException: For other request failures
        """
        url = f"{self.base_url}/api/v1/problems/{problem_id}"
        headers = {'X-Organization-Id': organization_id}
        
        logger.info(f"Fetching problem {problem_id} for organization {organization_id}")
        
        try:
            response = requests.get(url, headers=headers, timeout=self.timeout)
            response.raise_for_status()
            
            data = response.json()
            logger.debug(f"Successfully fetched problem {problem_id}")
            
            return data
            
        except HTTPError as e:
            logger.error(
                f"HTTP error fetching problem {problem_id}: "
                f"{e.response.status_code} - {e.response.text}"
            )
            raise
            
        except Timeout:
            logger.error(f"Timeout fetching problem {problem_id} after {self.timeout}s")
            raise
            
        except RequestException as e:
            logger.error(f"Request failed for problem {problem_id}: {str(e)}")
            raise
