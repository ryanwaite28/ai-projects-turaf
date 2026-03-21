import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DashboardData, DashboardStats, RecentExperiment, DashboardMetrics } from '../../../models/dashboard.model';
import { environment } from '../../../../environments/environment';

/**
 * Dashboard Service
 * 
 * Handles dashboard-related API calls.
 */
@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  
  private readonly apiUrl = `${environment.apiUrl}/dashboard`;
  
  constructor(private http: HttpClient) {}
  
  /**
   * Fetches complete dashboard data
   * 
   * @returns Observable<DashboardData> Complete dashboard data
   */
  getDashboardData(): Observable<DashboardData> {
    return this.http.get<DashboardData>(this.apiUrl);
  }
  
  /**
   * Fetches dashboard statistics
   * 
   * @returns Observable<DashboardStats> Dashboard statistics
   */
  getDashboardStats(): Observable<DashboardStats> {
    return this.http.get<DashboardStats>(`${this.apiUrl}/stats`);
  }
  
  /**
   * Fetches recent experiments
   * 
   * @param limit Number of experiments to fetch (default: 5)
   * @returns Observable<RecentExperiment[]> Recent experiments
   */
  getRecentExperiments(limit: number = 5): Observable<RecentExperiment[]> {
    return this.http.get<RecentExperiment[]>(`${this.apiUrl}/recent-experiments`, {
      params: { limit: limit.toString() }
    });
  }
  
  /**
   * Fetches dashboard metrics for charts
   * 
   * @param days Number of days to fetch (default: 30)
   * @returns Observable<DashboardMetrics> Dashboard metrics
   */
  getDashboardMetrics(days: number = 30): Observable<DashboardMetrics> {
    return this.http.get<DashboardMetrics>(`${this.apiUrl}/metrics`, {
      params: { days: days.toString() }
    });
  }
}
