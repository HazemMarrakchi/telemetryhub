import { createReducer, on } from '@ngrx/store';
import {
  AuthUser,
  loginFailure,
  loginRequest,
  loginSuccess,
  logout,
  restoreSession,
} from './auth.actions';

export const AUTH_FEATURE_KEY = 'auth';

export interface AuthState {
  user: AuthUser | null;
  accessToken: string | null;
  loading: boolean;
  error: string | null;
}

export const initialAuthState: AuthState = {
  user: null,
  accessToken: null,
  loading: false,
  error: null,
};

export const authReducer = createReducer(
  initialAuthState,
  on(loginRequest, (state) => ({ ...state, loading: true, error: null })),
  on(loginSuccess, (state, { user, accessToken }) => ({
    ...state,
    loading: false,
    user,
    accessToken,
    error: null,
  })),
  on(loginFailure, (state, { error }) => ({ ...state, loading: false, error })),
  on(restoreSession, (state, { accessToken, user }) => ({
    ...state,
    accessToken,
    user,
    error: null,
  })),
  on(logout, () => initialAuthState),
);