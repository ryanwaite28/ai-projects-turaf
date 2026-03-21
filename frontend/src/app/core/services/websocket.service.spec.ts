import { TestBed } from '@angular/core/testing';
import { WebSocketService, WebSocketMessage } from './websocket.service';

describe('WebSocketService', () => {
  let service: WebSocketService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [WebSocketService]
    });
    service = TestBed.inject(WebSocketService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should have messages observable', () => {
    expect(service.messages$).toBeDefined();
  });

  it('should filter messages by type', (done) => {
    const testMessage: WebSocketMessage = {
      type: 'test:event',
      payload: { data: 'test' }
    };

    service.on('test:event').subscribe(message => {
      expect(message).toEqual(testMessage);
      done();
    });

    // Simulate receiving a message
    (service as any).messagesSubject$.next(testMessage);
  });

  it('should not emit messages of different types', (done) => {
    const testMessage: WebSocketMessage = {
      type: 'other:event',
      payload: { data: 'test' }
    };

    let received = false;
    service.on('test:event').subscribe(() => {
      received = true;
    });

    (service as any).messagesSubject$.next(testMessage);

    setTimeout(() => {
      expect(received).toBe(false);
      done();
    }, 100);
  });

  it('should disconnect cleanly', () => {
    service.disconnect();
    expect((service as any).socket$).toBeNull();
  });
});
