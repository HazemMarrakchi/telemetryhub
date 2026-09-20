import { Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { of } from 'rxjs';
import { catchError, exhaustMap, map } from 'rxjs/operators';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';

import { environment } from '../../../environments/environment';
import { loginFailure, loginRequest, loginSuccess, logout } from './auth.actions';
import { LoginResponse } from '../../core/models';
import { DemoAuthService } from '../../core/services/demo-auth.service';

@Injectable()
export class AuthEffects {
  constructor(
    private actions$: Actions,
    private http: HttpClient,
    private router: Router,
    private demoAuth: DemoAuthService,
  ) {}

  login$ = createEffect(() =>
    this.actions$.pipe(
      ofType(loginRequest),
      exhaustMap(({ email, password }) =>
        // Demo mode runs standalone (no backend, e.g. GitHub Pages) — resolve
        // login locally instead of calling the (unreachable) API gateway.
        environment.demo
          ? of(this.demoAuth.login()).pipe(map((response) => this.completeLogin(response)))
          : this.http
              .post<LoginResponse>(`${environment.apiUrl}/v1/auth/login`, { email, password })
              .pipe(
                map((response) => this.completeLogin(response)),
                catchError((error) => of(loginFailure({ error: errorMessage(error) }))),
              ),
      ),
    ),
  );

  private completeLogin(response: LoginResponse) {
    localStorage.setItem('th_access_token', response.accessToken);
    localStorage.setItem('th_refresh_token', response.refreshToken);
    this.router.navigate(['/dashboard']);
    return loginSuccess({
      accessToken: response.accessToken,
      refreshToken: response.refreshToken,
      user: response.user,
    });
  }

  logout$ = createEffect(
    () =>
      this.actions$.pipe(
        ofType(logout),
        map(() => {
          localStorage.removeItem('th_access_token');
          localStorage.removeItem('th_refresh_token');
          this.router.navigate(['/login']);
        }),
      ),
    { dispatch: false },
  );
}

function errorMessage(error: { status?: number; error?: { message?: string } }): string {
  if (error.status === 401) {
    return 'Identifiants invalides';
  }
  return error.error?.message ?? 'Erreur de connexion';
}