import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { MetricsDashboardComponent } from './metrics-dashboard/metrics-dashboard.component';

/**
 * Metrics Routing Module
 * 
 * Defines routes for metrics feature module.
 */
const routes: Routes = [
  {
    path: '',
    component: MetricsDashboardComponent,
    data: { title: 'Metrics Dashboard' }
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class MetricsRoutingModule { }
