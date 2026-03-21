import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { Store } from '@ngrx/store';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { AppState } from '../../../store/app.state';
import { 
  loadHypotheses,
  loadHypothesesByProblem,
  deleteHypothesis
} from '../../../store/hypotheses/hypotheses.actions';
import {
  selectAllHypotheses,
  selectHypothesesLoading,
  selectHypothesesError,
  selectHypothesesPagination,
  selectHypothesesFilters
} from '../../../store/hypotheses/hypotheses.selectors';
import { Hypothesis } from '../../../models/hypothesis.model';

/**
 * Hypothesis List Component
 * 
 * Displays paginated list of hypotheses with filtering by problem.
 */
@Component({
  selector: 'app-hypothesis-list',
  templateUrl: './hypothesis-list.component.html',
  styleUrls: ['./hypothesis-list.component.scss']
})
export class HypothesisListComponent implements OnInit, OnDestroy {
  
  hypotheses$ = this.store.select(selectAllHypotheses);
  loading$ = this.store.select(selectHypothesesLoading);
  error$ = this.store.select(selectHypothesesError);
  pagination$ = this.store.select(selectHypothesesPagination);
  filters$ = this.store.select(selectHypothesesFilters);
  
  displayedColumns: string[] = ['title', 'problem', 'expectedOutcome', 'status', 'createdAt', 'actions'];
  
  private destroy$ = new Subject<void>();
  private problemId: string | null = null;
  
  constructor(
    private store: Store<AppState>,
    private router: Router,
    private route: ActivatedRoute
  ) {}
  
  ngOnInit(): void {
    this.route.queryParams
      .pipe(takeUntil(this.destroy$))
      .subscribe(params => {
        this.problemId = params['problemId'] || null;
        this.loadHypotheses();
      });
  }
  
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
  
  /**
   * Loads hypotheses from the store
   */
  loadHypotheses(page: number = 1): void {
    if (this.problemId) {
      this.store.dispatch(loadHypothesesByProblem({ 
        problemId: this.problemId,
        params: { page, limit: 10 }
      }));
    } else {
      this.store.dispatch(loadHypotheses({ params: { page, limit: 10 } }));
    }
  }
  
  /**
   * Navigates to create hypothesis page
   */
  createHypothesis(): void {
    const queryParams = this.problemId ? { problemId: this.problemId } : {};
    this.router.navigate(['/hypotheses/new'], { queryParams });
  }
  
  /**
   * Navigates to edit hypothesis page
   */
  editHypothesis(hypothesis: Hypothesis, event: Event): void {
    event.stopPropagation();
    this.router.navigate(['/hypotheses', hypothesis.id, 'edit']);
  }
  
  /**
   * Deletes a hypothesis
   */
  deleteHypothesis(hypothesis: Hypothesis, event: Event): void {
    event.stopPropagation();
    if (confirm(`Are you sure you want to delete hypothesis "${hypothesis.title}"?`)) {
      this.store.dispatch(deleteHypothesis({ id: hypothesis.id }));
    }
  }
  
  /**
   * Navigates to view experiments for hypothesis
   */
  viewExperiments(hypothesis: Hypothesis, event: Event): void {
    event.stopPropagation();
    this.router.navigate(['/experiments'], { 
      queryParams: { hypothesisId: hypothesis.id } 
    });
  }
  
  /**
   * Handles page change
   */
  onPageChange(page: number): void {
    this.loadHypotheses(page);
  }
  
  /**
   * Gets status badge class
   */
  getStatusClass(status: string): string {
    const statusMap: Record<string, string> = {
      'DRAFT': 'draft',
      'ACTIVE': 'active',
      'VALIDATED': 'validated',
      'INVALIDATED': 'invalidated',
      'ARCHIVED': 'archived'
    };
    return statusMap[status] || 'draft';
  }
}
