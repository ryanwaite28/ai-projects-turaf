import { Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { of } from 'rxjs';
import { map, catchError, exhaustMap, mergeMap } from 'rxjs/operators';
import * as ReportsActions from './reports.actions';
import { ReportsService } from '../../features/reports/services/reports.service';

/**
 * Reports Effects
 * 
 * Handles side effects for reports actions.
 */
@Injectable()
export class ReportsEffects {
  
  /**
   * Load Reports Effect
   */
  loadReports$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ReportsActions.loadReports),
      exhaustMap(({ params }) =>
        this.reportsService.getReports(params).pipe(
          map(response => ReportsActions.loadReportsSuccess({ response })),
          catchError(error =>
            of(ReportsActions.loadReportsFailure({
              error: error.error?.message || 'Failed to load reports'
            }))
          )
        )
      )
    )
  );
  
  /**
   * Load Report Effect
   */
  loadReport$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ReportsActions.loadReport),
      mergeMap(({ id }) =>
        this.reportsService.getReport(id).pipe(
          map(report => ReportsActions.loadReportSuccess({ report })),
          catchError(error =>
            of(ReportsActions.loadReportFailure({
              error: error.error?.message || 'Failed to load report'
            }))
          )
        )
      )
    )
  );
  
  /**
   * Create Report Effect
   */
  createReport$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ReportsActions.createReport),
      exhaustMap(({ request }) =>
        this.reportsService.createReport(request).pipe(
          map(report => ReportsActions.createReportSuccess({ report })),
          catchError(error =>
            of(ReportsActions.createReportFailure({
              error: error.error?.message || 'Failed to create report'
            }))
          )
        )
      )
    )
  );
  
  /**
   * Delete Report Effect
   */
  deleteReport$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ReportsActions.deleteReport),
      exhaustMap(({ id }) =>
        this.reportsService.deleteReport(id).pipe(
          map(() => ReportsActions.deleteReportSuccess({ id })),
          catchError(error =>
            of(ReportsActions.deleteReportFailure({
              error: error.error?.message || 'Failed to delete report'
            }))
          )
        )
      )
    )
  );
  
  /**
   * Download Report Effect
   */
  downloadReport$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ReportsActions.downloadReport),
      mergeMap(({ id, format }) =>
        this.reportsService.downloadReport(id, format).pipe(
          map(() => ReportsActions.downloadReportSuccess({ id })),
          catchError(error =>
            of(ReportsActions.downloadReportFailure({
              error: error.error?.message || 'Failed to download report'
            }))
          )
        )
      )
    )
  );
  
  /**
   * Load Report Preview Effect
   */
  loadReportPreview$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ReportsActions.loadReportPreview),
      exhaustMap(({ id }) =>
        this.reportsService.getReportPreview(id).pipe(
          map(preview => ReportsActions.loadReportPreviewSuccess({ preview })),
          catchError(error =>
            of(ReportsActions.loadReportPreviewFailure({
              error: error.error?.message || 'Failed to load report preview'
            }))
          )
        )
      )
    )
  );
  
  constructor(
    private actions$: Actions,
    private reportsService: ReportsService
  ) {}
}
