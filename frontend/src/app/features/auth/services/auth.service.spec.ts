import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AuthService, RegisterRequest } from './auth.service';
import { LoginRequest, LoginResponse } from '../../../models/user.model';
import { environment } from '../../../../environments/environment';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  const apiUrl = `${environment.apiUrl}/auth`;
  
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AuthService]
    });
    
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });
  
  afterEach(() => {
    httpMock.verify();
  });
  
  it('should be created', () => {
    expect(service).toBeTruthy();
  });
  
  describe('login', () => {
    it('should send login request with credentials', () => {
      const credentials: LoginRequest = {
        email: 'test@example.com',
        password: 'password123'
      };
      
      const mockResponse: LoginResponse = {
        user: {
          id: '1',
          email: 'test@example.com',
          firstName: 'Test',
          lastName: 'User',
          role: 'MEMBER' as any,
          organizationId: 'org-1',
          createdAt: new Date(),
          updatedAt: new Date()
        },
        token: 'mock-jwt-token'
      };
      
      service.login(credentials).subscribe(response => {
        expect(response).toEqual(mockResponse);
      });
      
      const req = httpMock.expectOne(`${apiUrl}/login`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(credentials);
      
      req.flush(mockResponse);
    });
  });
  
  describe('register', () => {
    it('should send registration request with user data', () => {
      const userData: RegisterRequest = {
        email: 'newuser@example.com',
        password: 'SecurePass123!',
        firstName: 'New',
        lastName: 'User',
        organizationName: 'Test Org'
      };
      
      const mockResponse: LoginResponse = {
        user: {
          id: '2',
          email: 'newuser@example.com',
          firstName: 'New',
          lastName: 'User',
          role: 'MEMBER' as any,
          organizationId: 'org-2',
          createdAt: new Date(),
          updatedAt: new Date()
        },
        token: 'mock-jwt-token'
      };
      
      service.register(userData).subscribe(response => {
        expect(response).toEqual(mockResponse);
      });
      
      const req = httpMock.expectOne(`${apiUrl}/register`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(userData);
      
      req.flush(mockResponse);
    });
  });
  
  describe('refreshToken', () => {
    it('should send token refresh request', () => {
      const mockResponse = { token: 'new-mock-jwt-token' };
      
      service.refreshToken().subscribe(response => {
        expect(response).toEqual(mockResponse);
      });
      
      const req = httpMock.expectOne(`${apiUrl}/refresh`);
      expect(req.request.method).toBe('POST');
      
      req.flush(mockResponse);
    });
  });
  
  describe('logout', () => {
    it('should send logout request', () => {
      service.logout().subscribe();
      
      const req = httpMock.expectOne(`${apiUrl}/logout`);
      expect(req.request.method).toBe('POST');
      
      req.flush(null);
    });
  });
  
  describe('requestPasswordReset', () => {
    it('should send password reset request with email', () => {
      const email = 'user@example.com';
      
      service.requestPasswordReset(email).subscribe();
      
      const req = httpMock.expectOne(`${apiUrl}/password-reset/request`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ email });
      
      req.flush(null);
    });
  });
  
  describe('resetPassword', () => {
    it('should send password reset confirmation with token and new password', () => {
      const token = 'reset-token-123';
      const newPassword = 'NewSecurePass123!';
      
      service.resetPassword(token, newPassword).subscribe();
      
      const req = httpMock.expectOne(`${apiUrl}/password-reset/confirm`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ token, newPassword });
      
      req.flush(null);
    });
  });
});
