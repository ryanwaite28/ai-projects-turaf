/**
 * Metric Models
 * 
 * Type definitions for metrics and measurement data structures.
 */

/**
 * Metric entity
 */
export interface Metric {
  id: string;
  experimentId: string;
  name: string;
  type: MetricType;
  value: number;
  unit?: string;
  timestamp: string;
  tags?: Record<string, string>;
  metadata?: Record<string, any>;
  organizationId: string;
  createdAt: string;
}

/**
 * Metric type enum
 */
export enum MetricType {
  COUNTER = 'COUNTER',
  GAUGE = 'GAUGE',
  HISTOGRAM = 'HISTOGRAM',
  TIMER = 'TIMER',
  RATE = 'RATE',
  PERCENTAGE = 'PERCENTAGE',
  CUSTOM = 'CUSTOM'
}

/**
 * Aggregated metric data
 */
export interface AggregatedMetric {
  name: string;
  type: MetricType;
  aggregation: AggregationType;
  value: number;
  unit?: string;
  count: number;
  min?: number;
  max?: number;
  avg?: number;
  sum?: number;
  stdDev?: number;
  percentiles?: Record<string, number>;
  timeRange: TimeRange;
}

/**
 * Aggregation type enum
 */
export enum AggregationType {
  SUM = 'SUM',
  AVG = 'AVG',
  MIN = 'MIN',
  MAX = 'MAX',
  COUNT = 'COUNT',
  PERCENTILE = 'PERCENTILE',
  STDDEV = 'STDDEV'
}

/**
 * Time range for metrics
 */
export interface TimeRange {
  start: string;
  end: string;
}

/**
 * Time-series metric data point
 */
export interface MetricDataPoint {
  timestamp: string;
  value: number;
  tags?: Record<string, string>;
}

/**
 * Time-series metric
 */
export interface TimeSeriesMetric {
  name: string;
  type: MetricType;
  unit?: string;
  dataPoints: MetricDataPoint[];
  aggregation?: AggregationType;
}

/**
 * Create metric request
 */
export interface CreateMetricRequest {
  experimentId: string;
  name: string;
  type: MetricType;
  value: number;
  unit?: string;
  timestamp?: string;
  tags?: Record<string, string>;
  metadata?: Record<string, any>;
}

/**
 * Batch create metrics request
 */
export interface BatchCreateMetricsRequest {
  metrics: CreateMetricRequest[];
}

/**
 * Metric query params
 */
export interface MetricQueryParams {
  experimentId?: string;
  name?: string;
  type?: MetricType;
  startTime?: string;
  endTime?: string;
  aggregation?: AggregationType;
  interval?: string;
  tags?: Record<string, string>;
  limit?: number;
  page?: number;
}

/**
 * Paginated metrics response
 */
export interface PaginatedMetricsResponse {
  metrics: Metric[];
  total: number;
  page: number;
  limit: number;
  totalPages: number;
}

/**
 * Metrics summary
 */
export interface MetricsSummary {
  experimentId: string;
  totalMetrics: number;
  metricsByType: Record<MetricType, number>;
  timeRange: TimeRange;
  topMetrics: AggregatedMetric[];
}

/**
 * Chart configuration
 */
export interface ChartConfig {
  type: ChartType;
  title: string;
  xAxisLabel?: string;
  yAxisLabel?: string;
  showLegend?: boolean;
  showGrid?: boolean;
  colors?: string[];
  height?: number;
}

/**
 * Chart type enum
 */
export enum ChartType {
  LINE = 'LINE',
  BAR = 'BAR',
  AREA = 'AREA',
  SCATTER = 'SCATTER',
  PIE = 'PIE',
  GAUGE = 'GAUGE'
}

/**
 * Chart data series
 */
export interface ChartSeries {
  name: string;
  data: { x: any; y: number }[];
  color?: string;
}

/**
 * Real-time metric update
 */
export interface MetricUpdate {
  experimentId: string;
  metric: Metric;
  action: 'created' | 'updated';
}
