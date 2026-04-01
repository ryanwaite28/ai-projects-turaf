import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { 
  Metric,
  CreateMetricRequest
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
   * Fetches metrics for a specific experiment
   * 
   * @param experimentId Experiment ID
   * @returns Observable<Metric[]> List of metrics
   */
  getExperimentMetrics(experimentId: string): Observable<Metric[]> {
    return this.http.get<Metric[]>(`${this.apiUrl}/experiments/${experimentId}`);
  }
  
  /**
   * Records a new metric
   * 
   * @param request Record metric request
   * @returns Observable<Metric> Recorded metric
   */
  recordMetric(request: CreateMetricRequest): Observable<Metric> {
    return this.http.post<Metric>(`${this.apiUrl}/metrics`, request);
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
