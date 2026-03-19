# Refactor Reporting Service Tasks from Java to Python

Align all 10 reporting-service task files with the Python-based specification already defined in `specs/reporting-service.md`.

## Current State

The reporting-service specification (`specs/reporting-service.md`) is **already written for Python**:
- Runtime: AWS Lambda (Python 3.11)
- Dependencies: boto3, jinja2, reportlab/weasyprint, requests
- All code examples are in Python

However, **all 10 task files** still reference Java/Maven implementation:
- Task 001-010 use Java syntax, Maven pom.xml, JUnit tests
- File paths reference Java package structure (`src/main/java/com/turaf/reporting/`)
- Dependencies reference Java libraries (iText, Handlebars, Jackson)

## Refactoring Scope

### Files to Update (10 task files)

1. **001-setup-lambda-project.md**
   - Replace Maven pom.xml → Python requirements.txt
   - Replace Java handler → Python lambda_handler
   - Update file paths to Python structure
   - Replace JUnit → pytest

2. **002-implement-event-handler.md**
   - Convert Java EventBridge handler → Python lambda_handler
   - Replace ObjectMapper → json module
   - Update exception handling to Python
   - Replace JUnit tests → pytest

3. **003-implement-data-fetching.md**
   - Convert Java HTTP clients → Python requests library
   - Replace service client classes → Python functions
   - Update error handling to Python exceptions
   - Replace JUnit → pytest

4. **004-implement-aggregation-logic.md**
   - Convert Java streams/collectors → Python list comprehensions
   - Replace Java Duration → Python datetime.timedelta
   - Update statistics calculations to Python statistics module
   - Replace JUnit → pytest

5. **005-create-report-templates.md**
   - Replace Handlebars → Jinja2 templates
   - Update template syntax from {{#each}} → {% for %}
   - Convert Java template engine → Python Jinja2 Environment
   - Replace JUnit → pytest

6. **006-implement-pdf-generation.md**
   - Replace iText library → WeasyPrint or ReportLab
   - Convert Java ByteArrayOutputStream → Python io.BytesIO
   - Update PDF generation code to Python
   - Replace JUnit → pytest

7. **007-implement-s3-storage.md**
   - Replace AWS SDK v2 Java → boto3 Python
   - Convert S3Client → boto3.client('s3')
   - Update presigned URL generation to boto3
   - Replace JUnit → pytest

8. **008-implement-event-publishing.md**
   - Replace EventBridgeClient Java → boto3.client('events')
   - Convert Jackson serialization → json.dumps()
   - Update event structure to Python dict
   - Replace JUnit → pytest

9. **009-add-idempotency.md**
   - Replace DynamoDbClient Java → boto3.resource('dynamodb')
   - Convert AttributeValue builders → Python dict
   - Update TTL calculation to Python
   - Replace JUnit → pytest

10. **010-add-unit-tests.md**
    - Update all test file references from .java → .py
    - Replace JUnit/Mockito → pytest/unittest.mock
    - Update testing framework references
    - Add pytest-mock, moto for AWS mocking

## Python Project Structure

```
services/reporting-service/
├── requirements.txt              # Python dependencies
├── requirements-dev.txt          # Development dependencies (pytest, moto, etc.)
├── src/
│   ├── __init__.py
│   ├── lambda_handler.py        # Main Lambda entry point
│   ├── services/
│   │   ├── __init__.py
│   │   ├── report_generation.py
│   │   ├── data_fetching.py
│   │   ├── aggregation.py
│   │   ├── pdf_generation.py
│   │   ├── s3_storage.py
│   │   └── idempotency.py
│   ├── clients/
│   │   ├── __init__.py
│   │   ├── experiment_client.py
│   │   └── metrics_client.py
│   ├── events/
│   │   ├── __init__.py
│   │   └── event_publisher.py
│   └── templates/
│       └── experiment-report.html
└── tests/
    ├── __init__.py
    ├── test_lambda_handler.py
    ├── test_report_generation.py
    ├── test_data_fetching.py
    ├── test_aggregation.py
    ├── test_pdf_generation.py
    ├── test_s3_storage.py
    ├── test_event_publisher.py
    └── test_idempotency.py
```

## Key Technology Replacements

| Java/Maven | Python |
|------------|--------|
| Maven (pom.xml) | pip (requirements.txt) |
| JUnit 5 | pytest |
| Mockito | unittest.mock / pytest-mock |
| Jackson | json (built-in) |
| AWS SDK v2 | boto3 |
| iText PDF | WeasyPrint or ReportLab |
| Handlebars | Jinja2 |
| Java HttpClient | requests library |
| Java Streams | List comprehensions / map/filter |
| Log4j/SLF4J | python-json-logger |

## Dependencies to Add

**requirements.txt**:
```
boto3>=1.28.0
jinja2>=3.1.0
weasyprint>=59.0
requests>=2.31.0
python-json-logger>=2.0.0
tenacity>=8.2.0
```

**requirements-dev.txt**:
```
pytest>=7.4.0
pytest-mock>=3.11.0
pytest-cov>=4.1.0
moto>=4.2.0
```

## Template Syntax Changes

**Handlebars (Java)** → **Jinja2 (Python)**:
- `{{variable}}` → `{{ variable }}` (same)
- `{{#each items}}` → `{% for item in items %}`
- `{{/each}}` → `{% endfor %}`
- `{{#if condition}}` → `{% if condition %}`
- `{{/if}}` → `{% endif %}`

## Testing Framework Changes

**JUnit/Mockito** → **pytest/moto**:
- `@Test` → `def test_*():`
- `@Mock` → `@patch()` or `mocker.Mock()`
- `@ExtendWith(MockitoExtension.class)` → `pytest fixtures`
- `assertEquals()` → `assert x == y`
- `verify()` → `mock.assert_called_with()`
- AWS mocking: Mockito → moto library

## Implementation Order

1. Update Task 001 (setup) - Foundation
2. Update Task 002 (event handler) - Core handler
3. Update Task 009 (idempotency) - Early dependency
4. Update Task 003 (data fetching) - Data layer
5. Update Task 004 (aggregation) - Processing
6. Update Task 005 (templates) - Rendering
7. Update Task 006 (PDF generation) - Output
8. Update Task 007 (S3 storage) - Persistence
9. Update Task 008 (event publishing) - Events
10. Update Task 010 (unit tests) - Comprehensive testing

## Changes Per Task File

### Common Changes (All Files)
- Update file paths from Java structure to Python structure
- Replace `.java` extensions with `.py`
- Update code examples from Java to Python syntax
- Replace JUnit test references with pytest
- Update dependency references

### Specific Considerations

**Task 001**: 
- Most critical - sets foundation
- Replace entire Maven section with pip/requirements.txt
- Update Lambda handler signature

**Task 002-009**:
- Convert all Java code blocks to Python
- Maintain same logic and architecture
- Keep Clean Architecture principles
- Preserve error handling patterns

**Task 010**:
- Update all test file names
- Add moto for AWS service mocking
- Include pytest configuration

## Validation Checklist

After refactoring, each task file should:
- [ ] Reference Python file paths (not Java package structure)
- [ ] Use Python syntax in all code examples
- [ ] Reference Python libraries (boto3, not AWS SDK v2)
- [ ] Use pytest for testing (not JUnit)
- [ ] Match the specification in `specs/reporting-service.md`
- [ ] Maintain Clean Architecture principles
- [ ] Include proper error handling
- [ ] Reference correct Python modules

## Notes

- The specification is already correct (Python-based)
- Only task files need updating
- No changes to `specs/reporting-service.md` required
- Maintain same architecture and patterns
- Keep DDD and Clean Architecture principles
- Preserve all acceptance criteria
- Update only implementation language, not design
