import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { Subject } from 'rxjs';
import { takeUntil, filter } from 'rxjs/operators';
import { AppState } from '../../../store/app.state';
import { 
  loadProblem,
  deleteProblem
} from '../../../store/problems/problems.actions';
import {
  selectSelectedProblem,
  selectProblemsLoading,
  selectProblemsError
} from '../../../store/problems/problems.selectors';

/**
 * Problem Detail Component
 * 
 * Displays detailed information about a single problem.
 */
@Component({
  selector: 'app-problem-detail',
  templateUrl: './problem-detail.component.html',
  styleUrls: ['./problem-detail.component.scss']
})
export class ProblemDetailComponent implements OnInit, OnDestroy {
  
  problem$ = this.store.select(selectSelectedProblem);
  loading$ = this.store.select(selectProblemsLoading);
  error$ = this.store.select(selectProblemsError);
  
  private destroy$ = new Subject<void>();
  private problemId: string = '';
  
  constructor(
    private store: Store<AppState>,
    private route: ActivatedRoute,
    private router: Router
  ) {}
  
  ngOnInit(): void {
    this.route.params
      .pipe(takeUntil(this.destroy$))
      .subscribe(params => {
        this.problemId = params['id'];
        if (this.problemId) {
          this.store.dispatch(loadProblem({ id: this.problemId }));
        }
      });
  }
  
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
  
  /**
   * Navigates to edit problem page
   */
  editProblem(): void {
    this.router.navigate(['/problems', this.problemId, 'edit']);
  }
  
  /**
   * Deletes the problem
   */
  deleteProblem(): void {
    if (confirm('Are you sure you want to delete this problem?')) {
      this.store.dispatch(deleteProblem({ id: this.problemId }));
    }
  }
  
  /**
   * Navigates back to problems list
   */
  goBack(): void {
    this.router.navigate(['/problems']);
  }
}
