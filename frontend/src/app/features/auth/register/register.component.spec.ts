import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideMockStore, MockStore } from '@ngrx/store/testing';
import { of, throwError } from 'rxjs';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { RegisterComponent } from './register.component';
import { AppState } from '../../../store/app.state';
import { AuthService } from '../services/auth.service';
import { clearAuthError } from '../../../store/auth/auth.actions';

describe('RegisterComponent', () => {
  let component: RegisterComponent;
  let fixture: ComponentFixture<RegisterComponent>;
  let store: MockStore<AppState>;
  let authService: jasmine.SpyObj<AuthService>;
  
  const initialState = {
    auth: {
      user: null,
      token: null,
      loading: false,
      error: null
    }
  };
  
  beforeEach(async () => {
    const authServiceSpy = jasmine.createSpyObj('AuthService', ['register']);
    
    await TestBed.configureTestingModule({
      declarations: [ RegisterComponent ],
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
        provideMockStore({ initialState }),
        { provide: AuthService, useValue: authServiceSpy }
      ]
    })
    .compileComponents();
    
    store = TestBed.inject(MockStore);
    authService = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
    spyOn(store, 'dispatch');
  });
  
  beforeEach(() => {
    fixture = TestBed.createComponent(RegisterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });
  
  it('should create', () => {
    expect(component).toBeTruthy();
  });
  
  it('should initialize form with empty values', () => {
    expect(component.registerForm.get('firstName')?.value).toBe('');
    expect(component.registerForm.get('lastName')?.value).toBe('');
    expect(component.registerForm.get('email')?.value).toBe('');
    expect(component.registerForm.get('password')?.value).toBe('');
    expect(component.registerForm.get('confirmPassword')?.value).toBe('');
  });
  
  it('should dispatch clearAuthError on init', () => {
    expect(store.dispatch).toHaveBeenCalledWith(clearAuthError());
  });
  
  describe('Form Validation', () => {
    it('should invalidate form when required fields are empty', () => {
      expect(component.registerForm.valid).toBe(false);
    });
    
    it('should invalidate form when passwords do not match', () => {
      component.registerForm.patchValue({
        firstName: 'John',
        lastName: 'Doe',
        email: 'john@example.com',
        password: 'SecurePass123!',
        confirmPassword: 'DifferentPass123!'
      });
      
      expect(component.registerForm.hasError('passwordMismatch')).toBe(true);
    });
    
    it('should invalidate form when password is weak', () => {
      component.registerForm.patchValue({
        firstName: 'John',
        lastName: 'Doe',
        email: 'john@example.com',
        password: 'weakpass',
        confirmPassword: 'weakpass'
      });
      
      expect(component.registerForm.get('password')?.hasError('passwordStrength')).toBe(true);
    });
    
    it('should validate form when all fields are correct', () => {
      component.registerForm.patchValue({
        firstName: 'John',
        lastName: 'Doe',
        email: 'john@example.com',
        password: 'SecurePass123!',
        confirmPassword: 'SecurePass123!'
      });
      
      expect(component.registerForm.valid).toBe(true);
    });
  });
  
  describe('Password Strength Validator', () => {
    it('should require uppercase letter', () => {
      component.registerForm.patchValue({
        password: 'securepass123!'
      });
      
      expect(component.registerForm.get('password')?.hasError('passwordStrength')).toBe(true);
    });
    
    it('should require lowercase letter', () => {
      component.registerForm.patchValue({
        password: 'SECUREPASS123!'
      });
      
      expect(component.registerForm.get('password')?.hasError('passwordStrength')).toBe(true);
    });
    
    it('should require number', () => {
      component.registerForm.patchValue({
        password: 'SecurePass!'
      });
      
      expect(component.registerForm.get('password')?.hasError('passwordStrength')).toBe(true);
    });
    
    it('should require special character', () => {
      component.registerForm.patchValue({
        password: 'SecurePass123'
      });
      
      expect(component.registerForm.get('password')?.hasError('passwordStrength')).toBe(true);
    });
    
    it('should accept strong password', () => {
      component.registerForm.patchValue({
        password: 'SecurePass123!',
        confirmPassword: 'SecurePass123!'
      });
      
      expect(component.registerForm.get('password')?.hasError('passwordStrength')).toBe(false);
    });
  });
  
  describe('onSubmit', () => {
    it('should call authService.register when form is valid', () => {
      const mockResponse = {
        user: {
          id: '1',
          email: 'john@example.com',
          firstName: 'John',
          lastName: 'Doe',
          role: 'MEMBER' as any,
          organizationId: 'org-1',
          createdAt: new Date(),
          updatedAt: new Date()
        },
        token: 'mock-token'
      };
      
      authService.register.and.returnValue(of(mockResponse));
      
      component.registerForm.patchValue({
        firstName: 'John',
        lastName: 'Doe',
        email: 'john@example.com',
        password: 'SecurePass123!',
        confirmPassword: 'SecurePass123!',
        organizationName: 'Test Org'
      });
      
      component.onSubmit();
      
      expect(authService.register).toHaveBeenCalledWith({
        firstName: 'John',
        lastName: 'Doe',
        email: 'john@example.com',
        password: 'SecurePass123!',
        organizationName: 'Test Org'
      });
    });
    
    it('should not call authService.register when form is invalid', () => {
      component.registerForm.patchValue({
        firstName: 'John',
        email: 'invalid-email'
      });
      
      component.onSubmit();
      
      expect(authService.register).not.toHaveBeenCalled();
    });
  });
  
  describe('getErrorMessage', () => {
    it('should return password mismatch error', () => {
      component.registerForm.patchValue({
        password: 'SecurePass123!',
        confirmPassword: 'DifferentPass123!'
      });
      component.registerForm.get('confirmPassword')?.markAsTouched();
      
      expect(component.getErrorMessage('confirmPassword')).toBe('Passwords do not match');
    });
    
    it('should return password strength error', () => {
      component.registerForm.patchValue({
        password: 'weakpass'
      });
      component.registerForm.get('password')?.markAsTouched();
      
      expect(component.getErrorMessage('password')).toContain('uppercase, lowercase, number, and special character');
    });
  });
});
