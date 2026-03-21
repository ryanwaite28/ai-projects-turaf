import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

// Angular Material Modules
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatTooltipModule } from '@angular/material/tooltip';

import { DashboardRoutingModule } from './dashboard-routing.module';
import { DashboardComponent } from './dashboard.component';
import { StatsWidgetComponent } from './widgets/stats-widget.component';
import { RecentExperimentsComponent } from './widgets/recent-experiments.component';
import { MetricsChartComponent } from './widgets/metrics-chart.component';

/**
 * Dashboard Feature Module
 * 
 * Lazy-loaded module for dashboard features.
 */
@NgModule({
  declarations: [
    DashboardComponent,
    StatsWidgetComponent,
    RecentExperimentsComponent,
    MetricsChartComponent
  ],
  imports: [
    CommonModule,
    DashboardRoutingModule,
    
    // Material Modules
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatProgressBarModule,
    MatButtonToggleModule,
    MatTooltipModule
  ]
})
export class DashboardModule { }
