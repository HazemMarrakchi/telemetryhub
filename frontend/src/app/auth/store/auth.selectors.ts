import { createFeatureSelector, createSelector } from '@ngrx/store';
import { AUTH_FEATURE_KEY, AuthState } from './auth.reducer';

export const selectAuthState = createFeatureSelector<AuthState>(AUTH_FEATURE_KEY);

export const selectUser = createSelector(selectAuthState, (state) => state.user);
export const selectAccessToken = createSelector(selectAuthState, (state) => state.accessToken);
export const selectAuthLoading = createSelector(selectAuthState, (state) => state.loading);
export const selectAuthError = createSelector(selectAuthState, (state) => state.error);