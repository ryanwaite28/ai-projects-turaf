import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { HTTP_INTERCEPTORS, HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { provideMockStore, MockStore } from '@ngrx/store/testing';
import { ErrorInterceptor } from './error.interceptor';
import { AppState } from '../../store/app.state';
import { logout } from '../../store/auth/auth.actions';

describe('ErrorInterceptor', () => {
  let httpMock: HttpTestingController;
  let httpClient: HttpClient;
  let store: MockStore<AppState>;
  let router: jasmine.SpyObj<Router>;
  
  const initialState = {
    auth: {
      user: null,
      token: null,
      loading: false,
      error: null
    }
  };
  
  beforeEach(() => {
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        provideMockStore({ initialState }),
        { provide: Router, useValue: routerSpy },
        {
          provide: HTTP_INTERCEPTORS,
          useClass: ErrorInterceptor,
          multi: true
        }
      ]
    });
    
    httpMock = TestBed.inject(HttpTestingController);
    httpClient = TestBed.inject(HttpClient);
    store = TestBed.inject(MockStore);
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
    
    spyOn(store, 'dispatch');
  });
  
  afterEach(() => {
    httpMock.verify();
  });
  
  it('should handle 401 Unauthorized by dispatching logout', () => {
    httpClient.get('/api/data').subscribe({
      next: () => fail('should have failed'),
      error: (error) => {
        expect(error.status).toBe(401);
        expect(store.dispatch).toHaveBeenCalledWith(logout());
      }
    });
    
    const req = httpMock.expectOne('/api/data');
    req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });
  });
  
  it('should handle 403 Forbidden by redirecting to dashboard', () => {
    httpClient.get('/api/admin').subscribe({
      next: () => fail('should have failed'),
      error: (error) => {
        expect(error.status).toBe(403);
        expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
      }
    });
    
    const req = httpMock.expectOne('/api/admin');
    req.flush('Forbidden', { status: 403, statusText: 'Forbidden' });
  });
  
  it('should handle 404 Not Found', () => {
    httpClient.get('/api/notfound').subscribe({
      next: () => fail('should have failed'),
      error: (error) => {
        expect(error.status).toBe(404);
        expect(error.message).toContain('not found');
      }
    });
    
    const req = httpMock.expectOne('/api/notfound');
    req.flush('Not Found', { status: 404, statusText: 'Not Found' });
  });
  
  it('should handle 500 Internal Server Error', () => {
    httpClient.get('/api/data').subscribe({
      next: () => fail('should have failed'),
      error: (error) => {
        expect(error.status).toBe(500);
        expect(error.message).toContain('Internal server error');
      }
    });
    
    const req = httpMock.expectOne('/api/data');
    req.flush('Server Error', { status: 500, statusText: 'Internal Server Error' });
  });
  
  it('should extract error message from response body', () => {
    const errorMessage = 'Custom error message';
    
    httpClient.get('/api/data').subscribe({
      next: () => fail('should have failed'),
      error: (error) => {
        expect(error.message).toBe(errorMessage);
      }
    });
    
    const req = httpMock.expectOne('/api/data');
    req.flush({ message: errorMessage }, { status: 400, statusText: 'Bad Request' });
  });
  
  it('should provide default message for unknown errors', () => {
    httpClient.get('/api/data').subscribe({
      next: () => fail('should have failed'),
      error: (error) => {
        expect(error.message).toContain('Server error');
      }
    });
    
    const req = httpMock.expectOne('/api/data');
    req.flush('Error', { status: 418, statusText: "I'm a teapot" });
  });
});
