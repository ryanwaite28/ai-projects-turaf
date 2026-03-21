import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ProblemListComponent } from './problem-list/problem-list.component';
import { ProblemDetailComponent } from './problem-detail/problem-detail.component';
import { ProblemFormComponent } from './problem-form/problem-form.component';

/**
 * Problems Routing Module
 * 
 * Defines routes for problems feature module.
 */
const routes: Routes = [
  {
    path: '',
    component: ProblemListComponent,
    data: { title: 'Problems' }
  },
  {
    path: 'new',
    component: ProblemFormComponent,
    data: { title: 'Create Problem' }
  },
  {
    path: ':id',
    component: ProblemDetailComponent,
    data: { title: 'Problem Details' }
  },
  {
    path: ':id/edit',
    component: ProblemFormComponent,
    data: { title: 'Edit Problem' }
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class ProblemsRoutingModule { }
