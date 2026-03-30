import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

// Angular Material Modules
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatMenuModule } from '@angular/material/menu';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatFormFieldModule } from '@angular/material/form-field';

import { ExperimentsRoutingModule } from './experiments-routing.module';
import { ExperimentListComponent } from './experiment-list/experiment-list.component';
import { ExperimentDetailComponent } from './experiment-detail/experiment-detail.component';
import { ExperimentControlsComponent } from './experiment-controls/experiment-controls.component';

/**
 * Experiments Feature Module
 * 
 * Lazy-loaded module for experiment management features.
 */
@NgModule({
  declarations: [
    ExperimentListComponent,
    ExperimentDetailComponent,
    ExperimentControlsComponent
  ],
  imports: [
    CommonModule,
    ExperimentsRoutingModule,
    
    // Material Modules
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatPaginatorModule,
    MatProgressSpinnerModule,
    MatProgressBarModule,
    MatMenuModule,
    MatChipsModule,
    MatTooltipModule,
    MatFormFieldModule
  ]
})
export class ExperimentsModule { }
