"""
Unit tests for TemplateEngine.

Tests cover template rendering, context preparation, and error handling.
"""

import pytest
import os
from datetime import timedelta
from jinja2 import TemplateNotFound
from src.services.template_engine import TemplateEngine
from src.models.aggregated_data import AggregatedReportData, MetricSummary


class TestTemplateEngine:
    """Test suite for TemplateEngine."""
    
    @pytest.fixture
    def template_dir(self, tmp_path):
        """Create temporary template directory for tests."""
        template_dir = tmp_path / "templates"
        template_dir.mkdir()
        return str(template_dir)
    
    @pytest.fixture
    def simple_template(self, template_dir):
        """Create a simple test template."""
        template_path = os.path.join(template_dir, 'test.html')
        with open(template_path, 'w') as f:
            f.write('<h1>{{ title }}</h1><p>{{ content }}</p>')
        return template_path
    
    @pytest.fixture
    def sample_aggregated_data(self):
        """Create sample aggregated data for testing."""
        metric_summary = MetricSummary(
            name='latency',
            count=100,
            average=125.5,
            min_value=50.0,
            max_value=200.0,
            trend='STABLE'
        )
        
        return AggregatedReportData(
            experiment={
                'id': 'exp-123',
                'name': 'Performance Test',
                'startedAt': '2024-01-01T00:00:00Z',
                'completedAt': '2024-01-01T02:30:00Z',
                'status': 'COMPLETED',
                'result': 'SUCCESS'
            },
            hypothesis={
                'id': 'hyp-456',
                'statement': 'Caching will improve response time',
                'expectedOutcome': 'Reduce latency by 50%',
                'criteria': 'Average latency < 100ms'
            },
            problem={
                'id': 'prob-789',
                'title': 'High Response Time',
                'description': 'API responses are too slow',
                'context': 'User complaints about performance'
            },
            duration=timedelta(hours=2, minutes=30),
            metric_summaries={'latency': metric_summary},
            insights=[
                'Average latency: 125.50 (min: 50.00, max: 200.00)',
                'Experiment result: SUCCESS'
            ]
        )
    
    def test_engine_initialization_with_default_directory(self):
        """Test engine initialization with default template directory."""
        # When
        engine = TemplateEngine()
        
        # Then
        assert engine is not None
        assert engine.env is not None
        assert 'templates' in engine.template_dir
    
    def test_engine_initialization_with_custom_directory(self, template_dir):
        """Test engine initialization with custom template directory."""
        # When
        engine = TemplateEngine(template_dir=template_dir)
        
        # Then
        assert engine.template_dir == template_dir
    
    def test_render_custom_template_success(self, template_dir, simple_template):
        """Test rendering a custom template successfully."""
        # Given
        engine = TemplateEngine(template_dir=template_dir)
        context = {
            'title': 'Test Report',
            'content': 'This is test content'
        }
        
        # When
        html = engine.render_custom_template('test.html', context)
        
        # Then
        assert '<h1>Test Report</h1>' in html
        assert '<p>This is test content</p>' in html
    
    def test_render_custom_template_raises_error_for_missing_template(self, template_dir):
        """Test that rendering raises error for missing template."""
        # Given
        engine = TemplateEngine(template_dir=template_dir)
        
        # When/Then
        with pytest.raises(TemplateNotFound):
            engine.render_custom_template('nonexistent.html', {})
    
    def test_render_report_raises_error_for_missing_data(self, template_dir):
        """Test that render_report raises error when data is None."""
        # Given
        engine = TemplateEngine(template_dir=template_dir)
        
        # When/Then
        with pytest.raises(ValueError) as exc_info:
            engine.render_report(None)
        assert 'data is required' in str(exc_info.value)
    
    def test_prepare_context(self, template_dir, sample_aggregated_data):
        """Test context preparation from aggregated data."""
        # Given
        engine = TemplateEngine(template_dir=template_dir)
        
        # When
        context = engine._prepare_context(sample_aggregated_data)
        
        # Then
        assert 'problem' in context
        assert 'hypothesis' in context
        assert 'experiment' in context
        assert 'duration' in context
        assert 'metricSummaries' in context
        assert 'insights' in context
        
        assert context['duration'] == '2 hours, 30 minutes'
        assert len(context['metricSummaries']) == 1
        assert len(context['insights']) == 2
    
    def test_prepare_context_includes_all_experiment_fields(
        self,
        template_dir,
        sample_aggregated_data
    ):
        """Test that context includes all experiment fields."""
        # Given
        engine = TemplateEngine(template_dir=template_dir)
        
        # When
        context = engine._prepare_context(sample_aggregated_data)
        
        # Then
        assert context['experiment']['id'] == 'exp-123'
        assert context['experiment']['name'] == 'Performance Test'
        assert context['experiment']['status'] == 'COMPLETED'
        assert context['experiment']['result'] == 'SUCCESS'
    
    def test_prepare_context_includes_hypothesis_fields(
        self,
        template_dir,
        sample_aggregated_data
    ):
        """Test that context includes hypothesis fields."""
        # Given
        engine = TemplateEngine(template_dir=template_dir)
        
        # When
        context = engine._prepare_context(sample_aggregated_data)
        
        # Then
        assert context['hypothesis']['statement'] == 'Caching will improve response time'
        assert context['hypothesis']['expectedOutcome'] == 'Reduce latency by 50%'
        assert context['hypothesis']['criteria'] == 'Average latency < 100ms'
    
    def test_prepare_context_includes_problem_fields(
        self,
        template_dir,
        sample_aggregated_data
    ):
        """Test that context includes problem fields."""
        # Given
        engine = TemplateEngine(template_dir=template_dir)
        
        # When
        context = engine._prepare_context(sample_aggregated_data)
        
        # Then
        assert context['problem']['title'] == 'High Response Time'
        assert context['problem']['description'] == 'API responses are too slow'
        assert context['problem']['context'] == 'User complaints about performance'
    
    def test_prepare_context_includes_metric_summaries(
        self,
        template_dir,
        sample_aggregated_data
    ):
        """Test that context includes metric summaries."""
        # Given
        engine = TemplateEngine(template_dir=template_dir)
        
        # When
        context = engine._prepare_context(sample_aggregated_data)
        
        # Then
        assert 'latency' in context['metricSummaries']
        summary = context['metricSummaries']['latency']
        assert summary.name == 'latency'
        assert summary.count == 100
        assert summary.average == 125.5
    
    def test_prepare_context_includes_insights(
        self,
        template_dir,
        sample_aggregated_data
    ):
        """Test that context includes insights."""
        # Given
        engine = TemplateEngine(template_dir=template_dir)
        
        # When
        context = engine._prepare_context(sample_aggregated_data)
        
        # Then
        assert len(context['insights']) == 2
        assert 'Average latency' in context['insights'][0]
        assert 'SUCCESS' in context['insights'][1]
    
    def test_render_custom_template_escapes_html(self, template_dir):
        """Test that HTML is properly escaped for security."""
        # Given
        template_path = os.path.join(template_dir, 'escape-test.html')
        with open(template_path, 'w') as f:
            f.write('<p>{{ content }}</p>')
        
        engine = TemplateEngine(template_dir=template_dir)
        context = {'content': '<script>alert("XSS")</script>'}
        
        # When
        html = engine.render_custom_template('escape-test.html', context)
        
        # Then
        assert '<script>' not in html
        assert '&lt;script&gt;' in html
    
    def test_render_custom_template_logs_activity(
        self,
        template_dir,
        simple_template,
        caplog
    ):
        """Test that template rendering logs activity."""
        # Given
        engine = TemplateEngine(template_dir=template_dir)
        
        # When
        with caplog.at_level('INFO'):
            engine.render_custom_template('test.html', {'title': 'Test', 'content': 'Test'})
        
        # Then
        assert 'Rendering custom template: test.html' in caplog.text
        assert 'Successfully rendered test.html' in caplog.text
    
    def test_engine_uses_autoescape(self, template_dir):
        """Test that engine has autoescape enabled."""
        # Given
        engine = TemplateEngine(template_dir=template_dir)
        
        # Then
        assert engine.env.autoescape is True
    
    def test_render_custom_template_with_empty_context(self, template_dir):
        """Test rendering template with empty context."""
        # Given
        template_path = os.path.join(template_dir, 'empty.html')
        with open(template_path, 'w') as f:
            f.write('<h1>Static Content</h1>')
        
        engine = TemplateEngine(template_dir=template_dir)
        
        # When
        html = engine.render_custom_template('empty.html', {})
        
        # Then
        assert '<h1>Static Content</h1>' in html
