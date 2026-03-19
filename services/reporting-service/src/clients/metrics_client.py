"""
HTTP client for Metrics Service API.

This module provides a client for fetching metrics data from the Metrics Service,
with automatic retry logic and error handling.
"""

import os
import requests
import logging
from typing import Dict, Any, List
from tenacity import retry, stop_after_attempt, wait_exponential, retry_if_exception_type
from requests.exceptions import RequestException, Timeout, HTTPError

logger = logging.getLogger(__name__)


class MetricsServiceClient:
    """
    Client for Metrics Service API.
    
    This client follows the Anti-Corruption Layer pattern from DDD,
    protecting the reporting service from changes in the metrics
    service's API structure.
    
    Features:
    - Automatic retry with exponential backoff (3 attempts)
    - Configurable timeout (10 seconds)
    - Organization-scoped requests via headers
    - Time-range filtering for metrics
    - Comprehensive error handling and logging
    
    Attributes:
        base_url: Base URL for the Metrics Service API
        timeout: Request timeout in seconds
    """
    
    def __init__(self, base_url: str = None, timeout: int = 10):
        """
        Initialize the Metrics Service client.
        
        Args:
            base_url: Optional base URL override (defaults to env var)
            timeout: Request timeout in seconds (default: 10)
        """
        self.base_url = base_url or os.environ.get(
            'METRICS_SERVICE_URL',
            'https://api.turaf.com'
        )
        self.timeout = timeout
        logger.info(f"MetricsServiceClient initialized with base_url: {self.base_url}")
    
    @retry(
        stop=stop_after_attempt(3),
        wait=wait_exponential(multiplier=1, min=1, max=10),
        retry=retry_if_exception_type((RequestException, Timeout)),
        reraise=True
    )
    def get_metrics(
        self,
        experiment_id: str,
        start_time: str,
        end_time: str,
        organization_id: str
    ) -> List[Dict[str, Any]]:
        """
        Fetch all metrics for an experiment within a time range.
        
        Retrieves raw metric data points collected during the experiment,
        filtered by time range.
        
        Args:
            experiment_id: Unique experiment identifier
            start_time: Start of time range (ISO-8601 format)
            end_time: End of time range (ISO-8601 format)
            organization_id: Organization identifier for authorization
            
        Returns:
            List of metric dictionaries, each containing:
                - id: Metric identifier
                - name: Metric name
                - value: Metric value
                - timestamp: Collection timestamp
                - experimentId: Associated experiment
                
        Raises:
            HTTPError: If the request fails (4xx, 5xx)
            Timeout: If the request times out
            RequestException: For other request failures
            
        Example:
            >>> client = MetricsServiceClient()
            >>> metrics = client.get_metrics(
            ...     'exp-123',
            ...     '2024-01-01T00:00:00Z',
            ...     '2024-01-01T12:00:00Z',
            ...     'org-456'
            ... )
            >>> len(metrics)
            42
        """
        url = f"{self.base_url}/api/v1/metrics"
        headers = {'X-Organization-Id': organization_id}
        params = {
            'experimentId': experiment_id,
            'startTime': start_time,
            'endTime': end_time
        }
        
        logger.info(
            f"Fetching metrics for experiment {experiment_id} "
            f"from {start_time} to {end_time}"
        )
        
        try:
            response = requests.get(
                url,
                headers=headers,
                params=params,
                timeout=self.timeout
            )
            response.raise_for_status()
            
            data = response.json()
            logger.debug(
                f"Successfully fetched {len(data)} metrics for experiment {experiment_id}"
            )
            
            return data
            
        except HTTPError as e:
            logger.error(
                f"HTTP error fetching metrics for experiment {experiment_id}: "
                f"{e.response.status_code} - {e.response.text}"
            )
            raise
            
        except Timeout:
            logger.error(
                f"Timeout fetching metrics for experiment {experiment_id} "
                f"after {self.timeout}s"
            )
            raise
            
        except RequestException as e:
            logger.error(
                f"Request failed for metrics of experiment {experiment_id}: {str(e)}"
            )
            raise
    
    @retry(
        stop=stop_after_attempt(3),
        wait=wait_exponential(multiplier=1, min=1, max=10),
        retry=retry_if_exception_type((RequestException, Timeout)),
        reraise=True
    )
    def get_aggregated_metrics(
        self,
        experiment_id: str,
        start_time: str,
        end_time: str,
        organization_id: str
    ) -> Dict[str, Any]:
        """
        Fetch aggregated metrics for an experiment within a time range.
        
        Retrieves pre-computed statistical aggregations of metrics,
        including counts, averages, min/max values, and percentiles.
        
        Args:
            experiment_id: Unique experiment identifier
            start_time: Start of time range (ISO-8601 format)
            end_time: End of time range (ISO-8601 format)
            organization_id: Organization identifier for authorization
            
        Returns:
            Dictionary mapping metric names to aggregations:
                {
                    'metricName': {
                        'count': int,
                        'average': float,
                        'min': float,
                        'max': float,
                        'p50': float,
                        'p95': float,
                        'p99': float
                    }
                }
                
        Raises:
            HTTPError: If the request fails (4xx, 5xx)
            Timeout: If the request times out
            RequestException: For other request failures
            
        Example:
            >>> client = MetricsServiceClient()
            >>> agg = client.get_aggregated_metrics(
            ...     'exp-123',
            ...     '2024-01-01T00:00:00Z',
            ...     '2024-01-01T12:00:00Z',
            ...     'org-456'
            ... )
            >>> agg['response_time']['average']
            245.7
        """
        url = f"{self.base_url}/api/v1/metrics/aggregated"
        headers = {'X-Organization-Id': organization_id}
        params = {
            'experimentId': experiment_id,
            'startTime': start_time,
            'endTime': end_time
        }
        
        logger.info(
            f"Fetching aggregated metrics for experiment {experiment_id} "
            f"from {start_time} to {end_time}"
        )
        
        try:
            response = requests.get(
                url,
                headers=headers,
                params=params,
                timeout=self.timeout
            )
            response.raise_for_status()
            
            data = response.json()
            logger.debug(
                f"Successfully fetched aggregated metrics for experiment {experiment_id}"
            )
            
            return data
            
        except HTTPError as e:
            logger.error(
                f"HTTP error fetching aggregated metrics for experiment {experiment_id}: "
                f"{e.response.status_code} - {e.response.text}"
            )
            raise
            
        except Timeout:
            logger.error(
                f"Timeout fetching aggregated metrics for experiment {experiment_id} "
                f"after {self.timeout}s"
            )
            raise
            
        except RequestException as e:
            logger.error(
                f"Request failed for aggregated metrics of experiment {experiment_id}: "
                f"{str(e)}"
            )
            raise
