import { Problem } from '../../models/problem.model';

/**
 * Problems State
 * 
 * Manages problems data in NgRx store.
 */
export interface ProblemsState {
  problems: Problem[];
  selectedProblem: Problem | null;
  loading: boolean;
  error: string | null;
  pagination: {
    page: number;
    limit: number;
    total: number;
    totalPages: number;
  };
}

/**
 * Initial problems state
 */
export const initialProblemsState: ProblemsState = {
  problems: [],
  selectedProblem: null,
  loading: false,
  error: null,
  pagination: {
    page: 1,
    limit: 10,
    total: 0,
    totalPages: 0
  }
};
