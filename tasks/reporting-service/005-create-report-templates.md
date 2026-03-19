# Task: Create Report Templates

**Service**: Reporting Service  
**Phase**: 7  
**Estimated Time**: 2-3 hours  

## Objective

Create HTML templates for experiment reports that will be converted to PDF.

## Prerequisites

- [x] Task 004: Aggregation logic implemented

## Scope

**Files to Create**:
- `services/reporting-service/src/templates/experiment-report.html`
- `services/reporting-service/src/services/template_engine.py`

## Implementation Details

### HTML Template

```html
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Experiment Report</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 40px; }
        h1 { color: #333; }
        .section { margin-bottom: 30px; }
        .metric { background: #f5f5f5; padding: 15px; margin: 10px 0; }
        table { width: 100%; border-collapse: collapse; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #4CAF50; color: white; }
    </style>
</head>
<body>
    <h1>Experiment Report</h1>
    
    <div class="section">
        <h2>Problem</h2>
        <p><strong>Title:</strong> {{problem.title}}</p>
        <p><strong>Description:</strong> {{problem.description}}</p>
    </div>
    
    <div class="section">
        <h2>Hypothesis</h2>
        <p><strong>Statement:</strong> {{hypothesis.statement}}</p>
        <p><strong>Expected Outcome:</strong> {{hypothesis.expectedOutcome}}</p>
    </div>
    
    <div class="section">
        <h2>Experiment Details</h2>
        <p><strong>Name:</strong> {{experiment.name}}</p>
        <p><strong>Duration:</strong> {{duration}}</p>
        <p><strong>Started:</strong> {{experiment.startedAt}}</p>
        <p><strong>Completed:</strong> {{experiment.completedAt}}</p>
    </div>
    
    <div class="section">
        <h2>Metrics Summary</h2>
        <table>
            <tr>
                <th>Metric</th>
                <th>Count</th>
                <th>Average</th>
                <th>Min</th>
                <th>Max</th>
                <th>Trend</th>
            </tr>
            {% for summary in metricSummaries.values() %}
            <tr>
                <td>{{ summary.name }}</td>
                <td>{{ summary.count }}</td>
                <td>{{ "%.2f"|format(summary.average) }}</td>
                <td>{{ "%.2f"|format(summary.min_value) }}</td>
                <td>{{ "%.2f"|format(summary.max_value) }}</td>
                <td>{{ summary.trend }}</td>
            </tr>
            {% endfor %}
        </table>
    </div>
    
    <div class="section">
        <h2>Key Insights</h2>
        <ul>
            {% for insight in insights %}
            <li>{{ insight }}</li>
            {% endfor %}
        </ul>
    </div>
</body>
</html>
```

### Template Engine

```python
import os
import logging
from typing import Dict, Any
from jinja2 import Environment, FileSystemLoader, select_autoescape
from models.aggregated_data import AggregatedReportData

logger = logging.getLogger(__name__)

class TemplateEngine:
    """Engine for rendering report templates using Jinja2"""
    
    def __init__(self, template_dir: str = None):
        """
        Initialize template engine.
        
        Args:
            template_dir: Directory containing templates (defaults to src/templates)
        """
        if template_dir is None:
            # Default to templates directory relative to this file
            current_dir = os.path.dirname(os.path.abspath(__file__))
            template_dir = os.path.join(os.path.dirname(current_dir), 'templates')
        
        self.env = Environment(
            loader=FileSystemLoader(template_dir),
            autoescape=select_autoescape(['html', 'xml'])
        )
        
        logger.info(f"Template engine initialized with directory: {template_dir}")
    
    def render_report(self, data: AggregatedReportData) -> str:
        """
        Render experiment report from template.
        
        Args:
            data: Aggregated report data
            
        Returns:
            Rendered HTML string
        """
        template = self.env.get_template('experiment-report.html')
        
        context = {
            'problem': data.problem,
            'hypothesis': data.hypothesis,
            'experiment': data.experiment,
            'duration': data.get_duration_string(),
            'metricSummaries': data.metric_summaries,
            'insights': data.insights
        }
        
        logger.info(f"Rendering report for experiment {data.experiment.get('id')}")
        
        return template.render(**context)
```

## Acceptance Criteria

- [x] HTML template created with Jinja2 syntax
- [x] Template engine renders correctly
- [x] All data fields populated
- [x] Styling applied
- [x] Jinja2 filters used for formatting
- [x] Unit tests pass

## Testing Requirements

**Unit Tests**:
- Test template rendering
- Test data binding
- Test template context
- Test missing data handling

**Test Files to Create**:
- `tests/test_template_engine.py`

## References

- Specification: `specs/reporting-service.md` (Templates section)
- Related Tasks: 006-implement-pdf-generation
