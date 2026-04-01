import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { 
  Experiment, 
  CreateExperimentRequest, 
  UpdateExperimentRequest
} from '../../../models/experiment.model';
import { environment } from '../../../../environments/environment';

/**
 * Experiments Service
 * 
 * Handles experiment-related API calls.
 */
@Injectable({
  providedIn: 'root'
})
export class ExperimentsService {
  
  private readonly apiUrl = `${environment.apiUrl}/experiments`;
  
  constructor(private http: HttpClient) {}
  
  /**
   * Fetches list of experiments for an organization
   * 
   * @param organizationId Organization ID
   * @returns Observable<Experiment[]> List of experiments
   */
  getExperiments(organizationId: string): Observable<Experiment[]> {
    const httpParams = new HttpParams().set('organizationId', organizationId);
    return this.http.get<Experiment[]>(this.apiUrl, { params: httpParams });
  }
  
  /**
   * Fetches a single experiment by ID
   * 
   * @param id Experiment ID
   * @param organizationId Organization ID
   * @returns Observable<Experiment> Experiment details
   */
  getExperiment(id: string, organizationId: string): Observable<Experiment> {
    const httpParams = new HttpParams().set('organizationId', organizationId);
    return this.http.get<Experiment>(`${this.apiUrl}/${id}`, { params: httpParams });
  }
  
  /**
   * Creates a new experiment
   * 
   * @param request Create experiment request
   * @returns Observable<Experiment> Created experiment
   */
  createExperiment(request: CreateExperimentRequest): Observable<Experiment> {
    return this.http.post<Experiment>(this.apiUrl, request);
  }
  
  /**
   * Updates an existing experiment
   * 
   * @param id Experiment ID
   * @param request Update experiment request
   * @returns Observable<Experiment> Updated experiment
   */
  updateExperiment(id: string, request: UpdateExperimentRequest): Observable<Experiment> {
    return this.http.put<Experiment>(`${this.apiUrl}/${id}`, request);
  }
  
  /**
   * Deletes an experiment
   * 
   * @param id Experiment ID
   * @param organizationId Organization ID
   * @returns Observable<void>
   */
  deleteExperiment(id: string, organizationId: string): Observable<void> {
    const httpParams = new HttpParams().set('organizationId', organizationId);
    return this.http.delete<void>(`${this.apiUrl}/${id}`, { params: httpParams });
  }
  
  /**
   * Starts an experiment
   * 
   * @param id Experiment ID
   * @param organizationId Organization ID
   * @returns Observable<Experiment> Updated experiment
   */
  startExperiment(id: string, organizationId: string): Observable<Experiment> {
    const httpParams = new HttpParams().set('organizationId', organizationId);
    return this.http.post<Experiment>(`${this.apiUrl}/${id}/start`, {}, { params: httpParams });
  }
  
  /**
   * Completes an experiment
   * 
   * @param id Experiment ID
   * @param organizationId Organization ID
   * @returns Observable<Experiment> Updated experiment
   */
  completeExperiment(id: string, organizationId: string): Observable<Experiment> {
    const httpParams = new HttpParams().set('organizationId', organizationId);
    return this.http.post<Experiment>(`${this.apiUrl}/${id}/complete`, {}, { params: httpParams });
  }
  
  /**
   * Cancels an experiment
   * 
   * @param id Experiment ID
   * @param organizationId Organization ID
   * @returns Observable<Experiment> Updated experiment
   */
  cancelExperiment(id: string, organizationId: string): Observable<Experiment> {
    const httpParams = new HttpParams().set('organizationId', organizationId);
    return this.http.post<Experiment>(`${this.apiUrl}/${id}/cancel`, {}, { params: httpParams });
  }
}
