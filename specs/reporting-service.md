# Reporting Service Specification

**Source**: PROJECT.md (Section 40)

This specification defines the Reporting Service, an event-driven Lambda function responsible for generating experiment reports.

---

## Service Overview

**Purpose**: Generate comprehensive reports for completed experiments

**Bounded Context**: Reporting and Analytics

**Service Type**: Event-driven processor (AWS Lambda)

---

## Responsibilities

- Listen for ExperimentCompleted events
- Fetch experiment data and metrics
- Calculate metric aggregations
- Generate report using templates
- Create PDF/HTML reports
- Store reports in S3
- Publish ReportGenerated events

---

## Technology Stack

**Runtime**: AWS Lambda (Python 3.11)  
**Framework**: Native Python Lambda  
**Report Generation**: ReportLab or WeasyPrint  
**Template Engine**: Jinja2  
**Storage**: Amazon S3 (via Boto3)  
**Events**: AWS EventBridge  
**Build Tool**: pip  

**Key Dependencies**:
- `boto3` (AWS SDK for S3, EventBridge, Secrets Manager)
- `jinja2` (report templates)
- `reportlab` or `weasyprint` (PDF generation)
- `requests` (API calls)
- `python-json-logger` (structured logging)

---

## Event Handlers

### ExperimentCompleted Event Handler

**Trigger**: ExperimentCompleted event from EventBridge

**Event Source**: Experiment Service

**Event Pattern**:
```json
{
  "source": ["turaf.experiment-service"],
  "detail-type": ["ExperimentCompleted"]
}
```

**Handler Logic**:
```python
import json
from typing import Dict, Any
import logging

logger = logging.getLogger(__name__)

def lambda_handler(event: Dict[str, Any], context: Any) -> Dict[str, Any]:
    """Handle ExperimentCompleted event from EventBridge"""
    event_id = event['id']
    
    # Check idempotency
    if is_already_processed(event_id):
        logger.info(f'Report already generated for event {event_id}')
        return {'statusCode': 200, 'body': 'Already processed'}
    
    # Parse payload
    detail = event['detail']
    experiment_id = detail['experimentId']
    
    # Generate report
    report = generate_report(experiment_id)
    
    # Store report in S3
    report_location = store_report(report)
    
    # Publish ReportGenerated event
    publish_report_generated_event(report, report_location)
    
    # Mark event as processed
    mark_event_processed(event_id, report['id'])
    
    return {'statusCode': 200, 'body': 'Success'}
```

---

## Report Generation Workflow

### Step 1: Fetch Experiment Data

**Data Sources**:
- Experiment Service API: Experiment details, hypothesis, problem
- Metrics Service API: All metrics for experiment

**API Calls**:
```python
import requests
from typing import Dict, List

def fetch_experiment_data(experiment_id: str) -> Dict:
    """Fetch all data needed for report generation"""
    # Call Experiment Service
    experiment = experiment_client.get_experiment(experiment_id)
    hypothesis = experiment_client.get_hypothesis(experiment['hypothesisId'])
    problem = experiment_client.get_problem(hypothesis['problemId'])
    
    # Call Metrics Service
    metrics = metrics_client.get_experiment_metrics(experiment_id)
    
    return {
        'experiment': experiment,
        'hypothesis': hypothesis,
        'problem': problem,
        'metrics': metrics
    }
```

---

### Step 2: Calculate Aggregations

**Aggregations to Calculate**:
- Average, min, max for each metric
- Total count of metrics
- Time-series trends
- Outcome classification

**Implementation**:
```python
from collections import defaultdict
from typing import List, Dict
import statistics

def analyze_metrics(metrics: List[Dict]) -> Dict:
    """Calculate aggregations for all metrics"""
    stats_by_name = defaultdict(list)
    
    # Group metrics by name
    for metric in metrics:
        stats_by_name[metric['name']].append(metric['value'])
    
    # Calculate stats for each metric
    analysis = {}
    for name, values in stats_by_name.items():
        analysis[name] = calculate_stats(values)
    
    return analysis

def calculate_stats(values: List[float]) -> Dict:
    """Calculate statistical aggregations"""
    return {
        'average': statistics.mean(values),
        'min': min(values),
        'max': max(values),
        'count': len(values)
    }
```

---

### Step 3: Apply Report Template

**Template Engine**: Jinja2

**Template Location**: `templates/report-template.html`

**Template Variables**:
```python
import uuid
from datetime import datetime

def prepare_template_variables(data: Dict, analysis: Dict) -> Dict:
    """Prepare variables for template rendering"""
    return {
        'experiment': data['experiment'],
        'hypothesis': data['hypothesis'],
        'problem': data['problem'],
        'metrics_analysis': analysis,
        'generated_at': datetime.utcnow().isoformat(),
        'report_id': str(uuid.uuid4())
    }
```

**Template Rendering**:
```python
from jinja2 import Environment, FileSystemLoader

def render_template(variables: Dict) -> str:
    """Render report template with variables"""
    env = Environment(loader=FileSystemLoader('templates'))
    template = env.get_template('report-template.html')
    return template.render(**variables)
```

---

### Step 4: Generate PDF

**PDF Generation**:
```python
from weasyprint import HTML
import io

def generate_pdf(html_content: str) -> bytes:
    """Convert HTML to PDF using WeasyPrint"""
    pdf_buffer = io.BytesIO()
    HTML(string=html_content).write_pdf(pdf_buffer)
    return pdf_buffer.getvalue()
```

---

### Step 5: Store in S3

**S3 Bucket Structure**:
```
turaf-reports-{env}/
  {organizationId}/
    {experimentId}/
      {reportId}.pdf
      {reportId}.html
```

**S3 Upload**:
```python
import boto3
import os

s3_client = boto3.client('s3')

def store_report(organization_id: str, experiment_id: str, report_id: str, 
                pdf_content: bytes, html_content: str) -> str:
    """Upload report files to S3"""
    environment = os.environ.get('ENVIRONMENT', 'dev')
    bucket_name = f'turaf-reports-{environment}'
    pdf_key = f'{organization_id}/{experiment_id}/{report_id}.pdf'
    html_key = f'{organization_id}/{experiment_id}/{report_id}.html'
    
    # Upload PDF
    s3_client.put_object(
        Bucket=bucket_name,
        Key=pdf_key,
        Body=pdf_content,
        ContentType='application/pdf'
    )
    
    # Upload HTML
    s3_client.put_object(
        Bucket=bucket_name,
        Key=html_key,
        Body=html_content,
        ContentType='text/html'
    )
    
    return f's3://{bucket_name}/{pdf_key}'
```

---

### Step 6: Publish ReportGenerated Event

**Event Publishing**:
```python
import boto3
import json
import uuid
from datetime import datetime

eventbridge_client = boto3.client('events')

def publish_report_generated_event(report: Dict, report_location: str):
    """Publish ReportGenerated event to EventBridge"""
    payload = {
        'reportId': report['id'],
        'experimentId': report['experimentId'],
        'reportLocation': report_location,
        'reportFormat': 'PDF',
        'generatedAt': datetime.utcnow().isoformat()
    }
    
    event = {
        'eventId': str(uuid.uuid4()),
        'eventType': 'ReportGenerated',
        'eventVersion': 1,
        'timestamp': datetime.utcnow().isoformat(),
        'sourceService': 'reporting-service',
        'organizationId': report['organizationId'],
        'payload': payload
    }
    
    eventbridge_client.put_events(
        Entries=[{
            'EventBusName': 'turaf-event-bus',
            'Source': 'turaf.reporting-service',
            'DetailType': 'ReportGenerated',
            'Detail': json.dumps(event)
        }]
    )
```

---

## Report Template Structure

### HTML Template

**Sections**:
1. **Header**: Report title, experiment name, generated date
2. **Executive Summary**: Outcome, key findings
3. **Problem Statement**: Original problem description
4. **Hypothesis**: Hypothesis statement and expected outcome
5. **Experiment Details**: Duration, status, dates
6. **Metrics Summary**: Table of all metrics with aggregations
7. **Metrics Visualization**: Charts and graphs (future)
8. **Conclusion**: Result summary and recommendations
9. **Footer**: Report ID, organization info

**Sample Template**:
```html
<!DOCTYPE html>
<html>
<head>
    <title>Experiment Report</title>
    <style>
        /* Report styling */
    </style>
</head>
<body>
    <h1>Experiment Report</h1>
    <h2>{{ experiment.name }}</h2>
    
    <section class="summary">
        <h3>Executive Summary</h3>
        <p><strong>Outcome:</strong> {{ experiment.result.outcome }}</p>
        <p>{{ experiment.result.summary }}</p>
    </section>
    
    <section class="problem">
        <h3>Problem Statement</h3>
        <p>{{ problem.title }}</p>
        <p>{{ problem.description }}</p>
    </section>
    
    <section class="hypothesis">
        <h3>Hypothesis</h3>
        <p>{{ hypothesis.statement }}</p>
    </section>
    
    <section class="metrics">
        <h3>Metrics Analysis</h3>
        <table>
            <thead>
                <tr>
                    <th>Metric Name</th>
                    <th>Average</th>
                    <th>Min</th>
                    <th>Max</th>
                    <th>Count</th>
                </tr>
            </thead>
            <tbody>
                {% for name, stat in metrics_analysis.items() %}
                <tr>
                    <td>{{ name }}</td>
                    <td>{{ "%.2f"|format(stat.average) }}</td>
                    <td>{{ "%.2f"|format(stat.min) }}</td>
                    <td>{{ "%.2f"|format(stat.max) }}</td>
                    <td>{{ stat.count }}</td>
                </tr>
                {% endfor %}
            </tbody>
        </table>
    </section>
    
    <footer>
        <p>Report ID: {{ report_id }}</p>
        <p>Generated: {{ generated_at }}</p>
    </footer>
</body>
</html>
```

---

## Error Handling

### Retry Strategy

**Retry Policy**:
- Automatic retries by EventBridge
- Exponential backoff: 1s, 2s, 4s, 8s, 16s
- Maximum 5 retry attempts
- Dead Letter Queue after max retries

**Error Scenarios**:
1. **Experiment data not found**: Log error, send to DLQ
2. **Metrics service unavailable**: Retry with backoff
3. **S3 upload failure**: Retry with backoff
4. **PDF generation failure**: Log error, send to DLQ

**Implementation**:
```python
from tenacity import retry, stop_after_attempt, wait_exponential

@retry(
    stop=stop_after_attempt(5),
    wait=wait_exponential(multiplier=1, min=1, max=16)
)
def generate_report(experiment_id: str) -> Dict:
    """Generate report with retry logic for transient failures"""
    # Report generation logic
    pass
```

---

## Idempotency

### Deduplication Strategy

**Idempotency Key**: EventBridge eventId

**Implementation**:
```python
import boto3
from datetime import datetime

dynamodb = boto3.resource('dynamodb')
table = dynamodb.Table('processed_events')

def is_already_processed(event_id: str) -> bool:
    """Check if event has already been processed"""
    response = table.get_item(Key={'eventId': event_id})
    return 'Item' in response

def mark_as_processed(event_id: str, report_id: str):
    """Mark event as processed in DynamoDB"""
    table.put_item(
        Item={
            'eventId': event_id,
            'processedAt': datetime.utcnow().isoformat(),
            'reportId': report_id
        }
    )
```

---

## Lambda Configuration

### Function Configuration

**Memory**: 1024 MB  
**Timeout**: 60 seconds  
**Concurrency**: 10 (reserved concurrency)  
**Environment Variables**:
- `ENVIRONMENT`: dev/qa/prod
- `S3_BUCKET_NAME`: turaf-reports-{env}
- `EVENT_BUS_NAME`: turaf-event-bus-{env}
- `EXPERIMENT_SERVICE_URL`: https://api.{env}.turaf.com
- `METRICS_SERVICE_URL`: https://api.{env}.turaf.com

### IAM Permissions

**Required Permissions**:
- `s3:PutObject` on reports bucket
- `s3:GetObject` on reports bucket
- `events:PutEvents` on event bus
- `logs:CreateLogGroup`, `logs:CreateLogStream`, `logs:PutLogEvents`
- `secretsmanager:GetSecretValue` for API credentials

---

## Events Published

### ReportGenerated

**Payload**:
```json
{
  "reportId": "uuid",
  "experimentId": "uuid",
  "reportLocation": "s3://bucket/path",
  "reportFormat": "PDF",
  "generatedAt": "ISO-8601"
}
```

**Consumers**:
- Notification Service (send report ready notification)

---

## Monitoring

### CloudWatch Metrics

**Custom Metrics**:
- `ReportsGenerated`: Count of reports generated
- `ReportGenerationTime`: Duration of report generation
- `ReportGenerationErrors`: Count of failures
- `S3UploadTime`: Duration of S3 uploads

**CloudWatch Logs**:
- Log all report generation attempts
- Log errors with full stack traces
- Include experiment ID in all logs

### Alarms

**Alarm Conditions**:
- Error rate > 5%
- Generation time > 30 seconds
- DLQ depth > 5 messages

---

## Testing Strategy

### Unit Tests
- Test report generation logic
- Test metric aggregation
- Test template rendering
- Test PDF generation

### Integration Tests
- Test with LocalStack S3
- Test with LocalStack EventBridge
- Test idempotency
- Test error scenarios

### End-to-End Tests
- Publish ExperimentCompleted event
- Verify report generated
- Verify S3 upload
- Verify ReportGenerated event published

---

## Performance Optimization

### Caching
- Cache experiment data during generation
- Cache template compilation
- Reuse S3 client connections

### Parallel Processing
- Fetch experiment data and metrics in parallel
- Calculate aggregations in parallel

### Memory Management
- Stream large PDFs to S3
- Clean up temporary files
- Optimize PDF generation settings

---

## Future Enhancements

- Charts and visualizations in reports
- Custom report templates per organization
- Report scheduling (weekly/monthly summaries)
- Report versioning
- Multi-format support (Excel, CSV)
- Report sharing and permissions
- Report annotations and comments

---

## References

- PROJECT.md: Reporting Service specification
- event-flow.md: Event specifications
- AWS Lambda Best Practices
- Thymeleaf Documentation
