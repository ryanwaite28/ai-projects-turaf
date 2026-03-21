import { AuthState } from './auth/auth.state';

/**
 * Root application state interface
 * 
 * This defines the complete state tree for the application.
 * Each feature module has its own slice of state.
 * 
 * Following NgRx best practices:
 * - Normalized state structure
 * - Feature-based state slices
 * - Immutable state updates
 */
export interface AppState {
  auth: AuthState;
  // Future feature states will be added here:
  // organizations: OrganizationState;
  // experiments: ExperimentState;
  // metrics: MetricsState;
}
