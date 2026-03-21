import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { Subject } from 'rxjs';
import { takeUntil, filter } from 'rxjs/operators';
import { AppState } from '../../../store/app.state';
import { 
  createHypothesis,
  updateHypothesis,
  loadHypothesis
} from '../../../store/hypotheses/hypotheses.actions';
import { loadProblems } from '../../../store/problems/problems.actions';
import {
  selectSelectedHypothesis,
  selectHypothesesLoading,
  selectHypothesesError
} from '../../../store/hypotheses/hypotheses.selectors';
import { selectAllProblems } from '../../../store/problems/problems.selectors';
import { HypothesisStatus } from '../../../models/hypothesis.model';

/**
 * Hypothesis Form Component
 * 
 * Form for creating and editing hypotheses.
 */
@Component({
  selector: 'app-hypothesis-form',
  templateUrl: './hypothesis-form.component.html',
  styleUrls: ['./hypothesis-form.component.scss']
})
export class HypothesisFormComponent implements OnInit, OnDestroy {
  
  hypothesisForm: FormGroup;
  loading$ = this.store.select(selectHypothesesLoading);
  error$ = this.store.select(selectHypothesesError);
  problems$ = this.store.select(selectAllProblems);
  
  isEditMode = false;
  hypothesisId: string | null = null;
  preselectedProblemId: string | null = null;
  
  statusOptions = Object.values(HypothesisStatus);
  
  private destroy$ = new Subject<void>();
  
  constructor(
    private fb: FormBuilder,
    private store: Store<AppState>,
    private route: ActivatedRoute,
    private router: Router
  ) {
    this.hypothesisForm = this.createForm();
  }
  
  ngOnInit(): void {
    // Load problems for dropdown
    this.store.dispatch(loadProblems({ params: { limit: 100 } }));
    
    // Check for preselected problem from query params
    this.route.queryParams
      .pipe(takeUntil(this.destroy$))
      .subscribe(params => {
        this.preselectedProblemId = params['problemId'] || null;
        if (this.preselectedProblemId) {
          this.hypothesisForm.patchValue({ problemId: this.preselectedProblemId });
        }
      });
    
    // Check if edit mode
    this.route.params
      .pipe(takeUntil(this.destroy$))
      .subscribe(params => {
        this.hypothesisId = params['id'];
        this.isEditMode = !!this.hypothesisId && this.hypothesisId !== 'new';
        
        if (this.isEditMode && this.hypothesisId) {
          this.store.dispatch(loadHypothesis({ id: this.hypothesisId }));
          this.loadHypothesisData();
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
      problemId: ['', Validators.required],
      title: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(200)]],
      description: ['', [Validators.required, Validators.minLength(10)]],
      expectedOutcome: ['', [Validators.required, Validators.minLength(10)]],
      successCriteria: ['', [Validators.required, Validators.minLength(10)]],
      status: [HypothesisStatus.DRAFT, Validators.required],
      tags: ['']
    });
  }
  
  /**
   * Loads hypothesis data into form
   */
  private loadHypothesisData(): void {
    this.store.select(selectSelectedHypothesis)
      .pipe(
        takeUntil(this.destroy$),
        filter(hypothesis => !!hypothesis)
      )
      .subscribe(hypothesis => {
        if (hypothesis) {
          this.hypothesisForm.patchValue({
            problemId: hypothesis.problemId,
            title: hypothesis.title,
            description: hypothesis.description,
            expectedOutcome: hypothesis.expectedOutcome,
            successCriteria: hypothesis.successCriteria,
            status: hypothesis.status,
            tags: hypothesis.tags?.join(', ') || ''
          });
        }
      });
  }
  
  /**
   * Handles form submission
   */
  onSubmit(): void {
    if (this.hypothesisForm.invalid) {
      this.hypothesisForm.markAllAsTouched();
      return;
    }
    
    const formValue = this.hypothesisForm.value;
    const tags = formValue.tags
      ? formValue.tags.split(',').map((tag: string) => tag.trim()).filter((tag: string) => tag)
      : [];
    
    const hypothesisData = {
      problemId: formValue.problemId,
      title: formValue.title,
      description: formValue.description,
      expectedOutcome: formValue.expectedOutcome,
      successCriteria: formValue.successCriteria,
      status: formValue.status,
      tags
    };
    
    if (this.isEditMode && this.hypothesisId) {
      const { problemId, ...updateData } = hypothesisData;
      this.store.dispatch(updateHypothesis({ 
        id: this.hypothesisId, 
        request: updateData 
      }));
    } else {
      this.store.dispatch(createHypothesis({ request: hypothesisData }));
    }
  }
  
  /**
   * Cancels form and navigates back
   */
  onCancel(): void {
    this.router.navigate(['/hypotheses']);
  }
  
  /**
   * Gets form control error message
   */
  getErrorMessage(controlName: string): string {
    const control = this.hypothesisForm.get(controlName);
    
    if (!control || !control.errors || !control.touched) {
      return '';
    }
    
    if (control.errors['required']) {
      return `${this.getFieldLabel(controlName)} is required`;
    }
    
    if (control.errors['minlength']) {
      return `Minimum length is ${control.errors['minlength'].requiredLength} characters`;
    }
    
    if (control.errors['maxlength']) {
      return `Maximum length is ${control.errors['maxlength'].requiredLength} characters`;
    }
    
    return 'Invalid value';
  }
  
  /**
   * Gets user-friendly field label
   */
  private getFieldLabel(controlName: string): string {
    const labels: Record<string, string> = {
      problemId: 'Problem',
      title: 'Title',
      description: 'Description',
      expectedOutcome: 'Expected Outcome',
      successCriteria: 'Success Criteria',
      status: 'Status',
      tags: 'Tags'
    };
    return labels[controlName] || controlName;
  }
}
