import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { of } from 'rxjs';
import { map, catchError, exhaustMap, mergeMap, tap } from 'rxjs/operators';
import * as ProblemsActions from './problems.actions';
import { ProblemsService } from '../../features/problems/services/problems.service';

/**
 * Problems Effects
 * 
 * Handles side effects for problems actions.
 */
@Injectable()
export class ProblemsEffects {
  
  /**
   * Load Problems Effect
   */
  loadProblems$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ProblemsActions.loadProblems),
      exhaustMap(({ params }) =>
        this.problemsService.getProblems(params).pipe(
          map(response => ProblemsActions.loadProblemsSuccess({ response })),
          catchError(error =>
            of(ProblemsActions.loadProblemsFailure({
              error: error.error?.message || 'Failed to load problems'
            }))
          )
        )
      )
    )
  );
  
  /**
   * Load Problem Effect
   */
  loadProblem$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ProblemsActions.loadProblem),
      mergeMap(({ id }) =>
        this.problemsService.getProblem(id).pipe(
          map(problem => ProblemsActions.loadProblemSuccess({ problem })),
          catchError(error =>
            of(ProblemsActions.loadProblemFailure({
              error: error.error?.message || 'Failed to load problem'
            }))
          )
        )
      )
    )
  );
  
  /**
   * Create Problem Effect
   */
  createProblem$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ProblemsActions.createProblem),
      exhaustMap(({ request }) =>
        this.problemsService.createProblem(request).pipe(
          map(problem => ProblemsActions.createProblemSuccess({ problem })),
          catchError(error =>
            of(ProblemsActions.createProblemFailure({
              error: error.error?.message || 'Failed to create problem'
            }))
          )
        )
      )
    )
  );
  
  /**
   * Create Problem Success Effect - Navigate to problem detail
   */
  createProblemSuccess$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ProblemsActions.createProblemSuccess),
      tap(({ problem }) => {
        this.router.navigate(['/problems', problem.id]);
      })
    ),
    { dispatch: false }
  );
  
  /**
   * Update Problem Effect
   */
  updateProblem$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ProblemsActions.updateProblem),
      exhaustMap(({ id, request }) =>
        this.problemsService.updateProblem(id, request).pipe(
          map(problem => ProblemsActions.updateProblemSuccess({ problem })),
          catchError(error =>
            of(ProblemsActions.updateProblemFailure({
              error: error.error?.message || 'Failed to update problem'
            }))
          )
        )
      )
    )
  );
  
  /**
   * Delete Problem Effect
   */
  deleteProblem$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ProblemsActions.deleteProblem),
      exhaustMap(({ id }) =>
        this.problemsService.deleteProblem(id).pipe(
          map(() => ProblemsActions.deleteProblemSuccess({ id })),
          catchError(error =>
            of(ProblemsActions.deleteProblemFailure({
              error: error.error?.message || 'Failed to delete problem'
            }))
          )
        )
      )
    )
  );
  
  /**
   * Delete Problem Success Effect - Navigate to problems list
   */
  deleteProblemSuccess$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ProblemsActions.deleteProblemSuccess),
      tap(() => {
        this.router.navigate(['/problems']);
      })
    ),
    { dispatch: false }
  );
  
  constructor(
    private actions$: Actions,
    private problemsService: ProblemsService,
    private router: Router
  ) {}
}
