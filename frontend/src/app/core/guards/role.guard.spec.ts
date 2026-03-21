import { TestBed } from '@angular/core/testing';
import { Router, ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree } from '@angular/router';
import { provideMockStore, MockStore } from '@ngrx/store/testing';
import { RoleGuard } from './role.guard';
import { AppState } from '../../store/app.state';
import { selectUserRole, selectIsAuthenticated } from '../../store/auth/auth.selectors';
import { UserRole } from '../../models/user.model';

describe('RoleGuard', () => {
  let guard: RoleGuard;
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
    const routerSpy = jasmine.createSpyObj('Router', ['navigate', 'createUrlTree']);
    
    TestBed.configureTestingModule({
      providers: [
        RoleGuard,
        provideMockStore({ initialState }),
        { provide: Router, useValue: routerSpy }
      ]
    });
    
    guard = TestBed.inject(RoleGuard);
    store = TestBed.inject(MockStore);
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
  });
  
  it('should be created', () => {
    expect(guard).toBeTruthy();
  });
  
  describe('canActivate', () => {
    let route: ActivatedRouteSnapshot;
    let state: RouterStateSnapshot;
    
    beforeEach(() => {
      route = { data: {} } as ActivatedRouteSnapshot;
      state = { url: '/admin' } as RouterStateSnapshot;
    });
    
    it('should return true when user has required role', (done) => {
      route.data = { roles: [UserRole.ADMIN] };
      store.overrideSelector(selectUserRole, UserRole.ADMIN);
      
      guard.canActivate(route, state).subscribe(result => {
        expect(result).toBe(true);
        done();
      });
    });
    
    it('should return true when user role is in required roles list', (done) => {
      route.data = { roles: [UserRole.ADMIN, UserRole.MEMBER] };
      store.overrideSelector(selectUserRole, UserRole.MEMBER);
      
      guard.canActivate(route, state).subscribe(result => {
        expect(result).toBe(true);
        done();
      });
    });
    
    it('should redirect to dashboard when user does not have required role', (done) => {
      const urlTree = {} as UrlTree;
      route.data = { roles: [UserRole.ADMIN] };
      router.createUrlTree.and.returnValue(urlTree);
      store.overrideSelector(selectUserRole, UserRole.VIEWER);
      
      guard.canActivate(route, state).subscribe(result => {
        expect(result).toBe(urlTree);
        expect(router.createUrlTree).toHaveBeenCalledWith(['/dashboard']);
        done();
      });
    });
    
    it('should redirect to login when user is not authenticated', (done) => {
      const urlTree = {} as UrlTree;
      route.data = { roles: [UserRole.ADMIN] };
      router.createUrlTree.and.returnValue(urlTree);
      store.overrideSelector(selectUserRole, null);
      
      guard.canActivate(route, state).subscribe(result => {
        expect(result).toBe(urlTree);
        expect(router.createUrlTree).toHaveBeenCalledWith(['/login']);
        done();
      });
    });
    
    it('should allow access when no roles are required and user is authenticated', (done) => {
      route.data = { roles: [] };
      store.overrideSelector(selectIsAuthenticated, true);
      
      guard.canActivate(route, state).subscribe(result => {
        expect(result).toBe(true);
        done();
      });
    });
  });
});
