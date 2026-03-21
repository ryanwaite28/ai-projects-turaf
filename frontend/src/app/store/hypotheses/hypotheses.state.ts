import { Hypothesis } from '../../models/hypothesis.model';

/**
 * Hypotheses State
 * 
 * Manages hypotheses data in NgRx store.
 */
export interface HypothesesState {
  hypotheses: Hypothesis[];
  selectedHypothesis: Hypothesis | null;
  loading: boolean;
  error: string | null;
  pagination: {
    page: number;
    limit: number;
    total: number;
    totalPages: number;
  };
  filters: {
    problemId?: string;
    status?: string;
  };
}

/**
 * Initial hypotheses state
 */
export const initialHypothesesState: HypothesesState = {
  hypotheses: [],
  selectedHypothesis: null,
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
