import { Component, DestroyRef, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { loginRequest } from './store/auth.actions';
import { selectAuthError, selectAuthLoading } from './store/auth.selectors';

@Component({
  selector: 'th-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="login-wrap">
      <div class="login-card">
        <div class="brand">
          <img src="/favicon.svg" alt="TelemetryHub" width="40" height="40" />
          <h1>TelemetryHub</h1>
        </div>
        <p class="muted">Supervision industrielle temps réel</p>
        <form [formGroup]="form" (ngSubmit)="submit()" class="mt">
          <label class="field">
            <span>Email</span>
            <input formControlName="email" type="email" autocomplete="email" />
          </label>
          <label class="field">
            <span>Mot de passe</span>
            <input formControlName="password" type="password" autocomplete="current-password" />
          </label>
          <p class="error-text" *ngIf="error()">{{ error() }}</p>
          <button type="submit" [disabled]="form.invalid || loading()" class="mt">
            {{ loading() ? 'Connexion…' : 'Se connecter' }}
          </button>
        </form>
        <p class="hint muted mt">
          Compte démo: <code>admin&#64;acme.com</code> / <code>Demo&#64;2026</code>
        </p>
      </div>
    </div>
  `,
  styles: `
    .login-wrap { display: grid; place-items: center; min-height: 100vh; padding: 20px; }
    .login-card {
      width: 100%; max-width: 380px; background: var(--th-surface);
      border: 1px solid var(--th-border); border-radius: 14px; padding: 28px;
    }
    .brand { display: flex; gap: 10px; align-items: center; }
    .brand h1 { font-size: 22px; margin: 0; }
    .field { display: block; margin-bottom: 12px; }
    .field span { display: block; margin-bottom: 4px; color: var(--th-muted); font-size: 12px; }
  `,
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  private store = inject(Store);
  private destroyRef = inject(DestroyRef);

  form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });

  loading = signal(false);
  error = signal<string | null>(null);

  constructor() {
    this.store
      .select(selectAuthLoading)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((loading) => this.loading.set(loading));
    this.store
      .select(selectAuthError)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((error) => this.error.set(error));
  }

  submit(): void {
    if (this.form.invalid) {
      return;
    }
    const { email, password } = this.form.getRawValue();
    this.store.dispatch(loginRequest({ email, password }));
  }
}