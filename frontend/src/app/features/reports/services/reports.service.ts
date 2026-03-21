import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { 
  Report,
  CreateReportRequest,
  ReportQueryParams,
  PaginatedReportsResponse,
  ReportPreview
} from '../../../models/report.model';
import { environment } from '../../../../environments/environment';

/**
 * Reports Service
 * 
 * Handles report-related API calls.
 */
@Injectable({
  providedIn: 'root'
})
export class ReportsService {
  
  private readonly apiUrl = `${environment.apiUrl}/reports`;
  
  constructor(private http: HttpClient) {}
  
  /**
   * Fetches paginated list of reports
   * 
   * @param params Query parameters for filtering
   * @returns Observable<PaginatedReportsResponse> Paginated reports
   */
  getReports(params?: ReportQueryParams): Observable<PaginatedReportsResponse> {
    let httpParams = new HttpParams();
    
    if (params) {
      if (params.type) httpParams = httpParams.set('type', params.type);
      if (params.format) httpParams = httpParams.set('format', params.format);
      if (params.status) httpParams = httpParams.set('status', params.status);
      if (params.experimentId) httpParams = httpParams.set('experimentId', params.experimentId);
      if (params.hypothesisId) httpParams = httpParams.set('hypothesisId', params.hypothesisId);
      if (params.problemId) httpParams = httpParams.set('problemId', params.problemId);
      if (params.startDate) httpParams = httpParams.set('startDate', params.startDate);
      if (params.endDate) httpParams = httpParams.set('endDate', params.endDate);
      if (params.limit) httpParams = httpParams.set('limit', params.limit.toString());
      if (params.page) httpParams = httpParams.set('page', params.page.toString());
    }
    
    return this.http.get<PaginatedReportsResponse>(this.apiUrl, { params: httpParams });
  }
  
  /**
   * Fetches a single report by ID
   * 
   * @param id Report ID
   * @returns Observable<Report> Report
   */
  getReport(id: string): Observable<Report> {
    return this.http.get<Report>(`${this.apiUrl}/${id}`);
  }
  
  /**
   * Creates a new report
   * 
   * @param request Create report request
   * @returns Observable<Report> Created report
   */
  createReport(request: CreateReportRequest): Observable<Report> {
    return this.http.post<Report>(this.apiUrl, request);
  }
  
  /**
   * Deletes a report
   * 
   * @param id Report ID
   * @returns Observable<void>
   */
  deleteReport(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
  
  /**
   * Downloads a report
   * 
   * @param id Report ID
   * @param format Optional format override
   * @returns Observable<Blob> Report file
   */
  downloadReport(id: string, format?: string): Observable<Blob> {
    let httpParams = new HttpParams();
    if (format) {
      httpParams = httpParams.set('format', format);
    }
    
    return this.http.get(`${this.apiUrl}/${id}/download`, {
      params: httpParams,
      responseType: 'blob'
    }).pipe(
      tap(blob => {
        // Trigger browser download
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `report-${id}.${format || 'pdf'}`;
        link.click();
        window.URL.revokeObjectURL(url);
      })
    );
  }
  
  /**
   * Fetches report preview
   * 
   * @param id Report ID
   * @returns Observable<ReportPreview> Report preview
   */
  getReportPreview(id: string): Observable<ReportPreview> {
    return this.http.get<ReportPreview>(`${this.apiUrl}/${id}/preview`);
  }
}
