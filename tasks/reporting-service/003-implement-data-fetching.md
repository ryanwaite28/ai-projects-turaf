# Task: Implement Data Fetching

**Service**: Reporting Service  
**Phase**: 7  
**Estimated Time**: 3 hours  

## Objective

Implement data fetching logic to retrieve experiment, hypothesis, problem, and metrics data from respective services.

## Prerequisites

- [x] Task 002: Event handler implemented

## Scope

**Files to Create**:
- `services/reporting-service/src/clients/experiment_client.py`
- `services/reporting-service/src/clients/metrics_client.py`
- `services/reporting-service/src/services/data_fetching.py`
- `services/reporting-service/src/models/report_data.py`

## Implementation Details

### Data Fetching Service

```python
import logging
from typing import Dict, Any
from clients.experiment_client import ExperimentServiceClient
from clients.metrics_client import MetricsServiceClient
from models.report_data import ReportData

logger = logging.getLogger(__name__)

class DataFetchingService:
    """Service for fetching all data needed for report generation"""
    
    def __init__(self):
        self.experiment_client = ExperimentServiceClient()
        self.metrics_client = MetricsServiceClient()
    
    def fetch_report_data(self, experiment_id: str, organization_id: str) -> ReportData:
        """
        Fetch all data needed for report generation.
        
        Args:
            experiment_id: Experiment identifier
            organization_id: Organization identifier
            
        Returns:
            ReportData object with all fetched data
        """
        logger.info(f"Fetching report data for experiment {experiment_id}")
        
        # Fetch experiment details
        experiment = self.experiment_client.get_experiment(experiment_id, organization_id)
        
        # Fetch hypothesis
        hypothesis = self.experiment_client.get_hypothesis(
            experiment['hypothesisId'],
            organization_id
        )
        
        # Fetch problem
        problem = self.experiment_client.get_problem(
            hypothesis['problemId'],
            organization_id
        )
        
        # Fetch metrics
        metrics = self.metrics_client.get_metrics(
            experiment_id,
            experiment['startedAt'],
            experiment['completedAt'],
            organization_id
        )
        
        # Fetch aggregated metrics
        aggregated_metrics = self.metrics_client.get_aggregated_metrics(
            experiment_id,
            experiment['startedAt'],
            experiment['completedAt'],
            organization_id
        )
        
        return ReportData(
            experiment=experiment,
            hypothesis=hypothesis,
            problem=problem,
            metrics=metrics,
            aggregated_metrics=aggregated_metrics
        )
```

### Experiment Service Client

```python
import os
import requests
import logging
from typing import Dict, Any
from tenacity import retry, stop_after_attempt, wait_exponential

logger = logging.getLogger(__name__)

class ExperimentServiceClient:
    """Client for Experiment Service API"""
    
    def __init__(self):
        self.base_url = os.environ.get('EXPERIMENT_SERVICE_URL', 'https://api.turaf.com')
        self.timeout = 10
    
    @retry(
        stop=stop_after_attempt(3),
        wait=wait_exponential(multiplier=1, min=1, max=10)
    )
    def get_experiment(self, experiment_id: str, organization_id: str) -> Dict[str, Any]:
        """Fetch experiment by ID"""
        url = f"{self.base_url}/api/v1/experiments/{experiment_id}"
        headers = {'X-Organization-Id': organization_id}
        
        logger.info(f"Fetching experiment {experiment_id}")
        
        response = requests.get(url, headers=headers, timeout=self.timeout)
        response.raise_for_status()
        
        return response.json()
    
    @retry(
        stop=stop_after_attempt(3),
        wait=wait_exponential(multiplier=1, min=1, max=10)
    )
    def get_hypothesis(self, hypothesis_id: str, organization_id: str) -> Dict[str, Any]:
        """Fetch hypothesis by ID"""
        url = f"{self.base_url}/api/v1/hypotheses/{hypothesis_id}"
        headers = {'X-Organization-Id': organization_id}
        
        logger.info(f"Fetching hypothesis {hypothesis_id}")
        
        response = requests.get(url, headers=headers, timeout=self.timeout)
        response.raise_for_status()
        
        return response.json()
    
    @retry(
        stop=stop_after_attempt(3),
        wait=wait_exponential(multiplier=1, min=1, max=10)
    )
    def get_problem(self, problem_id: str, organization_id: str) -> Dict[str, Any]:
        """Fetch problem by ID"""
        url = f"{self.base_url}/api/v1/problems/{problem_id}"
        headers = {'X-Organization-Id': organization_id}
        
        logger.info(f"Fetching problem {problem_id}")
        
        response = requests.get(url, headers=headers, timeout=self.timeout)
        response.raise_for_status()
        
        return response.json()
```

### Metrics Service Client

```python
import os
import requests
import logging
from typing import Dict, Any, List
from tenacity import retry, stop_after_attempt, wait_exponential

logger = logging.getLogger(__name__)

class MetricsServiceClient:
    """Client for Metrics Service API"""
    
    def __init__(self):
        self.base_url = os.environ.get('METRICS_SERVICE_URL', 'https://api.turaf.com')
        self.timeout = 10
    
    @retry(
        stop=stop_after_attempt(3),
        wait=wait_exponential(multiplier=1, min=1, max=10)
    )
    def get_metrics(self, experiment_id: str, start_time: str, end_time: str, 
                   organization_id: str) -> List[Dict[str, Any]]:
        """Fetch all metrics for experiment"""
        url = f"{self.base_url}/api/v1/metrics"
        headers = {'X-Organization-Id': organization_id}
        params = {
            'experimentId': experiment_id,
            'startTime': start_time,
            'endTime': end_time
        }
        
        logger.info(f"Fetching metrics for experiment {experiment_id}")
        
        response = requests.get(url, headers=headers, params=params, timeout=self.timeout)
        response.raise_for_status()
        
        return response.json()
    
    @retry(
        stop=stop_after_attempt(3),
        wait=wait_exponential(multiplier=1, min=1, max=10)
    )
    def get_aggregated_metrics(self, experiment_id: str, start_time: str, end_time: str,
                              organization_id: str) -> Dict[str, Any]:
        """Fetch aggregated metrics for experiment"""
        url = f"{self.base_url}/api/v1/metrics/aggregated"
        headers = {'X-Organization-Id': organization_id}
        params = {
            'experimentId': experiment_id,
            'startTime': start_time,
            'endTime': end_time
        }
        
        logger.info(f"Fetching aggregated metrics for experiment {experiment_id}")
        
        response = requests.get(url, headers=headers, params=params, timeout=self.timeout)
        response.raise_for_status()
        
        return response.json()
```

## Acceptance Criteria

- [x] Data fetching service retrieves all required data
- [x] Service clients make HTTP requests correctly
- [x] Error handling for failed requests
- [x] Retry logic implemented (3 attempts with exponential backoff)
- [x] Timeout configured
- [x] Unit tests pass

## Testing Requirements

**Unit Tests**:
- Test data fetching success
- Test HTTP client calls
- Test error handling
- Test retry logic
- Test timeout handling

**Test Files to Create**:
- `tests/test_data_fetching.py`
- `tests/test_experiment_client.py`
- `tests/test_metrics_client.py`

## References

- Specification: `specs/reporting-service.md` (Data Fetching section)
- Related Tasks: 004-implement-aggregation-logic
