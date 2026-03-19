# Task: Implement PDF Generation

**Service**: Reporting Service  
**Phase**: 7  
**Estimated Time**: 3 hours  

## Objective

Implement PDF generation from HTML templates using WeasyPrint library.

## Prerequisites

- [x] Task 005: Report templates created

## Scope

**Files to Create**:
- `services/reporting-service/src/services/pdf_generation.py`

## Implementation Details

### PDF Generation Service

```python
import io
import logging
from typing import Optional
from weasyprint import HTML, CSS
from weasyprint.text.fonts import FontConfiguration

logger = logging.getLogger(__name__)

class PdfGenerationService:
    """Service for generating PDFs from HTML content"""
    
    def __init__(self):
        self.font_config = FontConfiguration()
    
    def generate_pdf(self, html_content: str, css_content: Optional[str] = None) -> bytes:
        """
        Generate PDF from HTML content.
        
        Args:
            html_content: HTML string to convert
            css_content: Optional additional CSS
            
        Returns:
            PDF content as bytes
            
        Raises:
            Exception: If PDF generation fails
        """
        try:
            logger.info("Generating PDF from HTML")
            
            # Create HTML object
            html = HTML(string=html_content)
            
            # Generate PDF to bytes
            pdf_buffer = io.BytesIO()
            
            if css_content:
                css = CSS(string=css_content, font_config=self.font_config)
                html.write_pdf(pdf_buffer, stylesheets=[css], font_config=self.font_config)
            else:
                html.write_pdf(pdf_buffer, font_config=self.font_config)
            
            pdf_bytes = pdf_buffer.getvalue()
            
            logger.info(f"PDF generated successfully, size: {len(pdf_bytes)} bytes")
            
            return pdf_bytes
            
        except Exception as e:
            logger.error(f"Failed to generate PDF: {str(e)}", exc_info=True)
            raise Exception(f"PDF generation failed: {str(e)}")
    
    def generate_pdf_file(self, html_content: str, filepath: str) -> str:
        """
        Generate PDF and save to file.
        
        Args:
            html_content: HTML string to convert
            filepath: Path where PDF should be saved
            
        Returns:
            Path to generated PDF file
        """
        try:
            pdf_bytes = self.generate_pdf(html_content)
            
            with open(filepath, 'wb') as f:
                f.write(pdf_bytes)
            
            logger.info(f"PDF saved to {filepath}")
            
            return filepath
            
        except Exception as e:
            logger.error(f"Failed to save PDF to {filepath}: {str(e)}")
            raise
```

### Alternative: ReportLab Implementation

```python
# Alternative implementation using ReportLab for more control
import io
import logging
from reportlab.lib.pagesizes import letter
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer
from reportlab.lib.styles import getSampleStyleSheet
from reportlab.lib.units import inch

logger = logging.getLogger(__name__)

class ReportLabPdfGenerator:
    """Alternative PDF generator using ReportLab"""
    
    def generate_pdf_from_data(self, data: dict) -> bytes:
        """
        Generate PDF directly from data (without HTML template).
        Useful for programmatic PDF generation.
        """
        buffer = io.BytesIO()
        doc = SimpleDocTemplate(buffer, pagesize=letter)
        styles = getSampleStyleSheet()
        story = []
        
        # Add content
        story.append(Paragraph("Experiment Report", styles['Title']))
        story.append(Spacer(1, 0.2 * inch))
        
        # Add more content based on data...
        
        doc.build(story)
        
        return buffer.getvalue()
```

## Acceptance Criteria

- [x] PDF generation from HTML works
- [x] PDF formatting correct
- [x] File generation works
- [x] Error handling implemented
- [x] Font configuration handled
- [x] CSS styling applied
- [x] Unit tests pass

## Testing Requirements

**Unit Tests**:
- Test PDF generation from HTML
- Test file creation
- Test error handling
- Test with custom CSS
- Test PDF size validation

**Test Files to Create**:
- `tests/test_pdf_generation.py`

## References

- Specification: `specs/reporting-service.md` (PDF Generation section)
- WeasyPrint Documentation: https://weasyprint.readthedocs.io/
- Related Tasks: 007-implement-s3-storage
