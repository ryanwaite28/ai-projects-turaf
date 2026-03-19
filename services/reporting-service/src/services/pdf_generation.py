"""
PDF generation service for converting HTML to PDF.

This module provides services for generating PDF documents from HTML content
using WeasyPrint, following Clean Architecture principles.
"""

import io
import logging
from typing import Optional
from weasyprint import HTML, CSS
from weasyprint.text.fonts import FontConfiguration

logger = logging.getLogger(__name__)


class PdfGenerationService:
    """
    Service for generating PDF documents from HTML content.
    
    This service acts as an adapter between the application and the WeasyPrint
    library, following the Adapter pattern. It provides a clean interface for
    PDF generation while handling library-specific details internally.
    
    Features:
    - HTML to PDF conversion
    - Custom CSS support
    - Font configuration
    - In-memory PDF generation (returns bytes)
    - File-based PDF generation
    - Comprehensive error handling
    
    The service follows the Single Responsibility Principle (SOLID),
    focusing solely on PDF generation.
    
    Attributes:
        font_config: WeasyPrint font configuration for proper text rendering
    """
    
    def __init__(self):
        """
        Initialize PDF generation service with font configuration.
        
        Font configuration is required for proper text rendering in PDFs,
        especially for special characters and non-Latin scripts.
        """
        self.font_config = FontConfiguration()
        logger.info("PdfGenerationService initialized with font configuration")
    
    def generate_pdf(
        self,
        html_content: str,
        css_content: Optional[str] = None
    ) -> bytes:
        """
        Generate PDF from HTML content.
        
        This method converts HTML content to PDF format, optionally applying
        additional CSS styles. The PDF is generated in-memory and returned
        as bytes, suitable for uploading to S3 or other storage.
        
        Args:
            html_content: HTML string to convert to PDF. Should be valid HTML5.
            css_content: Optional additional CSS to apply. Will be merged with
                        inline styles in the HTML.
                        
        Returns:
            PDF content as bytes, ready for storage or transmission
            
        Raises:
            ValueError: If html_content is empty or None
            Exception: If PDF generation fails due to invalid HTML or other errors
            
        Example:
            >>> service = PdfGenerationService()
            >>> html = '<html><body><h1>Report</h1></body></html>'
            >>> pdf_bytes = service.generate_pdf(html)
            >>> len(pdf_bytes) > 0
            True
        """
        if not html_content:
            raise ValueError("html_content is required and cannot be empty")
        
        try:
            logger.info("Generating PDF from HTML content")
            logger.debug(f"HTML content length: {len(html_content)} characters")
            
            # Create HTML object from string
            html = HTML(string=html_content)
            
            # Create in-memory buffer for PDF
            pdf_buffer = io.BytesIO()
            
            # Generate PDF with optional custom CSS
            if css_content:
                logger.debug(f"Applying custom CSS: {len(css_content)} characters")
                css = CSS(string=css_content, font_config=self.font_config)
                html.write_pdf(
                    pdf_buffer,
                    stylesheets=[css],
                    font_config=self.font_config
                )
            else:
                html.write_pdf(pdf_buffer, font_config=self.font_config)
            
            # Get PDF bytes from buffer
            pdf_bytes = pdf_buffer.getvalue()
            
            logger.info(f"PDF generated successfully: {len(pdf_bytes)} bytes")
            
            return pdf_bytes
            
        except ValueError as e:
            logger.error(f"Validation error in PDF generation: {str(e)}")
            raise
            
        except Exception as e:
            logger.error(
                f"Failed to generate PDF: {str(e)}",
                exc_info=True
            )
            raise Exception(f"PDF generation failed: {str(e)}")
    
    def generate_pdf_file(
        self,
        html_content: str,
        filepath: str,
        css_content: Optional[str] = None
    ) -> str:
        """
        Generate PDF and save to file.
        
        This method generates a PDF from HTML content and saves it directly
        to the filesystem. Useful for local testing or when file-based
        storage is preferred over S3.
        
        Args:
            html_content: HTML string to convert to PDF
            filepath: Absolute path where PDF should be saved
            css_content: Optional additional CSS to apply
            
        Returns:
            Path to the generated PDF file (same as filepath parameter)
            
        Raises:
            ValueError: If html_content or filepath is empty
            Exception: If PDF generation or file writing fails
            IOError: If file cannot be written
            
        Example:
            >>> service = PdfGenerationService()
            >>> html = '<html><body><h1>Report</h1></body></html>'
            >>> path = service.generate_pdf_file(html, '/tmp/report.pdf')
            >>> path
            '/tmp/report.pdf'
        """
        if not filepath:
            raise ValueError("filepath is required and cannot be empty")
        
        try:
            logger.info(f"Generating PDF and saving to {filepath}")
            
            # Generate PDF bytes
            pdf_bytes = self.generate_pdf(html_content, css_content)
            
            # Write to file
            with open(filepath, 'wb') as f:
                f.write(pdf_bytes)
            
            logger.info(f"PDF saved successfully to {filepath}")
            
            return filepath
            
        except ValueError as e:
            logger.error(f"Validation error: {str(e)}")
            raise
            
        except IOError as e:
            logger.error(f"Failed to write PDF to {filepath}: {str(e)}")
            raise
            
        except Exception as e:
            logger.error(f"Failed to save PDF to {filepath}: {str(e)}")
            raise
    
    def validate_html(self, html_content: str) -> bool:
        """
        Validate HTML content before PDF generation.
        
        This method performs basic validation to ensure the HTML content
        is suitable for PDF generation. It checks for basic structure
        and common issues.
        
        Args:
            html_content: HTML string to validate
            
        Returns:
            True if HTML appears valid, False otherwise
            
        Note:
            This is a basic validation. WeasyPrint may still fail on
            complex or malformed HTML even if this returns True.
        """
        if not html_content or not html_content.strip():
            logger.warning("HTML content is empty")
            return False
        
        # Check for basic HTML structure
        has_html_tag = '<html' in html_content.lower()
        has_body_tag = '<body' in html_content.lower()
        
        if not has_html_tag or not has_body_tag:
            logger.warning("HTML content missing basic structure tags")
            return False
        
        logger.debug("HTML content passed basic validation")
        return True
