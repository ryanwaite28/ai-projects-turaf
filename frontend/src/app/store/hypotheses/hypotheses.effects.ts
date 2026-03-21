import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { of } from 'rxjs';
import { map, catchError, exhaustMap, mergeMap, tap } from 'rxjs/operators';
import * as HypothesesActions from './hypotheses.actions';
import { HypothesesService } from '../../features/hypotheses/services/hypotheses.service';

/**
 * Hypotheses Effects
 * 
 * Handles side effects for hypotheses actions.
 */
@Injectable()
export class HypothesesEffects {
  
  /**
   * Load Hypotheses Effect
   */
  loadHypotheses$ = createEffect(() =>
    this.actions$.pipe(
      ofType(HypothesesActions.loadHypotheses),
      exhaustMap(({ params }) =>
        this.hypothesesService.getHypotheses(params).pipe(
          map(response => HypothesesActions.loadHypothesesSuccess({ response })),
          catchError(error =>
            of(HypothesesActions.loadHypothesesFailure({
              error: error.error?.message || 'Failed to load hypotheses'
            }))
          )
        )
      )
    )
  );
  
  /**
   * Load Hypotheses by Problem Effect
   */
  loadHypothesesByProblem$ = createEffect(() =>
    this.actions$.pipe(
      ofType(HypothesesActions.loadHypothesesByProblem),
      exhaustMap(({ problemId, params }) =>
        this.hypothesesService.getHypothesesByProblem(problemId, params).pipe(
          map(response => HypothesesActions.loadHypothesesSuccess({ response })),
          catchError(error =>
            of(HypothesesActions.loadHypothesesFailure({
              error: error.error?.message || 'Failed to load hypotheses for problem'
            }))
          )
        )
      )
    )
  );
  
  /**
   * Load Hypothesis Effect
   */
  loadHypothesis$ = createEffect(() =>
    this.actions$.pipe(
      ofType(HypothesesActions.loadHypothesis),
      mergeMap(({ id }) =>
        this.hypothesesService.getHypothesis(id).pipe(
          map(hypothesis => HypothesesActions.loadHypothesisSuccess({ hypothesis })),
          catchError(error =>
            of(HypothesesActions.loadHypothesisFailure({
              error: error.error?.message || 'Failed to load hypothesis'
            }))
          )
        )
      )
    )
  );
  
  /**
   * Create Hypothesis Effect
   */
  createHypothesis$ = createEffect(() =>
    this.actions$.pipe(
      ofType(HypothesesActions.createHypothesis),
      exhaustMap(({ request }) =>
        this.hypothesesService.createHypothesis(request).pipe(
          map(hypothesis => HypothesesActions.createHypothesisSuccess({ hypothesis })),
          catchError(error =>
            of(HypothesesActions.createHypothesisFailure({
              error: error.error?.message || 'Failed to create hypothesis'
            }))
          )
        )
      )
    )
  );
  
  /**
   * Create Hypothesis Success Effect - Navigate to hypothesis list
   */
  createHypothesisSuccess$ = createEffect(() =>
    this.actions$.pipe(
      ofType(HypothesesActions.createHypothesisSuccess),
      tap(({ hypothesis }) => {
        this.router.navigate(['/hypotheses']);
      })
    ),
    { dispatch: false }
  );
  
  /**
   * Update Hypothesis Effect
   */
  updateHypothesis$ = createEffect(() =>
    this.actions$.pipe(
      ofType(HypothesesActions.updateHypothesis),
      exhaustMap(({ id, request }) =>
        this.hypothesesService.updateHypothesis(id, request).pipe(
          map(hypothesis => HypothesesActions.updateHypothesisSuccess({ hypothesis })),
          catchError(error =>
            of(HypothesesActions.updateHypothesisFailure({
              error: error.error?.message || 'Failed to update hypothesis'
            }))
          )
        )
      )
    )
  );
  
  /**
   * Delete Hypothesis Effect
   */
  deleteHypothesis$ = createEffect(() =>
    this.actions$.pipe(
      ofType(HypothesesActions.deleteHypothesis),
      exhaustMap(({ id }) =>
        this.hypothesesService.deleteHypothesis(id).pipe(
          map(() => HypothesesActions.deleteHypothesisSuccess({ id })),
          catchError(error =>
            of(HypothesesActions.deleteHypothesisFailure({
              error: error.error?.message || 'Failed to delete hypothesis'
            }))
          )
        )
      )
    )
  );
  
  /**
   * Delete Hypothesis Success Effect - Navigate to hypotheses list
   */
  deleteHypothesisSuccess$ = createEffect(() =>
    this.actions$.pipe(
      ofType(HypothesesActions.deleteHypothesisSuccess),
      tap(() => {
        this.router.navigate(['/hypotheses']);
      })
    ),
    { dispatch: false }
  );
  
  constructor(
    private actions$: Actions,
    private hypothesesService: HypothesesService,
    private router: Router
  ) {}
}
