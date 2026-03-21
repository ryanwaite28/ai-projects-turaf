import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ExperimentListComponent } from './experiment-list/experiment-list.component';
import { ExperimentDetailComponent } from './experiment-detail/experiment-detail.component';

/**
 * Experiments Routing Module
 * 
 * Defines routes for experiments feature module.
 */
const routes: Routes = [
  {
    path: '',
    component: ExperimentListComponent,
    data: { title: 'Experiments' }
  },
  {
    path: ':id',
    component: ExperimentDetailComponent,
    data: { title: 'Experiment Details' }
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class ExperimentsRoutingModule { }
