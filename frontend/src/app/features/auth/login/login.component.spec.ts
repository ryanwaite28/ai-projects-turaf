import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideMockStore, MockStore } from '@ngrx/store/testing';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { LoginComponent } from './login.component';
import { AppState } from '../../../store/app.state';
import { login, clearAuthError } from '../../../store/auth/auth.actions';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let store: MockStore<AppState>;
  
  const initialState = {
    auth: {
      user: null,
      token: null,
      loading: false,
      error: null
    }
  };
  
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ LoginComponent ],
      imports: [
        ReactiveFormsModule,
        NoopAnimationsModule,
        MatCardModule,
        MatFormFieldModule,
        MatInputModule,
        MatButtonModule,
        MatIconModule,
        MatProgressSpinnerModule
      ],
      providers: [
        provideMockStore({ initialState })
      ]
    })
    .compileComponents();
    
    store = TestBed.inject(MockStore);
    spyOn(store, 'dispatch');
  });
  
  beforeEach(() => {
    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });
  
  it('should create', () => {
    expect(component).toBeTruthy();
  });
  
  it('should initialize form with empty values', () => {
    expect(component.loginForm.get('email')?.value).toBe('');
    expect(component.loginForm.get('password')?.value).toBe('');
  });
  
  it('should dispatch clearAuthError on init', () => {
    expect(store.dispatch).toHaveBeenCalledWith(clearAuthError());
  });
  
  describe('Form Validation', () => {
    it('should invalidate form when email is empty', () => {
      component.loginForm.patchValue({
        email: '',
        password: 'password123'
      });
      
      expect(component.loginForm.valid).toBe(false);
      expect(component.loginForm.get('email')?.hasError('required')).toBe(true);
    });
    
    it('should invalidate form when email is invalid', () => {
      component.loginForm.patchValue({
        email: 'invalid-email',
        password: 'password123'
      });
      
      expect(component.loginForm.valid).toBe(false);
      expect(component.loginForm.get('email')?.hasError('email')).toBe(true);
    });
    
    it('should invalidate form when password is empty', () => {
      component.loginForm.patchValue({
        email: 'test@example.com',
        password: ''
      });
      
      expect(component.loginForm.valid).toBe(false);
      expect(component.loginForm.get('password')?.hasError('required')).toBe(true);
    });
    
    it('should invalidate form when password is too short', () => {
      component.loginForm.patchValue({
        email: 'test@example.com',
        password: 'short'
      });
      
      expect(component.loginForm.valid).toBe(false);
      expect(component.loginForm.get('password')?.hasError('minlength')).toBe(true);
    });
    
    it('should validate form when all fields are correct', () => {
      component.loginForm.patchValue({
        email: 'test@example.com',
        password: 'password123'
      });
      
      expect(component.loginForm.valid).toBe(true);
    });
  });
  
  describe('onSubmit', () => {
    it('should dispatch login action when form is valid', () => {
      component.loginForm.patchValue({
        email: 'test@example.com',
        password: 'password123'
      });
      
      component.onSubmit();
      
      expect(store.dispatch).toHaveBeenCalledWith(
        login({ email: 'test@example.com', password: 'password123' })
      );
    });
    
    it('should not dispatch login action when form is invalid', () => {
      component.loginForm.patchValue({
        email: 'invalid-email',
        password: 'short'
      });
      
      const dispatchCallCount = (store.dispatch as jasmine.Spy).calls.count();
      component.onSubmit();
      
      // Should only have the initial clearAuthError call
      expect((store.dispatch as jasmine.Spy).calls.count()).toBe(dispatchCallCount);
    });
  });
  
  describe('getErrorMessage', () => {
    it('should return required error message', () => {
      const control = component.loginForm.get('email');
      control?.markAsTouched();
      control?.setValue('');
      
      expect(component.getErrorMessage('email')).toBe('Email is required');
    });
    
    it('should return email validation error message', () => {
      const control = component.loginForm.get('email');
      control?.markAsTouched();
      control?.setValue('invalid-email');
      
      expect(component.getErrorMessage('email')).toBe('Please enter a valid email address');
    });
    
    it('should return minlength error message', () => {
      const control = component.loginForm.get('password');
      control?.markAsTouched();
      control?.setValue('short');
      
      expect(component.getErrorMessage('password')).toContain('at least 8 characters');
    });
  });
});
