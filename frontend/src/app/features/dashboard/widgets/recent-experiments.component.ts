import { Component, Input } from '@angular/core';
import { Router } from '@angular/router';
import { RecentExperiment, ExperimentStatus } from '../../../models/dashboard.model';

/**
 * Recent Experiments Widget Component
 * 
 * Displays list of recent experiments.
 */
@Component({
  selector: 'app-recent-experiments',
  templateUrl: './recent-experiments.component.html',
  styleUrls: ['./recent-experiments.component.scss']
})
export class RecentExperimentsComponent {
  
  @Input() experiments: RecentExperiment[] | null = null;
  
  ExperimentStatus = ExperimentStatus;
  
  constructor(private router: Router) {}
  
  /**
   * Gets status badge color
   */
  getStatusColor(status: ExperimentStatus): string {
    const colors: Record<ExperimentStatus, string> = {
      [ExperimentStatus.DRAFT]: '#9e9e9e',
      [ExperimentStatus.RUNNING]: '#2196f3',
      [ExperimentStatus.PAUSED]: '#ff9800',
      [ExperimentStatus.COMPLETED]: '#4caf50',
      [ExperimentStatus.CANCELLED]: '#f44336'
    };
    
    return colors[status] || '#9e9e9e';
  }
  
  /**
   * Gets status icon
   */
  getStatusIcon(status: ExperimentStatus): string {
    const icons: Record<ExperimentStatus, string> = {
      [ExperimentStatus.DRAFT]: 'edit',
      [ExperimentStatus.RUNNING]: 'play_circle',
      [ExperimentStatus.PAUSED]: 'pause_circle',
      [ExperimentStatus.COMPLETED]: 'check_circle',
      [ExperimentStatus.CANCELLED]: 'cancel'
    };
    
    return icons[status] || 'help';
  }
  
  /**
   * Calculates success percentage
   */
  getSuccessPercentage(experiment: RecentExperiment): number {
    if (experiment.totalMetrics === 0) {
      return 0;
    }
    return (experiment.successMetrics / experiment.totalMetrics) * 100;
  }
  
  /**
   * Navigates to experiment details
   */
  viewExperiment(experimentId: string): void {
    this.router.navigate(['/experiments', experimentId]);
  }
  
  /**
   * Navigates to all experiments
   */
  viewAllExperiments(): void {
    this.router.navigate(['/experiments']);
  }
}
