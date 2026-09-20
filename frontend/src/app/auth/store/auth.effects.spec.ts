import { TestBed } from '@angular/core/testing';
import { provideMockActions } from '@ngrx/effects-testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { Observable, of, throwError } from 'rxjs';
import { AuthEffects } from './auth.effects';
import { loginRequest, loginSuccess, loginFailure, logout } from './auth.actions';

describe('AuthEffects', () => {
  let actions$: Observable<any>;
  let effects: AuthEffects;
  let httpMock: HttpTestingController;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        AuthEffects,
        provideMockActions(() => actions$),
        provideRouter([]),
        provideHttpClient(),
      ],
    });
    effects = TestBed.inject(AuthEffects);
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
  });

  afterEach(() => httpMock.verify());

  describe('login$', () => {
    it('dispatches loginSuccess on 200 response', (done) => {
      const response = {
        accessToken: 'atok',
        refreshToken: 'rtok',
        user: { id: 'u1', email: 'admin@acme.com', name: 'Admin', role: 'ADMIN' },
      };
      actions$ = of(loginRequest({ email: 'admin@acme.com', password: 'Demo@2026!' }));

      effects.login$.subscribe((action) => {
        expect(action).toEqual(loginSuccess({
          accessToken: 'atok',
          refreshToken: 'rtok',
          user: response.user,
        }));
        done();
      });

      const req = httpMock.expectOne('/api/v1/auth/login');
      req.flush(response);
    });

    it('dispatches loginFailure on 401 response', (done) => {
      actions$ = of(loginRequest({ email: 'bad@bad.com', password: 'wrong' }));
      effects.login$.subscribe((action) => {
        expect(action.type).toBe('[Auth] Login Failure');
        done();
      });
      const req = httpMock.expectOne('/api/v1/auth/login');
      req.flush({ message: 'Invalid credentials' }, { status: 401, statusText: 'Unauthorized' });
    });

    it('maps 401 error to readable message', (done) => {
      actions$ = of(loginRequest({ email: 'bad', password: 'x' }));
      effects.login$.subscribe((action) => {
        expect(action).toEqual(loginFailure({ error: 'Identifiants invalides' }));
        done();
      });
      const req = httpMock.expectOne('/api/v1/auth/login');
      req.flush({}, { status: 401, statusText: 'Unauthorized' });
    });
  });

  describe('logout$', () => {
    it('navigates to /login and clears storage', () => {
      spyOn(router, 'navigate');
      localStorage.setItem('th_access_token', 'tok');
      localStorage.setItem('th_refresh_token', 'rtok');
      actions$ = of(logout());
      effects.logout$.subscribe();
      expect(localStorage.getItem('th_access_token')).toBeNull();
      expect(router.navigate).toHaveBeenCalledWith(['/login']);
    });
  });
});