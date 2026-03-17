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

**Runtime**: AWS Lambda (Java 17)  
**Framework**: Spring Cloud Function  
**Report Generation**: Apache PDFBox or iText  
**Template Engine**: Thymeleaf  
**Storage**: Amazon S3  
**Events**: AWS EventBridge  
**Build Tool**: Maven  

**Key Dependencies**:
- `spring-cloud-function-adapter-aws`
- `spring-boot-starter-thymeleaf`
- `pdfbox` or `itext7`
- `aws-java-sdk-s3`
- `aws-java-sdk-eventbridge`

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
```java
@Component
public class ExperimentCompletedHandler implements Function<EventBridgeEvent, Void> {
    
    @Override
    public Void apply(EventBridgeEvent event) {
        String eventId = event.getEventId();
        
        // Check idempotency
        if (reportRepository.existsByEventId(eventId)) {
            log.info("Report already generated for event {}", eventId);
            return null;
        }
        
        // Parse payload
        ExperimentCompletedPayload payload = parsePayload(event);
        
        // Generate report
        Report report = generateReport(payload.getExperimentId());
        
        // Store report
        storeReport(report);
        
        // Publish ReportGenerated event
        publishReportGeneratedEvent(report);
        
        // Mark event as processed
        markEventProcessed(eventId);
        
        return null;
    }
}
```

---

## Report Generation Workflow

### Step 1: Fetch Experiment Data

**Data Sources**:
- Experiment Service API: Experiment details, hypothesis, problem
- Metrics Service API: All metrics for experiment

**API Calls**:
```java
private ExperimentData fetchExperimentData(ExperimentId experimentId) {
    // Call Experiment Service
    ExperimentDto experiment = experimentClient.getExperiment(experimentId);
    HypothesisDto hypothesis = experimentClient.getHypothesis(experiment.getHypothesisId());
    ProblemDto problem = experimentClient.getProblem(hypothesis.getProblemId());
    
    // Call Metrics Service
    List<MetricDto> metrics = metricsClient.getExperimentMetrics(experimentId);
    
    return new ExperimentData(experiment, hypothesis, problem, metrics);
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
```java
private MetricsAnalysis analyzeMetrics(List<MetricDto> metrics) {
    Map<String, MetricStats> statsByName = metrics.stream()
        .collect(Collectors.groupingBy(
            MetricDto::getName,
            Collectors.collectingAndThen(
                Collectors.toList(),
                this::calculateStats
            )
        ));
    
    return new MetricsAnalysis(statsByName);
}

private MetricStats calculateStats(List<MetricDto> metrics) {
    DoubleSummaryStatistics stats = metrics.stream()
        .mapToDouble(MetricDto::getValue)
        .summaryStatistics();
    
    return new MetricStats(
        stats.getAverage(),
        stats.getMin(),
        stats.getMax(),
        stats.getCount()
    );
}
```

---

### Step 3: Apply Report Template

**Template Engine**: Thymeleaf

**Template Location**: `src/main/resources/templates/report-template.html`

**Template Variables**:
```java
private Map<String, Object> prepareTemplateVariables(ExperimentData data, MetricsAnalysis analysis) {
    Map<String, Object> variables = new HashMap<>();
    variables.put("experiment", data.getExperiment());
    variables.put("hypothesis", data.getHypothesis());
    variables.put("problem", data.getProblem());
    variables.put("metricsAnalysis", analysis);
    variables.put("generatedAt", Instant.now());
    variables.put("reportId", UUID.randomUUID());
    return variables;
}
```

**Template Rendering**:
```java
private String renderTemplate(Map<String, Object> variables) {
    Context context = new Context();
    context.setVariables(variables);
    return templateEngine.process("report-template", context);
}
```

---

### Step 4: Generate PDF

**PDF Generation**:
```java
private byte[] generatePdf(String htmlContent) {
    // Using iText or PDFBox
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    
    // Convert HTML to PDF
    HtmlConverter.convertToPdf(htmlContent, outputStream);
    
    return outputStream.toByteArray();
}
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
```java
private String storeReport(UUID organizationId, UUID experimentId, UUID reportId, byte[] pdfContent, String htmlContent) {
    String bucketName = "turaf-reports-" + environment;
    String pdfKey = String.format("%s/%s/%s.pdf", organizationId, experimentId, reportId);
    String htmlKey = String.format("%s/%s/%s.html", organizationId, experimentId, reportId);
    
    // Upload PDF
    s3Client.putObject(PutObjectRequest.builder()
        .bucket(bucketName)
        .key(pdfKey)
        .contentType("application/pdf")
        .build(), RequestBody.fromBytes(pdfContent));
    
    // Upload HTML
    s3Client.putObject(PutObjectRequest.builder()
        .bucket(bucketName)
        .key(htmlKey)
        .contentType("text/html")
        .build(), RequestBody.fromString(htmlContent));
    
    return String.format("s3://%s/%s", bucketName, pdfKey);
}
```

---

### Step 6: Publish ReportGenerated Event

**Event Publishing**:
```java
private void publishReportGeneratedEvent(Report report) {
    ReportGeneratedPayload payload = new ReportGeneratedPayload(
        report.getId(),
        report.getExperimentId(),
        report.getReportLocation(),
        report.getReportFormat(),
        Instant.now()
    );
    
    DomainEvent event = DomainEvent.builder()
        .eventId(UUID.randomUUID())
        .eventType("ReportGenerated")
        .eventVersion(1)
        .timestamp(Instant.now())
        .sourceService("reporting-service")
        .organizationId(report.getOrganizationId())
        .payload(payload)
        .build();
    
    eventBridgeClient.putEvents(PutEventsRequest.builder()
        .entries(PutEventsRequestEntry.builder()
            .eventBusName("turaf-event-bus")
            .source("turaf.reporting-service")
            .detailType("ReportGenerated")
            .detail(toJson(event))
            .build())
        .build());
}
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
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Experiment Report</title>
    <style>
        /* Report styling */
    </style>
</head>
<body>
    <h1>Experiment Report</h1>
    <h2 th:text="${experiment.name}">Experiment Name</h2>
    
    <section class="summary">
        <h3>Executive Summary</h3>
        <p><strong>Outcome:</strong> <span th:text="${experiment.result.outcome}">VALIDATED</span></p>
        <p th:text="${experiment.result.summary}">Summary text</p>
    </section>
    
    <section class="problem">
        <h3>Problem Statement</h3>
        <p th:text="${problem.title}">Problem title</p>
        <p th:text="${problem.description}">Problem description</p>
    </section>
    
    <section class="hypothesis">
        <h3>Hypothesis</h3>
        <p th:text="${hypothesis.statement}">Hypothesis statement</p>
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
                <tr th:each="stat : ${metricsAnalysis.stats}">
                    <td th:text="${stat.name}">Metric Name</td>
                    <td th:text="${stat.average}">0.00</td>
                    <td th:text="${stat.min}">0.00</td>
                    <td th:text="${stat.max}">0.00</td>
                    <td th:text="${stat.count}">0</td>
                </tr>
            </tbody>
        </table>
    </section>
    
    <footer>
        <p>Report ID: <span th:text="${reportId}">uuid</span></p>
        <p>Generated: <span th:text="${generatedAt}">timestamp</span></p>
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
```java
@Retryable(
    value = {TransientException.class},
    maxAttempts = 5,
    backoff = @Backoff(delay = 1000, multiplier = 2)
)
public Report generateReport(ExperimentId experimentId) {
    // Report generation logic
}
```

---

## Idempotency

### Deduplication Strategy

**Idempotency Key**: EventBridge eventId

**Implementation**:
```java
@Entity
@Table(name = "processed_events")
public class ProcessedEvent {
    @Id
    private String eventId;
    private Instant processedAt;
    private String reportId;
}

private boolean isAlreadyProcessed(String eventId) {
    return processedEventRepository.existsById(eventId);
}

private void markAsProcessed(String eventId, String reportId) {
    ProcessedEvent event = new ProcessedEvent(eventId, Instant.now(), reportId);
    processedEventRepository.save(event);
}
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
