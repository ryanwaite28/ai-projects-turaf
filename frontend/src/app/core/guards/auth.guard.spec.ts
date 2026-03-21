import { TestBed } from '@angular/core/testing';
import { Router, ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree } from '@angular/router';
import { provideMockStore, MockStore } from '@ngrx/store/testing';
import { AuthGuard } from './auth.guard';
import { AppState } from '../../store/app.state';
import { selectIsAuthenticated } from '../../store/auth/auth.selectors';

describe('AuthGuard', () => {
  let guard: AuthGuard;
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
        AuthGuard,
        provideMockStore({ initialState }),
        { provide: Router, useValue: routerSpy }
      ]
    });
    
    guard = TestBed.inject(AuthGuard);
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
      route = {} as ActivatedRouteSnapshot;
      state = { url: '/dashboard' } as RouterStateSnapshot;
    });
    
    it('should return true when user is authenticated', (done) => {
      store.overrideSelector(selectIsAuthenticated, true);
      
      guard.canActivate(route, state).subscribe(result => {
        expect(result).toBe(true);
        done();
      });
    });
    
    it('should return UrlTree to login when user is not authenticated', (done) => {
      const urlTree = {} as UrlTree;
      router.createUrlTree.and.returnValue(urlTree);
      store.overrideSelector(selectIsAuthenticated, false);
      
      guard.canActivate(route, state).subscribe(result => {
        expect(result).toBe(urlTree);
        expect(router.createUrlTree).toHaveBeenCalledWith(['/login'], {
          queryParams: { returnUrl: '/dashboard' }
        });
        done();
      });
    });
    
    it('should include return URL in query params', (done) => {
      const urlTree = {} as UrlTree;
      const testUrl = '/protected/resource';
      state = { url: testUrl } as RouterStateSnapshot;
      
      router.createUrlTree.and.returnValue(urlTree);
      store.overrideSelector(selectIsAuthenticated, false);
      
      guard.canActivate(route, state).subscribe(result => {
        expect(router.createUrlTree).toHaveBeenCalledWith(['/login'], {
          queryParams: { returnUrl: testUrl }
        });
        done();
      });
    });
  });
});
