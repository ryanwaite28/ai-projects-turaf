import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { 
  Hypothesis, 
  CreateHypothesisRequest, 
  UpdateHypothesisRequest,
  HypothesisQueryParams,
  PaginatedHypothesesResponse
} from '../../../models/hypothesis.model';
import { environment } from '../../../../environments/environment';

/**
 * Hypotheses Service
 * 
 * Handles hypothesis-related API calls.
 */
@Injectable({
  providedIn: 'root'
})
export class HypothesesService {
  
  private readonly apiUrl = `${environment.apiUrl}/hypotheses`;
  
  constructor(private http: HttpClient) {}
  
  /**
   * Fetches paginated list of hypotheses
   * 
   * @param params Query parameters for filtering and pagination
   * @returns Observable<PaginatedHypothesesResponse> Paginated hypotheses
   */
  getHypotheses(params?: HypothesisQueryParams): Observable<PaginatedHypothesesResponse> {
    let httpParams = new HttpParams();
    
    if (params) {
      if (params.page) httpParams = httpParams.set('page', params.page.toString());
      if (params.limit) httpParams = httpParams.set('limit', params.limit.toString());
      if (params.problemId) httpParams = httpParams.set('problemId', params.problemId);
      if (params.status) httpParams = httpParams.set('status', params.status);
      if (params.search) httpParams = httpParams.set('search', params.search);
      if (params.sortBy) httpParams = httpParams.set('sortBy', params.sortBy);
      if (params.sortOrder) httpParams = httpParams.set('sortOrder', params.sortOrder);
    }
    
    return this.http.get<PaginatedHypothesesResponse>(this.apiUrl, { params: httpParams });
  }
  
  /**
   * Fetches hypotheses for a specific problem
   * 
   * @param problemId Problem ID
   * @param params Additional query parameters
   * @returns Observable<PaginatedHypothesesResponse> Paginated hypotheses
   */
  getHypothesesByProblem(problemId: string, params?: HypothesisQueryParams): Observable<PaginatedHypothesesResponse> {
    return this.getHypotheses({ ...params, problemId });
  }
  
  /**
   * Fetches a single hypothesis by ID
   * 
   * @param id Hypothesis ID
   * @returns Observable<Hypothesis> Hypothesis details
   */
  getHypothesis(id: string): Observable<Hypothesis> {
    return this.http.get<Hypothesis>(`${this.apiUrl}/${id}`);
  }
  
  /**
   * Creates a new hypothesis
   * 
   * @param request Create hypothesis request
   * @returns Observable<Hypothesis> Created hypothesis
   */
  createHypothesis(request: CreateHypothesisRequest): Observable<Hypothesis> {
    return this.http.post<Hypothesis>(this.apiUrl, request);
  }
  
  /**
   * Updates an existing hypothesis
   * 
   * @param id Hypothesis ID
   * @param request Update hypothesis request
   * @returns Observable<Hypothesis> Updated hypothesis
   */
  updateHypothesis(id: string, request: UpdateHypothesisRequest): Observable<Hypothesis> {
    return this.http.put<Hypothesis>(`${this.apiUrl}/${id}`, request);
  }
  
  /**
   * Deletes a hypothesis
   * 
   * @param id Hypothesis ID
   * @returns Observable<void>
   */
  deleteHypothesis(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
