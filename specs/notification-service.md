# Notification Service Specification

**Source**: PROJECT.md (Section 40)

This specification defines the Notification Service, an event-driven Lambda function responsible for sending notifications via multiple channels.

---

## Service Overview

**Purpose**: Send notifications to users based on domain events

**Bounded Context**: Notifications and Communications

**Service Type**: Event-driven processor (AWS Lambda)

---

## Responsibilities

- Listen for domain events (ExperimentCompleted, ReportGenerated, etc.)
- Send email notifications via Amazon SES
- Send webhook notifications to external systems
- Manage notification preferences
- Track notification delivery status
- Handle notification failures and retries

---

## Technology Stack

**Runtime**: AWS Lambda (Java 17)  
**Framework**: Spring Cloud Function  
**Email**: Amazon SES  
**HTTP Client**: Spring WebClient  
**Events**: AWS EventBridge  
**Build Tool**: Maven  

**Key Dependencies**:
- `spring-cloud-function-adapter-aws`
- `aws-java-sdk-ses`
- `spring-boot-starter-webflux`
- `aws-java-sdk-eventbridge`
- `thymeleaf` (email templates)

---

## Event Handlers

### ExperimentCompleted Event Handler

**Trigger**: ExperimentCompleted event from EventBridge

**Notification Type**: Experiment completion notification

**Recipients**: Experiment creator and organization admins

**Handler Logic**:
```java
@Component
public class ExperimentCompletedNotificationHandler implements Function<EventBridgeEvent, Void> {
    
    @Override
    public Void apply(EventBridgeEvent event) {
        String eventId = event.getEventId();
        
        // Check idempotency
        if (isAlreadyProcessed(eventId)) {
            return null;
        }
        
        // Parse payload
        ExperimentCompletedPayload payload = parsePayload(event);
        
        // Fetch experiment details
        ExperimentDto experiment = experimentClient.getExperiment(payload.getExperimentId());
        
        // Get notification recipients
        List<User> recipients = getRecipients(experiment);
        
        // Send email notifications
        recipients.forEach(user -> sendExperimentCompletedEmail(user, experiment));
        
        // Send webhook notifications
        sendWebhookNotifications(experiment.getOrganizationId(), "experiment.completed", payload);
        
        // Mark as processed
        markAsProcessed(eventId);
        
        return null;
    }
}
```

---

### ReportGenerated Event Handler

**Trigger**: ReportGenerated event from EventBridge

**Notification Type**: Report ready notification

**Recipients**: Experiment creator and organization admins

**Handler Logic**:
```java
@Component
public class ReportGeneratedNotificationHandler implements Function<EventBridgeEvent, Void> {
    
    @Override
    public Void apply(EventBridgeEvent event) {
        String eventId = event.getEventId();
        
        // Check idempotency
        if (isAlreadyProcessed(eventId)) {
            return null;
        }
        
        // Parse payload
        ReportGeneratedPayload payload = parsePayload(event);
        
        // Fetch experiment and report details
        ExperimentDto experiment = experimentClient.getExperiment(payload.getExperimentId());
        
        // Get notification recipients
        List<User> recipients = getRecipients(experiment);
        
        // Send email notifications with report link
        recipients.forEach(user -> sendReportReadyEmail(user, experiment, payload.getReportLocation()));
        
        // Send webhook notifications
        sendWebhookNotifications(experiment.getOrganizationId(), "report.generated", payload);
        
        // Mark as processed
        markAsProcessed(eventId);
        
        return null;
    }
}
```

---

### MemberAdded Event Handler

**Trigger**: MemberAdded event from EventBridge

**Notification Type**: Welcome email

**Recipients**: New member

**Handler Logic**:
```java
@Component
public class MemberAddedNotificationHandler implements Function<EventBridgeEvent, Void> {
    
    @Override
    public Void apply(EventBridgeEvent event) {
        String eventId = event.getEventId();
        
        if (isAlreadyProcessed(eventId)) {
            return null;
        }
        
        MemberAddedPayload payload = parsePayload(event);
        
        // Fetch user and organization details
        User user = userClient.getUser(payload.getUserId());
        Organization org = organizationClient.getOrganization(payload.getOrganizationId());
        
        // Send welcome email
        sendWelcomeEmail(user, org);
        
        markAsProcessed(eventId);
        
        return null;
    }
}
```

---

## Email Notifications

### Email Templates

**Template Engine**: Thymeleaf

**Template Location**: `src/main/resources/email-templates/`

**Templates**:
- `experiment-completed.html`: Experiment completion notification
- `report-ready.html`: Report ready notification
- `welcome.html`: Welcome to organization
- `experiment-started.html`: Experiment started notification

---

### Experiment Completed Email

**Subject**: "Experiment Completed: {experiment.name}"

**Template Variables**:
```java
Map<String, Object> variables = Map.of(
    "userName", user.getName(),
    "experimentName", experiment.getName(),
    "outcome", experiment.getResult().getOutcome(),
    "summary", experiment.getResult().getSummary(),
    "experimentUrl", buildExperimentUrl(experiment.getId()),
    "organizationName", organization.getName()
);
```

**Email Content**:
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Experiment Completed</title>
</head>
<body>
    <h1>Experiment Completed</h1>
    <p>Hi <span th:text="${userName}">User</span>,</p>
    
    <p>Your experiment "<strong th:text="${experimentName}">Experiment Name</strong>" has been completed.</p>
    
    <p><strong>Outcome:</strong> <span th:text="${outcome}">VALIDATED</span></p>
    <p><strong>Summary:</strong> <span th:text="${summary}">Summary</span></p>
    
    <p>
        <a th:href="${experimentUrl}">View Experiment Details</a>
    </p>
    
    <p>Best regards,<br/>The Turaf Team</p>
</body>
</html>
```

---

### Report Ready Email

**Subject**: "Report Ready: {experiment.name}"

**Template Variables**:
```java
Map<String, Object> variables = Map.of(
    "userName", user.getName(),
    "experimentName", experiment.getName(),
    "reportUrl", buildReportDownloadUrl(reportLocation),
    "experimentUrl", buildExperimentUrl(experiment.getId())
);
```

---

### Email Sending

**Amazon SES Integration**:
```java
@Service
public class EmailService {
    
    private final AmazonSimpleEmailService sesClient;
    private final TemplateEngine templateEngine;
    
    public void sendEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        // Render template
        Context context = new Context();
        context.setVariables(variables);
        String htmlBody = templateEngine.process(templateName, context);
        
        // Create email message
        Message message = new Message()
            .withSubject(new Content(subject))
            .withBody(new Body().withHtml(new Content(htmlBody)));
        
        // Send email
        SendEmailRequest request = new SendEmailRequest()
            .withSource("notifications@turaf.com")
            .withDestination(new Destination().withToAddresses(to))
            .withMessage(message);
        
        try {
            SendEmailResult result = sesClient.sendEmail(request);
            log.info("Email sent to {}, messageId: {}", to, result.getMessageId());
        } catch (Exception e) {
            log.error("Failed to send email to {}", to, e);
            throw new EmailSendException("Failed to send email", e);
        }
    }
}
```

---

## Webhook Notifications

### Webhook Configuration

**Storage**: Organization settings in database

**Webhook Structure**:
```json
{
  "organizationId": "uuid",
  "webhookUrl": "https://example.com/webhooks/turaf",
  "events": ["experiment.completed", "report.generated"],
  "secret": "webhook-signing-secret",
  "enabled": true
}
```

---

### Webhook Delivery

**HTTP Request**:
```
POST {webhookUrl}
Content-Type: application/json
X-Turaf-Signature: sha256={signature}
X-Turaf-Event: {eventType}
X-Turaf-Delivery: {deliveryId}

{
  "eventId": "uuid",
  "eventType": "experiment.completed",
  "timestamp": "ISO-8601",
  "organizationId": "uuid",
  "data": {
    "experimentId": "uuid",
    "name": "Experiment Name",
    "outcome": "VALIDATED"
  }
}
```

**Signature Calculation**:
```java
private String calculateSignature(String payload, String secret) {
    Mac mac = Mac.getInstance("HmacSHA256");
    SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
    mac.init(secretKey);
    byte[] hash = mac.doFinal(payload.getBytes());
    return Hex.encodeHexString(hash);
}
```

**Webhook Sending**:
```java
@Service
public class WebhookService {
    
    private final WebClient webClient;
    
    public void sendWebhook(Webhook webhook, String eventType, Object payload) {
        String payloadJson = toJson(payload);
        String signature = calculateSignature(payloadJson, webhook.getSecret());
        String deliveryId = UUID.randomUUID().toString();
        
        webClient.post()
            .uri(webhook.getUrl())
            .header("Content-Type", "application/json")
            .header("X-Turaf-Signature", "sha256=" + signature)
            .header("X-Turaf-Event", eventType)
            .header("X-Turaf-Delivery", deliveryId)
            .bodyValue(payloadJson)
            .retrieve()
            .toBodilessEntity()
            .doOnSuccess(response -> log.info("Webhook delivered: {}", deliveryId))
            .doOnError(error -> log.error("Webhook delivery failed: {}", deliveryId, error))
            .subscribe();
    }
}
```

---

### Webhook Retry Strategy

**Retry Policy**:
- Retry on 5xx errors and network failures
- Exponential backoff: 1s, 2s, 4s, 8s, 16s
- Maximum 5 retry attempts
- No retry on 4xx errors (client errors)

**Implementation**:
```java
@Retryable(
    value = {WebhookDeliveryException.class},
    maxAttempts = 5,
    backoff = @Backoff(delay = 1000, multiplier = 2)
)
public void sendWebhookWithRetry(Webhook webhook, String eventType, Object payload) {
    sendWebhook(webhook, eventType, payload);
}
```

---

## Notification Preferences

### User Preferences

**Preference Structure**:
```json
{
  "userId": "uuid",
  "organizationId": "uuid",
  "emailNotifications": {
    "experimentCompleted": true,
    "reportGenerated": true,
    "experimentStarted": false
  },
  "webhookNotifications": {
    "enabled": true
  }
}
```

**Preference Checking**:
```java
private boolean shouldSendNotification(User user, String notificationType) {
    NotificationPreferences prefs = preferencesRepository.findByUserId(user.getId());
    if (prefs == null) {
        return true; // Default: send all notifications
    }
    return prefs.isEnabled(notificationType);
}
```

---

## Recipient Selection

### Get Recipients Logic

**Rules**:
- Experiment creator always receives notifications
- Organization admins receive notifications
- Users can opt-out via preferences

**Implementation**:
```java
private List<User> getRecipients(ExperimentDto experiment) {
    List<User> recipients = new ArrayList<>();
    
    // Add experiment creator
    User creator = userClient.getUser(experiment.getCreatedBy());
    if (shouldSendNotification(creator, "experimentCompleted")) {
        recipients.add(creator);
    }
    
    // Add organization admins
    List<User> admins = organizationClient.getAdmins(experiment.getOrganizationId());
    admins.stream()
        .filter(admin -> shouldSendNotification(admin, "experimentCompleted"))
        .forEach(recipients::add);
    
    // Remove duplicates
    return recipients.stream().distinct().collect(Collectors.toList());
}
```

---

## Error Handling

### Email Delivery Failures

**Failure Scenarios**:
- Invalid email address
- SES rate limit exceeded
- SES service unavailable
- Email bounced or rejected

**Handling**:
```java
try {
    emailService.sendEmail(user.getEmail(), subject, template, variables);
} catch (EmailSendException e) {
    log.error("Failed to send email to {}", user.getEmail(), e);
    // Record failure in database
    recordNotificationFailure(user.getId(), "email", e.getMessage());
    // Don't throw - continue processing other recipients
}
```

---

### Webhook Delivery Failures

**Failure Scenarios**:
- Webhook URL unreachable
- Timeout
- 5xx server errors
- Invalid webhook configuration

**Handling**:
- Retry with exponential backoff
- After max retries, record failure
- Send alert to organization admins
- Disable webhook after repeated failures

---

## Idempotency

### Deduplication Strategy

**Idempotency Key**: EventBridge eventId

**Implementation**:
```java
@Entity
@Table(name = "processed_notification_events")
public class ProcessedNotificationEvent {
    @Id
    private String eventId;
    private String eventType;
    private Instant processedAt;
    private Integer recipientCount;
}

private boolean isAlreadyProcessed(String eventId) {
    return processedEventRepository.existsById(eventId);
}

private void markAsProcessed(String eventId, String eventType, int recipientCount) {
    ProcessedNotificationEvent event = new ProcessedNotificationEvent(
        eventId, eventType, Instant.now(), recipientCount
    );
    processedEventRepository.save(event);
}
```

---

## Lambda Configuration

### Function Configuration

**Memory**: 512 MB  
**Timeout**: 30 seconds  
**Concurrency**: 20 (reserved concurrency)  
**Environment Variables**:
- `ENVIRONMENT`: dev/qa/prod
- `SES_FROM_EMAIL`: notifications@turaf.com
- `EXPERIMENT_SERVICE_URL`: https://api.{env}.turaf.com
- `ORGANIZATION_SERVICE_URL`: https://api.{env}.turaf.com
- `FRONTEND_URL`: https://app.{env}.turaf.com

### IAM Permissions

**Required Permissions**:
- `ses:SendEmail` for email notifications
- `logs:CreateLogGroup`, `logs:CreateLogStream`, `logs:PutLogEvents`
- `secretsmanager:GetSecretValue` for API credentials
- HTTP egress for webhook delivery

---

## Notification Channels

### Current Channels

1. **Email** (via Amazon SES)
   - Transactional emails
   - HTML templates
   - Delivery tracking

2. **Webhooks** (via HTTP POST)
   - Real-time event delivery
   - Signature verification
   - Retry logic

### Future Channels

- **Slack**: Direct messages and channel notifications
- **Microsoft Teams**: Team notifications
- **SMS**: Critical alerts via SNS
- **In-App**: Browser push notifications
- **Mobile Push**: iOS and Android push notifications

---

## Monitoring

### CloudWatch Metrics

**Custom Metrics**:
- `NotificationsSent`: Count by type (email, webhook)
- `NotificationFailures`: Count by type and reason
- `EmailDeliveryTime`: Duration to send emails
- `WebhookDeliveryTime`: Duration to deliver webhooks

**CloudWatch Logs**:
- Log all notification attempts
- Log delivery failures with details
- Include event ID and recipient info

### Alarms

**Alarm Conditions**:
- Email failure rate > 5%
- Webhook failure rate > 10%
- SES bounce rate > 5%
- Processing time > 15 seconds

---

## Testing Strategy

### Unit Tests
- Test email template rendering
- Test webhook signature calculation
- Test recipient selection logic
- Test preference checking

### Integration Tests
- Test with LocalStack SES
- Test webhook delivery to mock server
- Test idempotency
- Test error scenarios

### End-to-End Tests
- Publish domain events
- Verify emails sent
- Verify webhooks delivered
- Verify preferences honored

---

## Security Considerations

### Email Security
- SPF, DKIM, DMARC configured for domain
- No sensitive data in email bodies
- Secure links with time-limited tokens

### Webhook Security
- HMAC signature verification
- HTTPS only for webhook URLs
- Webhook secret rotation
- Rate limiting per organization

---

## Performance Optimization

### Batching
- Batch email sends when possible
- Parallel webhook delivery
- Async processing with reactive streams

### Caching
- Cache user preferences
- Cache organization settings
- Cache email templates

---

## Future Enhancements

- Notification scheduling (digest emails)
- Notification templates customization
- Multi-language support
- Notification analytics dashboard
- Delivery status tracking
- Read receipts for emails
- Webhook payload customization
- Notification rules engine

---

## References

- PROJECT.md: Notification Service specification
- event-flow.md: Event specifications
- Amazon SES Documentation
- Webhook Best Practices
