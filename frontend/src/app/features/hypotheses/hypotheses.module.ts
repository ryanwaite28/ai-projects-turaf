import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';

// Angular Material Modules
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatMenuModule } from '@angular/material/menu';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';

import { HypothesesRoutingModule } from './hypotheses-routing.module';
import { HypothesisListComponent } from './hypothesis-list/hypothesis-list.component';
import { HypothesisFormComponent } from './hypothesis-form/hypothesis-form.component';

/**
 * Hypotheses Feature Module
 * 
 * Lazy-loaded module for hypothesis management features.
 */
@NgModule({
  declarations: [
    HypothesisListComponent,
    HypothesisFormComponent
  ],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    HypothesesRoutingModule,
    
    // Material Modules
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatPaginatorModule,
    MatProgressSpinnerModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatMenuModule,
    MatChipsModule,
    MatTooltipModule
  ]
})
export class HypothesesModule { }
