import { initialAuthState } from './auth.reducer';
import { selectUser, selectAccessToken, selectAuthLoading, selectAuthError } from './auth.selectors';

describe('Auth Selectors', () => {
  const user = { id: 'u1', email: 'admin@acme.com', name: 'Admin', role: 'TENANT_ADMIN' };

  it('selectUser returns null when no user is logged in', () => {
    expect(selectUser({ auth: initialAuthState })).toBeNull();
  });

  it('selectUser returns the user when logged in', () => {
    const state = { auth: { ...initialAuthState, user } };
    expect(selectUser(state)).toEqual(user);
  });

  it('selectAccessToken returns null when no token', () => {
    expect(selectAccessToken({ auth: initialAuthState })).toBeNull();
  });

  it('selectAccessToken returns the token', () => {
    const state = { auth: { ...initialAuthState, accessToken: 'tok123' } };
    expect(selectAccessToken(state)).toBe('tok123');
  });

  it('selectAuthLoading returns loading state', () => {
    const state = { auth: { ...initialAuthState, loading: true } };
    expect(selectAuthLoading(state)).toBeTrue();
  });

  it('selectAuthError returns the error message', () => {
    const state = { auth: { ...initialAuthState, error: 'Identifiants invalides' } };
    expect(selectAuthError(state)).toBe('Identifiants invalides');
  });
});