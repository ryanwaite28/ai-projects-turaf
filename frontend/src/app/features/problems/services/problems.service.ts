import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { 
  Problem, 
  CreateProblemRequest, 
  UpdateProblemRequest
} from '../../../models/problem.model';
import { environment } from '../../../../environments/environment';

/**
 * Problems Service
 * 
 * Handles problem-related API calls.
 */
@Injectable({
  providedIn: 'root'
})
export class ProblemsService {
  
  private readonly apiUrl = `${environment.apiUrl}/problems`;
  
  constructor(private http: HttpClient) {}
  
  /**
   * Fetches list of problems
   * 
   * @returns Observable<Problem[]> List of problems
   */
  getProblems(): Observable<Problem[]> {
    return this.http.get<Problem[]>(this.apiUrl);
  }
  
  /**
   * Fetches a single problem by ID
   * 
   * @param id Problem ID
   * @returns Observable<Problem> Problem details
   */
  getProblem(id: string): Observable<Problem> {
    return this.http.get<Problem>(`${this.apiUrl}/${id}`);
  }
  
  /**
   * Creates a new problem
   * 
   * @param request Create problem request
   * @returns Observable<Problem> Created problem
   */
  createProblem(request: CreateProblemRequest): Observable<Problem> {
    return this.http.post<Problem>(this.apiUrl, request);
  }
  
  /**
   * Updates an existing problem
   * 
   * @param id Problem ID
   * @param request Update problem request
   * @returns Observable<Problem> Updated problem
   */
  updateProblem(id: string, request: UpdateProblemRequest): Observable<Problem> {
    return this.http.put<Problem>(`${this.apiUrl}/${id}`, request);
  }
  
  /**
   * Deletes a problem
   * 
   * @param id Problem ID
   * @returns Observable<void>
   */
  deleteProblem(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
