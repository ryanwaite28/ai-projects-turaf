import { Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { of } from 'rxjs';
import { map, catchError, exhaustMap, mergeMap } from 'rxjs/operators';
import * as DashboardActions from './dashboard.actions';
import { DashboardService } from '../../features/dashboard/services/dashboard.service';

/**
 * Dashboard Effects
 * 
 * Handles side effects for dashboard actions.
 */
@Injectable()
export class DashboardEffects {
  
  /**
   * Load Dashboard Effect
   * 
   * Fetches complete dashboard data.
   */
  loadDashboard$ = createEffect(() =>
    this.actions$.pipe(
      ofType(DashboardActions.loadDashboard, DashboardActions.refreshDashboard),
      exhaustMap(() =>
        this.dashboardService.getDashboardData().pipe(
          map(data => DashboardActions.loadDashboardSuccess({ data })),
          catchError(error =>
            of(DashboardActions.loadDashboardFailure({
              error: error.error?.message || 'Failed to load dashboard data'
            }))
          )
        )
      )
    )
  );
  
  /**
   * Load Dashboard Stats Effect
   * 
   * Fetches dashboard statistics.
   */
  loadDashboardStats$ = createEffect(() =>
    this.actions$.pipe(
      ofType(DashboardActions.loadDashboardStats),
      exhaustMap(() =>
        this.dashboardService.getDashboardStats().pipe(
          map(stats => DashboardActions.loadDashboardStatsSuccess({ stats })),
          catchError(error =>
            of(DashboardActions.loadDashboardStatsFailure({
              error: error.error?.message || 'Failed to load dashboard stats'
            }))
          )
        )
      )
    )
  );
  
  /**
   * Load Recent Experiments Effect
   * 
   * Fetches recent experiments.
   */
  loadRecentExperiments$ = createEffect(() =>
    this.actions$.pipe(
      ofType(DashboardActions.loadRecentExperiments),
      mergeMap(() =>
        this.dashboardService.getRecentExperiments().pipe(
          map(experiments => DashboardActions.loadRecentExperimentsSuccess({ experiments })),
          catchError(error =>
            of(DashboardActions.loadRecentExperimentsFailure({
              error: error.error?.message || 'Failed to load recent experiments'
            }))
          )
        )
      )
    )
  );
  
  /**
   * Load Dashboard Metrics Effect
   * 
   * Fetches dashboard metrics for charts.
   */
  loadDashboardMetrics$ = createEffect(() =>
    this.actions$.pipe(
      ofType(DashboardActions.loadDashboardMetrics),
      mergeMap(() =>
        this.dashboardService.getDashboardMetrics().pipe(
          map(metrics => DashboardActions.loadDashboardMetricsSuccess({ metrics })),
          catchError(error =>
            of(DashboardActions.loadDashboardMetricsFailure({
              error: error.error?.message || 'Failed to load dashboard metrics'
            }))
          )
        )
      )
    )
  );
  
  constructor(
    private actions$: Actions,
    private dashboardService: DashboardService
  ) {}
}
