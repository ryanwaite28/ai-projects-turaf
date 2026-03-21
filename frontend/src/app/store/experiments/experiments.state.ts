import { Experiment } from '../../models/experiment.model';

/**
 * Experiments State
 * 
 * Manages experiments data in NgRx store.
 */
export interface ExperimentsState {
  experiments: Experiment[];
  selectedExperiment: Experiment | null;
  loading: boolean;
  error: string | null;
  pagination: {
    page: number;
    limit: number;
    total: number;
    totalPages: number;
  };
  filters: {
    hypothesisId?: string;
    status?: string;
  };
  realTimeUpdates: boolean;
}

/**
 * Initial experiments state
 */
export const initialExperimentsState: ExperimentsState = {
  experiments: [],
  selectedExperiment: null,
  loading: false,
  error: null,
  pagination: {
    page: 1,
    limit: 10,
    total: 0,
    totalPages: 0
  },
  filters: {},
  realTimeUpdates: false
};
