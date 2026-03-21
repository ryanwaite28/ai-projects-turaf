import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { IdentityService, UserProfile } from './identity.service';
import { environment } from '../../../environments/environment';

describe('IdentityService', () => {
  let service: IdentityService;
  let httpMock: HttpTestingController;

  const mockUser: UserProfile = {
    id: '1',
    email: 'test@example.com',
    firstName: 'John',
    lastName: 'Doe',
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [IdentityService]
    });
    service = TestBed.inject(IdentityService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should get current user', () => {
    service.getCurrentUser().subscribe(user => {
      expect(user).toEqual(mockUser);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/users/me`);
    expect(req.request.method).toBe('GET');
    req.flush(mockUser);
  });

  it('should update current user subject when getting user', () => {
    service.getCurrentUser().subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/users/me`);
    req.flush(mockUser);

    service.currentUser$.subscribe(user => {
      expect(user).toEqual(mockUser);
    });
  });

  it('should get user by id', () => {
    service.getUser('123').subscribe(user => {
      expect(user).toEqual(mockUser);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/users/123`);
    expect(req.request.method).toBe('GET');
    req.flush(mockUser);
  });

  it('should update profile', () => {
    const updateData = { firstName: 'Jane', lastName: 'Smith' };
    
    service.updateProfile(updateData).subscribe(user => {
      expect(user.firstName).toBe('Jane');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/users/me`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual(updateData);
    req.flush({ ...mockUser, ...updateData });
  });

  it('should change password', () => {
    const passwordData = {
      currentPassword: 'old123',
      newPassword: 'new456'
    };
    
    service.changePassword(passwordData).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/users/me/password`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(passwordData);
    req.flush({});
  });

  it('should upload avatar', () => {
    const file = new File([''], 'avatar.jpg');
    
    service.uploadAvatar(file).subscribe(user => {
      expect(user).toEqual(mockUser);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/users/me/avatar`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body instanceof FormData).toBe(true);
    req.flush(mockUser);
  });

  it('should delete account', () => {
    service.deleteAccount().subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/users/me`);
    expect(req.request.method).toBe('DELETE');
    req.flush({});

    expect(service.getCurrentUserValue()).toBeNull();
  });

  it('should get current user value', () => {
    expect(service.getCurrentUserValue()).toBeNull();
    
    service.getCurrentUser().subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/users/me`);
    req.flush(mockUser);

    expect(service.getCurrentUserValue()).toEqual(mockUser);
  });

  it('should clear current user', () => {
    service.getCurrentUser().subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/users/me`);
    req.flush(mockUser);

    service.clearCurrentUser();
    expect(service.getCurrentUserValue()).toBeNull();
  });
});
