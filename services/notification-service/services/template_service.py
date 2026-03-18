"""
Template rendering service using Jinja2.
Renders email templates with provided data.
"""
import os
import logging
from typing import Dict, Any
from jinja2 import Environment, FileSystemLoader, Template, TemplateNotFound

logger = logging.getLogger(__name__)


class TemplateService:
    """
    Service for rendering email templates using Jinja2.
    Supports template inheritance and custom filters.
    """
    
    def __init__(self, template_dir: str = 'templates'):
        """
        Initialize template service.
        
        Args:
            template_dir: Directory containing email templates
        """
        self.template_dir = template_dir
        
        # Create Jinja2 environment
        self.env = Environment(
            loader=FileSystemLoader(template_dir),
            autoescape=True,  # Auto-escape HTML for security
            trim_blocks=True,
            lstrip_blocks=True
        )
        
        # Add custom filters if needed
        self._register_filters()
    
    def _register_filters(self) -> None:
        """Register custom Jinja2 filters."""
        # Example: Add date formatting filter
        def format_date(value, format='%Y-%m-%d'):
            """Format datetime to string."""
            if hasattr(value, 'strftime'):
                return value.strftime(format)
            return value
        
        self.env.filters['format_date'] = format_date
    
    def render(self, template_name: str, data: Dict[str, Any]) -> str:
        """
        Render template with provided data.
        
        Args:
            template_name: Name of template file (without .html extension)
            data: Dictionary of template variables
            
        Returns:
            Rendered HTML string
            
        Raises:
            TemplateNotFound: If template file doesn't exist
            Exception: For template rendering errors
        """
        try:
            # Add .html extension if not present
            if not template_name.endswith('.html'):
                template_name = f"{template_name}.html"
            
            # Load and render template
            template = self.env.get_template(template_name)
            rendered = template.render(**data)
            
            logger.debug(
                'Template rendered successfully',
                extra={'template': template_name}
            )
            
            return rendered
            
        except TemplateNotFound as e:
            logger.error(
                'Template not found',
                extra={'template': template_name},
                exc_info=True
            )
            raise
        
        except Exception as e:
            logger.error(
                'Error rendering template',
                extra={'template': template_name, 'error': str(e)},
                exc_info=True
            )
            raise
    
    def render_string(self, template_string: str, data: Dict[str, Any]) -> str:
        """
        Render template from string.
        
        Args:
            template_string: Template content as string
            data: Dictionary of template variables
            
        Returns:
            Rendered HTML string
        """
        try:
            template = Template(template_string, autoescape=True)
            return template.render(**data)
        except Exception as e:
            logger.error(
                'Error rendering template string',
                extra={'error': str(e)},
                exc_info=True
            )
            raise
