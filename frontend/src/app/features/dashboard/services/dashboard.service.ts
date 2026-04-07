import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DashboardOverview, ExperimentFull, OrganizationSummary } from '../../../models/dashboard.model';
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
   * Fetches dashboard overview with user, organizations, and active experiments
   * 
   * @returns Observable<DashboardOverview> Dashboard overview data
   */
  getDashboardOverview(): Observable<DashboardOverview> {
    return this.http.get<DashboardOverview>(`${this.apiUrl}/overview`);
  }
  
  /**
   * Fetches full experiment details including metrics
   * 
   * @param id Experiment ID
   * @param organizationId Organization ID
   * @returns Observable<ExperimentFull> Full experiment details
   */
  getExperimentFull(id: string, organizationId: string): Observable<ExperimentFull> {
    return this.http.get<ExperimentFull>(`${this.apiUrl}/experiments/${id}/full`, {
      params: { organizationId }
    });
  }
  
  /**
   * Fetches organization summary with members and experiments
   * 
   * @param id Organization ID
   * @returns Observable<OrganizationSummary> Organization summary
   */
  getOrganizationSummary(id: string): Observable<OrganizationSummary> {
    return this.http.get<OrganizationSummary>(`${this.apiUrl}/organizations/${id}/summary`);
  }
}
