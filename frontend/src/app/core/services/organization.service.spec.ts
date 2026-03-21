import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { OrganizationService, Organization, OrganizationRole } from './organization.service';
import { environment } from '../../../environments/environment';

describe('OrganizationService', () => {
  let service: OrganizationService;
  let httpMock: HttpTestingController;

  const mockOrg: Organization = {
    id: '1',
    name: 'Test Org',
    description: 'Test Description',
    ownerId: 'user1',
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [OrganizationService]
    });
    service = TestBed.inject(OrganizationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should get all organizations', () => {
    const mockOrgs = [mockOrg];
    
    service.getOrganizations().subscribe(orgs => {
      expect(orgs).toEqual(mockOrgs);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/organizations`);
    expect(req.request.method).toBe('GET');
    req.flush(mockOrgs);
  });

  it('should get organization by id', () => {
    service.getOrganization('1').subscribe(org => {
      expect(org).toEqual(mockOrg);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/organizations/1`);
    expect(req.request.method).toBe('GET');
    req.flush(mockOrg);
  });

  it('should create organization', () => {
    const createData = { name: 'New Org', description: 'New Description' };
    
    service.createOrganization(createData).subscribe(org => {
      expect(org.name).toBe('New Org');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/organizations`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(createData);
    req.flush({ ...mockOrg, ...createData });
  });

  it('should update organization', () => {
    const updateData = { name: 'Updated Org' };
    
    service.updateOrganization('1', updateData).subscribe(org => {
      expect(org.name).toBe('Updated Org');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/organizations/1`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual(updateData);
    req.flush({ ...mockOrg, ...updateData });
  });

  it('should delete organization', () => {
    service.deleteOrganization('1').subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/organizations/1`);
    expect(req.request.method).toBe('DELETE');
    req.flush({});
  });

  it('should get organization members', () => {
    const mockMembers = [
      {
        id: 'm1',
        organizationId: '1',
        userId: 'u1',
        role: OrganizationRole.ADMIN,
        joinedAt: '2024-01-01T00:00:00Z'
      }
    ];
    
    service.getMembers('1').subscribe(members => {
      expect(members).toEqual(mockMembers);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/organizations/1/members`);
    expect(req.request.method).toBe('GET');
    req.flush(mockMembers);
  });

  it('should invite member', () => {
    const inviteData = {
      email: 'newuser@example.com',
      role: OrganizationRole.MEMBER
    };
    
    service.inviteMember('1', inviteData).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/organizations/1/members`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(inviteData);
    req.flush({});
  });

  it('should update member role', () => {
    service.updateMemberRole('1', 'm1', OrganizationRole.ADMIN).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/organizations/1/members/m1`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ role: OrganizationRole.ADMIN });
    req.flush({});
  });

  it('should remove member', () => {
    service.removeMember('1', 'm1').subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/organizations/1/members/m1`);
    expect(req.request.method).toBe('DELETE');
    req.flush({});
  });
});
