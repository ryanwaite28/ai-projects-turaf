import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { 
  Hypothesis, 
  CreateHypothesisRequest, 
  UpdateHypothesisRequest
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
   * Fetches list of hypotheses
   * 
   * @param problemId Optional problem ID to filter by
   * @returns Observable<Hypothesis[]> List of hypotheses
   */
  getHypotheses(problemId?: string): Observable<Hypothesis[]> {
    let httpParams = new HttpParams();
    if (problemId) {
      httpParams = httpParams.set('problemId', problemId);
    }
    return this.http.get<Hypothesis[]>(this.apiUrl, { params: httpParams });
  }
  
  /**
   * Fetches hypotheses for a specific problem
   * 
   * @param problemId Problem ID
   * @returns Observable<Hypothesis[]> List of hypotheses
   */
  getHypothesesByProblem(problemId: string): Observable<Hypothesis[]> {
    return this.getHypotheses(problemId);
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
