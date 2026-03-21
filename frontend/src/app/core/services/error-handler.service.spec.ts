import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { ErrorHandlerService } from './error-handler.service';
import { NotificationService } from './notification.service';

describe('ErrorHandlerService', () => {
  let service: ErrorHandlerService;
  let notificationService: jasmine.SpyObj<NotificationService>;

  beforeEach(() => {
    const notificationSpy = jasmine.createSpyObj('NotificationService', ['error']);

    TestBed.configureTestingModule({
      providers: [
        ErrorHandlerService,
        { provide: NotificationService, useValue: notificationSpy }
      ]
    });

    service = TestBed.inject(ErrorHandlerService);
    notificationService = TestBed.inject(NotificationService) as jasmine.SpyObj<NotificationService>;
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('handleError', () => {
    it('should handle HTTP 400 error', () => {
      const error = new HttpErrorResponse({ status: 400 });
      service.handleError(error);
      expect(notificationService.error).toHaveBeenCalledWith('Bad request. Please check your input.');
    });

    it('should handle HTTP 401 error', () => {
      const error = new HttpErrorResponse({ status: 401 });
      service.handleError(error);
      expect(notificationService.error).toHaveBeenCalledWith('Unauthorized. Please log in.');
    });

    it('should handle HTTP 403 error', () => {
      const error = new HttpErrorResponse({ status: 403 });
      service.handleError(error);
      expect(notificationService.error).toHaveBeenCalledWith('Forbidden. You do not have permission.');
    });

    it('should handle HTTP 404 error', () => {
      const error = new HttpErrorResponse({ status: 404 });
      service.handleError(error);
      expect(notificationService.error).toHaveBeenCalledWith('Resource not found.');
    });

    it('should handle HTTP 500 error', () => {
      const error = new HttpErrorResponse({ status: 500 });
      service.handleError(error);
      expect(notificationService.error).toHaveBeenCalledWith('Internal server error. Please try again later.');
    });

    it('should handle HTTP 503 error', () => {
      const error = new HttpErrorResponse({ status: 503 });
      service.handleError(error);
      expect(notificationService.error).toHaveBeenCalledWith('Service unavailable. Please try again later.');
    });

    it('should handle error message from response', () => {
      const error = new HttpErrorResponse({
        status: 400,
        error: { message: 'Custom error message' }
      });
      service.handleError(error);
      expect(notificationService.error).toHaveBeenCalledWith('Custom error message');
    });

    it('should handle offline error', () => {
      spyOnProperty(navigator, 'onLine', 'get').and.returnValue(false);
      const error = new HttpErrorResponse({ status: 0 });
      service.handleError(error);
      expect(notificationService.error).toHaveBeenCalledWith('No internet connection');
    });

    it('should handle client-side error', () => {
      const error = new Error('Client error');
      service.handleError(error);
      expect(notificationService.error).toHaveBeenCalledWith('Client error');
    });
  });
});
