import { createAction, props } from '@ngrx/store';
import { 
  Metric, 
  CreateMetricRequest,
  BatchCreateMetricsRequest,
  MetricQueryParams,
  PaginatedMetricsResponse,
  TimeSeriesMetric,
  AggregatedMetric,
  MetricsSummary,
  MetricUpdate
} from '../../models/metric.model';

/**
 * Metrics Actions
 * 
 * Actions for managing metrics state.
 */

/**
 * Load Metrics
 */
export const loadMetrics = createAction(
  '[Metrics] Load Metrics',
  props<{ params?: MetricQueryParams }>()
);

export const loadMetricsSuccess = createAction(
  '[Metrics] Load Metrics Success',
  props<{ response: PaginatedMetricsResponse }>()
);

export const loadMetricsFailure = createAction(
  '[Metrics] Load Metrics Failure',
  props<{ error: string }>()
);

/**
 * Load Time Series Data
 */
export const loadTimeSeriesData = createAction(
  '[Metrics] Load Time Series Data',
  props<{ experimentId: string; metricName: string; params?: MetricQueryParams }>()
);

export const loadTimeSeriesDataSuccess = createAction(
  '[Metrics] Load Time Series Data Success',
  props<{ metricName: string; data: TimeSeriesMetric }>()
);

export const loadTimeSeriesDataFailure = createAction(
  '[Metrics] Load Time Series Data Failure',
  props<{ error: string }>()
);

/**
 * Load Aggregated Metrics
 */
export const loadAggregatedMetrics = createAction(
  '[Metrics] Load Aggregated Metrics',
  props<{ experimentId: string; params?: MetricQueryParams }>()
);

export const loadAggregatedMetricsSuccess = createAction(
  '[Metrics] Load Aggregated Metrics Success',
  props<{ metrics: AggregatedMetric[] }>()
);

export const loadAggregatedMetricsFailure = createAction(
  '[Metrics] Load Aggregated Metrics Failure',
  props<{ error: string }>()
);

/**
 * Load Metrics Summary
 */
export const loadMetricsSummary = createAction(
  '[Metrics] Load Metrics Summary',
  props<{ experimentId: string }>()
);

export const loadMetricsSummarySuccess = createAction(
  '[Metrics] Load Metrics Summary Success',
  props<{ summary: MetricsSummary }>()
);

export const loadMetricsSummaryFailure = createAction(
  '[Metrics] Load Metrics Summary Failure',
  props<{ error: string }>()
);

/**
 * Create Metric
 */
export const createMetric = createAction(
  '[Metrics] Create Metric',
  props<{ request: CreateMetricRequest }>()
);

export const createMetricSuccess = createAction(
  '[Metrics] Create Metric Success',
  props<{ metric: Metric }>()
);

export const createMetricFailure = createAction(
  '[Metrics] Create Metric Failure',
  props<{ error: string }>()
);

/**
 * Batch Create Metrics
 */
export const batchCreateMetrics = createAction(
  '[Metrics] Batch Create Metrics',
  props<{ request: BatchCreateMetricsRequest }>()
);

export const batchCreateMetricsSuccess = createAction(
  '[Metrics] Batch Create Metrics Success',
  props<{ metrics: Metric[] }>()
);

export const batchCreateMetricsFailure = createAction(
  '[Metrics] Batch Create Metrics Failure',
  props<{ error: string }>()
);

/**
 * Set Selected Experiment
 */
export const setSelectedExperiment = createAction(
  '[Metrics] Set Selected Experiment',
  props<{ experimentId: string | null }>()
);

/**
 * Set Filters
 */
export const setMetricsFilters = createAction(
  '[Metrics] Set Filters',
  props<{ filters: MetricQueryParams }>()
);

/**
 * Toggle Real-Time Updates
 */
export const toggleRealTimeMetrics = createAction(
  '[Metrics] Toggle Real-Time Updates',
  props<{ enabled: boolean }>()
);

/**
 * Real-Time Metric Update
 */
export const metricUpdateReceived = createAction(
  '[Metrics] Metric Update Received',
  props<{ update: MetricUpdate }>()
);

/**
 * Clear Metrics
 */
export const clearMetrics = createAction(
  '[Metrics] Clear Metrics'
);

/**
 * Clear Metrics Error
 */
export const clearMetricsError = createAction(
  '[Metrics] Clear Error'
);
