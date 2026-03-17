# Task: Implement Email Service

**Service**: Notification Service  
**Phase**: 8  
**Estimated Time**: 3 hours  

## Objective

Implement email sending service using Amazon SES with template support.

## Prerequisites

- [x] Task 002: Event handlers implemented

## Scope

**Files to Create**:
- `services/notification-service/src/main/java/com/turaf/notification/service/EmailService.java`
- `services/notification-service/src/main/java/com/turaf/notification/service/SesEmailSender.java`
- `services/notification-service/src/main/java/com/turaf/notification/model/EmailMessage.java`

## Implementation Details

### Email Service

```java
public class EmailService {
    private final SesEmailSender emailSender;
    private final TemplateService templateService;
    private final RecipientService recipientService;
    
    public void sendExperimentCompletedEmail(String organizationId, String experimentId) {
        // Get recipients
        List<String> recipients = recipientService.getRecipients(organizationId, "EXPERIMENT_COMPLETED");
        
        // Prepare email data
        Map<String, Object> data = Map.of(
            "experimentId", experimentId,
            "organizationId", organizationId
        );
        
        // Render template
        String subject = "Experiment Completed";
        String htmlBody = templateService.render("experiment-completed", data);
        
        // Send email
        EmailMessage message = new EmailMessage(
            recipients,
            subject,
            htmlBody
        );
        
        emailSender.send(message);
    }
    
    public void sendReportGeneratedEmail(String organizationId, String experimentId, String reportUrl) {
        List<String> recipients = recipientService.getRecipients(organizationId, "REPORT_GENERATED");
        
        Map<String, Object> data = Map.of(
            "experimentId", experimentId,
            "reportUrl", reportUrl
        );
        
        String subject = "Experiment Report Available";
        String htmlBody = templateService.render("report-generated", data);
        
        EmailMessage message = new EmailMessage(recipients, subject, htmlBody);
        emailSender.send(message);
    }
}
```

### SES Email Sender

```java
public class SesEmailSender {
    private final SesClient sesClient;
    private final String fromEmail;
    
    public SesEmailSender() {
        this.sesClient = SesClient.builder()
            .region(Region.US_EAST_1)
            .build();
        this.fromEmail = System.getenv("FROM_EMAIL");
    }
    
    public void send(EmailMessage message) {
        SendEmailRequest request = SendEmailRequest.builder()
            .source(fromEmail)
            .destination(Destination.builder()
                .toAddresses(message.getRecipients())
                .build())
            .message(Message.builder()
                .subject(Content.builder()
                    .data(message.getSubject())
                    .build())
                .body(Body.builder()
                    .html(Content.builder()
                        .data(message.getHtmlBody())
                        .build())
                    .build())
                .build())
            .build();
        
        try {
            sesClient.sendEmail(request);
        } catch (SesException e) {
            throw new EmailSendException("Failed to send email", e);
        }
    }
}
```

## Acceptance Criteria

- [ ] Email service sends emails via SES
- [ ] Templates rendered correctly
- [ ] Recipients fetched correctly
- [ ] Error handling implemented
- [ ] Unit tests pass

## Testing Requirements

**Unit Tests**:
- Test email sending
- Test template rendering
- Test recipient selection

**Test Files to Create**:
- `EmailServiceTest.java`
- `SesEmailSenderTest.java`

## References

- Specification: `specs/notification-service.md` (Email Service section)
- Related Tasks: 004-create-email-templates
