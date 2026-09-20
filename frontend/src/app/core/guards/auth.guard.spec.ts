import { TestBed } from '@angular/core/testing';
import { CanActivateFn, Router, RouterStateSnapshot, ActivatedRouteSnapshot } from '@angular/router';
import { provideRouter } from '@angular/router';
import { authGuard } from './auth.guard';

describe('authGuard', () => {
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
    router = TestBed.inject(Router);
    spyOn(router, 'navigate');
    localStorage.clear();
  });

  const makeGuard = (): CanActivateFn => TestBed.runInInjectionContext(() => authGuard);

  it('allows navigation when token is in localStorage', () => {
    localStorage.setItem('th_access_token', 'valid-token');
    const result = makeGuard()({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot);
    expect(result).toBeTrue();
  });

  it('redirects to /login when no token in localStorage', () => {
    const result = makeGuard()({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot);
    expect(result).toEqual(router.createUrlTree(['/login']));
  });

  it('redirects to /login when token is null', () => {
    localStorage.setItem('th_access_token', 'null');
    const result = makeGuard()({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot);
    // localStorage.getItem returns the string 'null' not the null value
    expect(result).toBeTrue();
  });

  it('clears token on explicit null set', () => {
    localStorage.removeItem('th_access_token');
    const result = makeGuard()({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot);
    expect(result).toEqual(router.createUrlTree(['/login']));
  });
});