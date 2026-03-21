import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { provideMockActions } from '@ngrx/effects/testing';
import { Observable, of, throwError } from 'rxjs';
import { AuthEffects } from './auth.effects';
import * as AuthActions from './auth.actions';
import { User, UserRole } from '../../models/user.model';

describe('AuthEffects', () => {
  let actions$: Observable<any>;
  let effects: AuthEffects;
  let router: jasmine.SpyObj<Router>;
  
  const mockUser: User = {
    id: '1',
    email: 'test@example.com',
    firstName: 'Test',
    lastName: 'User',
    organizationId: 'org-1',
    role: UserRole.ADMIN,
    createdAt: '2024-03-20T12:00:00Z',
    updatedAt: '2024-03-20T12:00:00Z'
  };
  
  const mockToken = 'mock-jwt-token';
  
  beforeEach(() => {
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    
    TestBed.configureTestingModule({
      providers: [
        AuthEffects,
        provideMockActions(() => actions$),
        { provide: Router, useValue: routerSpy }
      ]
    });
    
    effects = TestBed.inject(AuthEffects);
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
  });
  
  describe('login$', () => {
    it('should return loginSuccess action on successful login', (done) => {
      const action = AuthActions.login({
        email: 'test@example.com',
        password: 'password'
      });
      
      actions$ = of(action);
      
      effects.login$.subscribe(result => {
        expect(result.type).toEqual(AuthActions.loginSuccess.type);
        expect(localStorage.getItem('auth_token')).toBeTruthy();
        expect(localStorage.getItem('user')).toBeTruthy();
        done();
      });
    });
  });
  
  describe('loginSuccess$', () => {
    it('should navigate to dashboard on login success', (done) => {
      const action = AuthActions.loginSuccess({
        user: mockUser,
        token: mockToken
      });
      
      actions$ = of(action);
      
      effects.loginSuccess$.subscribe(() => {
        expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
        done();
      });
    });
  });
  
  describe('logout$', () => {
    it('should clear localStorage and return logoutSuccess', (done) => {
      localStorage.setItem('auth_token', mockToken);
      localStorage.setItem('user', JSON.stringify(mockUser));
      
      const action = AuthActions.logout();
      actions$ = of(action);
      
      effects.logout$.subscribe(result => {
        expect(result.type).toEqual(AuthActions.logoutSuccess.type);
        expect(localStorage.getItem('auth_token')).toBeNull();
        expect(localStorage.getItem('user')).toBeNull();
        done();
      });
    });
  });
  
  describe('logoutSuccess$', () => {
    it('should navigate to login on logout success', (done) => {
      const action = AuthActions.logoutSuccess();
      actions$ = of(action);
      
      effects.logoutSuccess$.subscribe(() => {
        expect(router.navigate).toHaveBeenCalledWith(['/login']);
        done();
      });
    });
  });
  
  describe('loadUserFromStorage$', () => {
    it('should return loadUserFromStorageSuccess when data exists', (done) => {
      localStorage.setItem('auth_token', mockToken);
      localStorage.setItem('user', JSON.stringify(mockUser));
      
      const action = AuthActions.loadUserFromStorage();
      actions$ = of(action);
      
      effects.loadUserFromStorage$.subscribe(result => {
        expect(result.type).toEqual(AuthActions.loadUserFromStorageSuccess.type);
        done();
      });
    });
    
    it('should return loadUserFromStorageFailure when no data exists', (done) => {
      localStorage.removeItem('auth_token');
      localStorage.removeItem('user');
      
      const action = AuthActions.loadUserFromStorage();
      actions$ = of(action);
      
      effects.loadUserFromStorage$.subscribe(result => {
        expect(result.type).toEqual(AuthActions.loadUserFromStorageFailure.type);
        done();
      });
    });
    
    it('should return loadUserFromStorageFailure on invalid JSON', (done) => {
      localStorage.setItem('auth_token', mockToken);
      localStorage.setItem('user', 'invalid-json');
      
      const action = AuthActions.loadUserFromStorage();
      actions$ = of(action);
      
      effects.loadUserFromStorage$.subscribe(result => {
        expect(result.type).toEqual(AuthActions.loadUserFromStorageFailure.type);
        done();
      });
    });
  });
  
  describe('refreshToken$', () => {
    it('should return refreshTokenSuccess with new token', (done) => {
      const action = AuthActions.refreshToken();
      actions$ = of(action);
      
      effects.refreshToken$.subscribe(result => {
        expect(result.type).toEqual(AuthActions.refreshTokenSuccess.type);
        expect(localStorage.getItem('auth_token')).toBeTruthy();
        done();
      });
    });
  });
  
  afterEach(() => {
    localStorage.clear();
  });
});
