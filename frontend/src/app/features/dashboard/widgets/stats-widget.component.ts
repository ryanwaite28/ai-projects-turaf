import { Component, Input } from '@angular/core';
import { DashboardStats } from '../../../models/dashboard.model';

/**
 * Stats Widget Component
 * 
 * Displays dashboard statistics in card format.
 */
@Component({
  selector: 'app-stats-widget',
  templateUrl: './stats-widget.component.html',
  styleUrls: ['./stats-widget.component.scss']
})
export class StatsWidgetComponent {
  
  @Input() stats: DashboardStats | null = null;
  
  /**
   * Gets stat cards configuration
   */
  get statCards() {
    if (!this.stats) {
      return [];
    }
    
    return [
      {
        title: 'Total Problems',
        value: this.stats.totalProblems,
        icon: 'lightbulb',
        color: '#1976d2',
        trend: null
      },
      {
        title: 'Active Experiments',
        value: this.stats.activeExperiments,
        icon: 'science',
        color: '#388e3c',
        trend: null
      },
      {
        title: 'Completed Experiments',
        value: this.stats.completedExperiments,
        icon: 'check_circle',
        color: '#7b1fa2',
        trend: null
      },
      {
        title: 'Total Metrics',
        value: this.stats.totalMetrics,
        icon: 'analytics',
        color: '#f57c00',
        trend: null
      },
      {
        title: 'Success Rate',
        value: `${this.stats.successRate.toFixed(1)}%`,
        icon: 'trending_up',
        color: '#0097a7',
        trend: this.stats.successRate >= 70 ? 'up' : this.stats.successRate >= 50 ? 'neutral' : 'down'
      },
      {
        title: 'Avg. Duration',
        value: `${this.stats.avgExperimentDuration} days`,
        icon: 'schedule',
        color: '#5e35b1',
        trend: null
      }
    ];
  }
}
