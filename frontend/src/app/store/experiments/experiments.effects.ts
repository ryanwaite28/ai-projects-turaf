import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { of } from 'rxjs';
import { map, catchError, exhaustMap, mergeMap, tap } from 'rxjs/operators';
import * as ExperimentsActions from './experiments.actions';
import { ExperimentsService } from '../../features/experiments/services/experiments.service';

/**
 * Experiments Effects
 * 
 * Handles side effects for experiments actions.
 */
@Injectable()
export class ExperimentsEffects {
  
  /**
   * Load Experiments Effect
   */
  loadExperiments$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ExperimentsActions.loadExperiments),
      exhaustMap(({ params }) =>
        this.experimentsService.getExperiments(params).pipe(
          map(response => ExperimentsActions.loadExperimentsSuccess({ response })),
          catchError(error =>
            of(ExperimentsActions.loadExperimentsFailure({
              error: error.error?.message || 'Failed to load experiments'
            }))
          )
        )
      )
    )
  );
  
  /**
   * Load Experiments by Hypothesis Effect
   */
  loadExperimentsByHypothesis$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ExperimentsActions.loadExperimentsByHypothesis),
      exhaustMap(({ hypothesisId, params }) =>
        this.experimentsService.getExperimentsByHypothesis(hypothesisId, params).pipe(
          map(response => ExperimentsActions.loadExperimentsSuccess({ response })),
          catchError(error =>
            of(ExperimentsActions.loadExperimentsFailure({
              error: error.error?.message || 'Failed to load experiments for hypothesis'
            }))
          )
        )
      )
    )
  );
  
  /**
   * Load Experiment Effect
   */
  loadExperiment$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ExperimentsActions.loadExperiment),
      mergeMap(({ id }) =>
        this.experimentsService.getExperiment(id).pipe(
          map(experiment => ExperimentsActions.loadExperimentSuccess({ experiment })),
          catchError(error =>
            of(ExperimentsActions.loadExperimentFailure({
              error: error.error?.message || 'Failed to load experiment'
            }))
          )
        )
      )
    )
  );
  
  /**
   * Create Experiment Effect
   */
  createExperiment$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ExperimentsActions.createExperiment),
      exhaustMap(({ request }) =>
        this.experimentsService.createExperiment(request).pipe(
          map(experiment => ExperimentsActions.createExperimentSuccess({ experiment })),
          catchError(error =>
            of(ExperimentsActions.createExperimentFailure({
              error: error.error?.message || 'Failed to create experiment'
            }))
          )
        )
      )
    )
  );
  
  /**
   * Create Experiment Success Effect - Navigate to experiment detail
   */
  createExperimentSuccess$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ExperimentsActions.createExperimentSuccess),
      tap(({ experiment }) => {
        this.router.navigate(['/experiments', experiment.id]);
      })
    ),
    { dispatch: false }
  );
  
  /**
   * Update Experiment Effect
   */
  updateExperiment$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ExperimentsActions.updateExperiment),
      exhaustMap(({ id, request }) =>
        this.experimentsService.updateExperiment(id, request).pipe(
          map(experiment => ExperimentsActions.updateExperimentSuccess({ experiment })),
          catchError(error =>
            of(ExperimentsActions.updateExperimentFailure({
              error: error.error?.message || 'Failed to update experiment'
            }))
          )
        )
      )
    )
  );
  
  /**
   * Delete Experiment Effect
   */
  deleteExperiment$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ExperimentsActions.deleteExperiment),
      exhaustMap(({ id }) =>
        this.experimentsService.deleteExperiment(id).pipe(
          map(() => ExperimentsActions.deleteExperimentSuccess({ id })),
          catchError(error =>
            of(ExperimentsActions.deleteExperimentFailure({
              error: error.error?.message || 'Failed to delete experiment'
            }))
          )
        )
      )
    )
  );
  
  /**
   * Delete Experiment Success Effect - Navigate to experiments list
   */
  deleteExperimentSuccess$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ExperimentsActions.deleteExperimentSuccess),
      tap(() => {
        this.router.navigate(['/experiments']);
      })
    ),
    { dispatch: false }
  );
  
  /**
   * Start Experiment Effect
   */
  startExperiment$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ExperimentsActions.startExperiment),
      exhaustMap(({ id, transition }) =>
        this.experimentsService.startExperiment(id, transition).pipe(
          map(experiment => ExperimentsActions.experimentStateTransitionSuccess({ experiment })),
          catchError(error =>
            of(ExperimentsActions.experimentStateTransitionFailure({
              error: error.error?.message || 'Failed to start experiment'
            }))
          )
        )
      )
    )
  );
  
  /**
   * Pause Experiment Effect
   */
  pauseExperiment$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ExperimentsActions.pauseExperiment),
      exhaustMap(({ id, transition }) =>
        this.experimentsService.pauseExperiment(id, transition).pipe(
          map(experiment => ExperimentsActions.experimentStateTransitionSuccess({ experiment })),
          catchError(error =>
            of(ExperimentsActions.experimentStateTransitionFailure({
              error: error.error?.message || 'Failed to pause experiment'
            }))
          )
        )
      )
    )
  );
  
  /**
   * Resume Experiment Effect
   */
  resumeExperiment$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ExperimentsActions.resumeExperiment),
      exhaustMap(({ id, transition }) =>
        this.experimentsService.resumeExperiment(id, transition).pipe(
          map(experiment => ExperimentsActions.experimentStateTransitionSuccess({ experiment })),
          catchError(error =>
            of(ExperimentsActions.experimentStateTransitionFailure({
              error: error.error?.message || 'Failed to resume experiment'
            }))
          )
        )
      )
    )
  );
  
  /**
   * Complete Experiment Effect
   */
  completeExperiment$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ExperimentsActions.completeExperiment),
      exhaustMap(({ id, transition }) =>
        this.experimentsService.completeExperiment(id, transition).pipe(
          map(experiment => ExperimentsActions.experimentStateTransitionSuccess({ experiment })),
          catchError(error =>
            of(ExperimentsActions.experimentStateTransitionFailure({
              error: error.error?.message || 'Failed to complete experiment'
            }))
          )
        )
      )
    )
  );
  
  /**
   * Cancel Experiment Effect
   */
  cancelExperiment$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ExperimentsActions.cancelExperiment),
      exhaustMap(({ id, transition }) =>
        this.experimentsService.cancelExperiment(id, transition).pipe(
          map(experiment => ExperimentsActions.experimentStateTransitionSuccess({ experiment })),
          catchError(error =>
            of(ExperimentsActions.experimentStateTransitionFailure({
              error: error.error?.message || 'Failed to cancel experiment'
            }))
          )
        )
      )
    )
  );
  
  /**
   * Fail Experiment Effect
   */
  failExperiment$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ExperimentsActions.failExperiment),
      exhaustMap(({ id, transition }) =>
        this.experimentsService.failExperiment(id, transition).pipe(
          map(experiment => ExperimentsActions.experimentStateTransitionSuccess({ experiment })),
          catchError(error =>
            of(ExperimentsActions.experimentStateTransitionFailure({
              error: error.error?.message || 'Failed to mark experiment as failed'
            }))
          )
        )
      )
    )
  );
  
  constructor(
    private actions$: Actions,
    private experimentsService: ExperimentsService,
    private router: Router
  ) {}
}
