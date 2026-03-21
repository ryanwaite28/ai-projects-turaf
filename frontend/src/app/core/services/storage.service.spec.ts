import { TestBed } from '@angular/core/testing';
import { StorageService } from './storage.service';

describe('StorageService', () => {
  let service: StorageService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [StorageService]
    });
    service = TestBed.inject(StorageService);
    localStorage.clear();
    sessionStorage.clear();
  });

  afterEach(() => {
    localStorage.clear();
    sessionStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('localStorage', () => {
    it('should set and get item', () => {
      const testData = { name: 'test', value: 123 };
      service.setLocal('testKey', testData);
      const result = service.getLocal('testKey');
      expect(result).toEqual(testData);
    });

    it('should return null for non-existent key', () => {
      const result = service.getLocal('nonExistent');
      expect(result).toBeNull();
    });

    it('should remove item', () => {
      service.setLocal('testKey', 'value');
      service.removeLocal('testKey');
      const result = service.getLocal('testKey');
      expect(result).toBeNull();
    });

    it('should clear all items', () => {
      service.setLocal('key1', 'value1');
      service.setLocal('key2', 'value2');
      service.clearLocal();
      expect(service.getLocal('key1')).toBeNull();
      expect(service.getLocal('key2')).toBeNull();
    });

    it('should handle complex objects', () => {
      const complexData = {
        nested: { value: 'test' },
        array: [1, 2, 3],
        boolean: true
      };
      service.setLocal('complex', complexData);
      const result = service.getLocal('complex');
      expect(result).toEqual(complexData);
    });

    it('should check localStorage availability', () => {
      expect(service.isLocalStorageAvailable()).toBe(true);
    });
  });

  describe('sessionStorage', () => {
    it('should set and get item', () => {
      const testData = { name: 'test', value: 456 };
      service.setSession('testKey', testData);
      const result = service.getSession('testKey');
      expect(result).toEqual(testData);
    });

    it('should return null for non-existent key', () => {
      const result = service.getSession('nonExistent');
      expect(result).toBeNull();
    });

    it('should remove item', () => {
      service.setSession('testKey', 'value');
      service.removeSession('testKey');
      const result = service.getSession('testKey');
      expect(result).toBeNull();
    });

    it('should clear all items', () => {
      service.setSession('key1', 'value1');
      service.setSession('key2', 'value2');
      service.clearSession();
      expect(service.getSession('key1')).toBeNull();
      expect(service.getSession('key2')).toBeNull();
    });

    it('should check sessionStorage availability', () => {
      expect(service.isSessionStorageAvailable()).toBe(true);
    });
  });
});
