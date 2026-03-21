import { Component, OnInit, OnDestroy } from '@angular/core';
import { Store } from '@ngrx/store';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { AppState } from '../../store/app.state';
import { 
  loadDashboard, 
  refreshDashboard 
} from '../../store/dashboard/dashboard.actions';
import {
  selectDashboardStats,
  selectRecentExperiments,
  selectDashboardMetrics,
  selectDashboardLoading,
  selectDashboardError
} from '../../store/dashboard/dashboard.selectors';

/**
 * Dashboard Component
 * 
 * Main dashboard view showing overview of problems, experiments, and metrics.
 * 
 * Features:
 * - Dashboard statistics
 * - Recent experiments
 * - Metrics visualization
 * - Refresh functionality
 */
@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit, OnDestroy {
  
  stats$ = this.store.select(selectDashboardStats);
  recentExperiments$ = this.store.select(selectRecentExperiments);
  metrics$ = this.store.select(selectDashboardMetrics);
  loading$ = this.store.select(selectDashboardLoading);
  error$ = this.store.select(selectDashboardError);
  
  private destroy$ = new Subject<void>();
  
  constructor(private store: Store<AppState>) {}
  
  ngOnInit(): void {
    // Load dashboard data on component initialization
    this.store.dispatch(loadDashboard());
  }
  
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
  
  /**
   * Refreshes dashboard data
   */
  onRefresh(): void {
    this.store.dispatch(refreshDashboard());
  }
}
