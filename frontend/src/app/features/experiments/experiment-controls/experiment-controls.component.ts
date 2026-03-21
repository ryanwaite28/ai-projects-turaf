import { Component, Input } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '../../../store/app.state';
import { 
  startExperiment,
  pauseExperiment,
  resumeExperiment,
  completeExperiment,
  cancelExperiment,
  failExperiment
} from '../../../store/experiments/experiments.actions';
import { Experiment, ExperimentStatus } from '../../../models/experiment.model';

/**
 * Experiment Controls Component
 * 
 * Provides state transition controls for experiments.
 */
@Component({
  selector: 'app-experiment-controls',
  templateUrl: './experiment-controls.component.html',
  styleUrls: ['./experiment-controls.component.scss']
})
export class ExperimentControlsComponent {
  
  @Input() experiment!: Experiment;
  
  ExperimentStatus = ExperimentStatus;
  
  constructor(private store: Store<AppState>) {}
  
  /**
   * Starts the experiment
   */
  start(): void {
    if (confirm('Start this experiment?')) {
      this.store.dispatch(startExperiment({ 
        id: this.experiment.id,
        transition: { action: 'start' }
      }));
    }
  }
  
  /**
   * Pauses the experiment
   */
  pause(): void {
    if (confirm('Pause this experiment?')) {
      this.store.dispatch(pauseExperiment({ 
        id: this.experiment.id,
        transition: { action: 'pause' }
      }));
    }
  }
  
  /**
   * Resumes the experiment
   */
  resume(): void {
    if (confirm('Resume this experiment?')) {
      this.store.dispatch(resumeExperiment({ 
        id: this.experiment.id,
        transition: { action: 'resume' }
      }));
    }
  }
  
  /**
   * Completes the experiment
   */
  complete(): void {
    if (confirm('Mark this experiment as completed?')) {
      this.store.dispatch(completeExperiment({ 
        id: this.experiment.id,
        transition: { action: 'complete' }
      }));
    }
  }
  
  /**
   * Cancels the experiment
   */
  cancel(): void {
    if (confirm('Cancel this experiment? This action cannot be undone.')) {
      this.store.dispatch(cancelExperiment({ 
        id: this.experiment.id,
        transition: { action: 'cancel' }
      }));
    }
  }
  
  /**
   * Marks experiment as failed
   */
  fail(): void {
    if (confirm('Mark this experiment as failed?')) {
      this.store.dispatch(failExperiment({ 
        id: this.experiment.id,
        transition: { action: 'fail' }
      }));
    }
  }
  
  /**
   * Checks if action is available for current status
   */
  canStart(): boolean {
    return [ExperimentStatus.DRAFT, ExperimentStatus.READY].includes(this.experiment.status);
  }
  
  canPause(): boolean {
    return this.experiment.status === ExperimentStatus.RUNNING;
  }
  
  canResume(): boolean {
    return this.experiment.status === ExperimentStatus.PAUSED;
  }
  
  canComplete(): boolean {
    return [ExperimentStatus.RUNNING, ExperimentStatus.PAUSED].includes(this.experiment.status);
  }
  
  canCancel(): boolean {
    return ![ExperimentStatus.COMPLETED, ExperimentStatus.CANCELLED, ExperimentStatus.FAILED].includes(this.experiment.status);
  }
  
  canFail(): boolean {
    return [ExperimentStatus.RUNNING, ExperimentStatus.PAUSED].includes(this.experiment.status);
  }
}
