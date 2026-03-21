import { Metric, TimeSeriesMetric, AggregatedMetric, MetricsSummary } from '../../models/metric.model';

/**
 * Metrics State
 * 
 * Manages metrics data in NgRx store.
 */
export interface MetricsState {
  metrics: Metric[];
  timeSeriesData: Record<string, TimeSeriesMetric>;
  aggregatedMetrics: AggregatedMetric[];
  summary: MetricsSummary | null;
  selectedExperimentId: string | null;
  loading: boolean;
  error: string | null;
  filters: {
    experimentId?: string;
    metricName?: string;
    startTime?: string;
    endTime?: string;
  };
  realTimeEnabled: boolean;
}

/**
 * Initial metrics state
 */
export const initialMetricsState: MetricsState = {
  metrics: [],
  timeSeriesData: {},
  aggregatedMetrics: [],
  summary: null,
  selectedExperimentId: null,
  loading: false,
  error: null,
  filters: {},
  realTimeEnabled: false
};
