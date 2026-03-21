import { Report, ReportPreview, ReportProgress } from '../../models/report.model';

/**
 * Reports State
 * 
 * Manages reports data in NgRx store.
 */
export interface ReportsState {
  reports: Report[];
  selectedReport: Report | null;
  reportPreview: ReportPreview | null;
  generationProgress: Record<string, ReportProgress>;
  loading: boolean;
  error: string | null;
  pagination: {
    page: number;
    limit: number;
    total: number;
    totalPages: number;
  };
  filters: {
    type?: string;
    format?: string;
    status?: string;
    experimentId?: string;
  };
}

/**
 * Initial reports state
 */
export const initialReportsState: ReportsState = {
  reports: [],
  selectedReport: null,
  reportPreview: null,
  generationProgress: {},
  loading: false,
  error: null,
  pagination: {
    page: 1,
    limit: 10,
    total: 0,
    totalPages: 0
  },
  filters: {}
};
