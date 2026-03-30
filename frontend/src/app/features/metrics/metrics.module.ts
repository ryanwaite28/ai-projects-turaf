import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

// Angular Material Modules
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatFormFieldModule } from '@angular/material/form-field';

import { MetricsRoutingModule } from './metrics-routing.module';
import { MetricsDashboardComponent } from './metrics-dashboard/metrics-dashboard.component';
import { MetricsChartComponent } from './metrics-chart/metrics-chart.component';
import { MetricsTableComponent } from './metrics-table/metrics-table.component';

/**
 * Metrics Feature Module
 * 
 * Lazy-loaded module for metrics visualization and management.
 */
@NgModule({
  declarations: [
    MetricsDashboardComponent,
    MetricsChartComponent,
    MetricsTableComponent
  ],
  imports: [
    CommonModule,
    MetricsRoutingModule,
    
    // Material Modules
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatSlideToggleModule,
    MatFormFieldModule
  ]
})
export class MetricsModule { }
