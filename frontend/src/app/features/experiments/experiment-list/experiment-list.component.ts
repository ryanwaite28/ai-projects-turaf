import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { Store } from '@ngrx/store';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { AppState } from '../../../store/app.state';
import { 
  loadExperiments,
  loadExperimentsByHypothesis,
  deleteExperiment,
  setExperimentsFilters
} from '../../../store/experiments/experiments.actions';
import {
  selectAllExperiments,
  selectExperimentsLoading,
  selectExperimentsError,
  selectExperimentsPagination,
  selectExperimentsFilters,
  selectExperimentsStats
} from '../../../store/experiments/experiments.selectors';
import { Experiment, ExperimentStatus } from '../../../models/experiment.model';

/**
 * Experiment List Component
 * 
 * Displays paginated list of experiments with filtering by hypothesis and status.
 */
@Component({
  selector: 'app-experiment-list',
  templateUrl: './experiment-list.component.html',
  styleUrls: ['./experiment-list.component.scss']
})
export class ExperimentListComponent implements OnInit, OnDestroy {
  
  experiments$ = this.store.select(selectAllExperiments);
  loading$ = this.store.select(selectExperimentsLoading);
  error$ = this.store.select(selectExperimentsError);
  pagination$ = this.store.select(selectExperimentsPagination);
  filters$ = this.store.select(selectExperimentsFilters);
  stats$ = this.store.select(selectExperimentsStats);
  
  displayedColumns: string[] = ['name', 'hypothesis', 'status', 'startDate', 'metrics', 'actions'];
  
  statusOptions = Object.values(ExperimentStatus);
  selectedStatus: ExperimentStatus | null = null;
  
  private destroy$ = new Subject<void>();
  private hypothesisId: string | null = null;
  
  constructor(
    private store: Store<AppState>,
    private router: Router,
    private route: ActivatedRoute
  ) {}
  
  ngOnInit(): void {
    this.route.queryParams
      .pipe(takeUntil(this.destroy$))
      .subscribe(params => {
        this.hypothesisId = params['hypothesisId'] || null;
        this.selectedStatus = params['status'] || null;
        this.loadExperiments();
      });
  }
  
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
  
  /**
   * Loads experiments from the store
   */
  loadExperiments(page: number = 1): void {
    const params: any = { page, limit: 10 };
    
    if (this.selectedStatus) {
      params.status = this.selectedStatus;
    }
    
    if (this.hypothesisId) {
      this.store.dispatch(loadExperimentsByHypothesis({ 
        hypothesisId: this.hypothesisId,
        params
      }));
    } else {
      this.store.dispatch(loadExperiments({ params }));
    }
  }
  
  /**
   * Filters experiments by status
   */
  filterByStatus(status: ExperimentStatus | null): void {
    this.selectedStatus = status;
    const queryParams: any = {};
    
    if (this.hypothesisId) {
      queryParams.hypothesisId = this.hypothesisId;
    }
    
    if (status) {
      queryParams.status = status;
    }
    
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams,
      queryParamsHandling: 'merge'
    });
  }
  
  /**
   * Navigates to create experiment page
   */
  createExperiment(): void {
    const queryParams = this.hypothesisId ? { hypothesisId: this.hypothesisId } : {};
    this.router.navigate(['/experiments/new'], { queryParams });
  }
  
  /**
   * Navigates to experiment detail page
   */
  viewExperiment(experiment: Experiment): void {
    this.router.navigate(['/experiments', experiment.id]);
  }
  
  /**
   * Deletes an experiment
   */
  deleteExperiment(experiment: Experiment, event: Event): void {
    event.stopPropagation();
    if (confirm(`Are you sure you want to delete experiment "${experiment.name}"?`)) {
      this.store.dispatch(deleteExperiment({ id: experiment.id }));
    }
  }
  
  /**
   * Handles page change
   */
  onPageChange(page: number): void {
    this.loadExperiments(page);
  }
  
  /**
   * Gets status badge class
   */
  getStatusClass(status: string): string {
    const statusMap: Record<string, string> = {
      'DRAFT': 'draft',
      'READY': 'ready',
      'RUNNING': 'running',
      'PAUSED': 'paused',
      'COMPLETED': 'completed',
      'FAILED': 'failed',
      'CANCELLED': 'cancelled'
    };
    return statusMap[status] || 'draft';
  }
  
  /**
   * Gets status icon
   */
  getStatusIcon(status: string): string {
    const iconMap: Record<string, string> = {
      'DRAFT': 'edit',
      'READY': 'check_circle',
      'RUNNING': 'play_circle',
      'PAUSED': 'pause_circle',
      'COMPLETED': 'done_all',
      'FAILED': 'error',
      'CANCELLED': 'cancel'
    };
    return iconMap[status] || 'help';
  }
  
  /**
   * Formats success rate
   */
  getSuccessRate(experiment: Experiment): string {
    if (!experiment.metrics?.successRate) {
      return 'N/A';
    }
    return `${(experiment.metrics.successRate * 100).toFixed(1)}%`;
  }
  
  /**
   * Gets duration in days
   */
  getDuration(experiment: Experiment): string {
    if (!experiment.startDate) {
      return 'Not started';
    }
    
    const start = new Date(experiment.startDate);
    const end = experiment.completedAt ? new Date(experiment.completedAt) : new Date();
    const days = Math.floor((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24));
    
    return `${days} day${days !== 1 ? 's' : ''}`;
  }
}
