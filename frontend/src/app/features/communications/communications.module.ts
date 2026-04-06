import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';
import { CommunicationsComponent } from './communications.component';

const routes: Routes = [
  {
    path: '',
    component: CommunicationsComponent
  }
];

@NgModule({
  declarations: [
    CommunicationsComponent
  ],
  imports: [
    CommonModule,
    RouterModule.forChild(routes)
  ]
})
export class CommunicationsModule { }
