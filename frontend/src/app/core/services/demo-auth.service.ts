import { Injectable } from '@angular/core';

import { LoginResponse } from '../models';

/**
 * Demo-mode authentication provider.
 *
 * The frontend is designed to run standalone (`environment.demo === true`) with
 * realistic mock data, so the login flow must not depend on a deployed backend.
 * This provider returns a valid `LoginResponse` synchronously, letting the app
 * work end-to-end on static hosts (GitHub Pages / Vercel) even when the API
 * gateway is unreachable.
 *
 * Credentials are not enforced here; the demo hint on the login screen is
 * `admin@acme.com` / `Demo@2026!`.
 */
@Injectable({ providedIn: 'root' })
export class DemoAuthService {
  login(): LoginResponse {
    const payload = {
      sub: 'u-demo',
      tenant: 'acme',
      role: 'ADMIN',
      email: 'admin@acme.com',
      name: 'Admin',
      iat: Math.floor(Date.now() / 1000),
      exp: Math.floor(Date.now() / 1000) + 60 * 60,
    };
    const jwt = `${btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }))}.${btoa(
      JSON.stringify(payload),
    )}.demo-signature`;

    return {
      accessToken: jwt,
      refreshToken: `${jwt}.refresh`,
      expiresIn: 3600,
      user: {
        id: 'u-demo',
        email: 'admin@acme.com',
        name: 'Admin',
        role: 'ADMIN',
      },
    };
  }
}

