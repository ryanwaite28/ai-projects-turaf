import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { TimeSeriesMetric, ChartConfig, ChartType } from '../../../models/metric.model';

/**
 * Metrics Chart Component
 * 
 * Displays time-series metrics data in various chart formats.
 * Note: This is a simplified implementation. In production, you would use
 * a charting library like Chart.js, D3.js, or ngx-charts.
 */
@Component({
  selector: 'app-metrics-chart',
  templateUrl: './metrics-chart.component.html',
  styleUrls: ['./metrics-chart.component.scss']
})
export class MetricsChartComponent implements OnChanges {
  
  @Input() data!: TimeSeriesMetric;
  @Input() config: ChartConfig = {
    type: ChartType.LINE,
    title: 'Metrics Chart',
    showLegend: true,
    showGrid: true,
    height: 300
  };
  
  chartData: any[] = [];
  maxValue: number = 0;
  minValue: number = 0;
  
  ngOnChanges(changes: SimpleChanges): void {
    if (changes['data'] && this.data) {
      this.processChartData();
    }
  }
  
  /**
   * Processes time series data for chart display
   */
  private processChartData(): void {
    if (!this.data || !this.data.dataPoints) {
      this.chartData = [];
      return;
    }
    
    this.chartData = this.data.dataPoints.map(point => ({
      timestamp: new Date(point.timestamp),
      value: point.value,
      label: this.formatTimestamp(point.timestamp)
    }));
    
    const values = this.chartData.map(d => d.value);
    this.maxValue = Math.max(...values);
    this.minValue = Math.min(...values);
  }
  
  /**
   * Formats timestamp for display
   */
  private formatTimestamp(timestamp: string): string {
    const date = new Date(timestamp);
    return date.toLocaleTimeString('en-US', { 
      hour: '2-digit', 
      minute: '2-digit' 
    });
  }
  
  /**
   * Gets Y position for data point (for simple line chart)
   */
  getYPosition(value: number): number {
    if (this.maxValue === this.minValue) {
      return 50;
    }
    const range = this.maxValue - this.minValue;
    const percentage = ((value - this.minValue) / range) * 100;
    return 100 - percentage;
  }
  
  /**
   * Gets X position for data point
   */
  getXPosition(index: number): number {
    if (this.chartData.length <= 1) {
      return 50;
    }
    return (index / (this.chartData.length - 1)) * 100;
  }
  
  /**
   * Generates SVG path for line chart
   */
  getLinePath(): string {
    if (!this.chartData || this.chartData.length === 0) {
      return '';
    }
    
    const points = this.chartData.map((point, index) => {
      const x = this.getXPosition(index);
      const y = this.getYPosition(point.value);
      return `${x},${y}`;
    });
    
    return `M ${points.join(' L ')}`;
  }
  
  /**
   * Gets color for chart based on metric type
   */
  getChartColor(): string {
    if (this.config.colors && this.config.colors.length > 0) {
      return this.config.colors[0];
    }
    return '#1976d2';
  }
  
  /**
   * Formats value for display
   */
  formatValue(value: number): string {
    if (this.data.unit) {
      return `${value.toFixed(2)} ${this.data.unit}`;
    }
    return value.toFixed(2);
  }
}
