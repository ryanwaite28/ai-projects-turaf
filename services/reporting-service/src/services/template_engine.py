"""
Template engine for rendering report templates.

This module provides services for rendering HTML templates using Jinja2,
following Clean Architecture principles.
"""

import os
import logging
from typing import Dict, Any
from jinja2 import Environment, FileSystemLoader, select_autoescape, TemplateNotFound
from models.aggregated_data import AggregatedReportData

logger = logging.getLogger(__name__)


class TemplateEngine:
    """
    Service for rendering report templates using Jinja2.
    
    This service follows the Single Responsibility Principle (SOLID),
    focusing solely on template rendering. It acts as an adapter between
    the application and the Jinja2 templating library.
    
    Features:
    - Automatic HTML escaping for security
    - Configurable template directory
    - Context preparation from domain models
    - Error handling for missing templates
    
    Attributes:
        env: Jinja2 Environment instance
        template_dir: Directory containing template files
    """
    
    def __init__(self, template_dir: str = None):
        """
        Initialize template engine with Jinja2 environment.
        
        Args:
            template_dir: Optional directory containing templates.
                         Defaults to src/templates relative to this file.
                         
        Example:
            >>> engine = TemplateEngine()
            >>> # Uses default templates directory
            
            >>> engine = TemplateEngine('/custom/templates')
            >>> # Uses custom directory
        """
        if template_dir is None:
            # Default to templates directory relative to this file
            # This file is in src/services/, templates are in src/templates/
            current_dir = os.path.dirname(os.path.abspath(__file__))
            template_dir = os.path.join(os.path.dirname(current_dir), 'templates')
        
        self.template_dir = template_dir
        
        # Initialize Jinja2 environment with security features
        self.env = Environment(
            loader=FileSystemLoader(template_dir),
            autoescape=select_autoescape(['html', 'xml']),
            trim_blocks=True,
            lstrip_blocks=True
        )
        
        logger.info(f"TemplateEngine initialized with directory: {template_dir}")
    
    def render_report(self, data: AggregatedReportData) -> str:
        """
        Render experiment report from template.
        
        This method transforms aggregated domain data into an HTML report
        by rendering the experiment-report.html template with the provided
        data context.
        
        Args:
            data: Aggregated report data containing all information
                 needed for the report
                 
        Returns:
            Rendered HTML string ready for PDF conversion
            
        Raises:
            TemplateNotFound: If the template file doesn't exist
            ValueError: If required data is missing
            
        Example:
            >>> engine = TemplateEngine()
            >>> aggregated_data = AggregatedReportData(...)
            >>> html = engine.render_report(aggregated_data)
            >>> len(html) > 0
            True
        """
        if not data:
            raise ValueError("data is required for rendering")
        
        experiment_id = data.experiment.get('id', 'unknown')
        logger.info(f"Rendering report for experiment {experiment_id}")
        
        try:
            # Load the experiment report template
            template = self.env.get_template('experiment-report.html')
            
            # Prepare template context from aggregated data
            context = self._prepare_context(data)
            
            # Render template with context
            html = template.render(**context)
            
            logger.debug(
                f"Successfully rendered report for experiment {experiment_id}: "
                f"{len(html)} characters"
            )
            
            return html
            
        except TemplateNotFound as e:
            logger.error(f"Template not found: {str(e)}")
            raise
            
        except Exception as e:
            logger.error(
                f"Error rendering report for experiment {experiment_id}: {str(e)}",
                exc_info=True
            )
            raise
    
    def _prepare_context(self, data: AggregatedReportData) -> Dict[str, Any]:
        """
        Prepare template context from aggregated data.
        
        This method transforms the domain model into a dictionary
        suitable for template rendering, ensuring all required fields
        are present and properly formatted.
        
        Args:
            data: Aggregated report data
            
        Returns:
            Dictionary with template context variables
            
        Example:
            >>> engine = TemplateEngine()
            >>> data = AggregatedReportData(...)
            >>> context = engine._prepare_context(data)
            >>> 'experiment' in context
            True
        """
        context = {
            'problem': data.problem,
            'hypothesis': data.hypothesis,
            'experiment': data.experiment,
            'duration': data.get_duration_string(),
            'metricSummaries': data.metric_summaries,
            'insights': data.insights
        }
        
        logger.debug(
            f"Prepared context with {len(data.metric_summaries)} metrics "
            f"and {len(data.insights)} insights"
        )
        
        return context
    
    def render_custom_template(
        self,
        template_name: str,
        context: Dict[str, Any]
    ) -> str:
        """
        Render a custom template with provided context.
        
        This method allows rendering any template in the templates directory
        with a custom context, providing flexibility for different report types.
        
        Args:
            template_name: Name of the template file (e.g., 'custom-report.html')
            context: Dictionary with template variables
            
        Returns:
            Rendered HTML string
            
        Raises:
            TemplateNotFound: If the template file doesn't exist
            
        Example:
            >>> engine = TemplateEngine()
            >>> html = engine.render_custom_template(
            ...     'summary.html',
            ...     {'title': 'Summary', 'data': [1, 2, 3]}
            ... )
        """
        logger.info(f"Rendering custom template: {template_name}")
        
        try:
            template = self.env.get_template(template_name)
            html = template.render(**context)
            
            logger.debug(f"Successfully rendered {template_name}: {len(html)} characters")
            
            return html
            
        except TemplateNotFound as e:
            logger.error(f"Template not found: {template_name}")
            raise
            
        except Exception as e:
            logger.error(f"Error rendering template {template_name}: {str(e)}")
            raise
