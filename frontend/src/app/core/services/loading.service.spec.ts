import { TestBed } from '@angular/core/testing';
import { LoadingService } from './loading.service';

describe('LoadingService', () => {
  let service: LoadingService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [LoadingService]
    });
    service = TestBed.inject(LoadingService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should start with loading false', (done) => {
    service.loading$.subscribe(loading => {
      expect(loading).toBe(false);
      done();
    });
  });

  it('should set loading to true', (done) => {
    service.setLoading(true);
    service.loading$.subscribe(loading => {
      expect(loading).toBe(true);
      done();
    });
  });

  it('should set loading to false', (done) => {
    service.setLoading(true);
    service.setLoading(false);
    service.loading$.subscribe(loading => {
      expect(loading).toBe(false);
      done();
    });
  });

  it('should track multiple loading states', (done) => {
    service.setLoading(true, 'key1');
    service.setLoading(true, 'key2');
    
    service.loading$.subscribe(loading => {
      expect(loading).toBe(true);
      expect(service.isLoadingKey('key1')).toBe(true);
      expect(service.isLoadingKey('key2')).toBe(true);
      done();
    });
  });

  it('should remain loading if one key is still active', (done) => {
    service.setLoading(true, 'key1');
    service.setLoading(true, 'key2');
    service.setLoading(false, 'key1');
    
    service.loading$.subscribe(loading => {
      expect(loading).toBe(true);
      expect(service.isLoadingKey('key1')).toBe(false);
      expect(service.isLoadingKey('key2')).toBe(true);
      done();
    });
  });

  it('should clear all loading states', (done) => {
    service.setLoading(true, 'key1');
    service.setLoading(true, 'key2');
    service.clearAll();
    
    service.loading$.subscribe(loading => {
      expect(loading).toBe(false);
      expect(service.isLoadingKey('key1')).toBe(false);
      expect(service.isLoadingKey('key2')).toBe(false);
      done();
    });
  });

  it('should return current loading state', () => {
    expect(service.isLoading()).toBe(false);
    service.setLoading(true);
    expect(service.isLoading()).toBe(true);
  });
});
