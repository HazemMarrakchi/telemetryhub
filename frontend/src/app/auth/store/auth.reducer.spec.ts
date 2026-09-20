import { authReducer, initialAuthState } from './auth.reducer';
import { loginFailure, loginRequest, loginSuccess, logout } from './auth.actions';

describe('authReducer', () => {
  it('marks the store as loading on login request', () => {
    const state = authReducer(
      initialAuthState,
      loginRequest({ email: 'a@b.c', password: 'x' }),
    );
    expect(state.loading).toBeTrue();
  });

  it('stores user and token on success', () => {
    const user = { id: 'u1', email: 'a@b.c', name: 'Ali', role: 'ADMIN' };
    const state = authReducer(
      initialAuthState,
      loginSuccess({ accessToken: 'token', refreshToken: 'refresh', user }),
    );
    expect(state.accessToken).toBe('token');
    expect(state.user).toEqual(user);
    expect(state.loading).toBeFalse();
  });

  it('stores the error message on failure', () => {
    const state = authReducer(initialAuthState, loginFailure({ error: 'Identifiants invalides' }));
    expect(state.error).toBe('Identifiants invalides');
  });

  it('clears the session on logout', () => {
    const loggedIn = authReducer(
      initialAuthState,
      loginSuccess({
        accessToken: 'token',
        refreshToken: 'refresh',
        user: { id: 'u1', email: 'a@b.c', name: 'Ali', role: 'ADMIN' },
      }),
    );
    const state = authReducer(loggedIn, logout());
    expect(state.accessToken).toBeNull();
    expect(state.user).toBeNull();
  });
});