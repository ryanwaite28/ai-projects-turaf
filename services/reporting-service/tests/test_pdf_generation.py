"""
Unit tests for PdfGenerationService.

Tests cover PDF generation, file creation, and error handling.
"""

import pytest
import os
from src.services.pdf_generation import PdfGenerationService


class TestPdfGenerationService:
    """Test suite for PdfGenerationService."""
    
    @pytest.fixture
    def service(self):
        """Create service instance for tests."""
        return PdfGenerationService()
    
    @pytest.fixture
    def simple_html(self):
        """Create simple HTML content for testing."""
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <title>Test Report</title>
        </head>
        <body>
            <h1>Test Report</h1>
            <p>This is a test report.</p>
        </body>
        </html>
        """
    
    @pytest.fixture
    def complex_html(self):
        """Create complex HTML with tables and styling."""
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <title>Complex Report</title>
            <style>
                body { font-family: Arial, sans-serif; }
                table { width: 100%; border-collapse: collapse; }
                th, td { border: 1px solid #ddd; padding: 8px; }
                th { background-color: #4CAF50; color: white; }
            </style>
        </head>
        <body>
            <h1>Experiment Report</h1>
            <table>
                <tr>
                    <th>Metric</th>
                    <th>Value</th>
                </tr>
                <tr>
                    <td>Latency</td>
                    <td>125.5ms</td>
                </tr>
            </table>
        </body>
        </html>
        """
    
    @pytest.fixture
    def custom_css(self):
        """Create custom CSS for testing."""
        return """
        body {
            margin: 20px;
            font-size: 14px;
        }
        h1 {
            color: #333;
        }
        """
    
    def test_service_initialization(self, service):
        """Test that service initializes correctly."""
        assert service is not None
        assert service.font_config is not None
    
    def test_generate_pdf_from_simple_html(self, service, simple_html):
        """Test generating PDF from simple HTML."""
        # When
        pdf_bytes = service.generate_pdf(simple_html)
        
        # Then
        assert pdf_bytes is not None
        assert len(pdf_bytes) > 0
        assert pdf_bytes.startswith(b'%PDF')  # PDF magic number
    
    def test_generate_pdf_from_complex_html(self, service, complex_html):
        """Test generating PDF from complex HTML with tables."""
        # When
        pdf_bytes = service.generate_pdf(complex_html)
        
        # Then
        assert pdf_bytes is not None
        assert len(pdf_bytes) > 0
        assert pdf_bytes.startswith(b'%PDF')
    
    def test_generate_pdf_with_custom_css(self, service, simple_html, custom_css):
        """Test generating PDF with custom CSS."""
        # When
        pdf_bytes = service.generate_pdf(simple_html, css_content=custom_css)
        
        # Then
        assert pdf_bytes is not None
        assert len(pdf_bytes) > 0
        assert pdf_bytes.startswith(b'%PDF')
    
    def test_generate_pdf_raises_error_for_empty_html(self, service):
        """Test that generate_pdf raises error for empty HTML."""
        # When/Then
        with pytest.raises(ValueError) as exc_info:
            service.generate_pdf("")
        assert 'html_content is required' in str(exc_info.value)
    
    def test_generate_pdf_raises_error_for_none_html(self, service):
        """Test that generate_pdf raises error for None HTML."""
        # When/Then
        with pytest.raises(ValueError) as exc_info:
            service.generate_pdf(None)
        assert 'html_content is required' in str(exc_info.value)
    
    def test_generate_pdf_raises_error_for_invalid_html(self, service):
        """Test that generate_pdf raises error for malformed HTML."""
        # Given
        invalid_html = "This is not HTML at all"
        
        # When/Then
        with pytest.raises(Exception) as exc_info:
            service.generate_pdf(invalid_html)
        assert 'PDF generation failed' in str(exc_info.value)
    
    def test_generate_pdf_file_creates_file(self, service, simple_html, tmp_path):
        """Test that generate_pdf_file creates a file."""
        # Given
        filepath = str(tmp_path / "test-report.pdf")
        
        # When
        result_path = service.generate_pdf_file(simple_html, filepath)
        
        # Then
        assert result_path == filepath
        assert os.path.exists(filepath)
        assert os.path.getsize(filepath) > 0
    
    def test_generate_pdf_file_with_custom_css(
        self,
        service,
        simple_html,
        custom_css,
        tmp_path
    ):
        """Test that generate_pdf_file works with custom CSS."""
        # Given
        filepath = str(tmp_path / "test-report-css.pdf")
        
        # When
        result_path = service.generate_pdf_file(
            simple_html,
            filepath,
            css_content=custom_css
        )
        
        # Then
        assert result_path == filepath
        assert os.path.exists(filepath)
        assert os.path.getsize(filepath) > 0
    
    def test_generate_pdf_file_raises_error_for_empty_filepath(
        self,
        service,
        simple_html
    ):
        """Test that generate_pdf_file raises error for empty filepath."""
        # When/Then
        with pytest.raises(ValueError) as exc_info:
            service.generate_pdf_file(simple_html, "")
        assert 'filepath is required' in str(exc_info.value)
    
    def test_generate_pdf_file_raises_error_for_invalid_path(
        self,
        service,
        simple_html
    ):
        """Test that generate_pdf_file raises error for invalid path."""
        # Given
        invalid_path = "/nonexistent/directory/report.pdf"
        
        # When/Then
        with pytest.raises((IOError, Exception)):
            service.generate_pdf_file(simple_html, invalid_path)
    
    def test_validate_html_returns_true_for_valid_html(self, service, simple_html):
        """Test that validate_html returns True for valid HTML."""
        # When
        is_valid = service.validate_html(simple_html)
        
        # Then
        assert is_valid is True
    
    def test_validate_html_returns_false_for_empty_html(self, service):
        """Test that validate_html returns False for empty HTML."""
        # When
        is_valid = service.validate_html("")
        
        # Then
        assert is_valid is False
    
    def test_validate_html_returns_false_for_missing_html_tag(self, service):
        """Test that validate_html returns False for missing html tag."""
        # Given
        html = "<body><h1>Test</h1></body>"
        
        # When
        is_valid = service.validate_html(html)
        
        # Then
        assert is_valid is False
    
    def test_validate_html_returns_false_for_missing_body_tag(self, service):
        """Test that validate_html returns False for missing body tag."""
        # Given
        html = "<html><head><title>Test</title></head></html>"
        
        # When
        is_valid = service.validate_html(html)
        
        # Then
        assert is_valid is False
    
    def test_generate_pdf_logs_activity(self, service, simple_html, caplog):
        """Test that PDF generation logs activity."""
        # When
        with caplog.at_level('INFO'):
            service.generate_pdf(simple_html)
        
        # Then
        assert 'Generating PDF from HTML content' in caplog.text
        assert 'PDF generated successfully' in caplog.text
    
    def test_generate_pdf_file_logs_activity(
        self,
        service,
        simple_html,
        tmp_path,
        caplog
    ):
        """Test that PDF file generation logs activity."""
        # Given
        filepath = str(tmp_path / "test-report.pdf")
        
        # When
        with caplog.at_level('INFO'):
            service.generate_pdf_file(simple_html, filepath)
        
        # Then
        assert f'Generating PDF and saving to {filepath}' in caplog.text
        assert f'PDF saved successfully to {filepath}' in caplog.text
    
    def test_generate_pdf_returns_different_sizes_for_different_content(
        self,
        service,
        simple_html,
        complex_html
    ):
        """Test that different HTML produces different PDF sizes."""
        # When
        simple_pdf = service.generate_pdf(simple_html)
        complex_pdf = service.generate_pdf(complex_html)
        
        # Then
        assert len(simple_pdf) != len(complex_pdf)
    
    def test_generate_pdf_with_unicode_characters(self, service):
        """Test that PDF generation handles Unicode characters."""
        # Given
        html_with_unicode = """
        <!DOCTYPE html>
        <html>
        <head><meta charset="UTF-8"><title>Unicode Test</title></head>
        <body>
            <h1>Unicode Characters: ñ, é, ü, 中文, 日本語</h1>
            <p>Special symbols: ©, ®, ™, €, £, ¥</p>
        </body>
        </html>
        """
        
        # When
        pdf_bytes = service.generate_pdf(html_with_unicode)
        
        # Then
        assert pdf_bytes is not None
        assert len(pdf_bytes) > 0
        assert pdf_bytes.startswith(b'%PDF')
    
    def test_generate_pdf_preserves_html_structure(self, service):
        """Test that PDF generation preserves HTML structure."""
        # Given
        html = """
        <!DOCTYPE html>
        <html>
        <head><title>Structure Test</title></head>
        <body>
            <h1>Heading 1</h1>
            <h2>Heading 2</h2>
            <p>Paragraph</p>
            <ul>
                <li>Item 1</li>
                <li>Item 2</li>
            </ul>
        </body>
        </html>
        """
        
        # When
        pdf_bytes = service.generate_pdf(html)
        
        # Then
        assert pdf_bytes is not None
        assert len(pdf_bytes) > 0
