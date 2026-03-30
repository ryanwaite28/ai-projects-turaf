import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Store } from '@ngrx/store';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { AppState } from '../../../store/app.state';
import {
  loadMetrics,
  loadTimeSeriesData,
  loadAggregatedMetrics,
  loadMetricsSummary,
  setSelectedExperiment,
  toggleRealTimeMetrics
} from '../../../store/metrics/metrics.actions';
import {
  selectAllMetrics,
  selectTimeSeriesData,
  selectAggregatedMetrics,
  selectMetricsSummary,
  selectMetricsLoading,
  selectMetricsError,
  selectRealTimeEnabled
} from '../../../store/metrics/metrics.selectors';
import { ChartConfig, ChartType } from '../../../models/metric.model';

/**
 * Metrics Dashboard Component
 * 
 * Main dashboard for viewing experiment metrics with charts and tables.
 */
@Component({
  selector: 'app-metrics-dashboard',
  templateUrl: './metrics-dashboard.component.html',
  styleUrls: ['./metrics-dashboard.component.scss']
})
export class MetricsDashboardComponent implements OnInit, OnDestroy {
  
  metrics$ = this.store.select(selectAllMetrics);
  timeSeriesData$ = this.store.select(selectTimeSeriesData);
  aggregatedMetrics$ = this.store.select(selectAggregatedMetrics);
  summary$ = this.store.select(selectMetricsSummary);
  loading$ = this.store.select(selectMetricsLoading);
  error$ = this.store.select(selectMetricsError);
  realTimeEnabled$ = this.store.select(selectRealTimeEnabled);
  
  experimentId: string | null = null;
  selectedMetricName: string | null = null;
  showAggregated: boolean = false;
  
  chartConfig: ChartConfig = {
    type: ChartType.LINE,
    title: 'Metric Trends',
    showLegend: true,
    showGrid: true,
    height: 300
  };
  
  private destroy$ = new Subject<void>();
  
  constructor(
    private store: Store<AppState>,
    private route: ActivatedRoute
  ) {}
  
  ngOnInit(): void {
    this.route.queryParams
      .pipe(takeUntil(this.destroy$))
      .subscribe(params => {
        this.experimentId = params['experimentId'] || null;
        
        if (this.experimentId) {
          this.store.dispatch(setSelectedExperiment({ experimentId: this.experimentId }));
          this.loadMetricsData();
        }
      });
  }
  
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
  
  /**
   * Loads all metrics data for the experiment
   */
  loadMetricsData(): void {
    if (!this.experimentId) return;
    
    this.store.dispatch(loadMetrics({ 
      params: { experimentId: this.experimentId } 
    }));
    
    this.store.dispatch(loadAggregatedMetrics({ 
      experimentId: this.experimentId 
    }));
    
    this.store.dispatch(loadMetricsSummary({ 
      experimentId: this.experimentId 
    }));
  }
  
  /**
   * Loads time series data for a specific metric
   */
  loadMetricTimeSeries(metricName: string): void {
    if (!this.experimentId) return;
    
    this.selectedMetricName = metricName;
    this.store.dispatch(loadTimeSeriesData({
      experimentId: this.experimentId,
      metricName
    }));
  }
  
  /**
   * Toggles between raw and aggregated metrics view
   */
  toggleAggregatedView(): void {
    this.showAggregated = !this.showAggregated;
  }
  
  /**
   * Toggles real-time metric updates
   */
  toggleRealTime(enabled: boolean): void {
    this.store.dispatch(toggleRealTimeMetrics({ enabled }));
  }
  
  /**
   * Refreshes metrics data
   */
  refresh(): void {
    this.loadMetricsData();
  }
  
  /**
   * Handles metric selection from table
   */
  onMetricSelected(metricName: string): void {
    this.loadMetricTimeSeries(metricName);
  }
  
  /**
   * Gets metric types from summary for iteration
   */
  getMetricTypes(summary: any): string[] {
    if (!summary || !summary.metricsByType) return [];
    return Object.keys(summary.metricsByType);
  }
}
