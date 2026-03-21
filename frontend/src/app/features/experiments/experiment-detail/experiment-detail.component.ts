import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { Subject } from 'rxjs';
import { takeUntil, filter } from 'rxjs/operators';
import { AppState } from '../../../store/app.state';
import { 
  loadExperiment,
  deleteExperiment
} from '../../../store/experiments/experiments.actions';
import {
  selectSelectedExperiment,
  selectExperimentsLoading,
  selectExperimentsError
} from '../../../store/experiments/experiments.selectors';
import { Experiment } from '../../../models/experiment.model';

/**
 * Experiment Detail Component
 * 
 * Displays detailed information about a single experiment.
 */
@Component({
  selector: 'app-experiment-detail',
  templateUrl: './experiment-detail.component.html',
  styleUrls: ['./experiment-detail.component.scss']
})
export class ExperimentDetailComponent implements OnInit, OnDestroy {
  
  experiment$ = this.store.select(selectSelectedExperiment);
  loading$ = this.store.select(selectExperimentsLoading);
  error$ = this.store.select(selectExperimentsError);
  
  private destroy$ = new Subject<void>();
  private experimentId: string | null = null;
  
  constructor(
    private store: Store<AppState>,
    private route: ActivatedRoute,
    private router: Router
  ) {}
  
  ngOnInit(): void {
    this.route.params
      .pipe(takeUntil(this.destroy$))
      .subscribe(params => {
        this.experimentId = params['id'];
        if (this.experimentId) {
          this.store.dispatch(loadExperiment({ id: this.experimentId }));
        }
      });
  }
  
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
  
  /**
   * Navigates back to experiments list
   */
  goBack(): void {
    this.router.navigate(['/experiments']);
  }
  
  /**
   * Deletes the experiment
   */
  deleteExperiment(experiment: Experiment): void {
    if (confirm(`Are you sure you want to delete experiment "${experiment.name}"?`)) {
      this.store.dispatch(deleteExperiment({ id: experiment.id }));
    }
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
}
