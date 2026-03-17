# Task: Implement PDF Generation

**Service**: Reporting Service  
**Phase**: 7  
**Estimated Time**: 3 hours  

## Objective

Implement PDF generation from HTML templates using iText library.

## Prerequisites

- [x] Task 005: Report templates created

## Scope

**Files to Create**:
- `services/reporting-service/src/main/java/com/turaf/reporting/service/PdfGenerationService.java`

## Implementation Details

### PDF Generation Service

```java
public class PdfGenerationService {
    
    public byte[] generatePdf(String html) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        try {
            ConverterProperties converterProperties = new ConverterProperties();
            HtmlConverter.convertToPdf(html, outputStream, converterProperties);
            
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new IOException("Failed to generate PDF", e);
        }
    }
    
    public File generatePdfFile(String html, String filename) throws IOException {
        byte[] pdfBytes = generatePdf(html);
        
        File pdfFile = new File("/tmp/" + filename);
        try (FileOutputStream fos = new FileOutputStream(pdfFile)) {
            fos.write(pdfBytes);
        }
        
        return pdfFile;
    }
}
```

## Acceptance Criteria

- [ ] PDF generation from HTML works
- [ ] PDF formatting correct
- [ ] File generation works
- [ ] Error handling implemented
- [ ] Unit tests pass

## Testing Requirements

**Unit Tests**:
- Test PDF generation
- Test file creation

**Test Files to Create**:
- `PdfGenerationServiceTest.java`

## References

- Specification: `specs/reporting-service.md` (PDF Generation section)
- Related Tasks: 007-implement-s3-storage
