import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { HTTP_INTERCEPTORS, HttpClient } from '@angular/common/http';
import { provideMockStore, MockStore } from '@ngrx/store/testing';
import { AuthInterceptor } from './auth.interceptor';
import { AppState } from '../../store/app.state';
import { selectToken } from '../../store/auth/auth.selectors';

describe('AuthInterceptor', () => {
  let httpMock: HttpTestingController;
  let httpClient: HttpClient;
  let store: MockStore<AppState>;
  
  const initialState = {
    auth: {
      user: null,
      token: null,
      loading: false,
      error: null
    }
  };
  
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        provideMockStore({ initialState }),
        {
          provide: HTTP_INTERCEPTORS,
          useClass: AuthInterceptor,
          multi: true
        }
      ]
    });
    
    httpMock = TestBed.inject(HttpTestingController);
    httpClient = TestBed.inject(HttpClient);
    store = TestBed.inject(MockStore);
  });
  
  afterEach(() => {
    httpMock.verify();
  });
  
  it('should add Authorization header when token exists', () => {
    const token = 'test-jwt-token';
    store.overrideSelector(selectToken, token);
    
    httpClient.get('/api/data').subscribe();
    
    const req = httpMock.expectOne('/api/data');
    expect(req.request.headers.has('Authorization')).toBe(true);
    expect(req.request.headers.get('Authorization')).toBe(`Bearer ${token}`);
    
    req.flush({});
  });
  
  it('should not add Authorization header when token is null', () => {
    store.overrideSelector(selectToken, null);
    
    httpClient.get('/api/data').subscribe();
    
    const req = httpMock.expectOne('/api/data');
    expect(req.request.headers.has('Authorization')).toBe(false);
    
    req.flush({});
  });
  
  it('should not add Authorization header for public URLs', () => {
    const token = 'test-jwt-token';
    store.overrideSelector(selectToken, token);
    
    httpClient.post('/api/auth/login', {}).subscribe();
    
    const req = httpMock.expectOne('/api/auth/login');
    expect(req.request.headers.has('Authorization')).toBe(false);
    
    req.flush({});
  });
  
  it('should not add Authorization header for register endpoint', () => {
    const token = 'test-jwt-token';
    store.overrideSelector(selectToken, token);
    
    httpClient.post('/api/auth/register', {}).subscribe();
    
    const req = httpMock.expectOne('/api/auth/register');
    expect(req.request.headers.has('Authorization')).toBe(false);
    
    req.flush({});
  });
  
  it('should not add Authorization header for refresh endpoint', () => {
    const token = 'test-jwt-token';
    store.overrideSelector(selectToken, token);
    
    httpClient.post('/api/auth/refresh', {}).subscribe();
    
    const req = httpMock.expectOne('/api/auth/refresh');
    expect(req.request.headers.has('Authorization')).toBe(false);
    
    req.flush({});
  });
});
