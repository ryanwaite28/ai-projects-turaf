"""
Unit tests for template service.
"""
import pytest
from unittest.mock import Mock, patch, MagicMock
from jinja2 import TemplateNotFound

from services.template_service import TemplateService


class TestTemplateService:
    """Test suite for TemplateService."""
    
    @pytest.fixture
    def template_service(self):
        """Create TemplateService with mocked environment."""
        with patch('services.template_service.FileSystemLoader'):
            service = TemplateService(template_dir='templates')
            return service
    
    def test_initializes_with_template_directory(self):
        """Test service initializes with template directory."""
        with patch('services.template_service.FileSystemLoader') as mock_loader:
            service = TemplateService(template_dir='custom/templates')
            
            assert service.template_dir == 'custom/templates'
            mock_loader.assert_called_once_with('custom/templates')
    
    def test_renders_template_successfully(self, template_service):
        """Test successful template rendering."""
        mock_template = Mock()
        mock_template.render.return_value = '<p>Rendered content</p>'
        template_service.env.get_template = Mock(return_value=mock_template)
        
        result = template_service.render('test-template', {'key': 'value'})
        
        assert result == '<p>Rendered content</p>'
        template_service.env.get_template.assert_called_once_with('test-template.html')
        mock_template.render.assert_called_once_with(key='value')
    
    def test_adds_html_extension_if_missing(self, template_service):
        """Test automatically adds .html extension."""
        mock_template = Mock()
        mock_template.render.return_value = '<p>Content</p>'
        template_service.env.get_template = Mock(return_value=mock_template)
        
        template_service.render('test-template', {})
        
        template_service.env.get_template.assert_called_once_with('test-template.html')
    
    def test_does_not_add_extension_if_present(self, template_service):
        """Test does not add extension if already present."""
        mock_template = Mock()
        mock_template.render.return_value = '<p>Content</p>'
        template_service.env.get_template = Mock(return_value=mock_template)
        
        template_service.render('test-template.html', {})
        
        template_service.env.get_template.assert_called_once_with('test-template.html')
    
    def test_raises_error_for_missing_template(self, template_service):
        """Test raises TemplateNotFound for missing template."""
        template_service.env.get_template = Mock(
            side_effect=TemplateNotFound('template.html')
        )
        
        with pytest.raises(TemplateNotFound):
            template_service.render('missing-template', {})
    
    def test_renders_template_with_data(self, template_service):
        """Test renders template with provided data."""
        mock_template = Mock()
        mock_template.render.return_value = '<p>Hello, World</p>'
        template_service.env.get_template = Mock(return_value=mock_template)
        
        data = {'name': 'World', 'count': 42}
        template_service.render('test', data)
        
        mock_template.render.assert_called_once_with(name='World', count=42)
    
    def test_render_string_renders_from_string(self, template_service):
        """Test rendering template from string."""
        template_string = '<p>Hello, {{ name }}</p>'
        data = {'name': 'World'}
        
        with patch('services.template_service.Template') as mock_template_class:
            mock_template = Mock()
            mock_template.render.return_value = '<p>Hello, World</p>'
            mock_template_class.return_value = mock_template
            
            result = template_service.render_string(template_string, data)
            
            assert result == '<p>Hello, World</p>'
            mock_template_class.assert_called_once_with(template_string, autoescape=True)
            mock_template.render.assert_called_once_with(name='World')
    
    def test_registers_custom_filters(self, template_service):
        """Test custom filters are registered."""
        assert 'format_date' in template_service.env.filters
    
    def test_format_date_filter_works(self, template_service):
        """Test format_date custom filter."""
        from datetime import datetime
        
        filter_func = template_service.env.filters['format_date']
        test_date = datetime(2024, 3, 18, 10, 30, 0)
        
        result = filter_func(test_date, '%Y-%m-%d')
        
        assert result == '2024-03-18'
    
    def test_autoescape_enabled(self, template_service):
        """Test autoescape is enabled for security."""
        assert template_service.env.autoescape is True
