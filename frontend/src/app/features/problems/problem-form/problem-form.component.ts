import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { Subject } from 'rxjs';
import { takeUntil, filter } from 'rxjs/operators';
import { AppState } from '../../../store/app.state';
import { 
  createProblem,
  updateProblem,
  loadProblem
} from '../../../store/problems/problems.actions';
import {
  selectSelectedProblem,
  selectProblemsLoading,
  selectProblemsError
} from '../../../store/problems/problems.selectors';
import { ProblemStatus } from '../../../models/problem.model';

/**
 * Problem Form Component
 * 
 * Form for creating and editing problems.
 */
@Component({
  selector: 'app-problem-form',
  templateUrl: './problem-form.component.html',
  styleUrls: ['./problem-form.component.scss']
})
export class ProblemFormComponent implements OnInit, OnDestroy {
  
  problemForm: FormGroup;
  loading$ = this.store.select(selectProblemsLoading);
  error$ = this.store.select(selectProblemsError);
  
  isEditMode = false;
  problemId: string | null = null;
  
  statusOptions = Object.values(ProblemStatus);
  
  private destroy$ = new Subject<void>();
  
  constructor(
    private fb: FormBuilder,
    private store: Store<AppState>,
    private route: ActivatedRoute,
    private router: Router
  ) {
    this.problemForm = this.createForm();
  }
  
  ngOnInit(): void {
    this.route.params
      .pipe(takeUntil(this.destroy$))
      .subscribe(params => {
        this.problemId = params['id'];
        this.isEditMode = !!this.problemId && this.problemId !== 'new';
        
        if (this.isEditMode && this.problemId) {
          this.store.dispatch(loadProblem({ id: this.problemId }));
          this.loadProblemData();
        }
      });
  }
  
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
  
  /**
   * Creates the form
   */
  private createForm(): FormGroup {
    return this.fb.group({
      title: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(200)]],
      description: ['', [Validators.required, Validators.minLength(10)]],
      status: [ProblemStatus.DRAFT, Validators.required],
      tags: ['']
    });
  }
  
  /**
   * Loads problem data into form
   */
  private loadProblemData(): void {
    this.store.select(selectSelectedProblem)
      .pipe(
        takeUntil(this.destroy$),
        filter(problem => !!problem)
      )
      .subscribe(problem => {
        if (problem) {
          this.problemForm.patchValue({
            title: problem.title,
            description: problem.description,
            status: problem.status,
            tags: problem.tags?.join(', ') || ''
          });
        }
      });
  }
  
  /**
   * Handles form submission
   */
  onSubmit(): void {
    if (this.problemForm.invalid) {
      this.problemForm.markAllAsTouched();
      return;
    }
    
    const formValue = this.problemForm.value;
    const tags = formValue.tags
      ? formValue.tags.split(',').map((tag: string) => tag.trim()).filter((tag: string) => tag)
      : [];
    
    const problemData = {
      title: formValue.title,
      description: formValue.description,
      status: formValue.status,
      tags
    };
    
    if (this.isEditMode && this.problemId) {
      this.store.dispatch(updateProblem({ 
        id: this.problemId, 
        request: problemData 
      }));
    } else {
      this.store.dispatch(createProblem({ request: problemData }));
    }
  }
  
  /**
   * Cancels form and navigates back
   */
  onCancel(): void {
    if (this.isEditMode && this.problemId) {
      this.router.navigate(['/problems', this.problemId]);
    } else {
      this.router.navigate(['/problems']);
    }
  }
  
  /**
   * Gets form control error message
   */
  getErrorMessage(controlName: string): string {
    const control = this.problemForm.get(controlName);
    
    if (!control || !control.errors || !control.touched) {
      return '';
    }
    
    if (control.errors['required']) {
      return `${controlName.charAt(0).toUpperCase() + controlName.slice(1)} is required`;
    }
    
    if (control.errors['minlength']) {
      return `Minimum length is ${control.errors['minlength'].requiredLength} characters`;
    }
    
    if (control.errors['maxlength']) {
      return `Maximum length is ${control.errors['maxlength'].requiredLength} characters`;
    }
    
    return 'Invalid value';
  }
}
