import { Component, Input, Output, EventEmitter } from '@angular/core';
import { Metric, AggregatedMetric } from '../../../models/metric.model';

/**
 * Metrics Table Component
 * 
 * Displays metrics data in a tabular format with sorting and filtering.
 */
@Component({
  selector: 'app-metrics-table',
  templateUrl: './metrics-table.component.html',
  styleUrls: ['./metrics-table.component.scss']
})
export class MetricsTableComponent {
  
  @Input() metrics: Metric[] = [];
  @Input() aggregatedMetrics: AggregatedMetric[] = [];
  @Input() showAggregated: boolean = false;
  @Input() loading: boolean = false;
  
  @Output() metricSelected = new EventEmitter<Metric>();
  @Output() metricDeleted = new EventEmitter<string>();
  
  displayedColumns: string[] = ['name', 'type', 'value', 'timestamp', 'actions'];
  aggregatedColumns: string[] = ['name', 'type', 'aggregation', 'value', 'count', 'stats'];
  
  /**
   * Handles metric row click
   */
  onMetricClick(metric: Metric): void {
    this.metricSelected.emit(metric);
  }
  
  /**
   * Handles metric deletion
   */
  onDeleteMetric(metric: Metric, event: Event): void {
    event.stopPropagation();
    if (confirm(`Delete metric "${metric.name}"?`)) {
      this.metricDeleted.emit(metric.id);
    }
  }
  
  /**
   * Formats metric value with unit
   */
  formatValue(value: number, unit?: string): string {
    const formattedValue = value.toFixed(2);
    return unit ? `${formattedValue} ${unit}` : formattedValue;
  }
  
  /**
   * Gets type badge class
   */
  getTypeBadgeClass(type: string): string {
    const typeMap: Record<string, string> = {
      'COUNTER': 'counter',
      'GAUGE': 'gauge',
      'HISTOGRAM': 'histogram',
      'TIMER': 'timer',
      'RATE': 'rate',
      'PERCENTAGE': 'percentage',
      'CUSTOM': 'custom'
    };
    return typeMap[type] || 'custom';
  }
  
  /**
   * Gets aggregation badge class
   */
  getAggregationBadgeClass(aggregation: string): string {
    const aggMap: Record<string, string> = {
      'SUM': 'sum',
      'AVG': 'avg',
      'MIN': 'min',
      'MAX': 'max',
      'COUNT': 'count',
      'PERCENTILE': 'percentile',
      'STDDEV': 'stddev'
    };
    return aggMap[aggregation] || 'avg';
  }
  
  /**
   * Formats stats for aggregated metrics
   */
  formatStats(metric: AggregatedMetric): string {
    const stats: string[] = [];
    if (metric.min !== undefined) stats.push(`Min: ${metric.min.toFixed(2)}`);
    if (metric.max !== undefined) stats.push(`Max: ${metric.max.toFixed(2)}`);
    if (metric.avg !== undefined) stats.push(`Avg: ${metric.avg.toFixed(2)}`);
    if (metric.stdDev !== undefined) stats.push(`StdDev: ${metric.stdDev.toFixed(2)}`);
    return stats.join(', ');
  }
}
