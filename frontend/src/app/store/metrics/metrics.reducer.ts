import { createReducer, on } from '@ngrx/store';
import { MetricsState, initialMetricsState } from './metrics.state';
import * as MetricsActions from './metrics.actions';

/**
 * Metrics Reducer
 * 
 * Handles metrics state updates.
 */
export const metricsReducer = createReducer(
  initialMetricsState,
  
  // Load Metrics
  on(MetricsActions.loadMetrics, (state): MetricsState => ({
    ...state,
    loading: true,
    error: null
  })),
  
  on(MetricsActions.loadMetricsSuccess, (state, { response }): MetricsState => ({
    ...state,
    metrics: response.metrics,
    loading: false,
    error: null
  })),
  
  on(MetricsActions.loadMetricsFailure, (state, { error }): MetricsState => ({
    ...state,
    loading: false,
    error
  })),
  
  // Load Time Series Data
  on(MetricsActions.loadTimeSeriesData, (state): MetricsState => ({
    ...state,
    loading: true,
    error: null
  })),
  
  on(MetricsActions.loadTimeSeriesDataSuccess, (state, { metricName, data }): MetricsState => ({
    ...state,
    timeSeriesData: {
      ...state.timeSeriesData,
      [metricName]: data
    },
    loading: false,
    error: null
  })),
  
  on(MetricsActions.loadTimeSeriesDataFailure, (state, { error }): MetricsState => ({
    ...state,
    loading: false,
    error
  })),
  
  // Load Aggregated Metrics
  on(MetricsActions.loadAggregatedMetrics, (state): MetricsState => ({
    ...state,
    loading: true,
    error: null
  })),
  
  on(MetricsActions.loadAggregatedMetricsSuccess, (state, { metrics }): MetricsState => ({
    ...state,
    aggregatedMetrics: metrics,
    loading: false,
    error: null
  })),
  
  on(MetricsActions.loadAggregatedMetricsFailure, (state, { error }): MetricsState => ({
    ...state,
    loading: false,
    error
  })),
  
  // Load Metrics Summary
  on(MetricsActions.loadMetricsSummary, (state): MetricsState => ({
    ...state,
    loading: true,
    error: null
  })),
  
  on(MetricsActions.loadMetricsSummarySuccess, (state, { summary }): MetricsState => ({
    ...state,
    summary,
    loading: false,
    error: null
  })),
  
  on(MetricsActions.loadMetricsSummaryFailure, (state, { error }): MetricsState => ({
    ...state,
    loading: false,
    error
  })),
  
  // Create Metric
  on(MetricsActions.createMetric, (state): MetricsState => ({
    ...state,
    loading: true,
    error: null
  })),
  
  on(MetricsActions.createMetricSuccess, (state, { metric }): MetricsState => ({
    ...state,
    metrics: [metric, ...state.metrics],
    loading: false,
    error: null
  })),
  
  on(MetricsActions.createMetricFailure, (state, { error }): MetricsState => ({
    ...state,
    loading: false,
    error
  })),
  
  // Batch Create Metrics
  on(MetricsActions.batchCreateMetrics, (state): MetricsState => ({
    ...state,
    loading: true,
    error: null
  })),
  
  on(MetricsActions.batchCreateMetricsSuccess, (state, { metrics }): MetricsState => ({
    ...state,
    metrics: [...metrics, ...state.metrics],
    loading: false,
    error: null
  })),
  
  on(MetricsActions.batchCreateMetricsFailure, (state, { error }): MetricsState => ({
    ...state,
    loading: false,
    error
  })),
  
  // Set Selected Experiment
  on(MetricsActions.setSelectedExperiment, (state, { experimentId }): MetricsState => ({
    ...state,
    selectedExperimentId: experimentId
  })),
  
  // Set Filters
  on(MetricsActions.setMetricsFilters, (state, { filters }): MetricsState => ({
    ...state,
    filters: { ...state.filters, ...filters }
  })),
  
  // Toggle Real-Time Updates
  on(MetricsActions.toggleRealTimeMetrics, (state, { enabled }): MetricsState => ({
    ...state,
    realTimeEnabled: enabled
  })),
  
  // Real-Time Metric Update
  on(MetricsActions.metricUpdateReceived, (state, { update }): MetricsState => {
    if (update.action === 'created') {
      return {
        ...state,
        metrics: [update.metric, ...state.metrics]
      };
    }
    return state;
  }),
  
  // Clear Metrics
  on(MetricsActions.clearMetrics, (): MetricsState => ({
    ...initialMetricsState
  })),
  
  // Clear Error
  on(MetricsActions.clearMetricsError, (state): MetricsState => ({
    ...state,
    error: null
  }))
);
