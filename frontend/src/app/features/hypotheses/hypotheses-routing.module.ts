import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { HypothesisListComponent } from './hypothesis-list/hypothesis-list.component';
import { HypothesisFormComponent } from './hypothesis-form/hypothesis-form.component';

/**
 * Hypotheses Routing Module
 * 
 * Defines routes for hypotheses feature module.
 */
const routes: Routes = [
  {
    path: '',
    component: HypothesisListComponent,
    data: { title: 'Hypotheses' }
  },
  {
    path: 'new',
    component: HypothesisFormComponent,
    data: { title: 'Create Hypothesis' }
  },
  {
    path: ':id/edit',
    component: HypothesisFormComponent,
    data: { title: 'Edit Hypothesis' }
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class HypothesesRoutingModule { }
