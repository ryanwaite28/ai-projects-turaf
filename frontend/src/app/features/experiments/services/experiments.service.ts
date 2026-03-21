import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { 
  Experiment, 
  CreateExperimentRequest, 
  UpdateExperimentRequest,
  ExperimentQueryParams,
  PaginatedExperimentsResponse,
  ExperimentStateTransition
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
   * Fetches paginated list of experiments
   * 
   * @param params Query parameters for filtering and pagination
   * @returns Observable<PaginatedExperimentsResponse> Paginated experiments
   */
  getExperiments(params?: ExperimentQueryParams): Observable<PaginatedExperimentsResponse> {
    let httpParams = new HttpParams();
    
    if (params) {
      if (params.page) httpParams = httpParams.set('page', params.page.toString());
      if (params.limit) httpParams = httpParams.set('limit', params.limit.toString());
      if (params.hypothesisId) httpParams = httpParams.set('hypothesisId', params.hypothesisId);
      if (params.status) httpParams = httpParams.set('status', params.status);
      if (params.search) httpParams = httpParams.set('search', params.search);
      if (params.sortBy) httpParams = httpParams.set('sortBy', params.sortBy);
      if (params.sortOrder) httpParams = httpParams.set('sortOrder', params.sortOrder);
    }
    
    return this.http.get<PaginatedExperimentsResponse>(this.apiUrl, { params: httpParams });
  }
  
  /**
   * Fetches experiments for a specific hypothesis
   * 
   * @param hypothesisId Hypothesis ID
   * @param params Additional query parameters
   * @returns Observable<PaginatedExperimentsResponse> Paginated experiments
   */
  getExperimentsByHypothesis(hypothesisId: string, params?: ExperimentQueryParams): Observable<PaginatedExperimentsResponse> {
    return this.getExperiments({ ...params, hypothesisId });
  }
  
  /**
   * Fetches a single experiment by ID
   * 
   * @param id Experiment ID
   * @returns Observable<Experiment> Experiment details
   */
  getExperiment(id: string): Observable<Experiment> {
    return this.http.get<Experiment>(`${this.apiUrl}/${id}`);
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
   * @returns Observable<void>
   */
  deleteExperiment(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
  
  /**
   * Transitions experiment state
   * 
   * @param id Experiment ID
   * @param transition State transition request
   * @returns Observable<Experiment> Updated experiment
   */
  transitionState(id: string, transition: ExperimentStateTransition): Observable<Experiment> {
    return this.http.post<Experiment>(`${this.apiUrl}/${id}/transition`, transition);
  }
  
  /**
   * Starts an experiment
   * 
   * @param id Experiment ID
   * @param transition Transition details
   * @returns Observable<Experiment> Updated experiment
   */
  startExperiment(id: string, transition: ExperimentStateTransition): Observable<Experiment> {
    return this.transitionState(id, { ...transition, action: 'start' });
  }
  
  /**
   * Pauses an experiment
   * 
   * @param id Experiment ID
   * @param transition Transition details
   * @returns Observable<Experiment> Updated experiment
   */
  pauseExperiment(id: string, transition: ExperimentStateTransition): Observable<Experiment> {
    return this.transitionState(id, { ...transition, action: 'pause' });
  }
  
  /**
   * Resumes an experiment
   * 
   * @param id Experiment ID
   * @param transition Transition details
   * @returns Observable<Experiment> Updated experiment
   */
  resumeExperiment(id: string, transition: ExperimentStateTransition): Observable<Experiment> {
    return this.transitionState(id, { ...transition, action: 'resume' });
  }
  
  /**
   * Completes an experiment
   * 
   * @param id Experiment ID
   * @param transition Transition details
   * @returns Observable<Experiment> Updated experiment
   */
  completeExperiment(id: string, transition: ExperimentStateTransition): Observable<Experiment> {
    return this.transitionState(id, { ...transition, action: 'complete' });
  }
  
  /**
   * Cancels an experiment
   * 
   * @param id Experiment ID
   * @param transition Transition details
   * @returns Observable<Experiment> Updated experiment
   */
  cancelExperiment(id: string, transition: ExperimentStateTransition): Observable<Experiment> {
    return this.transitionState(id, { ...transition, action: 'cancel' });
  }
  
  /**
   * Marks an experiment as failed
   * 
   * @param id Experiment ID
   * @param transition Transition details
   * @returns Observable<Experiment> Updated experiment
   */
  failExperiment(id: string, transition: ExperimentStateTransition): Observable<Experiment> {
    return this.transitionState(id, { ...transition, action: 'fail' });
  }
}
