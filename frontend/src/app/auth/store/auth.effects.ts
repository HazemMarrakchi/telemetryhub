import { Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { of } from 'rxjs';
import { catchError, exhaustMap, map } from 'rxjs/operators';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';

import { environment } from '../../../environments/environment';
import { loginFailure, loginRequest, loginSuccess, logout } from './auth.actions';
import { LoginResponse } from '../../core/models';

@Injectable()
export class AuthEffects {
  constructor(
    private actions$: Actions,
    private http: HttpClient,
    private router: Router,
  ) {}

  login$ = createEffect(() =>
    this.actions$.pipe(
      ofType(loginRequest),
      exhaustMap(({ email, password }) =>
        this.http
          .post<LoginResponse>(`${environment.apiUrl}/v1/auth/login`, { email, password })
          .pipe(
            map((response) => {
              localStorage.setItem('th_access_token', response.accessToken);
              localStorage.setItem('th_refresh_token', response.refreshToken);
              this.router.navigate(['/dashboard']);
              return loginSuccess({
                accessToken: response.accessToken,
                refreshToken: response.refreshToken,
                user: response.user,
              });
            }),
            catchError((error) => of(loginFailure({ error: errorMessage(error) }))),
          ),
      ),
    ),
  );

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