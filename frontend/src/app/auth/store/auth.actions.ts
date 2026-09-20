import { createAction, props } from '@ngrx/store';

export interface AuthUser {
  id: string;
  email: string;
  name: string;
  role: string;
}

export const loginRequest = createAction(
  '[Auth] Login Request',
  props<{ email: string; password: string }>(),
);

export const loginSuccess = createAction(
  '[Auth] Login Success',
  props<{ accessToken: string; refreshToken: string; user: AuthUser }>(),
);

export const loginFailure = createAction(
  '[Auth] Login Failure',
  props<{ error: string }>(),
);

export const logout = createAction('[Auth] Logout');

export const restoreSession = createAction(
  '[Auth] Restore Session',
  props<{ accessToken: string; user: AuthUser }>(),
);