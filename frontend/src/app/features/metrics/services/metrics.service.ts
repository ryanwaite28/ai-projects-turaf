import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { 
  Metric,
  CreateMetricRequest,
  BatchCreateMetricsRequest,
  MetricQueryParams,
  PaginatedMetricsResponse,
  TimeSeriesMetric,
  AggregatedMetric,
  MetricsSummary
} from '../../../models/metric.model';
import { environment } from '../../../../environments/environment';

/**
 * Metrics Service
 * 
 * Handles metric-related API calls.
 */
@Injectable({
  providedIn: 'root'
})
export class MetricsService {
  
  private readonly apiUrl = `${environment.apiUrl}/metrics`;
  
  constructor(private http: HttpClient) {}
  
  /**
   * Fetches paginated list of metrics
   * 
   * @param params Query parameters for filtering
   * @returns Observable<PaginatedMetricsResponse> Paginated metrics
   */
  getMetrics(params?: MetricQueryParams): Observable<PaginatedMetricsResponse> {
    let httpParams = new HttpParams();
    
    if (params) {
      if (params.experimentId) httpParams = httpParams.set('experimentId', params.experimentId);
      if (params.name) httpParams = httpParams.set('name', params.name);
      if (params.type) httpParams = httpParams.set('type', params.type);
      if (params.startTime) httpParams = httpParams.set('startTime', params.startTime);
      if (params.endTime) httpParams = httpParams.set('endTime', params.endTime);
      if (params.aggregation) httpParams = httpParams.set('aggregation', params.aggregation);
      if (params.interval) httpParams = httpParams.set('interval', params.interval);
      if (params.limit) httpParams = httpParams.set('limit', params.limit.toString());
      if (params.page) httpParams = httpParams.set('page', params.page.toString());
      if (params.tags) {
        httpParams = httpParams.set('tags', JSON.stringify(params.tags));
      }
    }
    
    return this.http.get<PaginatedMetricsResponse>(this.apiUrl, { params: httpParams });
  }
  
  /**
   * Fetches time-series data for a specific metric
   * 
   * @param experimentId Experiment ID
   * @param metricName Metric name
   * @param params Additional query parameters
   * @returns Observable<TimeSeriesMetric> Time-series data
   */
  getTimeSeriesData(
    experimentId: string,
    metricName: string,
    params?: MetricQueryParams
  ): Observable<TimeSeriesMetric> {
    let httpParams = new HttpParams()
      .set('experimentId', experimentId)
      .set('name', metricName);
    
    if (params) {
      if (params.startTime) httpParams = httpParams.set('startTime', params.startTime);
      if (params.endTime) httpParams = httpParams.set('endTime', params.endTime);
      if (params.aggregation) httpParams = httpParams.set('aggregation', params.aggregation);
      if (params.interval) httpParams = httpParams.set('interval', params.interval);
    }
    
    return this.http.get<TimeSeriesMetric>(`${this.apiUrl}/timeseries`, { params: httpParams });
  }
  
  /**
   * Fetches aggregated metrics for an experiment
   * 
   * @param experimentId Experiment ID
   * @param params Additional query parameters
   * @returns Observable<AggregatedMetric[]> Aggregated metrics
   */
  getAggregatedMetrics(
    experimentId: string,
    params?: MetricQueryParams
  ): Observable<AggregatedMetric[]> {
    let httpParams = new HttpParams().set('experimentId', experimentId);
    
    if (params) {
      if (params.startTime) httpParams = httpParams.set('startTime', params.startTime);
      if (params.endTime) httpParams = httpParams.set('endTime', params.endTime);
      if (params.aggregation) httpParams = httpParams.set('aggregation', params.aggregation);
    }
    
    return this.http.get<AggregatedMetric[]>(`${this.apiUrl}/aggregated`, { params: httpParams });
  }
  
  /**
   * Fetches metrics summary for an experiment
   * 
   * @param experimentId Experiment ID
   * @returns Observable<MetricsSummary> Metrics summary
   */
  getMetricsSummary(experimentId: string): Observable<MetricsSummary> {
    return this.http.get<MetricsSummary>(`${this.apiUrl}/summary/${experimentId}`);
  }
  
  /**
   * Creates a new metric
   * 
   * @param request Create metric request
   * @returns Observable<Metric> Created metric
   */
  createMetric(request: CreateMetricRequest): Observable<Metric> {
    return this.http.post<Metric>(this.apiUrl, request);
  }
  
  /**
   * Creates multiple metrics in batch
   * 
   * @param request Batch create metrics request
   * @returns Observable<Metric[]> Created metrics
   */
  batchCreateMetrics(request: BatchCreateMetricsRequest): Observable<Metric[]> {
    return this.http.post<Metric[]>(`${this.apiUrl}/batch`, request);
  }
  
  /**
   * Deletes a metric
   * 
   * @param id Metric ID
   * @returns Observable<void>
   */
  deleteMetric(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
