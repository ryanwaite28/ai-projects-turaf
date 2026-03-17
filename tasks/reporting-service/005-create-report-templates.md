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
- `services/reporting-service/src/main/resources/templates/experiment-report.html`
- `services/reporting-service/src/main/java/com/turaf/reporting/template/TemplateEngine.java`

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
            {{#each metricSummaries}}
            <tr>
                <td>{{this.name}}</td>
                <td>{{this.count}}</td>
                <td>{{this.average}}</td>
                <td>{{this.min}}</td>
                <td>{{this.max}}</td>
                <td>{{this.trend}}</td>
            </tr>
            {{/each}}
        </table>
    </div>
    
    <div class="section">
        <h2>Key Insights</h2>
        <ul>
            {{#each insights}}
            <li>{{this}}</li>
            {{/each}}
        </ul>
    </div>
</body>
</html>
```

### Template Engine

```java
public class TemplateEngine {
    private final Handlebars handlebars;
    
    public TemplateEngine() {
        this.handlebars = new Handlebars();
    }
    
    public String renderReport(AggregatedReportData data) throws IOException {
        Template template = handlebars.compile("templates/experiment-report");
        
        Map<String, Object> context = new HashMap<>();
        context.put("problem", data.getProblem());
        context.put("hypothesis", data.getHypothesis());
        context.put("experiment", data.getExperiment());
        context.put("duration", formatDuration(data.getDuration()));
        context.put("metricSummaries", data.getMetricSummaries().values());
        context.put("insights", data.getInsights());
        
        return template.apply(context);
    }
    
    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        return String.format("%d hours, %d minutes", hours, minutes);
    }
}
```

## Acceptance Criteria

- [ ] HTML template created
- [ ] Template engine renders correctly
- [ ] All data fields populated
- [ ] Styling applied
- [ ] Unit tests pass

## Testing Requirements

**Unit Tests**:
- Test template rendering
- Test data binding

**Test Files to Create**:
- `TemplateEngineTest.java`

## References

- Specification: `specs/reporting-service.md` (Templates section)
- Related Tasks: 006-implement-pdf-generation
