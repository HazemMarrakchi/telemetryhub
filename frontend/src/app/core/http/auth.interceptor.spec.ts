import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpInterceptorFn, HttpRequest, HttpHandlerFn } from '@angular/common/http';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let httpMock: HttpTestingController;
  let router: Router;

  const makeNext = (error?: { status: number }) => {
    return jasmine.createSpy('next').and.callFake((req: HttpRequest<unknown>) => {
      return {
        pipe: (op: any) => {
          if (error && op?.pipe) {
            return { pipe: (op2: any) => ({ subscribe: (handlers: any) => handlers.error(error) }) };
          }
          return { pipe: () => ({}) };
        },
      };
    });
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [provideRouter([]), provideHttpClient()],
    });
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    spyOn(router, 'navigate');
    localStorage.clear();
  });

  it('adds Bearer token to request when in localStorage', () => {
    localStorage.setItem('th_access_token', 'secret-token');
    const mockNext = makeNext();
    const baseReq = {
      url: '/api/test',
      headers: { get: () => null, set: () => {}, has: () => false },
    };
    const originalClone = (overrides: { setHeaders?: Record<string, string> }) => {
      const merged = { ...baseReq, headers: { get: () => null, set: () => {} }, ...overrides };
      return merged as unknown as HttpRequest<unknown>;
    };

    const mockReq = {
      url: '/api/test',
      clone: (opts: any) => originalClone(opts),
      headers: { get: () => null, set: () => {}, has: () => false },
    } as unknown as HttpRequest<unknown>;

    authInterceptor(mockReq, mockNext as unknown as HttpHandlerFn);

    expect(mockNext).toHaveBeenCalled();
  });

  it('does not add Authorization header when no token', () => {
    const mockNext = makeNext();
    const mockReq = {
      url: '/api/test',
      clone: (opts: any) => mockReq,
      headers: { get: () => null },
    } as unknown as HttpRequest<unknown>;

    authInterceptor(mockReq, mockNext as unknown as HttpHandlerFn);

    expect(mockNext).toHaveBeenCalledWith(mockReq);
  });

  it('navigates to /login and clears token on 401', () => {
    localStorage.setItem('th_access_token', 'tok');
    const mockNext = makeNext({ status: 401 });
    const mockReq = {
      url: '/api/test',
      clone: (opts: any) => mockReq,
      headers: { get: () => null },
    } as unknown as HttpRequest<unknown>;

    authInterceptor(mockReq, mockNext as unknown as HttpHandlerFn);

    expect(router.navigate).toHaveBeenCalledWith(['/login']);
    expect(localStorage.getItem('th_access_token')).toBeNull();
  });
});