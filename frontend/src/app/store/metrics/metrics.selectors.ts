import { createFeatureSelector, createSelector } from '@ngrx/store';
import { MetricsState } from './metrics.state';

/**
 * Metrics Selectors
 * 
 * Selectors for accessing metrics state.
 */

/**
 * Feature selector for metrics state
 */
export const selectMetricsState = createFeatureSelector<MetricsState>('metrics');

/**
 * Select all metrics
 */
export const selectAllMetrics = createSelector(
  selectMetricsState,
  (state: MetricsState) => state.metrics
);

/**
 * Select time series data
 */
export const selectTimeSeriesData = createSelector(
  selectMetricsState,
  (state: MetricsState) => state.timeSeriesData
);

/**
 * Select aggregated metrics
 */
export const selectAggregatedMetrics = createSelector(
  selectMetricsState,
  (state: MetricsState) => state.aggregatedMetrics
);

/**
 * Select metrics summary
 */
export const selectMetricsSummary = createSelector(
  selectMetricsState,
  (state: MetricsState) => state.summary
);

/**
 * Select selected experiment ID
 */
export const selectSelectedExperimentId = createSelector(
  selectMetricsState,
  (state: MetricsState) => state.selectedExperimentId
);

/**
 * Select metrics loading state
 */
export const selectMetricsLoading = createSelector(
  selectMetricsState,
  (state: MetricsState) => state.loading
);

/**
 * Select metrics error
 */
export const selectMetricsError = createSelector(
  selectMetricsState,
  (state: MetricsState) => state.error
);

/**
 * Select metrics filters
 */
export const selectMetricsFilters = createSelector(
  selectMetricsState,
  (state: MetricsState) => state.filters
);

/**
 * Select real-time enabled status
 */
export const selectRealTimeEnabled = createSelector(
  selectMetricsState,
  (state: MetricsState) => state.realTimeEnabled
);

/**
 * Select time series data by metric name
 */
export const selectTimeSeriesByName = (metricName: string) => createSelector(
  selectTimeSeriesData,
  (timeSeriesData) => timeSeriesData[metricName]
);

/**
 * Select metrics by experiment ID
 */
export const selectMetricsByExperiment = (experimentId: string) => createSelector(
  selectAllMetrics,
  (metrics) => metrics.filter(m => m.experimentId === experimentId)
);

/**
 * Select metrics count
 */
export const selectMetricsCount = createSelector(
  selectAllMetrics,
  (metrics) => metrics.length
);
