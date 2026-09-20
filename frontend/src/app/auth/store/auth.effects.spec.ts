import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ReplaySubject } from 'rxjs';
import { AuthEffects } from './auth.effects';
import { loginRequest, loginSuccess, logout } from './auth.actions';

describe('AuthEffects', () => {
  let effects: AuthEffects;
  let httpMock: HttpTestingController;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AuthEffects, provideRouter([]), provideHttpClient()],
    });
    effects = TestBed.inject(AuthEffects);
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    localStorage.clear();
  });

  afterEach(() => httpMock.verify());

  describe('login$', () => {
    it('resolves login locally in demo mode (no HTTP call) and dispatches loginSuccess', (done) => {
      const actions$ = new ReplaySubject<any>(1);
      effects.login$.subscribe((action) => {
        expect(action.type).toBe('[Auth] Login Success');
        const success = action as ReturnType<typeof loginSuccess>;
        expect(success.accessToken).toBeTruthy();
        expect(success.user.email).toBe('admin@acme.com');
        expect(localStorage.getItem('th_access_token')).toBeTruthy();
        done();
      });

      // No HTTP request is expected in demo mode — the effect resolves locally.
      actions$.next(loginRequest({ email: 'admin@acme.com', password: 'Demo@2026!' }));
    });
  });

  describe('logout$', () => {
    it('navigates to /login and clears storage', () => {
      spyOn(router, 'navigate');
      localStorage.setItem('th_access_token', 'tok');
      localStorage.setItem('th_refresh_token', 'rtok');
      const actions$ = new ReplaySubject<any>(1);
      effects.logout$.subscribe();
      actions$.next(logout());
      expect(localStorage.getItem('th_access_token')).toBeNull();
      expect(router.navigate).toHaveBeenCalledWith(['/login']);
    });
  });
});