import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { DashboardMetrics } from '../../../models/dashboard.model';

/**
 * Metrics Chart Widget Component
 * 
 * Displays dashboard metrics visualization.
 * 
 * Note: This is a placeholder implementation.
 * In production, integrate with a charting library like Chart.js or ngx-charts.
 */
@Component({
  selector: 'app-metrics-chart',
  templateUrl: './metrics-chart.component.html',
  styleUrls: ['./metrics-chart.component.scss']
})
export class MetricsChartComponent implements OnChanges {
  
  @Input() metrics: DashboardMetrics | null = null;
  
  selectedView: 'trends' | 'success' | 'distribution' = 'trends';
  
  ngOnChanges(changes: SimpleChanges): void {
    if (changes['metrics'] && this.metrics) {
      // In production, update chart data here
      this.updateChartData();
    }
  }
  
  /**
   * Updates chart data
   * 
   * Placeholder for chart library integration
   */
  private updateChartData(): void {
    // TODO: Integrate with charting library
    // Example: Chart.js, ngx-charts, or Highcharts
    console.log('Updating chart with metrics:', this.metrics);
  }
  
  /**
   * Changes the selected view
   */
  changeView(view: 'trends' | 'success' | 'distribution'): void {
    this.selectedView = view;
    this.updateChartData();
  }
  
  /**
   * Gets chart summary based on selected view
   */
  getChartSummary(): string {
    if (!this.metrics) {
      return 'No data available';
    }
    
    switch (this.selectedView) {
      case 'trends':
        return `${this.metrics.experimentTrends.length} data points`;
      case 'success':
        return `${this.metrics.successRateTrend.length} data points`;
      case 'distribution':
        return `${this.metrics.problemDistribution.length} problems`;
      default:
        return 'No data';
    }
  }
}
