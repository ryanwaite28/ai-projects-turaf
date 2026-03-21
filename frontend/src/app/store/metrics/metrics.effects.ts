import { Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { of } from 'rxjs';
import { map, catchError, exhaustMap, mergeMap } from 'rxjs/operators';
import * as MetricsActions from './metrics.actions';
import { MetricsService } from '../../features/metrics/services/metrics.service';

/**
 * Metrics Effects
 * 
 * Handles side effects for metrics actions.
 */
@Injectable()
export class MetricsEffects {
  
  /**
   * Load Metrics Effect
   */
  loadMetrics$ = createEffect(() =>
    this.actions$.pipe(
      ofType(MetricsActions.loadMetrics),
      exhaustMap(({ params }) =>
        this.metricsService.getMetrics(params).pipe(
          map(response => MetricsActions.loadMetricsSuccess({ response })),
          catchError(error =>
            of(MetricsActions.loadMetricsFailure({
              error: error.error?.message || 'Failed to load metrics'
            }))
          )
        )
      )
    )
  );
  
  /**
   * Load Time Series Data Effect
   */
  loadTimeSeriesData$ = createEffect(() =>
    this.actions$.pipe(
      ofType(MetricsActions.loadTimeSeriesData),
      mergeMap(({ experimentId, metricName, params }) =>
        this.metricsService.getTimeSeriesData(experimentId, metricName, params).pipe(
          map(data => MetricsActions.loadTimeSeriesDataSuccess({ metricName, data })),
          catchError(error =>
            of(MetricsActions.loadTimeSeriesDataFailure({
              error: error.error?.message || 'Failed to load time series data'
            }))
          )
        )
      )
    )
  );
  
  /**
   * Load Aggregated Metrics Effect
   */
  loadAggregatedMetrics$ = createEffect(() =>
    this.actions$.pipe(
      ofType(MetricsActions.loadAggregatedMetrics),
      exhaustMap(({ experimentId, params }) =>
        this.metricsService.getAggregatedMetrics(experimentId, params).pipe(
          map(metrics => MetricsActions.loadAggregatedMetricsSuccess({ metrics })),
          catchError(error =>
            of(MetricsActions.loadAggregatedMetricsFailure({
              error: error.error?.message || 'Failed to load aggregated metrics'
            }))
          )
        )
      )
    )
  );
  
  /**
   * Load Metrics Summary Effect
   */
  loadMetricsSummary$ = createEffect(() =>
    this.actions$.pipe(
      ofType(MetricsActions.loadMetricsSummary),
      exhaustMap(({ experimentId }) =>
        this.metricsService.getMetricsSummary(experimentId).pipe(
          map(summary => MetricsActions.loadMetricsSummarySuccess({ summary })),
          catchError(error =>
            of(MetricsActions.loadMetricsSummaryFailure({
              error: error.error?.message || 'Failed to load metrics summary'
            }))
          )
        )
      )
    )
  );
  
  /**
   * Create Metric Effect
   */
  createMetric$ = createEffect(() =>
    this.actions$.pipe(
      ofType(MetricsActions.createMetric),
      exhaustMap(({ request }) =>
        this.metricsService.createMetric(request).pipe(
          map(metric => MetricsActions.createMetricSuccess({ metric })),
          catchError(error =>
            of(MetricsActions.createMetricFailure({
              error: error.error?.message || 'Failed to create metric'
            }))
          )
        )
      )
    )
  );
  
  /**
   * Batch Create Metrics Effect
   */
  batchCreateMetrics$ = createEffect(() =>
    this.actions$.pipe(
      ofType(MetricsActions.batchCreateMetrics),
      exhaustMap(({ request }) =>
        this.metricsService.batchCreateMetrics(request).pipe(
          map(metrics => MetricsActions.batchCreateMetricsSuccess({ metrics })),
          catchError(error =>
            of(MetricsActions.batchCreateMetricsFailure({
              error: error.error?.message || 'Failed to batch create metrics'
            }))
          )
        )
      )
    )
  );
  
  constructor(
    private actions$: Actions,
    private metricsService: MetricsService
  ) {}
}
