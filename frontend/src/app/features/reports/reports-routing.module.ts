import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ReportListComponent } from './report-list/report-list.component';
import { ReportPreviewComponent } from './report-preview/report-preview.component';

/**
 * Reports Routing Module
 * 
 * Defines routes for reports feature module.
 */
const routes: Routes = [
  {
    path: '',
    component: ReportListComponent,
    data: { title: 'Reports' }
  },
  {
    path: ':id',
    component: ReportPreviewComponent,
    data: { title: 'Report Preview' }
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class ReportsRoutingModule { }
