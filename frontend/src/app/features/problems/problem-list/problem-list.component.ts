import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { AppState } from '../../../store/app.state';
import { 
  loadProblems 
} from '../../../store/problems/problems.actions';
import {
  selectAllProblems,
  selectProblemsLoading,
  selectProblemsError,
  selectProblemsPagination
} from '../../../store/problems/problems.selectors';
import { Problem } from '../../../models/problem.model';

/**
 * Problem List Component
 * 
 * Displays paginated list of problems with search and filtering.
 */
@Component({
  selector: 'app-problem-list',
  templateUrl: './problem-list.component.html',
  styleUrls: ['./problem-list.component.scss']
})
export class ProblemListComponent implements OnInit, OnDestroy {
  
  problems$ = this.store.select(selectAllProblems);
  loading$ = this.store.select(selectProblemsLoading);
  error$ = this.store.select(selectProblemsError);
  pagination$ = this.store.select(selectProblemsPagination);
  
  displayedColumns: string[] = ['title', 'description', 'status', 'createdAt', 'actions'];
  
  private destroy$ = new Subject<void>();
  
  constructor(
    private store: Store<AppState>,
    private router: Router
  ) {}
  
  ngOnInit(): void {
    this.loadProblems();
  }
  
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
  
  /**
   * Loads problems from the store
   */
  loadProblems(page: number = 1): void {
    this.store.dispatch(loadProblems({ params: { page, limit: 10 } }));
  }
  
  /**
   * Navigates to create problem page
   */
  createProblem(): void {
    this.router.navigate(['/problems/new']);
  }
  
  /**
   * Navigates to problem detail page
   */
  viewProblem(problem: Problem): void {
    this.router.navigate(['/problems', problem.id]);
  }
  
  /**
   * Navigates to edit problem page
   */
  editProblem(problem: Problem, event: Event): void {
    event.stopPropagation();
    this.router.navigate(['/problems', problem.id, 'edit']);
  }
  
  /**
   * Handles page change
   */
  onPageChange(page: number): void {
    this.loadProblems(page);
  }
}
