# Task: Create Email Templates

**Service**: Notification Service  
**Phase**: 8  
**Estimated Time**: 2 hours  

## Objective

Create HTML email templates for various notification types.

## Prerequisites

- [x] Task 003: Email service implemented

## Scope

**Files to Create**:
- `services/notification-service/templates/email/experiment-completed.html`
- `services/notification-service/templates/email/report-generated.html`
- `services/notification-service/templates/email/member-added.html`
- `services/notification-service/src/main/java/com/turaf/notification/template/TemplateService.java`

## Implementation Details

### Email Templates

```html
<!-- experiment-completed.html -->
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <style>
        body { font-family: Arial, sans-serif; }
        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
        .header { background: #4CAF50; color: white; padding: 20px; text-align: center; }
        .content { padding: 20px; background: #f9f9f9; }
        .button { background: #4CAF50; color: white; padding: 10px 20px; text-decoration: none; display: inline-block; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>Experiment Completed</h1>
        </div>
        <div class="content">
            <p>Your experiment has been completed successfully.</p>
            <p><strong>Experiment ID:</strong> {{ experimentId }}</p>
            <p>A report is being generated and will be available shortly.</p>
            <a href="https://app.turaf.com/experiments/{{ experimentId }}" class="button">View Experiment</a>
        </div>
    </div>
</body>
</html>
```

### Template Service

```java
public class TemplateService {
    private final TemplateEngine templateEngine;
    
    public TemplateService() {
        this.templateEngine = new Jinja2TemplateEngine();
    }
    
    public String render(String templateName, Map<String, Object> data) {
        try {
            Template template = templateEngine.getTemplate(templateName);
            return template.render(data);
        } catch (IOException e) {
            throw new TemplateRenderException("Failed to render template: " + templateName, e);
        }
    }
}
```

## Acceptance Criteria

- [x] All email templates created
- [x] Templates render correctly
- [x] Template service works
- [x] Styling applied
- [x] Unit tests pass

## Testing Requirements

**Unit Tests**:
- Test template rendering
- Test data binding

**Test Files to Create**:
- `test_template_engine.py`

## References

- Specification: `specs/notification-service.md` (Email Templates section)
- Related Tasks: 005-implement-webhook-service
