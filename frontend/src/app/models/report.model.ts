/**
 * Report Models
 * 
 * Type definitions for reports and report generation.
 */

/**
 * Report entity
 */
export interface Report {
  id: string;
  name: string;
  description?: string;
  type: ReportType;
  format: ReportFormat;
  status: ReportStatus;
  experimentId?: string;
  hypothesisId?: string;
  problemId?: string;
  generatedBy: string;
  generatedAt: string;
  fileUrl?: string;
  fileSize?: number;
  parameters?: Record<string, any>;
  metadata?: Record<string, any>;
  organizationId: string;
  createdAt: string;
  updatedAt: string;
}

/**
 * Report type enum
 */
export enum ReportType {
  EXPERIMENT_SUMMARY = 'EXPERIMENT_SUMMARY',
  HYPOTHESIS_ANALYSIS = 'HYPOTHESIS_ANALYSIS',
  PROBLEM_OVERVIEW = 'PROBLEM_OVERVIEW',
  METRICS_REPORT = 'METRICS_REPORT',
  COMPARISON_REPORT = 'COMPARISON_REPORT',
  CUSTOM = 'CUSTOM'
}

/**
 * Report format enum
 */
export enum ReportFormat {
  PDF = 'PDF',
  CSV = 'CSV',
  JSON = 'JSON',
  HTML = 'HTML',
  EXCEL = 'EXCEL'
}

/**
 * Report status enum
 */
export enum ReportStatus {
  PENDING = 'PENDING',
  GENERATING = 'GENERATING',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED'
}

/**
 * Create report request
 */
export interface CreateReportRequest {
  name: string;
  description?: string;
  type: ReportType;
  format: ReportFormat;
  experimentId?: string;
  hypothesisId?: string;
  problemId?: string;
  parameters?: Record<string, any>;
  metadata?: Record<string, any>;
}

/**
 * Report query params
 */
export interface ReportQueryParams {
  type?: ReportType;
  format?: ReportFormat;
  status?: ReportStatus;
  experimentId?: string;
  hypothesisId?: string;
  problemId?: string;
  startDate?: string;
  endDate?: string;
  limit?: number;
  page?: number;
}

/**
 * Paginated reports response
 */
export interface PaginatedReportsResponse {
  reports: Report[];
  total: number;
  page: number;
  limit: number;
  totalPages: number;
}

/**
 * Report preview data
 */
export interface ReportPreview {
  reportId: string;
  title: string;
  summary: string;
  sections: ReportSection[];
  generatedAt: string;
}

/**
 * Report section
 */
export interface ReportSection {
  title: string;
  content: string;
  charts?: ChartData[];
  tables?: TableData[];
  order: number;
}

/**
 * Chart data for reports
 */
export interface ChartData {
  title: string;
  type: string;
  data: any[];
  config?: Record<string, any>;
}

/**
 * Table data for reports
 */
export interface TableData {
  title: string;
  headers: string[];
  rows: any[][];
}

/**
 * Report download request
 */
export interface ReportDownloadRequest {
  reportId: string;
  format?: ReportFormat;
}

/**
 * Report generation progress
 */
export interface ReportProgress {
  reportId: string;
  status: ReportStatus;
  progress: number;
  message?: string;
  estimatedTimeRemaining?: number;
}
