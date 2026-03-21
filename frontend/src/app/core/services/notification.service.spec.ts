import { TestBed } from '@angular/core/testing';
import { NotificationService, NotificationType } from './notification.service';

describe('NotificationService', () => {
  let service: NotificationService;
  
  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(NotificationService);
  });
  
  it('should be created', () => {
    expect(service).toBeTruthy();
  });
  
  describe('success', () => {
    it('should emit success notification', (done) => {
      const message = 'Success message';
      
      service.notifications$.subscribe(notification => {
        expect(notification.type).toBe(NotificationType.SUCCESS);
        expect(notification.message).toBe(message);
        expect(notification.duration).toBe(3000);
        done();
      });
      
      service.success(message);
    });
    
    it('should use custom duration', (done) => {
      const message = 'Success message';
      const duration = 5000;
      
      service.notifications$.subscribe(notification => {
        expect(notification.duration).toBe(duration);
        done();
      });
      
      service.success(message, duration);
    });
  });
  
  describe('error', () => {
    it('should emit error notification', (done) => {
      const message = 'Error message';
      
      service.notifications$.subscribe(notification => {
        expect(notification.type).toBe(NotificationType.ERROR);
        expect(notification.message).toBe(message);
        expect(notification.duration).toBe(5000);
        done();
      });
      
      service.error(message);
    });
  });
  
  describe('warning', () => {
    it('should emit warning notification', (done) => {
      const message = 'Warning message';
      
      service.notifications$.subscribe(notification => {
        expect(notification.type).toBe(NotificationType.WARNING);
        expect(notification.message).toBe(message);
        expect(notification.duration).toBe(4000);
        done();
      });
      
      service.warning(message);
    });
  });
  
  describe('info', () => {
    it('should emit info notification', (done) => {
      const message = 'Info message';
      
      service.notifications$.subscribe(notification => {
        expect(notification.type).toBe(NotificationType.INFO);
        expect(notification.message).toBe(message);
        expect(notification.duration).toBe(3000);
        done();
      });
      
      service.info(message);
    });
  });
  
  it('should emit multiple notifications in sequence', () => {
    const notifications: any[] = [];
    
    service.notifications$.subscribe(notification => {
      notifications.push(notification);
    });
    
    service.success('Success 1');
    service.error('Error 1');
    service.warning('Warning 1');
    
    expect(notifications.length).toBe(3);
    expect(notifications[0].type).toBe(NotificationType.SUCCESS);
    expect(notifications[1].type).toBe(NotificationType.ERROR);
    expect(notifications[2].type).toBe(NotificationType.WARNING);
  });
});
